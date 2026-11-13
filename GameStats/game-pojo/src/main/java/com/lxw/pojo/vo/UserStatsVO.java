package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户游戏统计视图对象
 */
@Data
public class UserStatsVO implements Serializable {

    /** 游戏总数 */
    private Integer totalGames;

    /** 总游玩时长（分钟） */
    private Integer totalPlayTime;

    /** 成就总数 */
    private Integer achievements;

    /** 已通关游戏数 */
    private Integer completedGames;
}
