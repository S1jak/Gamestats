package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 年度游戏报告视图对象
 */
@Data
public class YearReportVO implements Serializable {

    private Integer year;

    /** 当年总游戏时长（小时） */
    private Integer totalHours;

    /** 游戏库总游戏数 */
    private Integer totalGames;

    /** 成就解锁数 */
    private Integer achievements;

    /** 通关数 */
    private Integer completedGames;

    private FavoriteGameVO favoriteGame;

    /** 玩家标签 */
    private List<String> tags;

    /** 游戏类型分布 */
    private List<GenreStatVO> genres;

    /** 月度游玩趋势（12 个月） */
    private List<MonthlyTrendVO> monthlyTrend;

    /** 游玩风格 */
    private String playStyle;

    @Data
    public static class FavoriteGameVO implements Serializable {
        private Long id;
        private String name;
        private String cover;
        /** 游玩时长（小时） */
        private Integer hours;
    }

    @Data
    public static class GenreStatVO implements Serializable {
        private String name;
        private Integer percent;
        private String color;
    }

    @Data
    public static class MonthlyTrendVO implements Serializable {
        private Integer month;
        private Integer hours;
    }
}
