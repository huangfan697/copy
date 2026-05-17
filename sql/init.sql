-- 错题集小程序数据库初始化脚本

CREATE DATABASE IF NOT EXISTS wrong_note DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE wrong_note;

-- 错题笔记表
CREATE TABLE IF NOT EXISTS wrong_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户标识',
    image_url VARCHAR(512) NOT NULL COMMENT '原图 OSS 地址',
    subject VARCHAR(50) DEFAULT NULL COMMENT '科目',
    knowledge_tags JSON DEFAULT NULL COMMENT 'AI 解析的知识点标签',
    raw_content TEXT DEFAULT NULL COMMENT '原始题目文本',
    analysis TEXT DEFAULT NULL COMMENT 'AI 解析的解题思路',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待复习 1-已掌握',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_subject (subject),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题笔记';

-- 练习题表
CREATE TABLE IF NOT EXISTS practice_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_note_id BIGINT NOT NULL COMMENT '来源错题笔记 ID',
    user_id BIGINT NOT NULL COMMENT '用户标识',
    question_text TEXT NOT NULL COMMENT '题目内容',
    options JSON DEFAULT NULL COMMENT '选项 ABCD',
    answer VARCHAR(20) NOT NULL COMMENT '正确答案',
    explanation TEXT DEFAULT NULL COMMENT '解析说明',
    is_correct TINYINT DEFAULT NULL COMMENT '答题结果 null-未答 0-错误 1-正确',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_source_note_id (source_note_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习题';

-- 答题记录表（按天聚合，用于错题率统计）
CREATE TABLE IF NOT EXISTS practice_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户标识',
    practice_date DATE NOT NULL COMMENT '答题日期',
    total_count INT NOT NULL DEFAULT 0 COMMENT '当日答题总数',
    correct_count INT NOT NULL DEFAULT 0 COMMENT '当日答对数',
    error_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '当日错题率',
    UNIQUE KEY uk_user_date (user_id, practice_date),
    INDEX idx_user_id (user_id),
    INDEX idx_practice_date (practice_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录统计';
