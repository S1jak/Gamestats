package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 游戏类型分布视图对象
 */
@Data
public class GenreDistributionVO implements Serializable {

    /** 类型名称 */
    private String name;

    /** 占比百分比（0-100） */
    private Double percent;

    /** 该类型累计游玩时长（小时） */
    private Double hours;
}
