package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 年度报告实体（对应 yearly_reports 表）
 */
@Data
public class YearlyReport implements Serializable {

    private Long id;

    private Long userId;

    private Integer year;

    private Long favoriteGameId;

    /** 总游玩时长（分钟） */
    private Integer totalPlayTime;

    /** 游玩风格 */
    private String playStyle;

    private Integer achievementsCount;

    private LocalDateTime createdAt;
}
