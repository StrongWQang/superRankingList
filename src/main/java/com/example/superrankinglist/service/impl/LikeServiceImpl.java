package com.example.superrankinglist.service.impl;

import com.example.superrankinglist.dto.LikeDto;
import com.example.superrankinglist.service.LikeService;
import com.example.superrankinglist.utils.UserContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

import static com.example.superrankinglist.common.RedisKey.RANKING_KEY_PREFIX;

/**
 * 点赞服务实现类
 */
@Log4j2
@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Long> redisScript;

    @Autowired
    private SegmentTreeServiceImpl segmentTreeServiceImpl;

    @PostConstruct
    public void init() {
        try {
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/update_ranking.lua")));
            redisScript.setResultType(Long.class);
            log.info("Lua脚本加载成功");
        } catch (Exception e) {
            log.error("Lua脚本加载失败", e);
            throw new RuntimeException("Lua脚本加载失败", e);
        }
    }

    @Override
    public boolean like(LikeDto likeDto) {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                throw new RuntimeException("User not logged in");
            }

            // 排行榜key
            String rankingKey = RANKING_KEY_PREFIX + likeDto.getRankingListId();
            log.info("用户 {} 点赞排行榜 {}, key: {}", userId, likeDto.getRankingListId(), rankingKey);

            Double oldscore = redisTemplate.opsForZSet().score(rankingKey, userId);
            // 准备Lua脚本参数
            String userIdStr = String.valueOf(userId);
            List<String> keys = Arrays.asList(rankingKey, userIdStr);
            // 确保score是数字类型，使用1.0而不是1
            List<Object> args = Arrays.asList(1.0, System.currentTimeMillis());
            log.debug("Lua脚本参数 - keys: {}, args: {}", keys, args);

            // 执行Lua脚本
            Long result = redisTemplate.execute(redisScript, keys, args.toArray());
            log.info("更新排行榜结果: {}", result);

            // 验证分数是否更新成功
            Double newscore = redisTemplate.opsForZSet().score(rankingKey, userId);
            log.info("用户 {} 在排行榜 {} 中的最新分数: {}", userId, likeDto.getRankingListId(), newscore);

            segmentTreeServiceImpl.updateUserScore(userId,oldscore,newscore);

            return true;
        } catch (RedisSystemException e) {
            log.error("Redis操作失败", e);
            throw new RuntimeException("点赞操作失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("点赞操作失败", e);
            throw new RuntimeException("点赞操作失败: " + e.getMessage(), e);
        }
    }


} 