package com.lxw.gamestats.controller;

import com.lxw.gamestats.service.YearlyReportService;
import com.lxw.pojo.vo.YearReportVO;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * 年度报告控制器
 * <ul>
 *   <li>GET&nbsp; /api/report/yearly?year=2026：只读读取，快照优先，缺失/损坏回退实时计算</li>
 *   <li>POST /api/report/yearly/generate?year=2026：显式生成/刷新快照，唯一写入入口</li>
 * </ul>
 * 年份缺省为当前年；非数字由参数类型转换异常处理，越界由服务层精确校验。
 */
@RestController
@RequestMapping("/api/report")
@Validated
public class YearlyReportController {

    @Autowired
    private YearlyReportService yearlyReportService;

    /** 读取指定年份的年度报告（只读，不写库） */
    @GetMapping("/yearly")
    public YearReportVO getYearlyReport(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year) {
        return yearlyReportService.getYearlyReport(year);
    }

    /** 显式生成/刷新指定年份的年度报告快照（幂等） */
    @PostMapping("/yearly/generate")
    public YearReportVO generateYearlyReport(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year) {
        return yearlyReportService.generateYearlyReport(year);
    }

    /**
     * 控制器级参数约束异常 → 400。
     * <p>
     * 就近处理，避免依赖全局异常处理器为参数校验新增分支；
     * 精确年份范围（当前年 +1）仍由服务层校验并抛 {@code BaseException}。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "年份需在 2000-2100 之间"));
    }

    /** year 非数字（类型转换失败）→ 400，就近处理不依赖全局异常处理器 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName() != null ? ex.getName() : "year";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "参数格式错误：" + name));
    }

    /** Spring MVC 内置方法校验失败（未走 @Validated AOP 代理时）→ 400 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "年份需在 2000-2100 之间"));
    }
}