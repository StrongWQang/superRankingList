package com.example.superrankinglist.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 排行榜配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "ranking")
public class RankingConfig {
    
    /**
     * 保留排行榜前N名
     */
    private Integer keepTopN = 10000; // 默认值10000

    /**
     * 清理任务的cron表达式
     * 默认每天凌晨2点执行
     */
    private String cleanupCron = "0 0 2 * * ?";
} 