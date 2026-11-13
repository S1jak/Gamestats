package com.lxw.gamestats;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * GameStats 启动入口。
 * 三个子模块包名根均为 com.lxw：common / pojo / server。
 * 由于 @SpringBootApplication 默认只扫描启动类所在包（com.lxw.gamestats）及子包，
 * 需要通过 @ComponentScan 显式扩大扫描范围至 com.lxw，使 game-common 中的
 * @Component（如 JwtProperties）能被 Spring 容器管理。
 * @MapperScan 显式扫描 mapper 接口所在包，确保 MyBatis 能注册代理实现。
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.lxw")
@MapperScan(basePackages = "com.lxw.gamestats.mapper")
public class GameStatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameStatsApplication.class, args);
    }

}
