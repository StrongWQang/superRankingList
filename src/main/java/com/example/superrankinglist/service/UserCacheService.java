package com.example.superrankinglist.service;

import com.example.superrankinglist.pojo.User;
import java.util.List;
import java.util.Map;

public interface UserCacheService {
    /**
     * 获取用户信息（优先从缓存获取）
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 批量获取用户信息（优先从缓存获取）
     * @param userIds 用户ID列表
     * @return 用户信息Map
     */
    Map<Long, User> getUsersByIds(List<Long> userIds);

    /**
     * 更新用户缓存
     * @param user 用户信息
     */
    void updateUserCache(User user);

    /**
     * 批量更新用户缓存
     * @param users 用户信息列表
     */
    void updateUsersCache(List<User> users);

    /**
     * 删除用户缓存
     * @param userId 用户ID
     */
    void deleteUserCache(Long userId);

    /**
     * 清除所有用户缓存
     */
    void clearAllUserCache();
} 