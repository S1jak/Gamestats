package com.lxw.gamestats.controller;

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

/**
 * 数据中心控制器
 * <ul>
 *   <li>GET /api/dashboard/stats              统计卡片</li>
 *   <li>GET /api/dashboard/recent-games       近期游戏排行</li>
 *   <li>GET /api/dashboard/play-trend         游玩时长趋势</li>
 *   <li>GET /api/dashboard/genre-distribution 游戏类型分布</li>
 *   <li>GET /api/dashboard/heatmap            游玩热力图</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** 统计卡片数据 */
    @GetMapping("/stats")
    public DashboardVO getStats() {
        return dashboardService.getStats();
    }

    /** 近期游戏排行（period: all/week/month，默认 all） */
    @GetMapping("/recent-games")
    public List<GameVO> listRecentGames(@RequestParam(defaultValue = "all") String period,
                                        @RequestParam(defaultValue = "5") Integer limit) {
        return dashboardService.listRecentGames(period, limit);
    }

    /** 游玩时长趋势（默认最近 30 天） */
    @GetMapping("/play-trend")
    public List<DailyPlayVO> listPlayTrend(@RequestParam(defaultValue = "30") Integer days) {
        return dashboardService.listPlayTrend(days);
    }

    /** 游戏类型分布 */
    @GetMapping("/genre-distribution")
    public List<GenreDistributionVO> listGenreDistribution() {
        return dashboardService.listGenreDistribution();
    }

    /** 游玩热力图（默认当年） */
    @GetMapping("/heatmap")
    public List<DailyPlayVO> listHeatmap(@RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return dashboardService.listHeatmap(year);
    }
}
