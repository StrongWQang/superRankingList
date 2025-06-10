package com;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SegmentTree {
    // 线段树节点结构
    static class SegmentTreeNode {
        long lower;  // 区间左值
        long upper;  // 区间右值
        SegmentTreeNode left;  // 左子节点
        SegmentTreeNode right; // 右子节点
        long count;  // 位于此区间内的数据数量

        public SegmentTreeNode(long lower, long upper) {
            this.lower = lower;
            this.upper = upper;
            this.count = 0;
        }
    }

    // 根据区间的最大值和分段数目创建线段树
    public static SegmentTreeNode buildSegmentTree(long maxScore, long segCount) {
        // 计算每个分段的长度
        long segLen = maxScore / segCount;
        if (maxScore % segCount != 0) {
            segLen++;
        }

        List<SegmentTreeNode> parentLayerNodes = new ArrayList<>();
        List<SegmentTreeNode> currentLayerNodes = new ArrayList<>();

        // 创建各个分段，作为叶节点
        for (long i = 1; i <= maxScore; i += segLen) {
            currentLayerNodes.add(new SegmentTreeNode(i, Math.min(i + segLen - 1, maxScore)));
        }

        // 循环构建完整的线段树
        while (currentLayerNodes.size() >= 2) {
            // 取出前两个节点
            SegmentTreeNode leftNode = currentLayerNodes.get(0);
            SegmentTreeNode rightNode = currentLayerNodes.get(1);
            currentLayerNodes = currentLayerNodes.subList(2, currentLayerNodes.size());

            // 创建父节点，合并数据区间
            SegmentTreeNode parentNode = new SegmentTreeNode(leftNode.lower, rightNode.upper);
            parentNode.left = leftNode;
            parentNode.right = rightNode;

            parentLayerNodes.add(parentNode);

            // 如果当前层节点已全部构建完成，需要到上一层继续构建
            if (currentLayerNodes.isEmpty()) {
                currentLayerNodes = parentLayerNodes;
                parentLayerNodes = new ArrayList<>();
            }
        }

        // 最终currentLayerNodes的首个节点是根节点
        return currentLayerNodes.get(0);
    }

    // 在线段树中查询某积分对应的排名
    public static long getRankFromSegmentTree(SegmentTreeNode root, long score) {
        SegmentTreeNode currentNode = root;
        // 记录分数高于score的节点的用户数量总和
        long biggerThanMe = 0;

        while (currentNode != null) {
            if (currentNode.lower > score || currentNode.upper < score) {
                break;
            }
            if (currentNode.left == null) {
                // 查询到分段了，开始预估score在此分段内的排名
                long numerator = (currentNode.upper - score) * currentNode.count;
                double fuzzyRank = (double) numerator / (currentNode.upper - currentNode.lower + 1);
                // 在此分段内将排名与biggerThanMe求和得到最终排名
                return (long) fuzzyRank + biggerThanMe;
            }
            // 在向下遍历时，以左子节点的右值进行划分
            long split = currentNode.left.upper;
            if (score <= split) {
                // score在左子节点范围内，接下来遍历左子节点
                // 右子节点的数据范围大于score，需要累加到biggerThanMe中
                SegmentTreeNode right = currentNode.right;
                biggerThanMe += right.count;
                currentNode = currentNode.left;
            } else {
                // score在右子节点范围内，接下来遍历右子节点
                currentNode = currentNode.right;
            }
        }
        return 0;
    }

    // 遍历线段树，并修改遍历过的节点
    private static void traverseSegmentTree(SegmentTreeNode root, long score, java.util.function.Consumer<SegmentTreeNode> f) {
        SegmentTreeNode currentNode = root;
        while (currentNode != null) {
            if (currentNode.lower > score || currentNode.upper < score) {
                break;
            }
            // 对遍历过的节点进行修改
            f.accept(currentNode);
            // 遍历结束
            if (currentNode.left == null) {
                break;
            }
            // 向下遍历
            long split = currentNode.left.upper;
            if (score <= split) {
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }
        }
    }

    // 从线段树的根节点开始查询某积分对应的排名
    public static void updateRankInSegmentTree(SegmentTreeNode root, long score1, long score2) {
        // 遍历 score1，执行删除操作
        traverseSegmentTree(root, score1, node -> node.count--);
        // 遍历 score2，执行插入操作
        traverseSegmentTree(root, score2, node -> node.count++);
    }

    // 获取线段树中需要更新的节点
    public static List<String> getSegmentToUpdate(SegmentTreeNode root, long score) {
        List<String> segmentFields = new ArrayList<>();
        SegmentTreeNode currentNode = root;

        while (currentNode != null) {
            if (currentNode.lower > score || currentNode.upper < score) {
                break;
            }
            // 将遍历过的节点加入数组中
            segmentFields.add(String.format("%d-%d", currentNode.lower, currentNode.upper));
            if (currentNode.left == null) {
                break;
            }
            long split = currentNode.left.upper;
            if (score <= split) {
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }
        }
        return segmentFields;
    }

    // 在查询score排名时，计算需要在线段树中读取哪些节点
    public static class SegmentReadResult {
        public List<String> segmentFields;
        public SegmentTreeNode segment;

        public SegmentReadResult(List<String> segmentFields, SegmentTreeNode segment) {
            this.segmentFields = segmentFields;
            this.segment = segment;
        }
    }

    public static SegmentReadResult getSegmentToRead(SegmentTreeNode root, long score) {
        List<String> segmentFields = new ArrayList<>();
        SegmentTreeNode currentNode = root;
        SegmentTreeNode targetSegment = null;

        while (currentNode != null) {
            if (currentNode.lower > score || currentNode.upper < score) {
                break;
            }
            if (currentNode.left == null) {
                // currentNode就是score所在的分段
                targetSegment = currentNode;
                // 将Field加入segmentFields中
                segmentFields.add(String.format("%d-%d", currentNode.lower, currentNode.upper));
                break;
            }
            long split = currentNode.left.upper;
            if (score <= split) {
                SegmentTreeNode right = currentNode.right;
                // 当选择左子节点遍历时，需要查询右子节点，于是将Field加入segmentFields中
                segmentFields.add(String.format("%d-%d", right.lower, right.upper));
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }
        }
        return new SegmentReadResult(segmentFields, targetSegment);
    }

    // 更新排行榜积分
    public static void updateScore(SegmentTreeNode root, long score1, long score2, RedisClient redisClient) {
        try {
            // 获取用户数量需要减1的节点列表
            List<String> reduces = getSegmentToUpdate(root, score1);
            // 获取用户数量需要加1的节点列表
            List<String> adds = getSegmentToUpdate(root, score2);

            // Lua脚本：在Hash中对每个传入的Field执行加1或减1操作
            String script = 
                "local M = tonumber(ARGV[1]) " +
                "local N = tonumber(ARGV[2]) " +
                "for i=1,M,1 do " +
                "    redis.call('HINCRBY', KEYS[1], KEYS[1+i], -1) " +
                "end " +
                "for i=1,N,1 do " +
                "    redis.call('HINCRBY', KEYS[1], KEYS[1+M+i], 1) " +
                "end " +
                "return 0";

            List<String> keys = new ArrayList<>();
            keys.add("fuzzy_rank");
            // 将需要减1的节点Field依次加入keys中
            keys.addAll(reduces);
            // 将需要加1的节点Field依次加入keys中
            keys.addAll(adds);

            // 将待减1的Field个数、待加1的Field个数作为ARGV，执行脚本
            redisClient.eval(script, keys, List.of(String.valueOf(reduces.size()), String.valueOf(adds.size())));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update score in Redis", e);
        }
    }

    // 获取用户排名
    public static long getRank(SegmentTreeNode root, long score, RedisClient redisClient) {
        try {
            // 需要读取哪些节点
            SegmentReadResult readResult = getSegmentToRead(root, score);
            if (readResult.segmentFields.isEmpty() || readResult.segment == null) {
                return 0;
            }

            // 读取这些节点对应的Field值
            Map<String, String> result = redisClient.hmget("fuzzy_rank", readResult.segmentFields);

            // segCounter表示score在所在分段内的排名
            // biggerCounter表示大于score的用户总数
            long segCounter = 0;
            long biggerCounter = 0;

            for (Map.Entry<String, String> entry : result.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null) continue;

                // 找到score所在的分段
                if (key.equals(String.format("%d-%d", readResult.segment.lower, readResult.segment.upper))) {
                    segCounter = Long.parseLong(value);
                } else {
                    biggerCounter += Long.parseLong(value);
                }
            }

            // 使用BigDecimal进行精确计算
            BigDecimal numerator = BigDecimal.valueOf(readResult.segment.upper - score)
                .multiply(BigDecimal.valueOf(segCounter));
            BigDecimal denominator = BigDecimal.valueOf(readResult.segment.upper - readResult.segment.lower + 1);
            BigDecimal more = numerator.divide(denominator, 10, RoundingMode.HALF_UP);

            // 得到最终排名
            return biggerCounter + more.longValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get rank from Redis", e);
        }
    }
}

// Redis客户端接口
interface RedisClient {
    void eval(String script, List<String> keys, List<String> args) throws Exception;
    Map<String, String> hmget(String key, List<String> fields) throws Exception;
} 