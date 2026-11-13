package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游戏实体（对应 games 表，全局游戏目录）
 */
@Data
public class Game implements Serializable {

    private Long id;

    /** 游戏名称 */
    private String name;

    /** 封面 URL */
    private String cover;

    /** 游戏描述 */
    private String description;

    /** 发行日期 */
    private LocalDate releaseDate;

    /** 开发商 */
    private String developer;

    /** 评分 */
    private BigDecimal rating;

    /** 来源平台 */
    private String platform;

    /** 平台内游戏 ID */
    private String platformGameId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
