package com.lxw.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新游戏状态请求
 */
@Data
public class GameStatusDTO implements Serializable {

    /**
     * 游戏状态：playing / completed / not_played / on_hold / dropped
     */
    private String status;
}
