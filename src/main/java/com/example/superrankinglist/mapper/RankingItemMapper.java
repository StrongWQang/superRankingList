package com.example.superrankinglist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.superrankinglist.pojo.RankingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 排行榜项目Mapper接口
 */
@Mapper
public interface RankingItemMapper extends BaseMapper<RankingItem> {
    
    /**
     * 根据排行榜ID查询排行榜项目，并关联用户信息
     * @param rankingListId 排行榜ID
     * @return 排行榜项目列表
     */
    @Select("SELECT ri.*, u.id as user_id, u.username, u.nickname, u.avatar, u.score as user_score " +
            "FROM ranking_item ri " +
            "LEFT JOIN user u ON ri.user_id = u.id " +
            "WHERE ri.ranking_list_id = #{rankingListId} " +
            "ORDER BY ri.score DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "rankingListId", column = "ranking_list_id"),
        @Result(property = "score", column = "score"),
        @Result(property = "ranking", column = "ranking"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "user.id", column = "user_id"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.nickname", column = "nickname"),
        @Result(property = "user.avatar", column = "avatar"),
        @Result(property = "user.score", column = "user_score")
    })
    List<RankingItem> selectWithUserByRankingListId(Long rankingListId);
} 