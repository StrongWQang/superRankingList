package com.example.superrankinglist.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.superrankinglist.common.Result;
import com.example.superrankinglist.dto.RankingListQueryDto;
import com.example.superrankinglist.pojo.RankingItem;
import com.example.superrankinglist.service.RankingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 排行榜控制器
 */
@RestController
@RequestMapping("/api/ranking")
public class RankingListController {

    private static final Logger log = LoggerFactory.getLogger(RankingListController.class);

    @Autowired
    private RankingListService rankingListService;

    /**
     * 查询排行榜
     * @param queryDto 查询参数
     * @return 排行榜数据
     */
    @PostMapping("/list")
    public Result<Page<RankingItem>> queryRankingList(@RequestBody RankingListQueryDto queryDto) {
        // 参数验证
        if (queryDto == null) {
            return Result.error("查询参数不能为空");
        }
        if (queryDto.getRankingListId() == null) {
            return Result.error("排行榜ID不能为空");
        }
        if (queryDto.getPageNum() == null || queryDto.getPageNum() < 1) {
            queryDto.setPageNum(1);
        }
        if (queryDto.getPageSize() == null || queryDto.getPageSize() < 1) {
            queryDto.setPageSize(10);
        }
        if (queryDto.getSortType() == null) {
            queryDto.setSortType(1);
        }
        
        try {
            Page<RankingItem> page = rankingListService.queryRankingList(queryDto);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error("查询排行榜失败：" + e.getMessage());
        }
    }

    @GetMapping("/user/rank")
    public Result<RankingItem> getUserRankAndScore(@RequestParam Long rankingListId, @RequestParam Long userId) {
        try {
            RankingItem rankingItem = rankingListService.getUserRankAndScore(rankingListId, userId);
            return Result.success(rankingItem);
        } catch (Exception e) {
            log.error("获取用户排名失败", e);
            return Result.error("获取用户排名失败");
        }
    }
} 