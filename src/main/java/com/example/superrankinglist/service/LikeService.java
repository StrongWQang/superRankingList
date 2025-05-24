package com.example.superrankinglist.service;

import com.example.superrankinglist.dto.LikeCheckDto;
import com.example.superrankinglist.dto.LikeCountDto;
import com.example.superrankinglist.dto.LikeDto;

/**
 * 点赞服务接口
 */
public interface LikeService {
    /**
     * 点赞
     * @param likeDto 点赞信息
     * @return 是否点赞成功
     */
    boolean like(LikeDto likeDto);



    /**
     * 获取点赞数
     * @param likeCountDto 获取点赞数请求
     * @return 点赞数
     */
    long getLikeCount(LikeCountDto likeCountDto);

    /**
     * 检查用户是否已点赞
     * @param likeCheckDto 检查请求
     * @return 是否已点赞
     */
    boolean checkLiked(LikeCheckDto likeCheckDto);
} 