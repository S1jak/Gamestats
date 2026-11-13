package com.lxw.gamestats.service.impl;

import com.lxw.common.context.BaseContext;
import com.lxw.common.exception.UserNotLoginException;
import com.lxw.gamestats.mapper.YearlyReportMapper;
import com.lxw.gamestats.service.YearlyReportService;
import com.lxw.pojo.entity.YearlyReport;
import com.lxw.pojo.vo.YearReportVO;
import com.lxw.pojo.vo.YearReportVO.FavoriteGameVO;
import com.lxw.pojo.vo.YearReportVO.GenreStatVO;
import com.lxw.pojo.vo.YearReportVO.MonthlyTrendVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 年度报告服务实现
 * <p>
 * 聚合 user_games / user_achievements / play_records / game_genres 数据，
 * 生成报告并 upsert 到 yearly_reports 表。
 */
@Service
public class YearlyReportServiceImpl implements YearlyReportService {

    @Autowired
    private YearlyReportMapper yearlyReportMapper;

    @Override
    public YearReportVO getYearlyReport(Integer year) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录");
        }

        // 1. 基础统计：游戏库总数、通关数
        YearReportVO vo = yearlyReportMapper.getBasicStats(userId);
        if (vo == null) {
            vo = new YearReportVO();
        }
        vo.setYear(year);

        // 2. 成就解锁数
        Integer ach = yearlyReportMapper.countAchievements(userId);
        vo.setAchievements(ach != null ? ach : 0);

        // 3. 最爱游戏（按累计时长）
        FavoriteGameVO fav = yearlyReportMapper.getFavoriteGame(userId);
        vo.setFavoriteGame(fav);

        // 4. 类型分布（按时长占比，后端分配颜色）
        List<GenreStatVO> genres = yearlyReportMapper.listGenreStats(userId);
        if (genres == null) {
            genres = new ArrayList<>();
        }
        String[] colors = {"#6366f1", "#22d3ee", "#f472b6", "#fbbf24", "#71717a"};
        for (int i = 0; i < genres.size(); i++) {
            genres.get(i).setColor(colors[i % colors.length]);
        }
        vo.setGenres(genres);

        // 5. 月度趋势（当年），累加为当年总时长，并补全 12 个月
        List<MonthlyTrendVO> trend = yearlyReportMapper.listMonthlyTrend(userId, year);
        Map<Integer, Integer> monthMap = new HashMap<>();
        int totalHours = 0;
        if (trend != null) {
            for (MonthlyTrendVO t : trend) {
                monthMap.put(t.getMonth(), t.getHours());
                totalHours += t.getHours();
            }
        }
        List<MonthlyTrendVO> fullTrend = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            MonthlyTrendVO mt = new MonthlyTrendVO();
            mt.setMonth(m);
            mt.setHours(monthMap.getOrDefault(m, 0));
            fullTrend.add(mt);
        }
        vo.setMonthlyTrend(fullTrend);
        vo.setTotalHours(totalHours);

        // 6. 生成玩家标签与游玩风格
        int completedGames = vo.getCompletedGames() != null ? vo.getCompletedGames() : 0;
        int achCount = vo.getAchievements() != null ? vo.getAchievements() : 0;
        List<String> tags = new ArrayList<>();
        String playStyle;
        if (totalHours >= 1000) {
            tags.add("硬核玩家");
            playStyle = "硬核玩家";
        } else if (totalHours >= 300) {
            tags.add("资深玩家");
            playStyle = "资深玩家";
        } else {
            tags.add("休闲玩家");
            playStyle = "休闲玩家";
        }
        if (achCount >= 50) {
            tags.add("成就猎人");
        }
        if (completedGames >= 10) {
            tags.add("通关达人");
        }
        if (!genres.isEmpty()) {
            tags.add(genres.get(0).getName() + "爱好者");
        }
        if (fav != null) {
            tags.add("热爱探索");
        }
        vo.setTags(tags);
        vo.setPlayStyle(playStyle);

        // 7. 持久化年度报告快照（upsert）
        YearlyReport report = new YearlyReport();
        report.setUserId(userId);
        report.setYear(year);
        report.setFavoriteGameId(fav != null ? fav.getId() : null);
        report.setTotalPlayTime(totalHours * 60);
        report.setPlayStyle(playStyle);
        report.setAchievementsCount(achCount);
        yearlyReportMapper.upsert(report);

        return vo;
    }
}
