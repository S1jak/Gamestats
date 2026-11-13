package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 游戏类型行（game_id + 类型名）
 * <p>
 * 用于列表接口一次性取出当前用户全部游戏的类型后按 gameId 分组填充 genres，避免 N+1。
 */
@Data
public class GameGenreRow implements Serializable {

    /** 游戏 ID */
    private Long gameId;

    /** 类型名称 */
    private String name;
}