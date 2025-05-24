package com.example.superrankinglist;

import com.example.superrankinglist.mapper.RankingItemMapper;
import com.example.superrankinglist.mapper.RankingListMapper;
import com.example.superrankinglist.mapper.UserMapper;
import com.example.superrankinglist.pojo.RankingItem;
import com.example.superrankinglist.pojo.RankingList;
import com.example.superrankinglist.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 测试数据插入类
 */
@SpringBootTest
public class InsertTestData {

    @Autowired
    private RankingItemMapper rankingItemMapper;

    @Autowired
    private RankingListMapper rankingListMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    public void insertTestData() {
        // 清空表
        rankingItemMapper.delete(null);
        rankingListMapper.delete(null);
        userMapper.delete(null);
        
        // 创建测试用户
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

        // 批量插入用户数据
        for (User user : users) {
            userMapper.insert(user);
        }
        
        // 创建测试排行榜
        RankingList rankingList = new RankingList();
        rankingList.setId(1L);
        rankingList.setName("测试排行榜");
        rankingList.setDescription("这是一个测试排行榜");
        rankingList.setType(1);  // 1-日榜
        rankingList.setStatus(1);  // 1-启用
        rankingList.setStartTime(LocalDateTime.now());  // 开始时间
        rankingList.setEndTime(LocalDateTime.now().plusDays(30));  // 结束时间
        rankingList.setCreateTime(LocalDateTime.now());
        rankingList.setUpdateTime(LocalDateTime.now());
        
        // 插入排行榜数据
        rankingListMapper.insert(rankingList);
        
        List<RankingItem> items = new ArrayList<>();

        // 创建100条测试数据
        for (int i = 1; i <= 100; i++) {
            RankingItem item = new RankingItem();
            
            // 设置用户ID（使用已创建的用户ID）
            item.setUserId((long) i);
            
            // 设置排行榜ID
            item.setRankingListId(1L);
            
            // 设置分数（0-100之间的随机数）
            item.setScore(random.nextDouble() * 100);
            
            // 设置排名（初始为0，后续会更新）
            item.setRanking(0L);
            
            // 设置创建时间和更新时间
            item.setCreateTime(now.minusDays(random.nextInt(30)));
            item.setUpdateTime(now);
            
            items.add(item);
        }

        // 批量插入数据
        for (RankingItem item : items) {
            rankingItemMapper.insert(item);
        }

        System.out.println("成功插入" + items.size() + "条测试数据");
    }
} 