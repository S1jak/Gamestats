package com.lxw.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 游戏标签更新请求 DTO
 * <p>
 * 全量替换语义：tags 为 null 视为非法请求，空数组表示清空。
 */
@Data
public class GameTagsDTO implements Serializable {

    /** 用户标签列表 */
    private List<String> tags;
}