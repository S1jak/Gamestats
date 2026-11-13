package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 游戏列表项视图对象
 */
@Data
public class GameVO implements Serializable {

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

    /** 自定义标签 */
    private List<String> tags;
}
