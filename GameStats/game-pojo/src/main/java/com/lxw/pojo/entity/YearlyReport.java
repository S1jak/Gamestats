package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 年度报告实体（对应 yearly_reports 表）。
 * <p>
 * 旧字段（favoriteGameId / totalPlayTime / playStyle / achievementsCount）
 * 仅作兼容与审计；完整内容以 snapshotJson 为准。
 */
@Data
public class YearlyReport implements Serializable {

    private Long id;

    private Long userId;

    private Integer year;

    private Long favoriteGameId;

    /** 总游玩时长（分钟） */
    private Integer totalPlayTime;

    /** 游玩风格 */
    private String playStyle;

    private Integer achievementsCount;

    /** 完整报告快照 JSON，结构 {_v:1, report:{...}}；旧行为 null */
    private String snapshotJson;

    /** 快照生成时间；仅显式生成时写入 */
    private LocalDateTime generatedAt;

    private LocalDateTime createdAt;
}