package com.example.superrankinglist.common;


public class SegmentTreeNode {
    private double lower;  // 区间下界
    private double upper;  // 区间上界
    private long count;    // 该区间内的用户数量
    private SegmentTreeNode left;  // 左子节点
    private SegmentTreeNode right; // 右子节点

    public SegmentTreeNode(double lower, double upper) {
        this.lower = lower;
        this.upper = upper;
        this.count = 0;
    }

    // getter和setter方法
    public double getLower() { return lower; }
    public void setLower(double lower) { this.lower = lower; }
    public double getUpper() { return upper; }
    public void setUpper(double upper) { this.upper = upper; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public SegmentTreeNode getLeft() { return left; }
    public void setLeft(SegmentTreeNode left) { this.left = left; }
    public SegmentTreeNode getRight() { return right; }
    public void setRight(SegmentTreeNode right) { this.right = right; }

    // 获取区间标识，用于Redis Hash的field，保留4位小数
    public String getSegmentKey() {
        return String.format("%.4f-%.4f", lower, upper);
    }
}
