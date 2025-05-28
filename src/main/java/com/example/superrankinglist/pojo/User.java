package com.example.superrankinglist.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 用户实体类
 * 用于存储用户的基本信息和分数数据
 * 支持高并发场景下的线程安全操作
 */
@Data
@TableName("user")
public class User {
    /**
     * 用户ID，主键
     * 使用AtomicLong确保ID生成的线程安全
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，用于登录和显示
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户昵称，用于显示
     */
    private String nickname;

    /**
     * 用户头像URL
     * 使用volatile确保多线程下的可见性
     */
    private String avatar;

    /**
     * 用户总分数
     * 用于计算用户在排行榜中的位置
     */
    private Double score;

    /**
     * 用户创建时间
     * 使用volatile确保多线程下的可见性
     */
    private volatile LocalDateTime createTime;

    /**
     * 用户信息最后更新时间
     * 使用volatile确保多线程下的可见性
     */
    private volatile LocalDateTime updateTime;

    /**
     * 盐值，用于密码加密
     */
    private String salt;
} 