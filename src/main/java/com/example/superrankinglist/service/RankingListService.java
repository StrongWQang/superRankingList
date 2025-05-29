package com.example.superrankinglist.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.superrankinglist.dto.RankingListQueryDto;
import com.example.superrankinglist.pojo.RankingItem;
import com.example.superrankinglist.pojo.RankingList;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排行榜服务接口
 */
public interface RankingListService {
    /**
     * 查询排行榜
     * @param queryDto 查询参数
     * @return 排行榜分页数据
     */
    Page<RankingItem> queryRankingList(RankingListQueryDto queryDto);

    /**
     * 创建排行榜
     * @param rankingList 排行榜信息
     * @return 创建后的排行榜
     */
    RankingList createRankingList(RankingList rankingList);

    /**
     * 根据ID获取排行榜
     * @param id 排行榜ID
     * @return 排行榜信息
     */
    RankingList getRankingListById(Long id);

    /**
     * 更新排行榜
     * @param rankingList 排行榜信息
     * @return 更新后的排行榜
     */
    RankingList updateRankingList(RankingList rankingList);

    /**
     * 更新排行榜状态
     * @param id 排行榜ID
     * @param status 状态值
     * @return 更新后的排行榜
     */
    RankingList updateStatus(Long id, Integer status);

    /**
     * 根据类型获取排行榜列表
     * @param type 排行榜类型
     * @return 排行榜列表
     */
    List<RankingList> getRankingListsByType(Integer type);

    /**
     * 获取活跃的排行榜列表
     * @return 活跃的排行榜列表
     */
    List<RankingList> getActiveRankingLists();

    /**
     * 根据时间范围获取排行榜列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 排行榜列表
     */
    List<RankingList> getRankingListsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据状态获取排行榜列表
     * @param status 状态值
     * @return 排行榜列表
     */
    List<RankingList> getRankingListsByStatus(Integer status);

    /**
     * 删除排行榜
     * @param id 排行榜ID
     */
    void deleteRankingList(Long id);

    /**
     * 获取用户的排名和积分
     * @param rankingListId 排行榜ID
     * @param userId 用户ID
     * @return 包含用户排名和积分的对象
     */
    RankingItem getUserRankAndScore(Long rankingListId, Long userId);


    boolean updateRankingScore(Long rankingListId, Long userId, Double score);
}