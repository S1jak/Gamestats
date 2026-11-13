package com.lxw.gamestats.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxw.common.context.BaseContext;
import com.lxw.common.exception.BaseException;
import com.lxw.common.exception.UserNotLoginException;
import com.lxw.gamestats.mapper.YearlyReportMapper;
import com.lxw.gamestats.service.YearlyReportService;
import com.lxw.pojo.entity.YearlyReport;
import com.lxw.pojo.vo.YearReportSnapshot;
import com.lxw.pojo.vo.YearReportVO;
import com.lxw.pojo.vo.YearReportVO.FavoriteGameVO;
import com.lxw.pojo.vo.YearReportVO.GenreStatVO;
import com.lxw.pojo.vo.YearReportVO.MonthlyTrendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 年度报告服务实现。
 * <p>
 * 职责拆分：
 * <ul>
 *   <li>{@link #getYearlyReport(Integer)}：只读；快照优先，无效快照回退实时计算且不落库（BUG-15 / D4）；</li>
 *   <li>{@link #generateYearlyReport(Integer)}：唯一写入入口，实时计算后序列化完整快照并 upsert（D8）；</li>
 *   <li>{@link #computeReport(Long, int)}：读取与生成共用同一口径，避免"两套算法"。</li>
 * </ul>
 * 八项指标全部严格按所选自然年聚合（D1），无数据时返回 0 / 空数组 / null，不回退其他年份。
 */
@Slf4j
@Service
public class YearlyReportServiceImpl implements YearlyReportService {

    /** 快照内容版本号（C2：缺失或不等于该值即视为无效快照） */
    private static final long SNAPSHOT_VERSION = 1L;

    /** 快照 JSON 序列化/反序列化，独立实例，避免与 MVC 序列化配置耦合（G13） */
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper();

    /** 快照生成时间格式（设计约定 yyyy-MM-dd HH:mm:ss） */
    private static final DateTimeFormatter GENERATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 类型分布配色，按 percent 降序依次分配 */
    private static final String[] GENRE_COLORS = {"#6366f1", "#22d3ee", "#f472b6", "#fbbf24", "#71717a"};

    private static final String SOURCE_SNAPSHOT = "snapshot";

    private static final String SOURCE_REALTIME = "realtime";

    /** 年份下限（更早的年份无业务意义，按非法参数处理） */
    private static final int MIN_YEAR = 2000;

    /** 年份上限允许超出当前年一年，用于查看下一年空态 */
    private static final int MAX_YEAR_OFFSET = 1;

    @Autowired
    private YearlyReportMapper yearlyReportMapper;

    @Override
    public YearReportVO getYearlyReport(Integer year) {
        Long userId = requireLogin();
        int resolvedYear = resolveYear(year);

        YearlyReport stored = yearlyReportMapper.getByUserIdAndYear(userId, resolvedYear);
        YearReportVO snapshot = readSnapshot(stored, userId, resolvedYear);
        if (snapshot != null) {
            snapshot.setSnapshotSource(SOURCE_SNAPSHOT);
            snapshot.setSnapshotGeneratedAt(formatGeneratedAt(stored.getGeneratedAt()));
            return snapshot;
        }

        // 无有效快照：实时计算，只读不落库
        YearReportVO vo = computeReport(userId, resolvedYear).report;
        vo.setSnapshotSource(SOURCE_REALTIME);
        vo.setSnapshotGeneratedAt(null);
        return vo;
    }

    @Override
    @Transactional
    public YearReportVO generateYearlyReport(Integer year) {
        Long userId = requireLogin();
        int resolvedYear = resolveYear(year);

        ComputedReport computed = computeReport(userId, resolvedYear);
        YearReportVO vo = computed.report;
        LocalDateTime generatedAt = LocalDateTime.now();

        // 快照只保存报告本体，来源与生成时间在读取时覆盖（避免自引用与时间失真）
        YearReportSnapshot snapshot = new YearReportSnapshot();
        snapshot.setVersion(SNAPSHOT_VERSION);
        snapshot.setReport(vo);

        String snapshotJson;
        try {
            snapshotJson = SNAPSHOT_MAPPER.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("年度报告快照序列化失败：userId={}, year={}", userId, resolvedYear, e);
            throw new BaseException("年度报告快照生成失败");
        }

        YearlyReport entity = new YearlyReport();
        entity.setUserId(userId);
        entity.setYear(resolvedYear);
        entity.setFavoriteGameId(vo.getFavoriteGame() != null ? vo.getFavoriteGame().getId() : null);
        entity.setTotalPlayTime(computed.totalPlayMinutes);
        entity.setPlayStyle(vo.getPlayStyle());
        entity.setAchievementsCount(vo.getAchievements());
        entity.setSnapshotJson(snapshotJson);
        entity.setGeneratedAt(generatedAt);
        yearlyReportMapper.upsert(entity);

        vo.setSnapshotSource(SOURCE_SNAPSHOT);
        vo.setSnapshotGeneratedAt(formatGeneratedAt(generatedAt));
        return vo;
    }

    /**
     * 按年度口径实时计算完整报告（读取与生成共用）。
     *
     * @return 报告本体 + 年度总分钟数（写库 total_play_time 用，避免二次查询口径漂移）
     */
    private ComputedReport computeReport(Long userId, int year) {
        YearReportVO vo = yearlyReportMapper.getBasicStats(userId, year);
        if (vo == null) {
            vo = new YearReportVO();
        }
        vo.setYear(year);
        vo.setTotalGames(vo.getTotalGames() != null ? vo.getTotalGames() : 0);
        vo.setCompletedGames(vo.getCompletedGames() != null ? vo.getCompletedGames() : 0);

        // 年度总时长：整体聚合后四舍五入，不再逐月累加（G8）
        Integer minutes = yearlyReportMapper.sumPlayMinutes(userId, year);
        int totalPlayMinutes = minutes != null ? minutes : 0;
        int totalHours = (int) Math.round(totalPlayMinutes / 60.0);
        vo.setTotalHours(totalHours);

        // 年度成就
        Integer ach = yearlyReportMapper.countAchievements(userId, year);
        vo.setAchievements(ach != null ? ach : 0);

        // 年度最爱（该年时长最高者，可能为 null）
        FavoriteGameVO favorite = yearlyReportMapper.getFavoriteGame(userId, year);
        vo.setFavoriteGame(favorite);

        // 年度类型分布（主类型 + 其他）
        List<GenreStatVO> genres = yearlyReportMapper.listGenreStats(userId, year);
        if (genres == null) {
            genres = new ArrayList<>();
        }
        for (int i = 0; i < genres.size(); i++) {
            genres.get(i).setColor(GENRE_COLORS[i % GENRE_COLORS.length]);
        }
        alignGenreHours(genres, totalPlayMinutes, year);
        vo.setGenres(genres);

        // 年度月度趋势：补全 12 个月
        List<MonthlyTrendVO> trend = yearlyReportMapper.listMonthlyTrend(userId, year);
        Map<Integer, Integer> monthMap = new HashMap<>();
        if (trend != null) {
            for (MonthlyTrendVO t : trend) {
                monthMap.put(t.getMonth(), t.getHours());
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

        // 年度标签与游玩风格：全部基于本年度指标，空库时给出合理默认值
        vo.setTags(buildTags(vo, totalHours, genres, favorite));
        vo.setPlayStyle(resolvePlayStyle(totalHours));

        return new ComputedReport(vo, totalPlayMinutes);
    }

    /** 年度游玩风格（沿用既有阈值，仅口径改为年度总时长） */
    private String resolvePlayStyle(int totalHours) {
        if (totalHours >= 1000) {
            return "硬核玩家";
        }
        if (totalHours >= 300) {
            return "资深玩家";
        }
        return "休闲玩家";
    }

    /** 年度玩家标签：首项与游玩风格一致，其余按年度成就 / 通关 / 主类型 / 最爱追加 */
    private List<String> buildTags(YearReportVO vo, int totalHours, List<GenreStatVO> genres, FavoriteGameVO favorite) {
        List<String> tags = new ArrayList<>();
        tags.add(resolvePlayStyle(totalHours));
        if (vo.getAchievements() >= 50) {
            tags.add("成就猎人");
        }
        if (vo.getCompletedGames() >= 10) {
            tags.add("通关达人");
        }
        if (!genres.isEmpty() && genres.get(0).getName() != null) {
            tags.add(genres.get(0).getName() + "爱好者");
        }
        if (favorite != null) {
            tags.add("热爱探索");
        }
        return tags;
    }

    /**
     * 类型小时数尾差校正：把逐项 ROUND(...,1) 的舍入误差归入占比最小的类型，
     * 使 hours 合计与本年度总时长（分钟折算小时，一位小数）严格一致。
     */
    private void alignGenreHours(List<GenreStatVO> genres, int totalPlayMinutes, int year) {
        if (genres.isEmpty() || totalPlayMinutes <= 0) {
            return;
        }
        double target = Math.round(totalPlayMinutes / 60.0 * 10) / 10.0;
        double percentSum = 0.0;
        double othersHours = 0.0;
        for (int i = 0; i < genres.size(); i++) {
            Double percent = genres.get(i).getPercent();
            percentSum += percent != null ? percent : 0.0;
            if (i < genres.size() - 1) {
                Double hours = genres.get(i).getHours();
                othersHours += hours != null ? hours : 0.0;
            }
        }
        double tail = Math.round((target - othersHours) * 10) / 10.0;
        if (tail >= 0) {
            genres.get(genres.size() - 1).setHours(tail);
        }
        if (Math.abs(percentSum - 100.0) > 0.1) {
            log.warn("年度类型占比合计偏离 100%：year={}, percentSum={}", year, percentSum);
        }
    }

    /**
     * 读取并校验快照。任何一条不满足即返回 null 并记 WARN（不回写、不修复、不抛异常）。
     */
    private YearReportVO readSnapshot(YearlyReport stored, Long userId, int year) {
        if (stored == null) {
            log.debug("年度快照不存在，回退实时计算：userId={}, year={}", userId, year);
            return null;
        }
        String json = stored.getSnapshotJson();
        if (json == null || json.isBlank()) {
            log.warn("年度快照无效（旧格式或内容为空），回退实时计算：userId={}, year={}", userId, year);
            return null;
        }

        YearReportSnapshot snapshot;
        try {
            snapshot = SNAPSHOT_MAPPER.readValue(json, YearReportSnapshot.class);
        } catch (Exception e) {
            log.warn("年度快照解析失败，回退实时计算：userId={}, year={}, 原因={}", userId, year, e.getMessage());
            return null;
        }

        if (snapshot == null || snapshot.getVersion() == null
                || snapshot.getVersion().longValue() != SNAPSHOT_VERSION) {
            log.warn("年度快照版本无效，回退实时计算：userId={}, year={}, _v={}",
                    userId, year, snapshot != null ? snapshot.getVersion() : null);
            return null;
        }

        YearReportVO report = snapshot.getReport();
        if (!isComplete(report)) {
            log.warn("年度快照内容不完整，回退实时计算：userId={}, year={}", userId, year);
            return null;
        }
        if (report.getYear().intValue() != year) {
            log.warn("年度快照年份不匹配，回退实时计算：userId={}, year={}, snapshotYear={}",
                    userId, year, report.getYear());
            return null;
        }
        return report;
    }

    /** 快照必需字段校验：favoriteGame 允许为 null，其余指标与集合不得缺失 */
    private boolean isComplete(YearReportVO report) {
        return report != null
                && report.getYear() != null
                && report.getTotalHours() != null
                && report.getTotalGames() != null
                && report.getAchievements() != null
                && report.getCompletedGames() != null
                && report.getTags() != null
                && report.getGenres() != null
                && report.getMonthlyTrend() != null
                && report.getPlayStyle() != null;
    }

    /** 年份解析与范围校验：缺省当前年；允许 [2000, 当前年+1] */
    private int resolveYear(Integer year) {
        int currentYear = LocalDate.now().getYear();
        int resolved = year != null ? year : currentYear;
        if (resolved < MIN_YEAR || resolved > currentYear + MAX_YEAR_OFFSET) {
            throw new BaseException("年份需在 " + MIN_YEAR + "-" + (currentYear + MAX_YEAR_OFFSET) + " 之间");
        }
        return resolved;
    }

    private Long requireLogin() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException("用户未登录");
        }
        return userId;
    }

    private String formatGeneratedAt(LocalDateTime generatedAt) {
        return generatedAt == null ? null : generatedAt.format(GENERATED_AT_FORMATTER);
    }

    /** 报告本体 + 年度总分钟数（写库汇总字段用） */
    private static final class ComputedReport {
        private final YearReportVO report;
        private final int totalPlayMinutes;

        private ComputedReport(YearReportVO report, int totalPlayMinutes) {
            this.report = report;
            this.totalPlayMinutes = totalPlayMinutes;
        }
    }
}