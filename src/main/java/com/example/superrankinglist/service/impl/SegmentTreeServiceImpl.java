package com.example.superrankinglist.service.impl;

import com.example.superrankinglist.common.SegmentTreeNode;
import com.example.superrankinglist.service.SegmentTree;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.example.superrankinglist.common.RedisKey.RANKING_KEY_PREFIX;
import static com.example.superrankinglist.common.RedisKey.SEGMENT_KEY_PREFIX;

@Slf4j
@Service
public class SegmentTreeServiceImpl {
    private final SegmentTree segmentTree;
    private final RedisTemplate<String, String> redisTemplate;
    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 1000000.0;

    public SegmentTreeServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.segmentTree = new SegmentTree(SEGMENT_KEY_PREFIX+ 1, redisTemplate);
    }

    @PostConstruct
    public void init() {
        try {
            // 检查Redis中是否已存在线段树数据
            Boolean exists = redisTemplate.hasKey(SEGMENT_KEY_PREFIX+1);
            if (Boolean.FALSE.equals(exists)) {
                log.info("初始化排行榜线段树...");
                //buildSegmentTree((long) MIN_SCORE,(long) MAX_SCORE);
                segmentTree.buildTree(MIN_SCORE, MAX_SCORE);
                log.info("排行榜线段树初始化完成");
                // 从Redis中获取所有排行榜数据
                Set<String> rankingKeys = redisTemplate.keys(RANKING_KEY_PREFIX + "1");
                if (rankingKeys != null && !rankingKeys.isEmpty()) {
                    log.info("开始同步排行榜数据到线段树...");

                    for (String rankingKey : rankingKeys) {
                        // 获取排行榜ID
                        String rankingId = rankingKey.substring(RANKING_KEY_PREFIX.length());

                        // 获取该排行榜的所有用户分数
                        Set<ZSetOperations.TypedTuple<String>> scores = redisTemplate.opsForZSet()
                                .rangeWithScores(rankingKey, 0, -1);

                        if (scores != null && !scores.isEmpty()) {
                            log.info("同步排行榜 {} 的数据，共 {} 条记录", rankingId, scores.size());

                            // 遍历所有用户分数，更新线段树
                            for (ZSetOperations.TypedTuple<String> tuple : scores) {
                                if (tuple.getValue() != null && tuple.getScore() != null) {
                                    double score = tuple.getScore();
                                    // 更新线段树中的计数
                                    updateSegmentTreeCount(score);
                                }
                            }
                        }
                    }
                    log.info("排行榜数据同步完成");
                }
                log.info("排行榜线段树初始化完成");
            } else {
                log.info("使用已存在的排行榜线段树数据");
            }
        } catch (Exception e) {
            log.error("初始化排行榜线段树失败", e);
            throw new RuntimeException("初始化排行榜线段树失败", e);
        }
    }

    /**
     * 更新线段树中的计数
     */
    private void updateSegmentTreeCount(double score) {
        try {
            // 获取该分数所在的区间
            List<SegmentTreeNode> segments = segmentTree.getSegmentsForScore(score);

            // 更新每个区间的计数
            for (SegmentTreeNode segment : segments) {
                String segmentKey = segment.getSegmentKey();
                redisTemplate.opsForHash().increment(SEGMENT_KEY_PREFIX + 1, segmentKey, 1);
            }
        } catch (Exception e) {
            log.error("更新线段树计数失败 - score: {}", score, e);
        }
    }

    /**
     * 更新用户分数
     */
    public void updateUserScore(long userId, double oldScore, double newScore) {
        try {
            // 更新线段树中的计数
            segmentTree.updateScore(oldScore, newScore);


            log.debug("更新用户分数成功 - userId: {}, oldScore: {}, newScore: {}",
                    userId, oldScore, newScore);
        } catch (Exception e) {
            log.error("更新用户分数失败 - userId: {}, oldScore: {}, newScore: {}",
                    userId, oldScore, newScore, e);
            throw new RuntimeException("更新用户分数失败", e);
        }
    }

    /**
     * 获取用户排名
     */
    public long getUserRank(double score) {
        try {
            long rank = segmentTree.getRank(score);
            log.debug("获取用户排名成功 - score: {}, rank: {}", score, rank);
            return rank;
        } catch (Exception e) {
            log.error("获取用户排名失败 - score: {}", score, e);
            throw new RuntimeException("获取用户排名失败", e);
        }
    }

    /**
     * 重置排行榜
     */
    public void resetRanking() {
        try {
            // 删除Redis中的线段树数据
            redisTemplate.delete(SEGMENT_KEY_PREFIX + 1);
            // 重新初始化线段树
            segmentTree.buildTree(MIN_SCORE, MAX_SCORE);
            log.info("排行榜重置完成");
        } catch (Exception e) {
            log.error("重置排行榜失败", e);
            throw new RuntimeException("重置排行榜失败", e);
        }
    }

    /**
     * 获取排行榜统计信息
     */
    public Map<String, Object> getRankingStats() {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(SEGMENT_KEY_PREFIX+1);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSegments", entries.size());
            stats.put("minScore", MIN_SCORE);
            stats.put("maxScore", MAX_SCORE);
            return stats;
        } catch (Exception e) {
            log.error("获取排行榜统计信息失败", e);
            throw new RuntimeException("获取排行榜统计信息失败", e);
        }
    }
}