//package com.example.superrankinglist.config;
//
//import com.example.superrankinglist.utils.SegmentTreeUtil;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 线段树配置
// */
//@Configuration
//public class SegmentTreeConfig {
//
//    @Autowired
//    private SegmentTreeUtil segmentTreeUtil;
//
//    /**
//     * 创建线段树根节点
//     * 这里设置积分范围为0-1000000，分为1000个区间
//     */
//    @Bean
//    public SegmentTreeNode segmentTreeRoot() {
//        return segmentTreeUtil.buildSegmentTree(0.0, 1000000.0, 1000);
//    }
//}