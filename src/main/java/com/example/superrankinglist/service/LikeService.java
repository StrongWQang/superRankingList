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
} 