package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户游戏平台拥有实体（对应 user_game_platforms 表）
 */
@Data
public class UserGamePlatform implements Serializable {

    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 游戏 ID */
    private Long gameId;

    /** 平台名称 */
    private String name;

    /** 是否拥有 */
    private Boolean owned;
}
