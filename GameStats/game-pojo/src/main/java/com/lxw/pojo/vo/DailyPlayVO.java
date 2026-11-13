package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 每日游玩统计视图对象
 * <p>用于游玩时长趋势折线图与游玩热力图</p>
 */
@Data
public class DailyPlayVO implements Serializable {

    /** 日期，格式 yyyy-MM-dd */
    private String date;

    /** 当日游玩时长（分钟） */
    private Integer minutes;
}
