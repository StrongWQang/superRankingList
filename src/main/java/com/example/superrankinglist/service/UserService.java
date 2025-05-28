package com.example.superrankinglist.service;

import com.example.superrankinglist.pojo.User;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建的用户
     */
    User createUser(User user);
    
    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserById(Long id);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
    
    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 更新后的用户
     */
    User updateUser(User user);
    
    /**
     * 更新用户分数
     * @param userId 用户ID
     * @param score 新的分数
     * @return 更新后的用户
     */
    User updateUserScore(Long userId, Double score);
    
    /**
     * 获取分数最高的前N个用户
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> getTopUsers(int limit);
    
    /**
     * 根据分数范围查询用户
     * @param minScore 最低分数
     * @param maxScore 最高分数
     * @return 用户列表
     */
    List<User> getUsersByScoreRange(Double minScore, Double maxScore);
    
    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param salt 盐值
     * @param hashedPassword 加密后的密码
     * @return 是否验证成功
     */
    boolean verifyPassword(String rawPassword, String salt, String hashedPassword);
} 