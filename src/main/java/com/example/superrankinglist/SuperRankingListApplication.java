package com.example.superrankinglist;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 超级排行榜应用启动类
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.example.superrankinglist.mapper")
public class SuperRankingListApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperRankingListApplication.class, args);
    }

}
