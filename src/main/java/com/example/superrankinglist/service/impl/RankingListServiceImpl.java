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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.superrankinglist.common.RedisKey.LIKE_KEY_PREFIX;

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

    @Override
    public Page<RankingItem> queryRankingList(RankingListQueryDto queryDto) {
        // 参数验证
        if (queryDto == null || queryDto.getRankingListId() == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }

        // 排行榜key
        String rankingKey = LIKE_KEY_PREFIX + queryDto.getRankingListId();

        // 计算分页参数
        int startIndex = (queryDto.getPageNum() - 1) * queryDto.getPageSize();
        int endIndex = startIndex + queryDto.getPageSize() - 1;

        try {
            // 使用ZREVRANGE获取排行榜数据
            Set<ZSetOperations.TypedTuple<Object>> rankingData = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rankingKey, startIndex, endIndex);

            if (rankingData == null || rankingData.isEmpty()) {
                return new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            }

            // 获取总记录数
            Long total = redisTemplate.opsForZSet().size(rankingKey);

            // 收集所有用户ID
            List<String> userIds = rankingData.stream()
                    .map(tuple -> {
                        Object value = tuple.getValue();
                        return value instanceof String ? (String) value : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 批量查询用户信息
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                    .stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            // 转换数据
            List<RankingItem> items = rankingData.stream()
                    .map(tuple -> {
                        Object value = tuple.getValue();
                        if (!(value instanceof String)) {
                            return null;
                        }
                        String userId = (String) value;
                        Double score = tuple.getScore();

                        User user = userMap.get(Long.valueOf(userId));
                        if (user == null) {
                            log.warn("用户不存在: {}", userId);
                            return null;
                        }

                        RankingItem item = new RankingItem();
                        item.setUserId(user.getId());
                        item.setScore(score != null ? score : 0.0);
                        item.setRankingListId(queryDto.getRankingListId());
                        //item.setUserName(user.getUsername()); // 假设需要显示用户名

                        return item;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 创建分页对象
            Page<RankingItem> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            page.setRecords(items);
            page.setTotal(total != null ? total : 0);

            return page;
        } catch (Exception e) {
            log.error("查询排行榜失败", e);
            throw new RuntimeException("查询排行榜失败", e);
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