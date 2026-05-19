# 错题集小程序

微信小程序上传错题图片，AI 自动识别红线标记的错题并解析为错题笔记，支持生成变式练习、每日复习、错题率统计。

## 功能

- **上传错题**：拍照/相册 → AI 自动识别红线标记的错题 → 解析科目/题目/知识点/正确答案/解题思路
- **异步解析**：上传立即返回，AI 解析后台异步执行，不阻塞用户操作
- **每日栏目**：每天自动创建栏目，当日上传的错题自动归类
- **错题管理**：按科目/状态筛选，查看详情，标记已掌握
- **生成练习**：从错题自动生成同知识点变式题，逐题答题+即时批改
- **错题率统计**：近 7 天/30 天错题率折线图

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | 微信小程序（原生） |
| 后端 | Spring Boot 3.2 + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 存储 | 阿里云 OSS（可选，未配置时使用 base64 存储） |
| AI | 通义千问 qwen3.5-plus（自定义代理端点） |
| 认证 | 微信小程序 login code → openid |

## 项目结构

```
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/wrongnote/
│       ├── WrongNoteApplication.java
│       ├── config/                   # 配置类（OSS/AI/CORS/MyBatis）
│       ├── controller/               # REST API
│       │   ├── AuthController.java   # 登录
│       │   ├── NoteController.java   # 错题管理
│       │   ├── PracticeController.java # 练习题
│       │   ├── StatsController.java  # 统计
│       │   └── CollectionController.java # 每日栏目
│       ├── dto/                      # 请求/响应对象
│       ├── entity/                   # 数据库实体
│       ├── mapper/                   # MyBatis-Plus Mapper
│       └── service/                  # 业务逻辑
│           ├── AuthService.java      # 微信登录
│           ├── NoteService.java      # 错题上传+解析（异步）
│           ├── DashScopeService.java # AI 解析（HTTP 直连）
│           ├── OssService.java       # 图片存储（OSS/base64）
│           ├── PracticeService.java  # 练习题生成
│           └── CollectionService.java # 每日栏目
├── miniprogram/                      # 微信小程序前端
│   ├── app.js                        # 全局配置 + 微信自动登录
│   ├── pages/
│   │   ├── index/                    # 首页：统计卡片 + 今日训练 + 最近错题
│   │   ├── upload/                   # 上传页：拍照/选图 → 异步解析
│   │   ├── detail/                   # 详情页：错题解析 + 生成练习
│   │   ├── practice/                 # 练习页：逐题答题 + 批改
│   │   ├── stats/                    # 统计页：错题率折线图
│   │   └── collection/               # 栏目页：每日错题栏目
│   └── utils/api.js                  # API 封装
├── sql/
│   ├── init.sql                      # 初始建表脚本
│   ├── v2_daily_collection.sql       # 每日栏目表
│   └── v3_user.sql                   # 用户表（openid）
└── README.md
```

## 快速开始

详见 [DEPLOY.md](./DEPLOY.md)

## 架构设计

详见 [ARCHITECTURE.md](./ARCHITECTURE.md)

## 数据库

```
user                 — 用户表（微信 openid 映射）
wrong_note           — 错题笔记（图片、科目、知识点、AI 解析结果、栏目关联）
daily_collection     — 每日栏目（日期、错题数量）
practice_question    — 练习题（题目、选项、答案、答题结果）
practice_record      — 每日统计（日期、总题数、正确数、错题率）
```

## 状态码

| 状态 | 含义 |
|------|------|
| -2   | 解析失败 |
| -1   | 解析中（异步处理） |
| 0    | 待复习 |
| 1    | 已掌握 |

## API 列表

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 小程序登录（code → openid → userId） |

### 错题管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/notes/upload | 上传错题图片（立即返回，AI 异步解析） |
| GET  | /api/notes | 错题列表（按科目/状态筛选） |
| GET  | /api/notes/{id} | 错题详情 |
| PUT  | /api/notes/{id}/status | 标记掌握状态 |

### 每日栏目

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | /api/collections | 用户栏目列表 |
| GET  | /api/collections/{id} | 栏目详情+错题 |

### 练习

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/practice/generate/{noteId} | 生成练习题 |
| GET  | /api/practice/list/{noteId} | 获取练习题列表 |
| GET  | /api/practice/today | 今日训练信息 |
| GET  | /api/practice/today/questions | 今日训练题目列表 |
| POST | /api/practice/submit | 提交答案 |

### 统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | /api/stats/error-rate | 错题率趋势 |

## 上传异步流程

```
客户端 POST /api/notes/upload
  → 保存图片到 OSS/base64
  → 创建 wrong_note 记录，status = -1（解析中）
  → 立即返回 note 信息
  → 后台异步线程 @Async 执行 AI 解析
    → 识别成功 → status = 0，填充 subject/rawContent/analysis/correctAnswer
    → 未识别到 → status = 0，提示"未识别到错题"
    → AI 出错 → status = -2，记录错误信息
客户端轮询或刷新获取最新状态
```

## 配置项

### 后端（application.yml）

```yaml
dashscope:
  base-url: http://newapi.raycloud.cn  # 自定义 AI 代理端点
  model: qwen3.5-plus                   # 使用的模型
  api-key: ${DASHSCOPE_API_KEY}        # API Key

wechat:
  app-id: ${WECHAT_APP_ID}             # 小程序 AppID
  app-secret: ${WECHAT_APP_SECRET}     # 小程序 AppSecret

oss:                                   # 可选，不配置则使用 base64 存储
  endpoint: ${OSS_ENDPOINT}
  access-key-id: ${OSS_ACCESS_KEY_ID}
  access-key-secret: ${OSS_ACCESS_KEY_SECRET}
  bucket-name: ${OSS_BUCKET_NAME}
```

### 前端（app.js）

无需额外配置，`wx.login()` code 自动发送到后端换取 userId。
