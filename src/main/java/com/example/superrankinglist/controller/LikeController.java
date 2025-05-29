package com.example.superrankinglist.controller;

import com.example.superrankinglist.common.Result;

import com.example.superrankinglist.dto.LikeDto;
import com.example.superrankinglist.service.LikeService;
import com.example.superrankinglist.service.RankingListService;
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
} 