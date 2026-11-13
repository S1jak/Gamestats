package com.lxw.gamestats.service;

import com.lxw.pojo.vo.YearReportVO;

/**
 * 年度报告服务
 */
public interface YearlyReportService {

    /**
     * 获取（必要时生成）指定年份的个人年度报告
     */
    YearReportVO getYearlyReport(Integer year);
}
