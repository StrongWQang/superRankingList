package com.example.superrankinglist.controller;

import com.example.superrankinglist.common.Result;

import com.example.superrankinglist.dto.LikeCheckDto;
import com.example.superrankinglist.dto.LikeCountDto;
import com.example.superrankinglist.dto.LikeDto;
import com.example.superrankinglist.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 点赞控制器
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    /**
     * 点赞
     * @param request 点赞请求xx
     */
    @PostMapping("/like")
    public Result<Boolean> like(@RequestBody LikeDto request) {
        return Result.success(likeService.like(request));
    }


//
//    /**
//     * 取消点赞
//     * @param request 取消点赞请求
//     * @return 取消点赞结果
//     */
//    @DeleteMapping("/unlike")
//    public Result<Boolean> unlike(@RequestBody LikeDto request) {
//        return Result.success(likeService.unlike(request));
//    }

    /**
     * 获取点赞数
     * @param request 获取点赞数请求
     * @return 点赞数
     */
    @PostMapping("/count")
    public Result<Long> getLikeCount(@RequestBody LikeCountDto request) {
        return Result.success(likeService.getLikeCount(request));
    }

    /**
     * 检查用户是否已点赞
     * @param request 检查请求
     * @return 是否已点赞
     */
    @PostMapping("/check")
    public Result<Boolean> checkLiked(@RequestBody LikeCheckDto request) {
        return Result.success(likeService.checkLiked(request));
    }
} 