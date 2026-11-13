package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户游戏库实体（对应 user_games 表）
 * <p>
 * 表示用户拥有的某款游戏：游玩时长、状态、笔记等。
 */
@Data
public class UserGame implements Serializable {

    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 游戏 ID */
    private Long gameId;

    /** 游玩时长（分钟） */
    private Integer playTime;

    /** 最后游玩时间 */
    private LocalDateTime lastPlayed;

    /**
     * 游戏状态：未玩 / 在玩 / 已完成 / 搁置 / 弃坑
     */
    private String status;

    /** 游戏笔记 */
    private String notes;

    /** 完成时间（状态进入"已完成"时写入，离开"已完成"时清空） */
    private LocalDateTime completedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
