package com.example.superrankinglist.pojo;

import lombok.Data;

/**
 * 线段树节点类
 * 用于实现排行榜的粗估排名功能
 * 每个节点代表一个积分区间，并记录该区间内的用户数量
 */
@Data
public class SegmentTreeNode {
    /**
     * 区间下界，表示该节点所代表区间的最小积分值
     */
    private Double lower;

    /**
     * 区间上界，表示该节点所代表区间的最大积分值
     */
    private Double upper;

    /**
     * 该区间内的用户数量
     */
    private Long count;

    /**
     * 左子节点，代表区间[lower, left.upper]
     */
    private SegmentTreeNode left;

    /**
     * 右子节点，代表区间[right.lower, upper]
     */
    private SegmentTreeNode right;

    /**
     * 构造函数
     * @param lower 区间下界
     * @param upper 区间上界
     */
    public SegmentTreeNode(Double lower, Double upper) {
        this.lower = lower;
        this.upper = upper;
        this.count = 0L;  // 初始化用户数量为0
    }

    /**
     * 获取区间标识
     * @return 格式为"lower-upper"的字符串，用于Redis存储
     */
    public String getSegmentKey() {
        return String.format("%.4f-%.4f", lower, upper);
    }
} 