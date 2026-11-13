package com.lxw.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 年度报告快照外层结构（yearly_reports.snapshot_json）。
 * <p>
 * 固定结构 {@code {"_v":1,"report":{...}}}；版本号用于字段演进，
 * 缺失、不等于 1 或报告内容不完整即判定为无效快照，回退实时计算（D4 / C2）。
 * <p>
 * 快照不含 {@code snapshotSource} / {@code snapshotGeneratedAt}，
 * 二者在读取时按来源覆盖，避免自引用与时间失真。
 */
@Data
public class YearReportSnapshot implements Serializable {

    /** 快照内容版本号，当前固定为 1 */
    @JsonProperty("_v")
    private Long version;

    /** 完整年度报告内容 */
    private YearReportVO report;
}