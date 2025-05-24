-- 添加password列到user表
ALTER TABLE `user` 
ADD COLUMN `password` VARCHAR(100) NOT NULL COMMENT '密码' AFTER `username`;

-- 为现有用户设置默认密码（这里使用123456作为默认密码）
UPDATE `user` SET `password` = '123456' WHERE `password` IS NULL; 