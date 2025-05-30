package com.example.superrankinglist.task;

import com.example.superrankinglist.config.RankingConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.example.superrankinglist.common.RedisKey.RANKING_KEY_PREFIX;

/**
 * 排行榜清理任务
 * 定期清理排行榜中排名靠后的数据
 */
@Log4j2
@Component
public class RankingListCleanupTask {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RankingConfig rankingConfig;
    
    /**
     * 定时清理任务
     * cron表达式可在配置文件中修改
     */
    @Scheduled(cron = "#{@rankingConfig.cleanupCron}")
    public void cleanupRankingList() {
        log.info("开始执行排行榜清理任务，保留前{}名", rankingConfig.getKeepTopN());
        
        try {
            // 获取所有排行榜的key
            Set<String> rankingKeys = redisTemplate.keys(RANKING_KEY_PREFIX + "*");
            if (rankingKeys == null || rankingKeys.isEmpty()) {
                log.info("没有找到需要清理的排行榜");
                return;
            }
            
            for (String rankingKey : rankingKeys) {
                try {
                    // 获取排行榜总人数
                    Long totalCount = redisTemplate.opsForZSet().size(rankingKey);
                    if (totalCount == null || totalCount <= rankingConfig.getKeepTopN()) {
                        log.info("排行榜 {} 人数未超过限制({}人)，无需清理", 
                            rankingKey, rankingConfig.getKeepTopN());
                        continue;
                    }
                    
                    // 计算需要删除的数量
                    long removeCount = totalCount - rankingConfig.getKeepTopN();
                    
                    // 获取需要删除的成员（分数从低到高，保留前N名）
                    Set<Object> membersToRemove = redisTemplate.opsForZSet()
                        .range(rankingKey, 0, removeCount - 1);
                        
                    if (membersToRemove != null && !membersToRemove.isEmpty()) {
                        // 删除这些成员
                        Long removedCount = redisTemplate.opsForZSet()
                            .remove(rankingKey, membersToRemove.toArray());
                            
                        log.info("排行榜 {} 清理完成，共删除 {} 条数据，现保留 {} 条数据", 
                            rankingKey, removedCount, rankingConfig.getKeepTopN());
                    }
                } catch (Exception e) {
                    log.error("清理排行榜 {} 时发生错误", rankingKey, e);
                }
            }
            
            log.info("排行榜清理任务执行完成");
        } catch (Exception e) {
            log.error("执行排行榜清理任务时发生错误", e);
        }
    }
} 