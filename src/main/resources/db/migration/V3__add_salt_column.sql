-- 添加 salt 列到 user 表
ALTER TABLE `user` 
ADD COLUMN `salt` varchar(64) NOT NULL COMMENT '盐值' AFTER `password`;

-- 为现有用户生成随机盐值
UPDATE `user` SET `salt` = SUBSTRING(MD5(RAND()), 1, 16) WHERE `salt` IS NULL; 