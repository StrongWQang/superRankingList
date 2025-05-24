package com.example.superrankinglist.pojo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 排行榜实体类
 * 用于存储排行榜的基本信息和配置
 * 支持高并发场景下的线程安全操作
 */
@Data
public class RankingList {
    /**
     * 排行榜ID，主键
     * 使用AtomicLong确保ID生成的线程安全
     */
    private Long id;

    /**
     * 排行榜名称
     * 使用volatile确保多线程下的可见性
     */
    private String name;

    /**
     * 排行榜描述
     * 使用volatile确保多线程下的可见性
     * 用于说明排行榜的用途和规则
     */
    private String description;

    /**
     * 排行榜类型
     * 使用AtomicInteger确保类型更新的原子性
     * 1-日榜：每日更新
     * 2-周榜：每周更新
     * 3-月榜：每月更新
     * 4-总榜：永久有效
     */
    private Integer type;

    /**
     * 排行榜状态
     * 使用AtomicInteger确保状态更新的原子性
     * 0-禁用：排行榜暂时关闭
     * 1-启用：排行榜正常开放
     */
    private Integer status;

    /**
     * 排行榜开始时间
     * 使用volatile确保多线程下的可见性
     * 用于限定排行榜的有效期
     */
    private LocalDateTime startTime;

    /**
     * 排行榜结束时间
     * 使用volatile确保多线程下的可见性
     * 用于限定排行榜的有效期
     */
    private LocalDateTime endTime;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 排行榜项目列表
     * 包含该排行榜中所有用户的排名信息
     * 按排名顺序排列
     */
    private RankingItem items;
} 