package com.example.superrankinglist.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 排行榜项目实体类
 * 用于存储用户在特定排行榜中的排名信息
 */
@Data
@TableName("ranking_item")
public class RankingItem {
    /**
     * 排行榜项目ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联用户表
     */
    private Long userId;

    /**
     * 所属排行榜ID，关联排行榜表
     */
    private Long rankingListId;

    /**
     * 用户在该排行榜中的分数
     */
    private Double score;

    /**
     * 用户在该排行榜中的排名
     * 排名从1开始，数字越小排名越高
     */
    private Long ranking;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 关联的用户信息
     * 用于在查询排行榜时直接获取用户信息，避免多次查询
     */
    private User user;
} 