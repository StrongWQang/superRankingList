-- 创建点赞记录表
CREATE TABLE `like_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `ranking_list_id` BIGINT NOT NULL COMMENT '排行榜ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_ranking` (`user_id`, `ranking_list_id`) COMMENT '用户排行榜唯一索引',
    KEY `idx_ranking_list` (`ranking_list_id`) COMMENT '排行榜索引',
    CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_like_ranking_list` FOREIGN KEY (`ranking_list_id`) REFERENCES `ranking_list` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表'; 