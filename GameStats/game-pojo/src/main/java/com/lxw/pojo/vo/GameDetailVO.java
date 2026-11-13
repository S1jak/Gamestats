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
     * 游戏状态：在玩 / 已完成 / 未玩 / 搁置 / 弃坑
     */
    private String status;

    private String note;

    /** 用户自定义标签（来自 user_game_tags，空为 []，不为 null） */
    private List<String> tags;

    /** 游戏全部类型名（按 game_genres.id 升序，空为 []） */
    private List<String> genres;

    /** 平台拥有情况 */
    private List<PlatformVO> platforms;

    /** 成就列表 */
    private List<AchievementVO> achievementList;
}
