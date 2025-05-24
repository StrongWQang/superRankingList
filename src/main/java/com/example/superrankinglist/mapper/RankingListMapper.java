package com.example.superrankinglist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.superrankinglist.pojo.RankingList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排行榜数据访问接口
 */
@Mapper
public interface RankingListMapper extends BaseMapper<RankingList> {

    /**
     * 根据类型查询排行榜
     * @param type 排行榜类型
     * @return 排行榜列表
     */
    @Select("SELECT * FROM ranking_list WHERE type = #{type} AND status = 1 ORDER BY create_time DESC")
    List<RankingList> findByType(Integer type);

    /**
     * 查询当前有效的排行榜
     * @param now 当前时间
     * @return 排行榜列表
     */
    @Select("SELECT * FROM ranking_list WHERE status = 1 AND start_time <= #{now} AND end_time >= #{now} ORDER BY create_time DESC")
    List<RankingList> findActiveRankingLists(LocalDateTime now);

    /**
     * 更新排行榜状态
     * @param id 排行榜ID
     * @param status 新状态
     * @return 影响的行数
     */
    @Update("UPDATE ranking_list SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(Long id, Integer status);

    /**
     * 根据时间范围查询排行榜
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 排行榜列表
     */
    @Select("SELECT * FROM ranking_list WHERE start_time >= #{startTime} AND end_time <= #{endTime} ORDER BY create_time DESC")
    List<RankingList> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定状态的所有排行榜
     * @param status 状态
     * @return 排行榜列表
     */
    @Select("SELECT * FROM ranking_list WHERE status = #{status} ORDER BY create_time DESC")
    List<RankingList> findByStatus(Integer status);
}