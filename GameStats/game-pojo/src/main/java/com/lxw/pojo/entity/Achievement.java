package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成就实体（对应 achievements 表，全局成就定义）
 */
@Data
public class Achievement implements Serializable {

    private Long id;

    /** 游戏 ID */
    private Long gameId;

    /** 成就名称 */
    private String name;

    /** 成就描述 */
    private String description;

    /** 图标 URL */
    private String icon;

    /** 平台内成就 ID */
    private String platformAchievementId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
