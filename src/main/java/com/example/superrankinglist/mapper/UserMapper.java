package com.example.superrankinglist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.superrankinglist.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户数据访问接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
    
    /**
     * 更新用户分数
     * @param userId 用户ID
     * @param score 新的分数
     * @return 影响的行数
     */
    @Update("UPDATE user SET score = #{score}, update_time = NOW() WHERE id = #{userId}")
    int updateScore(Long userId, Double score);
    
    /**
     * 获取分数最高的前N个用户
     * @param limit 限制数量
     * @return 用户列表
     */
    @Select("SELECT * FROM user ORDER BY score DESC LIMIT #{limit}")
    List<User> findTopUsers(int limit);
    
    /**
     * 根据分数范围查询用户
     * @param minScore 最低分数
     * @param maxScore 最高分数
     * @return 用户列表
     */
    @Select("SELECT * FROM user WHERE score BETWEEN #{minScore} AND #{maxScore} ORDER BY score DESC")
    List<User> findByScoreRange(Double minScore, Double maxScore);
} 