package com.example.superrankinglist.utils;

import com.example.superrankinglist.pojo.SegmentTreeNode;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 线段树工具类
 */
@Component
public class SegmentTreeUtil {

    /**
     * 构建线段树
     * @param min 最小值
     * @param max 最大值
     * @param segments 分段数
     * @return 线段树根节点
     */
    public SegmentTreeNode buildSegmentTree(double min, double max, int segments) {
        if (segments <= 0) {
            throw new IllegalArgumentException("分段数必须大于0");
        }
        return buildSegmentTreeBySegments(min, max, segments);
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
        SegmentTreeNode node = new SegmentTreeNode((double) start, (double) end);
        
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
     * 获取需要更新的节点
     * @param root 线段树根节点
     * @param score 积分
     * @return 需要更新的节点列表
     */
    public List<String> getSegmentsToUpdate(SegmentTreeNode root, double score) {
        List<String> segmentFields = new ArrayList<>();
        SegmentTreeNode currentNode = root;

        while (currentNode != null) {
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            segmentFields.add(currentNode.getSegmentKey());

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

        return segmentFields;
    }

    /**
     * 获取需要读取的节点
     * @param root 线段树根节点
     * @param score 积分
     * @return 需要读取的节点列表和目标节点
     */
    public SegmentTreeReadResult getSegmentsToRead(SegmentTreeNode root, double score) {
        List<String> segmentFields = new ArrayList<>();
        SegmentTreeNode targetSegment = null;
        SegmentTreeNode currentNode = root;

        while (currentNode != null) {
            if (currentNode.getLower() > score || currentNode.getUpper() < score) {
                break;
            }

            if (currentNode.getLeft() == null) {
                targetSegment = currentNode;
                segmentFields.add(currentNode.getSegmentKey());
                break;
            }

            double split = currentNode.getLeft().getUpper();
            if (score <= split) {
                if (currentNode.getRight() != null) {
                    segmentFields.add(currentNode.getRight().getSegmentKey());
                }
                currentNode = currentNode.getLeft();
            } else {
                currentNode = currentNode.getRight();
            }
        }

        return new SegmentTreeReadResult(segmentFields, targetSegment);
    }

    /**
     * 线段树读取结果
     */
    @Data
    public static class SegmentTreeReadResult {
        private final List<String> segmentFields;
        private final SegmentTreeNode targetSegment;

        public SegmentTreeReadResult(List<String> segmentFields, SegmentTreeNode targetSegment) {
            this.segmentFields = segmentFields;
            this.targetSegment = targetSegment;
        }
    }
} 