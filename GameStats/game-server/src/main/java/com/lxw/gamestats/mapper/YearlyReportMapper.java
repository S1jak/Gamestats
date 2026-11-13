package com.lxw.gamestats.mapper;

import com.lxw.pojo.entity.YearlyReport;
import com.lxw.pojo.vo.YearReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 年度报告 Mapper
 * <p>
 * 所有聚合方法都必须同时带 {@code userId} 与 {@code year}，
 * 时间窗统一为 [当年 1 月 1 日, 次年 1 月 1 日) 的半开区间（D1 严格按年）。
 */
@Mapper
public interface YearlyReportMapper {

    /**
     * 年度基础统计：该年有游玩记录的不同游戏数、该年通关数（按 completed_at）
     */
    YearReportVO getBasicStats(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 年度解锁成就数（按 unlocked_at）
     */
    Integer countAchievements(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 年度游玩总分钟数（总时长由 Service 整体四舍五入，不逐月累加）
     */
    Integer sumPlayMinutes(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 年度最爱游戏（该年游玩时长最高者，无记录返回 null）
     */
    YearReportVO.FavoriteGameVO getFavoriteGame(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 年度游戏类型分布（每游戏按主类型计一次，未归类归"其他"）
     */
    List<YearReportVO.GenreStatVO> listGenreStats(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 月度游玩趋势（当年，按月聚合时长，单位小时）
     */
    List<YearReportVO.MonthlyTrendVO> listMonthlyTrend(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 查询已有年度报告（含快照 JSON 与生成时间）
     */
    YearlyReport getByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 新增或更新年度报告（upsert，唯一键 user_id + year）
     */
    int upsert(YearlyReport report);
}