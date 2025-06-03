package com.example.superrankinglist.service.impl;

import com.example.superrankinglist.pojo.RankingItem;
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
                initSegmentTree(rankingListId, 0.0, 1000000.0, 10000);
                log.info("排行榜 {} 的线段树初始化完成", rankingListId);
            }
            // 构建线段树
            SegmentTreeNode root = buildSegmentTree(rankingKey);
            log.info("构建的线段树: {}", root);
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
            
            // 构建更新参数列表
            List<String> segmentsToUpdate = new ArrayList<>();
            segmentsToUpdate.addAll(buildUpdateParams(oldPath, -1));
            segmentsToUpdate.addAll(buildUpdateParams(newPath, 1));
            
            if (segmentsToUpdate.isEmpty()) {
                log.warn("没有需要更新的区间 - rankingListId: {}, oldScore: {}, newScore: {}", 
                    rankingListId, oldScore, newScore);
                return;
            }
            
            log.debug("准备更新区间计数 - 更新数据: {}", segmentsToUpdate);
            
            // 确保所有参数都是字符串类型
            String[] args = segmentsToUpdate.toArray(new String[0]);
            
            try {
                // 使用Lua脚本原子性地更新区间计数
                Long result = redisTemplate.execute(
                    updateScoreScript, 
                    Collections.singletonList(rankingKey), 
                    (Object[]) args
                );
                
                if (result == null || result != 1) {
                    log.error("更新区间计数失败 - rankingListId: {}, result: {}", rankingListId, result);
                    throw new RuntimeException("更新区间计数失败");
                }
                
                // 验证更新结果
                Map<Object, Object> updatedSegments = redisTemplate.opsForHash().entries(rankingKey);
                log.debug("更新后的区间计数: {}", updatedSegments);
            } catch (Exception e) {
                log.error("执行Lua脚本失败 - rankingListId: {}, error: {}", rankingListId, e.getMessage());
                throw new RuntimeException("更新排行榜分数失败", e);
            }
            
            log.info("成功更新排行榜分数 - rankingListId: {}, oldScore: {}, newScore: {}", 
                rankingListId, oldScore, newScore);
        } catch (Exception e) {
            log.error("更新排行榜分数失败 - rankingListId: {}, oldScore: {}, newScore: {}", 
                rankingListId, oldScore, newScore, e);
            throw new RuntimeException("更新排行榜分数失败", e);
        }
    }

    // 根据区间的最大值和分段数目创建线段树
    public static SegmentTreeNode buildSegmentTree(long maxScore, long segCount) {
        // 计算每个分段的长度
        long segLen = maxScore / segCount;
        if (maxScore % segCount != 0) {
            segLen++;
        }

        java.util.List<SegmentTreeNode> parentLayerNodes = new java.util.ArrayList<>();
        java.util.List<SegmentTreeNode> currentLayerNodes = new java.util.ArrayList<>();

        // 创建各个分段
        for (long i = 1; i <= maxScore; i += segLen) {
            currentLayerNodes.add(new SegmentTreeNode((double) i, (double) (i + segLen - 1)));
        }

        // 循环构建完整的线段树
        while (currentLayerNodes.size() >= 2) {
            // 取出前两个节点
            SegmentTreeNode leftNode = currentLayerNodes.get(0);
            SegmentTreeNode rightNode = currentLayerNodes.get(1);
            currentLayerNodes = currentLayerNodes.subList(2, currentLayerNodes.size());

            // 创建父节点
            SegmentTreeNode parentNode = new SegmentTreeNode(leftNode.getLower(), rightNode.getUpper());
            parentNode.setLeft(leftNode);
            parentNode.setRight(rightNode);

            parentLayerNodes.add(parentNode);
        }

        // 如果currentLayerNodes为空，则说明某层节点已全部构建完成，需要到上一层继续构建
        if (currentLayerNodes.isEmpty()) {
            currentLayerNodes = parentLayerNodes;
            parentLayerNodes = new java.util.ArrayList<>();
        }

        // 最终currentLayerNodes的首个节点是根节点
        return currentLayerNodes.get(0);
    }

    @Override
    public void initSegmentTree(Long rankingListId, Double minScore, Double maxScore, int segmentCount) {
        String rankingKey = SEGMENT_KEY_PREFIX + rankingListId;
        String zsetKey = RANKING_KEY_PREFIX + rankingListId;  // Redis ZSet的key

        // 构建分段线段树（如[1,100],[101,200]...）
        SegmentTreeNode root = buildSegmentTree(maxScore.intValue(), segmentCount);

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
     * 构建分段线段树
     */
    private SegmentTreeNode buildSegmentTreeBySegments(double minScore, double maxScore, int segmentCount) {
        // 确保使用整数边界
        int min = (int)Math.floor(minScore);
        int max = (int)Math.floor(maxScore);
        return buildSegmentTreeRecursive(min, max);
    }

    /**
     * 递归构建线段树，按照指定的区间结构
     */
    private SegmentTreeNode buildSegmentTreeRecursive(int start, int end) {
        // 创建当前节点
        SegmentTreeNode node = new SegmentTreeNode((double)start, (double)end);
        
        // 计算当前区间长度
        int length = end - start + 1;
        
        // 如果区间长度小于等于100，则为叶子节点
        if (length <= 100) {
            return node;
        }
        
        if (length > 500) {
            // 第一层划分：[0,499] 和 [500,1000]
            int mid = start + 499;
            node.setLeft(buildSegmentTreeRecursive(start, mid));
            node.setRight(buildSegmentTreeRecursive(mid + 1, end));
        } 
        else if (length > 200) {
            // 第二层划分：确保每个子区间能被100整除
            int mid = start + ((length / 2) / 100) * 100 - 1;
            node.setLeft(buildSegmentTreeRecursive(start, mid));
            node.setRight(buildSegmentTreeRecursive(mid + 1, end));
        }
        else {
            // 第三层划分：每100一段
            int mid = start + 99;
            node.setLeft(buildSegmentTreeRecursive(start, mid));
            node.setRight(buildSegmentTreeRecursive(mid + 1, end));
        }
        
        return node;
    }

    /**
     * 从Redis数据构建线段树
     */
    private SegmentTreeNode buildSegmentTree(String rankingKey) {
        Map<Object, Object> segmentMap = redisTemplate.opsForHash().entries(rankingKey);
        if (segmentMap.isEmpty()) {
            return null;
        }

        // 找到最小和最大边界
        int minBound = Integer.MAX_VALUE;
        int maxBound = Integer.MIN_VALUE;
        
        // 解析所有区间，找到整体边界
        for (Object key : segmentMap.keySet()) {
            try {
                // 移除多余的引号和空格
                String keyStr = key.toString().trim().replaceAll("^\"|\"$", "");
                String[] range = keyStr.split("-");
                if (range.length != 2) {
                    log.warn("无效的区间格式: {}", key);
                    continue;
                }
                
                // 处理可能的小数点格式
                double lower = Double.parseDouble(range[0].trim());
                double upper = Double.parseDouble(range[1].trim());
                
                // 转换为整数
                int lowerInt = (int)Math.floor(lower);
                int upperInt = (int)Math.floor(upper);
                
                minBound = Math.min(minBound, lowerInt);
                maxBound = Math.max(maxBound, upperInt);
            } catch (NumberFormatException e) {
                log.error("解析区间边界失败: {}, error: {}", key, e.getMessage());
                continue;
            }
        }
        
        if (minBound == Integer.MAX_VALUE || maxBound == Integer.MIN_VALUE) {
            log.error("未找到有效的区间边界");
            return null;
        }
        
        // 构建树结构
        SegmentTreeNode root = buildSegmentTreeRecursive(minBound, maxBound);
        
        // 填充计数
        for (Map.Entry<Object, Object> entry : segmentMap.entrySet()) {
            try {
                // 移除多余的引号和空格
                String keyStr = entry.getKey().toString().trim().replaceAll("^\"|\"$", "");
                String[] range = keyStr.split("-");
                if (range.length != 2) {
                    continue;
                }
                
                // 处理可能的小数点格式
                double lower = Double.parseDouble(range[0].trim());
                double upper = Double.parseDouble(range[1].trim());
                
                // 转换为整数
                int lowerInt = (int)Math.floor(lower);
                int upperInt = (int)Math.floor(upper);
                
                // 处理值中可能的引号
                String valueStr = entry.getValue().toString().trim().replaceAll("^\"|\"$", "");
                long count = Long.parseLong(valueStr);
                
                // 找到对应节点并设置计数
                setNodeCount(root, lowerInt, upperInt, count);
            } catch (NumberFormatException e) {
                log.error("解析区间数据失败: key={}, value={}, error: {}", 
                    entry.getKey(), entry.getValue(), e.getMessage());
                continue;
            }
        }
        
        return root;
    }

    /**
     * 设置节点的计数
     */
    private void setNodeCount(SegmentTreeNode node, int targetLower, int targetUpper, long count) {
        if (node == null) return;
        
        // 使用Math.floor()去掉小数部分
        int nodeLower = (int)Math.floor(node.getLower());
        int nodeUpper = (int)Math.floor(node.getUpper());
        
        // 如果当前节点的区间完全匹配目标区间，设置计数
        if (nodeLower == targetLower && nodeUpper == targetUpper) {
            node.setCount(count);
            return;
        }
        
        // 否则递归设置子节点
        setNodeCount(node.getLeft(), targetLower, targetUpper, count);
        setNodeCount(node.getRight(), targetLower, targetUpper, count);
    }

    /**
     * 使用线段树计算积分对应的排名
     * @param root 线段树根节点
     * @param score 用户积分
     * @return 粗估排名
     */
    private Long getRankFromSegmentTree(SegmentTreeNode root, Double score) {
        SegmentTreeNode currentNode = root;
        Long biggerThanMe = 0L;  // 记录分数高于score的节点的用户数量总和

        while (currentNode != null) {
            // 如果当前节点的区间不包含score，退出循环
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            // 如果是叶子节点，计算在当前区间内的预估排名
            if (currentNode.getLeft() == null) {
                // 计算分数在当前区间内的相对位置
                double numerator = (currentNode.getUpper() - score) * currentNode.getCount();
                // 使用区间长度作为分母，加1避免除0
                double fuzzyRank = numerator / (currentNode.getUpper() - currentNode.getLower() + 1);
                // 返回当前区间的预估排名加上之前累积的更高分数区间的用户数量
                return (long) fuzzyRank + biggerThanMe;
            }

            // 使用左子节点的上界作为分割点
            Double split = currentNode.getLeft().getUpper();
            if (score <= split) {
                // 如果score在左子树范围内
                // 需要将右子树的用户数量加入到biggerThanMe中
                if (currentNode.getRight() != null) {
                    biggerThanMe += currentNode.getRight().getCount();
                }
                // 继续遍历左子树
                currentNode = currentNode.getLeft();
            } else {
                // 如果score在右子树范围内，继续遍历右子树
                currentNode = currentNode.getRight();
            }
        }

        return biggerThanMe;
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

    /**
     * 构建更新参数
     */
    private List<String> buildUpdateParams(List<SegmentTreeNode> path, int delta) {
        return path.stream().map(segment -> {
            String[] bounds = segment.getSegmentKey().split("-");
            // 使用4位小数点格式，与Redis中的格式保持一致
            return String.format("%.4f-%.4f:%d", 
                Double.parseDouble(bounds[0].trim()),
                Double.parseDouble(bounds[1].trim()),
                delta);
        }).collect(Collectors.toList());
    }
} 