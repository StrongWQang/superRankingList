package com.example.superrankinglist.utils;

/**
 * 用户上下文工具类
 * 用于在请求线程中存储和获取用户ID
 */
public class UserContext {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();

    /**
     * 设置用户ID
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public static Long getUserId() {
        return userIdHolder.get();
    }

    /**
     * 清除用户ID
     */
    public static void clear() {
        userIdHolder.remove();
    }
} 