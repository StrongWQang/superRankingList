package com.example.superrankinglist.dto;

import lombok.Data;

/**
 * 注册DTO
 */
@Data
public class RegisterDto {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;
} 