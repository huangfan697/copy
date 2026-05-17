# 架构设计

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        微信小程序                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐ │
│  │  首页     │  │  上传页   │  │  练习页   │  │  统计页       │ │
│  │ 统计+训练 │  │ 拍照/选图 │  │ 答题+批改│  │ 错题率折线图  │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘ │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP/HTTPS
┌──────────────────────────▼──────────────────────────────────┐
│                   Spring Boot 后端 (8080)                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Controller 层                            │   │
│  │  NoteController │ PracticeController │ StatsController│  │
│  └──────────────────┬──────────────────┬────────────────┘   │
│                     │                  │                     │
│  ┌──────────────────▼──────────────────▼────────────────┐   │
│  │                  Service 层                           │   │
│  │  NoteService │ PracticeService │ StatsService         │   │
│  │  DashScopeService │ OssService                        │   │
│  └──────┬───────────┬──────────────┬────────────────────┘   │
│         │           │              │                         │
│  ┌──────▼──────┐ ┌──▼──────────┐ ┌▼────────────────────┐   │
│  │  DashScope  │ │ 阿里云 OSS   │ │  MyBatis-Plus Mapper│   │
│  │  (AI 解析)  │ │  (图片存储)  │ │  (数据库访问)        │   │
│  └─────────────┘ └─────────────┘ └──────┬──────────────┘   │
└─────────────────────────────────────────┼───────────────────┘
                                          │
                           ┌──────────────▼──────────────┐
                           │         MySQL 8.0            │
                           │  wrong_note                  │
                           │  practice_question           │
                           │  practice_record             │
                           └─────────────────────────────┘
```

## 模块结构

```
com.wrongnote
├── config/                      # 配置层
│   ├── CorsConfig               # 跨域配置
│   ├── DashScopeConfig          # 通义千问 API 配置
│   ├── OssConfig                # 阿里云 OSS 配置
│   └── MyBatisPlusMetaHandler   # 自动填充 createdAt/updatedAt
├── controller/                  # 控制层（REST API）
│   ├── NoteController           # 错题笔记 CRUD
│   ├── PracticeController       # 练习题 + 今日训练
│   └── StatsController          # 错题率统计
├── dto/                         # 数据传输对象
├── entity/                      # 数据库实体
├── mapper/                      # MyBatis-Plus 接口
└── service/                     # 业务逻辑层
    ├── NoteService              # 笔记上传/查询/状态更新
    ├── PracticeService          # 出题/批改/每日聚合
    ├── StatsService             # 错题率趋势查询
    ├── DashScopeService         # 通义千问 VL 调用
    └── OssService               # OSS 图片上传
```

## 核心流程

### 1. 上传错题 → AI 解析

```
用户拍照/选图
    │
    ▼
wx.uploadFile → POST /api/notes/upload
    │
    ▼
OssService.uploadImage() → 阿里云 OSS → 返回图片 URL
    │
    ▼
DashScopeService.parseWrongNote(imageUrl)
    │  调用 qwen-vl-plus 模型，传入图片 + 系统提示词
    │  返回 JSON: {subject, content, analysis, tags}
    ▼
NoteService → 写入 wrong_note 表
    │
    ▼
返回笔记信息给前端
```

### 2. 生成练习 → 答题

```
用户点击"生成练习"
    │
    ▼
POST /api/practice/generate/{noteId}
    │
    ▼
PracticeService.generateQuestions()
    │  调用 DashScopeService.generatePractice()
    │  基于错题内容生成 3-5 道变式题
    ▼
写入 practice_question 表（is_correct = null）
    │
    ▼
用户逐题选择答案 → POST /api/practice/submit
    │
    ▼
PracticeService.submitAnswer()
    │  比对答案 → 设置 is_correct（0/1）
    │  更新 practice_record（当日统计）
    ▼
返回批改结果（对错 + 解析）
```

### 3. 今日训练

```
首页加载 → GET /api/practice/today
    │
    ▼
PracticeService.getTodayTrainInfo()
    │  查询昨日答错的题（is_correct=0, updated_at 昨天）
    │  查询昨日新导入未答题（is_correct=null, created_at 昨天）
    ▼
返回 { yesterdayWrong, yesterdayNew, totalCount }
    │
    ▼
点击"开始今日训练" → GET /api/practice/today/questions
    │  合并两组题目（去重）
    ▼
练习页逐题展示 → 答题 → 批改
```

## 数据库设计

```sql
wrong_note                    practice_question                 practice_record
──────────────                ─────────────────                 ───────────────
id              BIGINT PK     id              BIGINT PK         id              BIGINT PK
user_id         BIGINT        source_note_id  BIGINT FK         user_id         BIGINT
image_url       VARCHAR(512)  user_id         BIGINT            practice_date   DATE
subject         VARCHAR(50)   question_text   TEXT              total_count     INT
knowledge_tags  JSON          options         JSON              correct_count   INT
raw_content     TEXT          answer          VARCHAR(20)       error_rate      DECIMAL(5,2)
analysis        TEXT          explanation     TEXT              created_at      DATETIME
status          TINYINT       is_correct      TINYINT           updated_at      DATETIME
created_at      DATETIME      created_at      DATETIME
updated_at      DATETIME      updated_at      DATETIME

索引:
  wrong_note: idx_user_id, idx_subject, idx_status, idx_created_at
  practice_question: idx_source_note_id, idx_user_id
  practice_record: uk_user_date (唯一索引), idx_user_id, idx_practice_date
```

## API 交互时序

```
客户端                    Spring Boot                  OSS            通义千问
  │                          │                          │                │
  │──上传 POST /notes/upload─▶│                          │                │
  │                          │──PUT 图片──────────────▶│                │
  │                          │◀──返回 URL───────────────│                │
  │                          │──POST 图片URL────────────────────────────▶│
  │                          │◀──JSON 解析结果──────────────────────────│
  │                          │──INSERT wrong_note                       │
  │◀──返回笔记信息────────────│                          │                │
  │                          │                          │                │
  │──POST /generate/{id}────▶│                          │                │
  │                          │──POST 错题内容──────────────────────────▶│
  │                          │◀──JSON 变式题────────────────────────────│
  │                          │──INSERT practice_question                 │
  │◀──返回题目列表────────────│                          │                │
  │                          │                          │                │
  │──POST /submit───────────▶│                          │                │
  │                          │──UPDATE is_correct                        │
  │                          │──UPDATE practice_record                   │
  │◀──返回批改结果────────────│                          │                │
```
