//package com.example.superrankinglist.service;
//
//import com.example.superrankinglist.utils.SegmentTreeUtil;
//import lombok.Getter;
//import lombok.extern.log4j.Log4j2;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import static com.example.superrankinglist.common.RedisKey.RANKING_KEY_PREFIX;
//import static com.example.superrankinglist.common.RedisKey.SEGMENT_KEY_PREFIX;
//
///**
// * 排行榜服务
// */
//@Log4j2
//@Service
//public class RankingService {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Autowired
//    private SegmentTreeUtil segmentTreeUtil;
//
//    /**
//     * -- GETTER --
//     *  获取线段树根节点
//     */
//    @Getter
//    @Autowired
//    private SegmentTreeNode segmentTreeRoot;
//
//    /**
//     * 更新用户积分
//     * @param userId 用户ID
//     * @param oldScore 旧积分
//     * @param newScore 新积分
//     * @param root 线段树根节点
//     */
//    public void updateScore(String userId, long oldScore, long newScore, SegmentTreeNode root) {
//        String rankingKey = RANKING_KEY_PREFIX + userId;
//
//        // 获取需要更新的节点
//        List<String> reduceSegments = segmentTreeUtil.getSegmentsToUpdate(root, oldScore);
//        List<String> addSegments = segmentTreeUtil.getSegmentsToUpdate(root, newScore);
//
//        // 构建Lua脚本参数
//        List<String> keys = new ArrayList<>();
//        keys.add(SEGMENT_KEY_PREFIX);
//        keys.addAll(reduceSegments);
//        keys.addAll(addSegments);
//
//        // Lua脚本：对每个传入的Field执行加1或减1操作
//        String script =
//            "local M = tonumber(ARGV[1]) " +
//            "local N = tonumber(ARGV[2]) " +
//            "for i=1,M,1 do " +
//            "    redis.call('HINCRBY', KEYS[1], KEYS[1+i], -1) " +
//            "end " +
//            "for i=1,N,1 do " +
//            "    redis.call('HINCRBY', KEYS[1], KEYS[1+M+i], 1) " +
//            "end " +
//            "return 0";
//
//        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
//
//        // 执行Lua脚本
//        redisTemplate.execute(redisScript, keys, reduceSegments.size(), addSegments.size());
//    }
//
//    /**
//     * 获取用户排名
//     * @param userId 用户ID
//     * @param score 用户积分
//     * @param root 线段树根节点
//     * @return 用户排名
//     */
//    public long getRank(String userId, long score, SegmentTreeNode root) {
//        // 获取需要读取的节点
//        SegmentTreeUtil.SegmentTreeReadResult readResult = segmentTreeUtil.getSegmentsToRead(root, score);
//
//        if (readResult.getSegmentFields().isEmpty()) {
//            return 0;
//        }
//
//        // 读取节点值
//        List<Object> values = redisTemplate.opsForHash().multiGet(SEGMENT_KEY_PREFIX, Collections.singleton(readResult.getSegmentFields()));
//
//        long segCounter = 0;
//        long biggerCounter = 0;
//
//        // 计算排名
//        for (int i = 0; i < values.size(); i++) {
//            String segmentKey = readResult.getSegmentFields().get(i);
//            Object value = values.get(i);
//
//            if (value == null) {
//                continue;
//            }
//
//            if (segmentKey.equals(readResult.getTargetSegment().getSegmentKey())) {
//                segCounter = Long.parseLong(value.toString());
//            } else {
//                biggerCounter += Long.parseLong(value.toString());
//            }
//        }
//
//        // 计算预估排名
//        SegmentTreeNode targetSegment = readResult.getTargetSegment();
//        double more = (double) ((targetSegment.getUpper() - score) * segCounter) /
//                     (targetSegment.getUpper() - targetSegment.getLower());
//
//        return biggerCounter + (long) more;
//    }
//}