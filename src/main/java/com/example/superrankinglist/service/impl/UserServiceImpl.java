package com.example.superrankinglist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.superrankinglist.mapper.UserMapper;
import com.example.superrankinglist.pojo.User;
import com.example.superrankinglist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public User createUser(User user) {
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
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