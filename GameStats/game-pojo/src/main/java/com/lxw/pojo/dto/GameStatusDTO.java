package com.lxw.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新游戏状态请求
 */
@Data
public class GameStatusDTO implements Serializable {

    /**
     * 游戏状态：在玩 / 已完成 / 未玩 / 搁置 / 弃坑
     */
    private String status;
}
