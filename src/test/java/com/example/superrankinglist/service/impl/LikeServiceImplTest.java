package com.example.superrankinglist.service.impl;

import com.example.superrankinglist.common.RedisKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 测试数据插入redis
 */
@SpringBootTest
@ActiveProfiles("test")
public class LikeServiceImplTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String RANKING_KEY = RedisKey.RANKING_KEY_PREFIX + 1;
    private static final int TEST_DATA_SIZE = 10000;
    private static final Random random = new Random();

    @Test
    public void generateTestData() {
        // 清空测试数据
        //redisTemplate.delete(RANKING_KEY);
        
        System.out.println("开始生成测试数据...");
        long startTime = System.currentTimeMillis();

        // 生成并插入测试数据
        for (int i = 1; i <= TEST_DATA_SIZE; i++) {
            long userId = i;
            // 生成1-1000之间的随机点赞数
            int likes = random.nextInt(10000) + 1;
            
            // 使用ZADD命令添加数据到排行榜
            redisTemplate.opsForZSet().add(RANKING_KEY, userId, likes);
            
            if (i % 100 == 0) {
                System.out.printf("已生成 %d 条数据...\n", i);
            }
        }

        // 验证数据
        Long size = redisTemplate.opsForZSet().size(RANKING_KEY);
        System.out.printf("测试数据生成完成，共 %d 条记录\n", size);

        // 打印前10名数据
        System.out.println("\n排行榜前10名：");
        redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, 9)
            .forEach(tuple -> {
                System.out.printf("用户ID: %s, 点赞数: %d\n", 
                    tuple.getValue(), 
                    tuple.getScore().intValue());
            });

        long endTime = System.currentTimeMillis();
        System.out.printf("\n数据生成耗时: %d ms\n", endTime - startTime);
    }

    @Test
    public void testGetRankingList() {
        // 获取排行榜数据
        System.out.println("\n获取排行榜数据：");
        redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, -1)
            .forEach(tuple -> {
                System.out.printf("用户ID: %s, 点赞数: %d\n", 
                    tuple.getValue(), 
                    tuple.getScore().intValue());
            });
    }

    @Test
    public void testGetUserRank() {
        // 测试获取指定用户的排名
        String testUserId = "1";
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, testUserId);
        Double score = redisTemplate.opsForZSet().score(RANKING_KEY, testUserId);
        
        System.out.printf("\n用户 %s 的排名信息：\n", testUserId);
        System.out.printf("排名: %d\n", rank != null ? rank + 1 : -1);
        System.out.printf("点赞数: %d\n", score != null ? score.intValue() : 0);
    }
} 