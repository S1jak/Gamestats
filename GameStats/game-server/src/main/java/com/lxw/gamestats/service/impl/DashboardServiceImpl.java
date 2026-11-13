package com.lxw.gamestats.service.impl;

import com.lxw.common.context.BaseContext;
import com.lxw.common.exception.UserNotLoginException;
import com.lxw.gamestats.mapper.DashboardMapper;
import com.lxw.gamestats.service.DashboardService;
import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.DashboardVO;
import com.lxw.pojo.vo.GameVO;
import com.lxw.pojo.vo.GenreDistributionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据中心服务实现
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public DashboardVO getStats() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录");
        }
        DashboardVO vo = dashboardMapper.getStats(userId);
        if (vo == null) {
            vo = new DashboardVO();
            vo.setTotalPlayTime(0);
            vo.setTotalGames(0);
            vo.setPlayDays(0);
            vo.setAchievements(0);
            vo.setTotalAchievements(0);
            vo.setCompletedGames(0);
            vo.setPlayTimeTrend(0.0);
            vo.setGamesTrend(0.0);
        }
        return vo;
    }

    @Override
    public List<GameVO> listRecentGames(String period, Integer limit) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录");
        }
        List<GameVO> list = dashboardMapper.listRecentGames(userId, period, limit);
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public List<DailyPlayVO> listPlayTrend(Integer days) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录");
        }
        List<DailyPlayVO> list = dashboardMapper.listPlayTrend(userId, days);
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public List<GenreDistributionVO> listGenreDistribution() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录");
        }
        // 总时长为 0 时直接返回空列表，避免除零与 NaN（BUG-13）
        Integer totalMinutes = dashboardMapper.sumUserPlayMinutes(userId);
        if (totalMinutes == null || totalMinutes <= 0) {
            return new ArrayList<>();
        }
        List<GenreDistributionVO> list = dashboardMapper.listGenreDistribution(userId);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        alignHours(list, totalMinutes);
        return list;
    }

    /**
     * 类型小时数尾差校正：把逐项 ROUND(...,1) 的舍入误差归入占比最小的类型，
     * 使 hours 合计等于游戏库总时长（分钟折算小时，一位小数）。
     */
    private void alignHours(List<GenreDistributionVO> list, int totalMinutes) {
        double target = Math.round(totalMinutes / 60.0 * 10) / 10.0;
        double othersHours = 0.0;
        for (int i = 0; i < list.size() - 1; i++) {
            Double hours = list.get(i).getHours();
            othersHours += hours != null ? hours : 0.0;
        }
        double tail = Math.round((target - othersHours) * 10) / 10.0;
        if (tail >= 0) {
            list.get(list.size() - 1).setHours(tail);
        }
    }

    @Override
    public List<DailyPlayVO> listHeatmap(Integer year) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录");
        }
        List<DailyPlayVO> list = dashboardMapper.listHeatmap(userId, year);
        return list != null ? list : new ArrayList<>();
    }
}
