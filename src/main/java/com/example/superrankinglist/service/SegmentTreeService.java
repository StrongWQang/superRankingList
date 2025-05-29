package com.example.superrankinglist.service;

import com.example.superrankinglist.pojo.SegmentTreeNode;

/**
 * 线段树服务接口
 * 提供基于线段树的排行榜粗估排名功能
 * 通过将积分范围划分为多个区间，实现高效的排名估算
 */
public interface SegmentTreeService {
    /**
     * 获取用户积分对应的粗估排名
     * 通过线段树快速定位积分所在区间，并计算估算排名
     * 
     * @param rankingListId 排行榜ID
     * @param score 用户积分
     * @return 粗估排名，从1开始
     */
    Long getFuzzyRank(Long rankingListId, Double score);

    /**
     * 更新用户积分
     * 当用户积分发生变化时，更新相关区间的用户数量
     * 
     * @param rankingListId 排行榜ID
     * @param oldScore 用户旧积分
     * @param newScore 用户新积分
     */
    void updateScore(Long rankingListId, Double oldScore, Double newScore);

    /**
     * 初始化线段树
     * 创建指定积分范围的线段树结构，并初始化所有区间的用户数量为0
     * 
     * @param rankingListId 排行榜ID
     * @param minScore 最小积分值
     * @param maxScore 最大积分值
     * @param segmentCount 区间数量，决定线段树的深度和精度
     */
    void initSegmentTree(Long rankingListId, Double minScore, Double maxScore, int segmentCount);
} 