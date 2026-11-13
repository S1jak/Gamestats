package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户游戏自定义标签实体（对应 user_game_tags 表）
 * <p>
 * 用户 × 游戏 × 标签 三元组，唯一键 (user_id, game_id, tag)。
 * 标签与游戏类型完全解耦，类型由 game_genre_relations 提供。
 */
@Data
public class UserGameTag implements Serializable {

    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 游戏 ID */
    private Long gameId;

    /** 用户自定义标签 */
    private String tag;

    /** 创建时间 */
    private LocalDateTime createdAt;
}