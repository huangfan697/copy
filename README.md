# 错题集小程序

上传错题图片 → AI 自动解析为错题笔记 → 生成练习题继续刷 → 每日错题率统计

## 功能

- **上传错题**：拍照/相册选择 → 上传到 OSS → 通义千问 VL 自动解析科目/题目/知识点/解题思路
- **错题管理**：按科目/状态筛选，查看详情，标记已掌握
- **生成练习**：从错题自动生成 3-5 道同知识点变式题，逐题答题+即时批改
- **今日训练**：每日自动聚合昨日答错题 + 新导入未答题，一键开始复习
- **错题率统计**：近 7 天/30 天错题率折线图，连续打卡天数

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | 微信小程序（原生） |
| 后端 | Spring Boot 3.2 + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 存储 | 阿里云 OSS |
| AI | 通义千问 DashScope（qwen-vl-plus） |

## 项目结构

```
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/wrongnote/
│       ├── WrongNoteApplication.java
│       ├── config/                   # 配置类（OSS/通义千问/CORS/MyBatis）
│       ├── controller/               # REST API（笔记/练习/统计）
│       ├── dto/                      # 请求/响应对象
│       ├── entity/                   # 数据库实体
│       ├── mapper/                   # MyBatis-Plus Mapper
│       └── service/                  # 业务逻辑
├── miniprogram/                      # 微信小程序前端
│   ├── pages/
│   │   ├── index/                    # 首页：统计卡片 + 今日训练 + 最近错题
│   │   ├── upload/                   # 上传页：拍照/选图 → AI 解析
│   │   ├── detail/                   # 详情页：错题解析 + 生成练习
│   │   ├── practice/                 # 练习页：逐题答题 + 批改
│   │   └── stats/                    # 统计页：错题率折线图
│   └── utils/api.js                  # API 封装
├── sql/init.sql                      # 数据库建表脚本
├── debug.html                        # H5 API 调试工具（浏览器打开）
└── practice-quiz.html                # H5 刷题工具（从后端拉数据）
```

## 快速开始

详见 [DEPLOY.md](./DEPLOY.md)

## 架构设计

详见 [ARCHITECTURE.md](./ARCHITECTURE.md)

## 数据库

```
wrong_note           — 错题笔记（图片URL、科目、知识点、AI解析结果）
practice_question    — 练习题（题目、选项、答案、答题结果）
practice_record      — 每日统计（日期、总题数、正确数、错题率）
```

## API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/notes/upload | 上传错题图片并解析 |
| GET  | /api/notes | 错题列表 |
| GET  | /api/notes/{id} | 错题详情 |
| PUT  | /api/notes/{id}/status | 标记掌握状态 |
| POST | /api/practice/generate/{noteId} | 生成练习题 |
| GET  | /api/practice/list/{noteId} | 获取练习题列表 |
| GET  | /api/practice/today | 今日训练信息 |
| GET  | /api/practice/today/questions | 今日训练题目列表 |
| POST | /api/practice/submit | 提交答案 |
| GET  | /api/stats/error-rate | 错题率趋势 |
