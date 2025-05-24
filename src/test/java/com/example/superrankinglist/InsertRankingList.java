package com.example.superrankinglist;

import com.example.superrankinglist.mapper.RankingListMapper;
import com.example.superrankinglist.pojo.RankingList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * 插入排行榜测试数据  2
 */
@SpringBootTest
public class InsertRankingList {

    @Autowired
    private RankingListMapper rankingListMapper;

    @Test
    public void insertRankingList() {
        // 清空表
        rankingListMapper.delete(null);
        
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
        
        // 插入数据
        rankingListMapper.insert(rankingList);
        
        System.out.println("成功插入排行榜数据");
    }
} 