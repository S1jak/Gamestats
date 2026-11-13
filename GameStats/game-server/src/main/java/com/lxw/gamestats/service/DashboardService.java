package com.lxw.gamestats.service;

import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.DashboardVO;
import com.lxw.pojo.vo.GameVO;
import com.lxw.pojo.vo.GenreDistributionVO;

import java.util.List;

/**
 * 数据中心服务
 */
public interface DashboardService {

    /** 统计卡片数据 */
    DashboardVO getStats();

    /** 近期游戏排行（period: all/week/month） */
    List<GameVO> listRecentGames(String period, Integer limit);

    /** 游玩时长趋势（最近 N 天按日聚合） */
    List<DailyPlayVO> listPlayTrend(Integer days);

    /** 游戏类型分布（按时长占比） */
    List<GenreDistributionVO> listGenreDistribution();

    /** 游玩热力图（指定年份按日聚合） */
    List<DailyPlayVO> listHeatmap(Integer year);
}
