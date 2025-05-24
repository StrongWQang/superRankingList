package com.example.superrankinglist.controller;

import com.example.superrankinglist.common.Result;
import com.example.superrankinglist.dto.LoginDto;
import com.example.superrankinglist.dto.RegisterDto;
import com.example.superrankinglist.pojo.User;
import com.example.superrankinglist.service.UserService;
import com.example.superrankinglist.utils.JwtUtil;
import com.example.superrankinglist.utils.UserContext;
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
        if (userId == null) {
            return Result.error("用户未登录");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 清除敏感信息
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 用户注册
     * @param registerDto 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterDto registerDto) {
        // 检查用户名是否已存在
        User existingUser = userService.getUserByUsername(registerDto.getUsername());
        if (existingUser != null) {
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

        // 生成token
        String token = jwtUtil.generateToken(user.getId());

        // 返回用户信息和token
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        return Result.success(result);
    }

    /**
     * 用户登录
     * @param loginDto 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDto loginDto) {
        // 验证用户名和密码
        User user = userService.getUserByUsername(loginDto.getUsername());
        if (user == null || !Objects.equals(user.getPassword(), loginDto.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId());

        // 返回用户信息和token
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        return Result.success(result);
    }

    /**
     * 用户登出
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 清除用户上下文
        UserContext.clear();
        return Result.success();
    }
} 