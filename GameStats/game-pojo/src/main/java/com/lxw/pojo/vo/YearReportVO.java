package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 年度游戏报告视图对象。
 * <p>
 * 所有指标均只统计所选自然年（D1 严格按年）：无数据的年份返回 0 / 空数组 / null，
 * 不得回退其他年份或生涯累计值。
 */
@Data
public class YearReportVO implements Serializable {

    /** 所选自然年 */
    private Integer year;

    /** 本年度总游戏时长（小时，由年度游玩分钟整体四舍五入） */
    private Integer totalHours;

    /** 本年度有游玩记录的不同游戏数（不等于游戏库总数） */
    private Integer totalGames;

    /** 本年度解锁的成就数（按 unlocked_at） */
    private Integer achievements;

    /** 本年度通关数（按 completed_at） */
    private Integer completedGames;

    /** 本年度最爱游戏（该年游玩时长最高者，无记录时为 null） */
    private FavoriteGameVO favoriteGame;

    /** 基于本年度指标生成的玩家标签 */
    private List<String> tags;

    /** 本年度游戏类型分布（主类型 + 其他，percent 合计 100） */
    private List<GenreStatVO> genres;

    /** 本年度月度游玩趋势（固定 12 个月，缺失补 0） */
    private List<MonthlyTrendVO> monthlyTrend;

    /** 本年度游玩风格 */
    private String playStyle;

    /** 报告来源：snapshot（命中有效快照）/ realtime（实时计算） */
    private String snapshotSource;

    /** 快照生成时间（yyyy-MM-dd HH:mm:ss）；实时来源时为 null */
    private String snapshotGeneratedAt;

    @Data
    public static class FavoriteGameVO implements Serializable {
        private Long id;
        private String name;
        private String cover;
        /** 本年度游玩时长（小时） */
        private Integer hours;
    }

    @Data
    public static class GenreStatVO implements Serializable {
        /** 类型名称（未归类游戏归入"其他"） */
        private String name;
        /** 本年度占比（百分比，两位小数，合计 100%） */
        private Double percent;
        /** 本年度该类型游玩时长（小时，一位小数） */
        private Double hours;
        /** 图表配色（由后端分配） */
        private String color;
    }

    @Data
    public static class MonthlyTrendVO implements Serializable {
        private Integer month;
        /** 该月游玩时长（小时） */
        private Integer hours;
    }
}