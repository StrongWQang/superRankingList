package com.example.superrankinglist.common;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数（默认错误码500）
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(500, message);
    }
} 