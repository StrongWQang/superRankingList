package com.example.superrankinglist.controller;

import com.example.superrankinglist.common.Result;
import com.example.superrankinglist.dto.LoginDto;
import com.example.superrankinglist.dto.RegisterDto;
import com.example.superrankinglist.pojo.User;
import com.example.superrankinglist.service.UserService;
import com.example.superrankinglist.utils.JwtUtil;
import com.example.superrankinglist.utils.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        Long userId = UserContext.getUserId();
        log.info("[获取用户信息] userId={}", userId);
        if (userId == null) {
            log.warn("[获取用户信息] 用户未登录");
            return Result.error("用户未登录");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            log.warn("[获取用户信息] 用户不存在 userId={}", userId);
            return Result.error("用户不存在");
        }

        // 清除敏感信息
        user.setPassword(null);
        log.info("[获取用户信息] 成功 user={}", user);
        return Result.success(user);
    }

    /**
     * 用户注册
     * @param registerDto 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterDto registerDto) {
        log.info("[用户注册] 请求参数: {}", registerDto);
        // 检查用户名是否已存在
        User existingUser = userService.getUserByUsername(registerDto.getUsername());
        if (existingUser != null) {
            log.warn("[用户注册] 用户名已存在: {}", registerDto.getUsername());
            return Result.error("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPassword(registerDto.getPassword());
        user.setNickname(registerDto.getNickname());
        user.setScore(0.0);

        // 保存用户
        user = userService.createUser(user);
        log.info("[用户注册] 新用户已创建: {}", user.getUsername());

        // 生成token
        String token = jwtUtil.generateToken(user.getId());
        log.info("[用户注册] 生成token: {}", token);

        // 返回用户信息和token
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        log.info("[用户注册] 注册成功: {}", user.getUsername());
        return Result.success(result);
    }

    /**
     * 用户登录
     * @param loginDto 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDto loginDto) {
        log.info("[用户登录] 请求参数: {}", loginDto);
        // 验证用户名和密码
        User user = userService.getUserByUsername(loginDto.getUsername());
        if (user == null) {
            log.warn("[用户登录] 用户不存在: {}", loginDto.getUsername());
            return Result.error("用户名或密码错误");
        }
        boolean passwordValid = userService.verifyPassword(loginDto.getPassword(), user.getSalt(), user.getPassword());
        log.info("[用户登录] 密码校验结果: {}", passwordValid);
        if (!passwordValid) {
            log.warn("[用户登录] 密码错误: {}", loginDto.getUsername());
            return Result.error("用户名或密码错误");
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId());
        log.info("[用户登录] 生成token: {}", token);

        // 清除敏感信息
        user.setPassword(null);
        user.setSalt(null);

        // 返回用户信息和token
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        log.info("[用户登录] 登录成功: {}", user.getUsername());
        return Result.success(result);
    }

    /**
     * 用户登出
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("[用户登出] 用户ID: {}", UserContext.getUserId());
        // 清除用户上下文
        UserContext.clear();
        log.info("[用户登出] 成功");
        return Result.success();
    }
} 