package com.example.superrankinglist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.superrankinglist.mapper.UserMapper;
import com.example.superrankinglist.pojo.User;
import com.example.superrankinglist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    private static final int SALT_LENGTH = 16;
    private static final int HASH_ITERATIONS = 3; // MD5 加密次数
    
    // 生成随机盐值
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    // MD5 加密
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
    
    // 使用盐值和多次MD5加密密码
    private String hashPassword(String password, String salt) {
        String hashedPassword = password + salt;
        // 多次MD5加密增加安全性
        for (int i = 0; i < HASH_ITERATIONS; i++) {
            hashedPassword = md5(hashedPassword);
        }
        return hashedPassword;
    }
    
    // 验证密码
    private boolean checkPassword(String password, String salt, String hashedPassword) {
        String newHashedPassword = hashPassword(password, salt);
        return newHashedPassword.equals(hashedPassword);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        // 生成盐值并加密密码
        String salt = generateSalt();
        String hashedPassword = hashPassword(user.getPassword(), salt);
        
        // 存储加密后的密码和盐值
        user.setPassword(hashedPassword);
        user.setSalt(salt);
        
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        
        // 返回用户信息时清除敏感数据
        user.setPassword(null);
        user.setSalt(null);
        return user;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
            user.setSalt(null);
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        // 如果密码被修改，需要重新加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String salt = generateSalt();
            String hashedPassword = hashPassword(user.getPassword(), salt);
            user.setPassword(hashedPassword);
            user.setSalt(salt);
        }
        
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 返回用户信息时清除敏感数据
        user.setPassword(null);
        user.setSalt(null);
        return user;
    }

    @Override
    public boolean verifyPassword(String rawPassword, String salt, String hashedPassword) {
        return checkPassword(rawPassword, salt, hashedPassword);
    }

    @Override
    @Transactional
    public User updateUserScore(Long userId, Double score) {
        userMapper.updateScore(userId, score);
        return getUserById(userId);
    }

    @Override
    public List<User> getTopUsers(int limit) {
        return userMapper.findTopUsers(limit);
    }

    @Override
    public List<User> getUsersByScoreRange(Double minScore, Double maxScore) {
        return userMapper.findByScoreRange(minScore, maxScore);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
} 