package com.example.superrankinglist.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 点赞Mapper接口
 */
@Mapper
public interface LikeMapper {
    /**
     * 添加点赞记录
     * @param userId 用户ID
     * @param rankingListId 排行榜ID
     * @return 影响行数
     */
    int insertLike(@Param("userId") Long userId, @Param("rankingListId") Long rankingListId);

    /**
     * 删除点赞记录
     * @param userId 用户ID
     * @param rankingListId 排行榜ID
     * @return 影响行数
     */
    int deleteLike(@Param("userId") Long userId, @Param("rankingListId") Long rankingListId);

    /**
     * 获取点赞数
     * @param rankingListId 排行榜ID
     * @return 点赞数
     */
    long countLikes(@Param("rankingListId") Long rankingListId);

    /**
     * 检查用户是否已点赞
     * @param userId 用户ID
     * @param rankingListId 排行榜ID
     * @return 点赞记录数
     */
    int checkLiked(@Param("userId") Long userId, @Param("rankingListId") Long rankingListId);
} 