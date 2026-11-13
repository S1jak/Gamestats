package com.lxw.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新游戏笔记请求
 */
@Data
public class GameNoteDTO implements Serializable {

    private String note;
}
