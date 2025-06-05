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
     * 获取用户的排名和积分
     * @param rankingListId 排行榜ID
     * @param userId 用户ID
     * @return 包含用户排名和积分的对象
     */
    RankingItem getUserRankAndScore(Long rankingListId, Long userId);

}