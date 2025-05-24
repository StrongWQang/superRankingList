-- 删除旧表（如果存在）
DROP TABLE IF EXISTS `ranking_item`;
DROP TABLE IF EXISTS `ranking_list`;
DROP TABLE IF EXISTS `user`;

-- 创建用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) NOT NULL COMMENT '用户昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像URL',
    `score` DOUBLE DEFAULT 0.0 COMMENT '用户总分数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_score` (`score`) COMMENT '分数索引，用于排行榜查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建排行榜表
CREATE TABLE `ranking_list` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '排行榜ID',
    `name` VARCHAR(100) NOT NULL COMMENT '排行榜名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '排行榜描述',
    `type` INT NOT NULL COMMENT '排行榜类型：1-日榜，2-周榜，3-月榜，4-总榜',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`) COMMENT '类型和状态联合索引',
    KEY `idx_time` (`start_time`, `end_time`) COMMENT '时间范围索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜表';

-- 创建排行榜项目表
CREATE TABLE `ranking_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '排行榜项目ID',
    `ranking_list_id` BIGINT NOT NULL COMMENT '所属排行榜ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '用户在该排行榜中的分数',
    `ranking` BIGINT NOT NULL COMMENT '用户在该排行榜中的排名',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ranking_user` (`ranking_list_id`, `user_id`) COMMENT '排行榜用户唯一索引',
    KEY `idx_ranking_score` (`ranking_list_id`, `score`) COMMENT '排行榜分数索引',
    KEY `idx_ranking_rank` (`ranking_list_id`, `ranking`) COMMENT '排行榜排名索引',
    CONSTRAINT `fk_ranking_list` FOREIGN KEY (`ranking_list_id`) REFERENCES `ranking_list` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜项目表'; 