package com.lxw.gamestats.controller;

import com.lxw.common.constant.MessageConstant;
import com.lxw.common.exception.BaseException;
import com.lxw.gamestats.service.DashboardService;
import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.DashboardVO;
import com.lxw.pojo.vo.GameVO;
import com.lxw.pojo.vo.GenreDistributionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 数据中心控制器
 * <ul>
 *   <li>GET /api/dashboard/stats              统计卡片</li>
 *   <li>GET /api/dashboard/recent-games       近期游戏排行</li>
 *   <li>GET /api/dashboard/play-trend         游玩时长趋势</li>
 *   <li>GET /api/dashboard/genre-distribution 游戏类型分布</li>
 *   <li>GET /api/dashboard/heatmap            游玩热力图</li>
 * </ul>
 * <p>
 * 参数范围与枚举在控制器内显式校验并抛 BaseException（统一 400），
 * 不启用 Bean Validation 注解校验，减少配置面。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Set<String> ALLOWED_PERIODS = Set.of("all", "week", "month");
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 2100;

    @Autowired
    private DashboardService dashboardService;

    /** 统计卡片数据 */
    @GetMapping("/stats")
    public DashboardVO getStats() {
        return dashboardService.getStats();
    }

    /** 近期游戏排行（period: all/week/month，默认 all；limit 1-50，默认 5） */
    @GetMapping("/recent-games")
    public List<GameVO> listRecentGames(@RequestParam(defaultValue = "all") String period,
                                        @RequestParam(defaultValue = "5") Integer limit) {
        String normalizedPeriod = period == null ? "" : period.trim();
        if (!ALLOWED_PERIODS.contains(normalizedPeriod)) {
            throw new BaseException(MessageConstant.INVALID_PARAM + "：period 仅允许 all/week/month");
        }
        if (limit == null || limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new BaseException(MessageConstant.INVALID_PARAM + "：limit 需在 1-50 之间");
        }
        return dashboardService.listRecentGames(normalizedPeriod, limit);
    }

    /** 游玩时长趋势（days 1-365，默认 30） */
    @GetMapping("/play-trend")
    public List<DailyPlayVO> listPlayTrend(@RequestParam(defaultValue = "30") Integer days) {
        if (days == null || days < MIN_DAYS || days > MAX_DAYS) {
            throw new BaseException(MessageConstant.INVALID_PARAM + "：days 需在 1-365 之间");
        }
        return dashboardService.listPlayTrend(days);
    }

    /** 游戏类型分布 */
    @GetMapping("/genre-distribution")
    public List<GenreDistributionVO> listGenreDistribution() {
        return dashboardService.listGenreDistribution();
    }

    /** 游玩热力图（默认当年；传入年份需在 1970-2100） */
    @GetMapping("/heatmap")
    public List<DailyPlayVO> listHeatmap(@RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        } else if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BaseException(MessageConstant.INVALID_PARAM + "：year 需在 1970-2100 之间");
        }
        return dashboardService.listHeatmap(year);
    }
}