package com.lxw.gamestats.service;

import com.lxw.pojo.vo.YearReportVO;

/**
 * 年度报告服务
 * <p>
 * 约定：读取（GET）只读、不落库；只有显式生成（POST）才写入快照。
 */
public interface YearlyReportService {

    /**
     * 读取指定年份的个人年度报告。
     * <p>
     * 命中有效快照直接返回快照内容（snapshotSource=snapshot）；
     * 无快照 / 旧格式 / 损坏时回退实时计算（snapshotSource=realtime），且不写库。
     *
     * @param year 自然年；为 null 时取当前年
     */
    YearReportVO getYearlyReport(Integer year);

    /**
     * 显式生成/刷新指定年份的快照，并返回生成后的报告。
     * <p>
     * 同用户同年份重复调用只更新同一行（幂等）；是唯一允许写入快照的入口。
     *
     * @param year 自然年；为 null 时取当前年
     */
    YearReportVO generateYearlyReport(Integer year);
}