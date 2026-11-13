package com.lxw.gamestats.mapper;

import com.lxw.pojo.entity.YearlyReport;
import com.lxw.pojo.vo.YearReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 年度报告 Mapper
 */
@Mapper
public interface YearlyReportMapper {

    /**
     * 基础统计：游戏库总数、通关数
     */
    YearReportVO getBasicStats(@Param("userId") Long userId);

    /**
     * 成就解锁数
     */
    Integer countAchievements(@Param("userId") Long userId);

    /**
     * 最爱游戏（按累计时长降序取第一）
     */
    YearReportVO.FavoriteGameVO getFavoriteGame(@Param("userId") Long userId);

    /**
     * 游戏类型分布（按用户游戏时长占比）
     */
    List<YearReportVO.GenreStatVO> listGenreStats(@Param("userId") Long userId);

    /**
     * 月度游玩趋势（当年，按月聚合时长，单位小时）
     */
    List<YearReportVO.MonthlyTrendVO> listMonthlyTrend(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 查询已有年度报告
     */
    YearlyReport getByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 新增或更新年度报告（upsert）
     */
    int upsert(YearlyReport report);
}
