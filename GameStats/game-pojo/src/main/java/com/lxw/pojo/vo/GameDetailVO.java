package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 游戏详情视图对象
 */
@Data
public class GameDetailVO implements Serializable {

    private Long id;

    private String name;

    private String cover;

    private String genre;

    private String developer;

    private LocalDate releaseDate;

    private BigDecimal rating;

    private Integer playTime;

    private Integer sessions;

    private LocalDate lastPlayed;

    private Integer achievements;

    private Integer totalAchievements;

    /**
     * 游戏状态：playing / completed / not_played / on_hold / dropped
     */
    private String status;

    private String note;

    private List<String> tags;

    /** 平台拥有情况 */
    private List<PlatformVO> platforms;

    /** 成就列表 */
    private List<AchievementVO> achievementList;
}
