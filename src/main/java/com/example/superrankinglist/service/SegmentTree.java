package com.example.superrankinglist.service;

import com.example.superrankinglist.common.SegmentTreeNode;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SegmentTree {
    private SegmentTreeNode root;
    private final String redisKey;  // Redis Hash的key
    private final RedisTemplate<String, String> redisTemplate;
    private static final int DECIMAL_PLACES = 4;  // 小数位数
    private static final int BASE_SEGMENT_SIZE = 100; // 基础区间大小

    public SegmentTree(String redisKey, RedisTemplate<String, String> redisTemplate) {
        this.redisKey = redisKey;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 构建线段树
     * @param minScore 最小积分
     * @param maxScore 最大积分
     */
    public void buildTree(double minScore, double maxScore) {
        // 确保边界是整数
        minScore = Math.floor(minScore);
        maxScore = Math.floor(maxScore);
        
        // 计算区间数量（确保是2的幂）
        int segmentCount = calculateSegmentCount(minScore, maxScore);
        root = buildTreeRecursive(minScore, maxScore, segmentCount);
        initializeRedisCounts();
    }

    /**
     * 计算区间数量
     * 确保区间数量是2的幂，并且每个区间大小是BASE_SEGMENT_SIZE的倍数
     */
    private int calculateSegmentCount(double minScore, double maxScore) {
        double range = maxScore - minScore;
        // 确保至少有2个区间
        int minSegments = 2;
        // 计算需要的区间数量（向上取整到最接近的2的幂）
        int count = Math.max(minSegments, (int) Math.ceil(range / BASE_SEGMENT_SIZE));
        // 确保是2的幂
        return (int) Math.pow(2, Math.ceil(Math.log(count) / Math.log(2)));
    }

    /**
     * 递归构建线段树
     */
    private SegmentTreeNode buildTreeRecursive(double lower, double upper, int remainingSegments) {
        // 确保边界是整数
        lower = Math.floor(lower);
        upper = Math.floor(upper);

        // 如果区间太小或剩余段数为1，直接返回叶子节点
        if (remainingSegments <= 1 || upper <= lower) {
            return new SegmentTreeNode(roundToDecimalPlaces(lower), roundToDecimalPlaces(upper));
        }

        // 计算区间大小
        double totalRange = upper - lower;
        if (totalRange < BASE_SEGMENT_SIZE) {
            return new SegmentTreeNode(roundToDecimalPlaces(lower), roundToDecimalPlaces(upper));
        }

        // 计算每个区间的大小（确保是BASE_SEGMENT_SIZE的倍数）
        double segmentSize = Math.max(BASE_SEGMENT_SIZE, 
            Math.ceil(totalRange / remainingSegments / BASE_SEGMENT_SIZE) * BASE_SEGMENT_SIZE);
        
        // 计算中间值
        double mid = lower + segmentSize;
        mid = Math.min(mid, upper - BASE_SEGMENT_SIZE); // 确保右区间至少有BASE_SEGMENT_SIZE大小
        mid = Math.floor(mid);

        // 如果中间值等于下界或上界，说明无法继续分割
        if (mid <= lower || mid >= upper) {
            return new SegmentTreeNode(roundToDecimalPlaces(lower), roundToDecimalPlaces(upper));
        }

        // 创建当前节点
        SegmentTreeNode node = new SegmentTreeNode(roundToDecimalPlaces(lower), roundToDecimalPlaces(upper));

        // 计算左右子树的区间数量
        int leftSegments = remainingSegments / 2;
        int rightSegments = remainingSegments - leftSegments;

        // 递归构建左右子树
        node.setLeft(buildTreeRecursive(lower, mid, leftSegments));
        node.setRight(buildTreeRecursive(mid, upper, rightSegments));

        return node;
    }

    /**
     * 将数字四舍五入到指定小数位数
     */
    private double roundToDecimalPlaces(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * 初始化Redis Hash中的所有区间计数
     */
    private void initializeRedisCounts() {
        Map<String, String> counts = new HashMap<>();
        traverseTree(root, node -> counts.put(node.getSegmentKey(), "0"));
        redisTemplate.opsForHash().putAll(redisKey, counts);
    }

    /**
     * 遍历线段树
     */
    private void traverseTree(SegmentTreeNode node, Consumer<SegmentTreeNode> action) {
        if (node == null) return;
        action.accept(node);
        traverseTree(node.getLeft(), action);
        traverseTree(node.getRight(), action);
    }

    /**
     * 更新用户积分
     * @param oldScore 旧积分
     * @param newScore 新积分
     */
    public void updateScore(double oldScore, double newScore) {
        oldScore = roundToDecimalPlaces(oldScore);
        newScore = roundToDecimalPlaces(newScore);

        // 遍历旧积分所在的区间，执行减1操作
        traverseScore(oldScore, node -> {
            String key = node.getSegmentKey();
            redisTemplate.opsForHash().increment(redisKey, key, -1);
        });

        // 遍历新积分所在的区间，执行加1操作
        traverseScore(newScore, node -> {
            String key = node.getSegmentKey();
            redisTemplate.opsForHash().increment(redisKey, key, 1);
        });
    }

    /**
     * 遍历指定积分所在的区间
     */
    private void traverseScore(double score, Consumer<SegmentTreeNode> action) {
        SegmentTreeNode currentNode = root;
        while (currentNode != null) {
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            action.accept(currentNode);

            if (currentNode.getLeft() == null) {
                break;
            }

            double split = currentNode.getLeft().getUpper();
            if (score <= split) {
                currentNode = currentNode.getLeft();
            } else {
                currentNode = currentNode.getRight();
            }
        }
    }

    /**
     * 获取用户排名
     * @param score 用户积分
     * @return 排名
     */
    public long getRank(double score) {
        score = roundToDecimalPlaces(score);
        BigDecimal rank = BigDecimal.ZERO;
        SegmentTreeNode currentNode = root;

        while (currentNode != null) {
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            // 获取当前区间的用户数量
            String countStr = (String) redisTemplate.opsForHash().get(redisKey, currentNode.getSegmentKey());
            long count = countStr != null ? Long.parseLong(countStr) : 0;

            if (currentNode.getLeft() == null) {
                // 叶子节点，计算精确排名
                BigDecimal upper = BigDecimal.valueOf(currentNode.getUpper());
                BigDecimal lower = BigDecimal.valueOf(currentNode.getLower());
                BigDecimal currentScore = BigDecimal.valueOf(score);

                // 使用BigDecimal进行精确计算
                BigDecimal numerator = upper.subtract(currentScore)
                        .multiply(BigDecimal.valueOf(count));
                BigDecimal denominator = upper.subtract(lower).add(BigDecimal.ONE);

                rank = rank.add(numerator.divide(denominator, DECIMAL_PLACES, RoundingMode.HALF_UP));
                break;
            }

            double split = currentNode.getLeft().getUpper();
            if (score <= split) {
                // 如果右子树存在，加上右子树的用户数量
                if (currentNode.getRight() != null) {
                    String rightCountStr = (String) redisTemplate.opsForHash()
                            .get(redisKey, currentNode.getRight().getSegmentKey());
                    if (rightCountStr != null) {
                        rank = rank.add(BigDecimal.valueOf(Long.parseLong(rightCountStr)));
                    }
                }
                currentNode = currentNode.getLeft();
            } else {
                currentNode = currentNode.getRight();
            }
        }

        return rank.longValue();
    }

    /**
     * 获取指定分数所在的所有区间节点
     * @param score 用户积分
     * @return 包含该分数的所有区间节点列表
     */
    public List<SegmentTreeNode> getSegmentsForScore(double score) {
        score = roundToDecimalPlaces(score);
        List<SegmentTreeNode> segments = new ArrayList<>();
        SegmentTreeNode currentNode = root;

        while (currentNode != null) {
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            // 将当前节点添加到结果列表
            segments.add(currentNode);

            if (currentNode.getLeft() == null) {
                break;
            }

            double split = currentNode.getLeft().getUpper();
            if (score <= split) {
                currentNode = currentNode.getLeft();
            } else {
                currentNode = currentNode.getRight();
            }
        }

        return segments;
    }
}