package com.example.superrankinglist.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.superrankinglist.dto.RankingListQueryDto;
import com.example.superrankinglist.mapper.RankingItemMapper;
import com.example.superrankinglist.mapper.RankingListMapper;
import com.example.superrankinglist.mapper.UserMapper;
import com.example.superrankinglist.pojo.RankingItem;
import com.example.superrankinglist.pojo.RankingList;
import com.example.superrankinglist.pojo.User;
import com.example.superrankinglist.service.RankingListService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static com.example.superrankinglist.common.RedisKey.RANKING_KEY_PREFIX;


/**
 * 排行榜服务实现类
 */
@Log4j2
@Service
public class RankingListServiceImpl implements RankingListService {

    @Autowired
    private RankingItemMapper rankingItemMapper;

    @Autowired
    private RankingListMapper rankingListMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    private DefaultRedisScript<List> getUserRankScript;

    @PostConstruct
    public void init() {
        // 初始化Lua脚本
        getUserRankScript = new DefaultRedisScript<>();
        getUserRankScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/get_user_rank.lua")));
        getUserRankScript.setResultType(List.class);
    }

    @Override
    public Page<RankingItem> queryRankingList(RankingListQueryDto queryDto) {
        // 参数验证
        if (queryDto == null || queryDto.getRankingListId() == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }

        // 排行榜key
        String rankingKey = RANKING_KEY_PREFIX + queryDto.getRankingListId();
        log.info("查询排行榜，key: {}", rankingKey);

        try {
            // 计算分页参数
            long start;
            long end;
            if (queryDto.getPageNum() == 0) {
                // 不分页时，获取完整的排行榜数据
                start = 0;
                end = -1;
            } else {
                // 分页时，获取指定区间的数据
                start = (long) (queryDto.getPageNum() - 1) * queryDto.getPageSize();
                end = start + queryDto.getPageSize() - 1;
            }
            log.info("分页参数 - start: {}, end: {}", start, end);

            // 使用ZREVRANGE获取排行榜数据
            Set<ZSetOperations.TypedTuple<Object>> rankingData = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rankingKey, start, end);
            log.info("从Redis获取到的排行榜数据: {}", rankingData);

            if (rankingData == null || rankingData.isEmpty()) {
                log.info("Redis中没有找到排行榜数据");
                return new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            }

            // 获取总记录数
            Long total = redisTemplate.opsForZSet().size(rankingKey);
            log.info("排行榜总记录数: {}", total);

            // 转换数据
            List<RankingItem> items = new ArrayList<>();
            for (ZSetOperations.TypedTuple<Object> tuple : rankingData) {
                Object value = tuple.getValue();
                if (value == null) {
                    continue;
                }
                
                String userId = String.valueOf(value);
                Double score = tuple.getScore();
                
                // 获取用户在整个排行榜中的真实排名
                Long rank = redisTemplate.opsForZSet().reverseRank(rankingKey, Long.valueOf(userId));
                log.debug("查询用户排名 - userId: {}, rank: {}", userId, rank);
                
                // 只显示前10000名的数据
                if (rank != null && rank >= 10000) {
                    continue;
                }
                
                RankingItem item = new RankingItem();
                // 确保用户ID格式一致
                item.setUserId(Long.valueOf(userId));
                item.setScore(score != null ? score : 0.0);
                item.setRankingListId(queryDto.getRankingListId());
                // 排名从1开始，所以需要+1
                item.setRanking(rank != null ? rank + 1 : 0L);
                items.add(item);
            }

            // 创建分页对象
            Page<RankingItem> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            page.setRecords(items);
            // 限制总数为10000
            page.setTotal(Math.min(total != null ? total : 0, 10000));

            return page;
        } catch (Exception e) {
            log.error("查询排行榜失败", e);
            throw new RuntimeException("查询排行榜失败", e);
        }
    }

    /**
     * 获取用户的排名和积分
     * @param rankingListId 排行榜ID
     * @param userId 用户ID
     * @return 包含用户排名和积分的对象
     */
    public RankingItem getUserRankAndScore(Long rankingListId, Long userId) {
        if (rankingListId == null || userId == null) {
            throw new IllegalArgumentException("排行榜ID和用户ID不能为空");
        }

        String rankingKey = RANKING_KEY_PREFIX + rankingListId;
        log.info("获取用户排名和积分，key: {}, userId: {}", rankingKey, userId);

        try {
            // 执行Lua脚本，确保参数类型正确
            List<Object> result = redisTemplate.execute(
                getUserRankScript,
                Arrays.asList(rankingKey, userId.toString()),
                Collections.emptyList()
            );

            if (result == null || result.size() != 2) {
                log.warn("获取用户排名和积分失败，userId: {}", userId);
                return null;
            }

            // 解析结果
            Long rank = ((Number) result.get(0)).longValue();
            Double score = ((Number) result.get(1)).doubleValue();

            // 创建返回对象
            RankingItem item = new RankingItem();
            item.setUserId(userId);
            item.setRankingListId(rankingListId);
            item.setScore(score);
            // 排名从1开始，所以需要+1
            item.setRanking(rank >= 0 ? rank + 1 : 0L);

            return item;
        } catch (Exception e) {
            log.error("获取用户排名和积分失败", e);
            throw new RuntimeException("获取用户排名和积分失败", e);
        }
    }

    @Override
    @Transactional
    public RankingList createRankingList(RankingList rankingList) {
        rankingList.setCreateTime(LocalDateTime.now());
        rankingList.setUpdateTime(LocalDateTime.now());
        rankingListMapper.insert(rankingList);
        return rankingList;
    }

    @Override
    public RankingList getRankingListById(Long id) {
        return rankingListMapper.selectById(id);
    }

    @Override
    @Transactional
    public RankingList updateRankingList(RankingList rankingList) {
        rankingList.setUpdateTime(LocalDateTime.now());
        rankingListMapper.updateById(rankingList);
        return rankingList;
    }

    @Override
    @Transactional
    public RankingList updateStatus(Long id, Integer status) {
        rankingListMapper.updateStatus(id, status);
        return getRankingListById(id);
    }

    @Override
    public List<RankingList> getRankingListsByType(Integer type) {
        return rankingListMapper.findByType(type);
    }

    @Override
    public List<RankingList> getActiveRankingLists() {
        return rankingListMapper.findActiveRankingLists(LocalDateTime.now());
    }

    @Override
    public List<RankingList> getRankingListsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return rankingListMapper.findByTimeRange(startTime, endTime);
    }

    @Override
    public List<RankingList> getRankingListsByStatus(Integer status) {
        return rankingListMapper.findByStatus(status);
    }

    @Override
    @Transactional
    public void deleteRankingList(Long id) {
        rankingListMapper.deleteById(id);
    }
} 