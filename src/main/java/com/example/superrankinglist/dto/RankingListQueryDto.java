package com.example.superrankinglist.dto;

import lombok.Data;

/**
 * 排行榜查询DTO
 */
@Data
public class RankingListQueryDto {
    /**
     * 排行榜ID
     */
    private Long rankingListId;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 排序方式（1: 按点赞数降序, 2: 按创建时间降序）
     */
    private Integer sortType = 1;
} 