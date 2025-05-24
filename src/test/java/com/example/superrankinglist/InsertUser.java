package com.example.superrankinglist;

import com.example.superrankinglist.mapper.UserMapper;
import com.example.superrankinglist.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 插入用户测试数据
 */
@SpringBootTest
public class InsertUser {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void insertUser() {
        // 清空表
        userMapper.delete(null);
        
        List<User> users = new ArrayList<>();
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();

        // 创建100个测试用户
        for (int i = 1; i <= 100; i++) {
            User user = new User();
            user.setId((long) i);
            user.setUsername("user" + i);
            user.setNickname("用户" + i);
            user.setAvatar("https://example.com/avatar/" + i + ".jpg");
            user.setScore(random.nextDouble() * 100);
            user.setCreateTime(now.minusDays(random.nextInt(30)));
            user.setUpdateTime(now);
            
            users.add(user);
        }

        // 批量插入数据
        for (User user : users) {
            userMapper.insert(user);
        }

        System.out.println("成功插入" + users.size() + "条用户数据");
    }
} 