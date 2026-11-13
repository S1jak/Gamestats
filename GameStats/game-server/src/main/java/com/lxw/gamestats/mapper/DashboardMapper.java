package com.lxw.gamestats.mapper;

import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.DashboardVO;
import com.lxw.pojo.vo.GameVO;
import com.lxw.pojo.vo.GenreDistributionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据中心 Mapper
 */
@Mapper
public interface DashboardMapper {

    /**
     * 统计卡片数据（含环比趋势）
     * <ul>
     *   <li>totalPlayTime / totalGames / playDays / achievements / totalAchievements / completedGames</li>
     *   <li>playTimeTrend：近 30 天时长相对前 30 天的环比百分比</li>
     *   <li>gamesTrend：近 30 天新增游戏数占总数的百分比</li>
     * </ul>
     */
    DashboardVO getStats(@Param("userId") Long userId);

    /**
     * 近期游戏排行
     * <ul>
     *   <li>period = all（默认）：按 user_games 累计游玩时长降序</li>
     *   <li>period = week / month：按 play_records 最近 7 / 30 天内每日时长聚合降序</li>
     * </ul>
     */
    List<GameVO> listRecentGames(@Param("userId") Long userId,
                                 @Param("period") String period,
                                 @Param("limit") Integer limit);

    /**
     * 游玩时长趋势（按日聚合，最近 N 天）
     */
    List<DailyPlayVO> listPlayTrend(@Param("userId") Long userId, @Param("days") Integer days);

    /**
     * 游戏类型分布（按用户游戏库时长占比）
     */
    List<GenreDistributionVO> listGenreDistribution(@Param("userId") Long userId);

    /**
     * 游玩热力图（指定年份按日聚合）
     */
    List<DailyPlayVO> listHeatmap(@Param("userId") Long userId, @Param("year") Integer year);
}
