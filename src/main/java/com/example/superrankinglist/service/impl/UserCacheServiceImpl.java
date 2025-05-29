package com.example.superrankinglist.service.impl;

import com.example.superrankinglist.mapper.UserMapper;
import com.example.superrankinglist.pojo.User;
import com.example.superrankinglist.service.UserCacheService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserCacheServiceImpl implements UserCacheService {

    private static final String USER_CACHE_KEY_PREFIX = "user:info:";
    private static final long USER_CACHE_EXPIRE_TIME = 24; // 缓存过期时间（小时）

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getUserById(Long userId) {
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        
        // 尝试从缓存获取
        User user = (User) redisTemplate.opsForValue().get(cacheKey);
        if (user != null) {
            return user;
        }

        // 缓存未命中，从数据库获取
        user = userMapper.selectById(userId);
        if (user != null) {
            // 更新缓存
            updateUserCache(user);
        }
        return user;
    }

    @Override
    public Map<Long, User> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建缓存key列表
        List<String> cacheKeys = userIds.stream()
                .map(id -> USER_CACHE_KEY_PREFIX + id)
                .collect(Collectors.toList());

        // 批量获取缓存
        List<Object> cachedUsers = redisTemplate.opsForValue().multiGet(cacheKeys);
        
        // 找出缓存未命中的用户ID
        List<Long> missedUserIds = new ArrayList<>();
        Map<Long, User> result = new HashMap<>();
        
        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            User user = (User) cachedUsers.get(i);
            if (user != null) {
                result.put(userId, user);
            } else {
                missedUserIds.add(userId);
            }
        }

        // 如果有缓存未命中的用户，从数据库获取
        if (!missedUserIds.isEmpty()) {
            List<User> dbUsers = userMapper.selectBatchIds(missedUserIds);
            if (dbUsers != null) {
                // 更新缓存
                updateUsersCache(dbUsers);
                // 添加到结果集
                dbUsers.forEach(user -> result.put(user.getId(), user));
            }
        }

        return result;
    }

    @Override
    public void updateUserCache(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String cacheKey = USER_CACHE_KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user, USER_CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }

    @Override
    public void updateUsersCache(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<String, User> cacheMap = users.stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(
                    user -> USER_CACHE_KEY_PREFIX + user.getId(),
                    user -> user
                ));
        
        if (!cacheMap.isEmpty()) {
            redisTemplate.opsForValue().multiSet(cacheMap);
            // 设置过期时间
            cacheMap.keySet().forEach(key -> 
                redisTemplate.expire(key, USER_CACHE_EXPIRE_TIME, TimeUnit.HOURS)
            );
        }
    }

    @Override
    public void deleteUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }

    /**
     * 清除所有用户缓存
     */
    public void clearAllUserCache() {
        Set<String> keys = redisTemplate.keys(USER_CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("已清除所有用户缓存，共 {} 条记录", keys.size());
        }
    }
} 