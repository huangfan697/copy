-- 新增每日栏目表
CREATE TABLE IF NOT EXISTS daily_collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户标识',
    collection_date DATE NOT NULL COMMENT '栏目日期',
    note_count INT NOT NULL DEFAULT 0 COMMENT '错题数量',
    question_count INT NOT NULL DEFAULT 0 COMMENT '练习题数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, collection_date),
    INDEX idx_user_id (user_id),
    INDEX idx_collection_date (collection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日错题栏目';

-- wrong_note 表新增 collection_id
ALTER TABLE wrong_note ADD COLUMN collection_id BIGINT DEFAULT NULL COMMENT '所属每日栏目ID';
ALTER TABLE wrong_note ADD INDEX idx_collection_id (collection_id);
