package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据中心统计卡片视图对象
 */
@Data
public class DashboardVO implements Serializable {

    /** 总游戏时长（分钟） */
    private Integer totalPlayTime;

    /** 游戏数量（用户游戏库总数） */
    private Integer totalGames;

    /** 总游玩天数（play_records 不同 start_time 日期数） */
    private Integer playDays;

    /** 已解锁成就数 */
    private Integer achievements;

    /** 全部游戏成就总数 */
    private Integer totalAchievements;

    /** 已通关游戏数 */
    private Integer completedGames;

    /** 游玩时长环比（百分比，与上一周期对比） */
    private Double playTimeTrend;

    /** 游戏数量环比（百分比，与上一周期对比） */
    private Double gamesTrend;
}
