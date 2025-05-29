package com.example.superrankinglist.service.impl;

import com.example.superrankinglist.pojo.SegmentTreeNode;
import com.example.superrankinglist.service.SegmentTreeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.superrankinglist.common.RedisKey.RANKING_KEY_PREFIX;
import static com.example.superrankinglist.common.RedisKey.SEGMENT_KEY_PREFIX;

/**
 * 线段树服务实现类
 * 使用Redis Hash存储线段树数据，实现高效的排行榜粗估排名功能
 */
@Log4j2
@Service
public class SegmentTreeServiceImpl implements SegmentTreeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 更新区间计数的Lua脚本
     */
    private final DefaultRedisScript<Long> updateScoreScript;

    public SegmentTreeServiceImpl() {
        this.updateScoreScript = new DefaultRedisScript<>();
        this.updateScoreScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/update_segment.lua")));
        this.updateScoreScript.setResultType(Long.class);
    }

    @Override
    public Long getFuzzyRank(Long rankingListId, Double score) {
        String rankingKey = SEGMENT_KEY_PREFIX + rankingListId;
        // 从Redis构建线段树
        SegmentTreeNode root = buildSegmentTree(rankingKey);
        
        if (root == null) {
            return 0L;
        }

        // 使用线段树计算粗估排名
        return getRankFromSegmentTree(root, score);
    }

    @Override
    public void updateScore(Long rankingListId, Double oldScore, Double newScore) {
        // 参数验证
        if (rankingListId == null || oldScore == null || newScore == null) {
            log.error("更新分数失败：参数不能为空 - rankingListId: {}, oldScore: {}, newScore: {}", 
                rankingListId, oldScore, newScore);
            throw new IllegalArgumentException("参数不能为空");
        }

        String rankingKey = SEGMENT_KEY_PREFIX + rankingListId;
        log.info("开始更新排行榜分数 - rankingListId: {}, oldScore: {}, newScore: {}", 
            rankingListId, oldScore, newScore);
        try {
            // 检查并初始化线段树
            if(redisTemplate.keys(rankingKey).isEmpty()) {
                log.info("排行榜 {} 的线段树不存在，开始初始化", rankingListId);
                initSegmentTree(rankingListId, 0.0, 10000.0, 1000);
                log.info("排行榜 {} 的线段树初始化完成", rankingListId);
            }
            // 构建线段树
            SegmentTreeNode root = buildSegmentTree(rankingKey);
            if (root == null) {
                log.error("构建线段树失败 - rankingListId: {}", rankingListId);
                throw new RuntimeException("构建线段树失败");
            }
            // 获取旧分数路径
            List<SegmentTreeNode> oldPath = getPathSegments(root, oldScore);
            // 获取新分数路径
            List<SegmentTreeNode> newPath = getPathSegments(root, newScore);
            log.info("需要更新的区间 - 旧分数路径: {}, 新分数路径: {}", 
                oldPath.stream().map(SegmentTreeNode::getSegmentKey).collect(Collectors.toList()),
                newPath.stream().map(SegmentTreeNode::getSegmentKey).collect(Collectors.toList()));
            List<String> segmentsToUpdate = new ArrayList<>();
            oldPath.forEach(segment -> segmentsToUpdate.add(segment.getSegmentKey() + ":-1"));
            newPath.forEach(segment -> segmentsToUpdate.add(segment.getSegmentKey() + ":1"));
            if (segmentsToUpdate.isEmpty()) {
                log.warn("没有需要更新的区间 - rankingListId: {}, oldScore: {}, newScore: {}", 
                    rankingListId, oldScore, newScore);
                return;
            }
            log.info("准备更新区间计数 - 更新数据: {}", segmentsToUpdate);
            // 使用Lua脚本原子性地更新区间计数
            Long result = redisTemplate.execute(updateScoreScript, Arrays.asList(rankingKey), segmentsToUpdate.toArray());
            if (result == null || result != 1) {
                log.error("更新区间计数失败 - rankingListId: {}, result: {}", rankingListId, result);
                throw new RuntimeException("更新区间计数失败");
            }
            // 验证更新结果
            Map<Object, Object> updatedSegments = redisTemplate.opsForHash().entries(rankingKey);
            log.info("更新后的区间计数: {}", updatedSegments);
            log.info("成功更新排行榜分数 - rankingListId: {}, oldScore: {}, newScore: {}", 
                rankingListId, oldScore, newScore);
        } catch (Exception e) {
            log.error("更新排行榜分数失败 - rankingListId: {}, oldScore: {}, newScore: {}", 
                rankingListId, oldScore, newScore, e);
            throw new RuntimeException("更新排行榜分数失败", e);
        }
    }

    @Override
    public void initSegmentTree(Long rankingListId, Double minScore, Double maxScore, int segmentCount) {
        String rankingKey = SEGMENT_KEY_PREFIX + rankingListId;
        String zsetKey = RANKING_KEY_PREFIX + rankingListId;  // Redis ZSet的key

        // 构建分段线段树（如[1,100],[101,200]...）
        SegmentTreeNode root = buildSegmentTreeBySegments(minScore.intValue(), maxScore.intValue(), segmentCount);

        // 获取所有用户及其分数
        Set<Object> members = redisTemplate.opsForZSet().range(zsetKey, 0, -1);
        if (members == null || members.isEmpty()) {
            // 如果没有用户，初始化所有区间为0
            Map<String, Long> segmentMap = new HashMap<>();
            traverseTree(root, node -> segmentMap.put(node.getSegmentKey(), 0L));
            redisTemplate.opsForHash().putAll(rankingKey, segmentMap);
            return;
        }

        // 统计每个区间的用户数量
        Map<String, Long> segmentMap = new HashMap<>();
        traverseTree(root, node -> segmentMap.put(node.getSegmentKey(), 0L));  // 初始化所有区间为0

        for (Object member : members) {
            Double score = redisTemplate.opsForZSet().score(zsetKey, member);
            if (score != null) {
                // 找到分数所在的区间并增加计数
                List<SegmentTreeNode> segments = getSegmentToUpdate(root, score);
                for (SegmentTreeNode segment : segments) {
                    String segmentKey = segment.getSegmentKey();
                    segmentMap.put(segmentKey, segmentMap.get(segmentKey) + 1);
                }
            }
        }

        // 将统计结果保存到Redis
        redisTemplate.opsForHash().putAll(rankingKey, segmentMap);
        log.info("初始化线段树完成 - rankingListId: {}, 区间统计: {}", rankingListId, segmentMap);
    }

    /**
     * 从Redis数据构建线段树
     * @param rankingKey Redis中的key
     * @return 线段树根节点
     */
    private SegmentTreeNode buildSegmentTree(String rankingKey) {
        Map<Object, Object> segmentMap = redisTemplate.opsForHash().entries(rankingKey);
        if (segmentMap.isEmpty()) {
            return null;
        }

        // 构建线段树节点
        List<SegmentTreeNode> nodes = new ArrayList<>();
        segmentMap.forEach((key, value) -> {
            String[] range = key.toString().split("-");
            SegmentTreeNode node = new SegmentTreeNode(
                Double.parseDouble(range[0]), 
                Double.parseDouble(range[1])
            );
            node.setCount(Long.parseLong(value.toString()));
            nodes.add(node);
        });

        return buildTreeFromNodes(nodes);
    }

    /**
     * 构建分段线段树（如[1,100],[101,200]...）
     * @param minScore 最小积分
     * @param maxScore 最大积分
     * @param segmentCount 分段数
     * @return 线段树根节点
     */
    private SegmentTreeNode buildSegmentTreeBySegments(int minScore, int maxScore, int segmentCount) {
        int segmentLen = (maxScore - minScore + 1) / segmentCount;
        List<SegmentTreeNode> leaves = new ArrayList<>();
        for (int i = 0; i < segmentCount; i++) {
            int lower = minScore + i * segmentLen;
            int upper = (i == segmentCount - 1) ? maxScore : lower + segmentLen - 1;
            leaves.add(new SegmentTreeNode((double)lower, (double)upper));
        }
        return buildTreeFromLeaves(leaves, 0, leaves.size() - 1);
    }

    /**
     * 递归合并叶子节点，构建线段树
     */
    private SegmentTreeNode buildTreeFromLeaves(List<SegmentTreeNode> leaves, int l, int r) {
        if (l == r) return leaves.get(l);
        int mid = (l + r) / 2;
        SegmentTreeNode left = buildTreeFromLeaves(leaves, l, mid);
        SegmentTreeNode right = buildTreeFromLeaves(leaves, mid + 1, r);
        SegmentTreeNode parent = new SegmentTreeNode(left.getLower(), right.getUpper());
        parent.setLeft(left);
        parent.setRight(right);
        return parent;
    }


    /**
     * 从节点列表构建平衡树
     * @param nodes 节点列表
     * @return 平衡树的根节点
     */
    private SegmentTreeNode buildTreeFromNodes(List<SegmentTreeNode> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }

        // 按照区间下界排序
        nodes.sort((a, b) -> a.getLower().compareTo(b.getLower()));

        // 构建平衡树
        return buildBalancedTree(nodes, 0, nodes.size() - 1);
    }

    /**
     * 递归构建平衡树
     * @param nodes 节点列表
     * @param start 起始索引
     * @param end 结束索引
     * @return 子树根节点
     */
    private SegmentTreeNode buildBalancedTree(List<SegmentTreeNode> nodes, int start, int end) {
        if (start > end) {
            return null;
        }

        int mid = (start + end) / 2;
        SegmentTreeNode root = nodes.get(mid);
        
        // 递归构建左右子树
        root.setLeft(buildBalancedTree(nodes, start, mid - 1));
        root.setRight(buildBalancedTree(nodes, mid + 1, end));
        
        return root;
    }

    /**
     * 使用线段树计算积分对应的粗估排名
     * @param root 线段树根节点
     * @param score 用户积分
     * @return 粗估排名
     */
    private Long getRankFromSegmentTree(SegmentTreeNode root, Double score) {
        SegmentTreeNode currentNode = root;
        Long biggerThanMe = 0L;  // 记录大于当前积分的用户数量

        while (currentNode != null) {
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            if (currentNode.getLeft() == null) {
                // 找到积分所在区间，计算区间内排名
                double numerator = (currentNode.getUpper() - score) * currentNode.getCount();
                double fuzzyRank = numerator / (currentNode.getUpper() - currentNode.getLower() + 0.0001);
                return (long) fuzzyRank + biggerThanMe;
            }

            Double split = currentNode.getLeft().getUpper();
            if (score <= split) {
                // 积分在左子树，累加右子树的用户数量
                if (currentNode.getRight() != null) {
                    biggerThanMe += currentNode.getRight().getCount();
                }
                currentNode = currentNode.getLeft();
            } else {
                // 积分在右子树
                currentNode = currentNode.getRight();
            }
        }

        return 0L;
    }

    /**
     * 获取需要更新的区间节点
     * @param root 线段树根节点
     * @param score 用户积分
     * @return 需要更新的区间节点列表
     */
    private List<SegmentTreeNode> getSegmentToUpdate(SegmentTreeNode root, Double score) {
        List<SegmentTreeNode> segments = new ArrayList<>();
        if (root == null) {
            return segments;
        }

        SegmentTreeNode currentNode = root;
        while (currentNode != null) {
            // 如果分数不在当前节点范围内，退出循环
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            // 将当前节点加入结果列表
            segments.add(currentNode);

            // 如果是叶子节点，退出循环
            if (currentNode.getLeft() == null) {
                break;
            }

            // 使用左子节点的上界作为分割点
            Double split = currentNode.getLeft().getUpper();
            if (score <= split) {
                currentNode = currentNode.getLeft();
            } else {
                currentNode = currentNode.getRight();
            }
        }

        return segments;
    }

    /**
     * 遍历线段树
     * @param node 当前节点
     * @param action 对每个节点执行的操作
     */
    private void traverseTree(SegmentTreeNode node, java.util.function.Consumer<SegmentTreeNode> action) {
        if (node == null) {
            return;
        }

        action.accept(node);
        traverseTree(node.getLeft(), action);
        traverseTree(node.getRight(), action);
    }

    /**
     * 沿着score路径遍历线段树，收集所有经过的节点（区间判断采用左闭右闭）
     */
    private List<SegmentTreeNode> getPathSegments(SegmentTreeNode root, Double score) {
        List<SegmentTreeNode> path = new ArrayList<>();
        SegmentTreeNode currentNode = root;
        while (currentNode != null) {
            // 判断区间是否包含score（左闭右闭）
            if (currentNode.getLower() <= score && score <= currentNode.getUpper()) {
                path.add(currentNode);
                if (currentNode.getLeft() == null) break;
                Double split = currentNode.getLeft().getUpper();
                if (score <= split) {
                    currentNode = currentNode.getLeft();
                } else {
                    currentNode = currentNode.getRight();
                }
            } else {
                // 不在任何区间，直接break
                break;
            }
        }
        return path;
    }
} 