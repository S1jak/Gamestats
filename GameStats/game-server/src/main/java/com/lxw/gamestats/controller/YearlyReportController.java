package com.lxw.gamestats.controller;

import com.lxw.gamestats.service.YearlyReportService;
import com.lxw.pojo.vo.YearReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 年度报告控制器
 * <ul>
 *   <li>GET /api/report/yearly?year=2025  获取（自动生成）个人年度报告</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/report")
public class YearlyReportController {

    @Autowired
    private YearlyReportService yearlyReportService;

    /** 获取指定年份的年度报告（不存在则自动生成） */
    @GetMapping("/yearly")
    public YearReportVO getYearlyReport(@RequestParam(defaultValue = "2025") Integer year) {
        return yearlyReportService.getYearlyReport(year);
    }
}
