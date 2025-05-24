package com.example.superrankinglist.service.impl;

import com.example.superrankinglist.dto.LikeCheckDto;
import com.example.superrankinglist.dto.LikeCountDto;
import com.example.superrankinglist.dto.LikeDto;
import com.example.superrankinglist.service.LikeService;
import com.example.superrankinglist.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import static com.example.superrankinglist.common.RedisKey.LIKE_KEY_PREFIX;
import static com.example.superrankinglist.common.RedisKey.USER_ID_MEMBER;


/**
 * 点赞服务实现类
 */
@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public boolean like(LikeDto likeDto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not logged in");
        }

        //排行榜key
        String likeKey = LIKE_KEY_PREFIX  + likeDto.getRankingListId();
        //排行榜memeber
        String userLikeKey = USER_ID_MEMBER + userId;

        //如果member不在ZSET中，则将其作为新成员加入并设置初始score=1
        redisTemplate.opsForZSet().incrementScore(likeKey, userLikeKey, 1);

        return true;
    }





    @Override
    public long getLikeCount(LikeCountDto likeCountDto) {
        String likeKey = LIKE_KEY_PREFIX + likeCountDto.getRankingListId();
        Long count = redisTemplate.opsForValue().increment(likeKey, 0);
        return count != null ? count : 0;
    }

    @Override
    public boolean checkLiked(LikeCheckDto likeCheckDto) {
        return false;
    }


} 