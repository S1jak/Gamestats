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
        List<GenreDistributionVO> list = dashboardMapper.listGenreDistribution(userId);
        return list != null ? list : new ArrayList<>();
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
