package com.example.superrankinglist.common;

import lombok.Data;

/**
 * 统一响应结果类
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 成功响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    
    /**
     * 成功响应（无数据）
     * @return 响应结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }
    
    /**
     * 失败响应
     * @param code 状态码
     * @param message 错误信息
     * @return 响应结果
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    
    /**
     * 失败响应（默认状态码500）
     * @param message 错误信息
     * @return 响应结果
     */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
} 