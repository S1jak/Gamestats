package com.lxw.gamestats.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据库版本化迁移：补列、历史状态清洗、完成时间锚点/回填、一次性演示种子。
 * <p>
 * 因 Spring Boot 的 ScriptUtils 不支持 DELIMITER / 存储过程，
 * 故在应用启动后通过 JdbcTemplate + information_schema 动态判断并补列。
 * <p>
 * 执行顺序不可颠倒（补列 → 状态清洗 → 清空脏组合 → 通关锚点 → 通用回填 →
 * 成就锚点 → 标签种子 → 一次性标记），锚点必须先于通用回填，否则锚点条件失效。
 * 该 Runner 幂等：列已存在不会重复 ALTER，数据更新带条件，种子由 app_seed_log 控制。
 */
@Slf4j
@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final String SEED_DEMO_ANCHORS = "seed_demo_anchors_v1";
    private static final String SEED_USER_GAME_TAGS = "seed_user_game_tags_v1";

    private static final String STATUS_PLAYING = "在玩";
    private static final String STATUS_COMPLETED = "已完成";
    private static final String STATUS_NOT_PLAYED = "未玩";
    private static final String STATUS_ON_HOLD = "搁置";
    private static final String STATUS_DROPPED = "弃坑";

    private static final String CURRENT_YEAR = "YEAR(CURDATE())";
    private static final String LAST_YEAR = "YEAR(CURDATE()) - 1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final Map<String, String> USER_COLUMNS = new LinkedHashMap<>();

    static {
        USER_COLUMNS.put("gender", "ALTER TABLE users ADD COLUMN gender VARCHAR(16) DEFAULT 'other' COMMENT '性别'");
        USER_COLUMNS.put("birthday", "ALTER TABLE users ADD COLUMN birthday DATE DEFAULT NULL COMMENT '生日'");
        USER_COLUMNS.put("region", "ALTER TABLE users ADD COLUMN region VARCHAR(32) DEFAULT NULL COMMENT '地区'");
        USER_COLUMNS.put("preferred_language", "ALTER TABLE users ADD COLUMN preferred_language VARCHAR(8) DEFAULT 'zh-CN' COMMENT '偏好语言'");
        USER_COLUMNS.put("profile_visibility", "ALTER TABLE users ADD COLUMN profile_visibility VARCHAR(16) DEFAULT 'public' COMMENT '资料可见性'");
        USER_COLUMNS.put("allow_comments", "ALTER TABLE users ADD COLUMN allow_comments TINYINT(1) DEFAULT 1 COMMENT '允许评论'");
        USER_COLUMNS.put("email_notify", "ALTER TABLE users ADD COLUMN email_notify TINYINT(1) DEFAULT 1 COMMENT '邮件通知'");
        USER_COLUMNS.put("site_notify", "ALTER TABLE users ADD COLUMN site_notify TINYINT(1) DEFAULT 1 COMMENT '站内通知'");
    }

    @Override
    public void run(String... args) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        log.info("开始执行数据库迁移");

        int addedColumns = migrateColumns();
        int normalizedStatus = normalizeLegacyStatus();
        int invalidStatus = normalizeInvalidStatus();
        int clearedCompletedAt = clearCompletedAtForNonCompleted();
        int demoAnchors = applyDemoAnchorsIfNeeded(transactionTemplate);
        int tagSeeds = applyTagSeedIfNeeded(transactionTemplate);
        upsertDemoUser();

        log.info("数据库迁移完成：补列 {} 个，状态归一 {} 行，非法状态兜底 {} 行，清空完成时间 {} 行，锚点/回填 {} 行，标签种子 {} 行",
                addedColumns, normalizedStatus, invalidStatus, clearedCompletedAt, demoAnchors, tagSeeds);
    }

    /**
     * 步骤 1：为旧库补齐 users 8 列、user_games.completed_at、yearly_reports 快照两列。
     */
    private int migrateColumns() {
        int added = 0;
        for (Map.Entry<String, String> entry : USER_COLUMNS.entrySet()) {
            try {
                if (addColumnIfNotExists("users", entry.getKey(), entry.getValue())) {
                    added++;
                }
            } catch (Exception e) {
                log.error("补列 users.{} 失败", entry.getKey(), e);
            }
        }
        added += addBusinessColumn("user_games", "completed_at",
                "ALTER TABLE user_games ADD COLUMN completed_at DATETIME DEFAULT NULL COMMENT '完成时间'");
        added += addBusinessColumn("yearly_reports", "snapshot_json",
                "ALTER TABLE yearly_reports ADD COLUMN snapshot_json JSON DEFAULT NULL COMMENT '完整年度报告快照(JSON)'");
        added += addBusinessColumn("yearly_reports", "generated_at",
                "ALTER TABLE yearly_reports ADD COLUMN generated_at DATETIME DEFAULT NULL COMMENT '快照生成时间'");
        return added;
    }

    private int addBusinessColumn(String tableName, String columnName, String ddl) {
        try {
            return addColumnIfNotExists(tableName, columnName, ddl) ? 1 : 0;
        } catch (Exception e) {
            log.error("补列 {}.{} 失败", tableName, columnName, e);
            return 0;
        }
    }

    /**
     * 步骤 2：历史双轨状态归一，"正在玩"统一为"在玩"。
     */
    private int normalizeLegacyStatus() {
        try {
            int rows = jdbcTemplate.update("UPDATE user_games SET status = ? WHERE status = '正在玩'", STATUS_PLAYING);
            log.info("状态归一：正在玩→在玩，影响 {} 行", rows);
            return rows;
        } catch (Exception e) {
            log.error("状态归一失败", e);
            return 0;
        }
    }

    /**
     * 步骤 3：非法状态兜底，不在五值白名单内的历史值统一归"未玩"。
     */
    private int normalizeInvalidStatus() {
        try {
            int rows = jdbcTemplate.update(
                    "UPDATE user_games SET status = ? WHERE status NOT IN (?, ?, ?, ?, ?)",
                    STATUS_NOT_PLAYED, STATUS_PLAYING, STATUS_COMPLETED, STATUS_NOT_PLAYED, STATUS_ON_HOLD, STATUS_DROPPED);
            log.info("非法状态兜底：→未玩，影响 {} 行", rows);
            return rows;
        } catch (Exception e) {
            log.error("非法状态兜底失败", e);
            return 0;
        }
    }

    /**
     * 步骤 4：清空非"已完成"行的完成时间，避免"未玩但有完成时间"的脏组合被年度通关误计。
     */
    private int clearCompletedAtForNonCompleted() {
        try {
            int rows = jdbcTemplate.update(
                    "UPDATE user_games SET completed_at = NULL WHERE status <> ? AND completed_at IS NOT NULL",
                    STATUS_COMPLETED);
            if (rows > 0) {
                log.info("清空非已完成行的完成时间，影响 {} 行", rows);
            }
            return rows;
        } catch (Exception e) {
            log.error("清空非已完成行完成时间失败", e);
            return 0;
        }
    }

    /**
     * 步骤 5-8（同一事务）：演示通关锚点 → 历史完成时间通用回填 → 演示成就锚点 → 写一次性标记。
     * 锚点必须先于通用回填，否则 game 9 / game 7 会被 last_played 占用，年度通关退化为 0。
     */
    private int applyDemoAnchorsIfNeeded(TransactionTemplate transactionTemplate) {
        try {
            Integer affected = transactionTemplate.execute(txStatus -> {
                if (isSeedApplied(SEED_DEMO_ANCHORS)) {
                    log.info("演示锚点已执行（{}），跳过锚点与历史回填", SEED_DEMO_ANCHORS);
                    return 0;
                }

                int lastYearAnchor = applyCompletionAnchor(9L, LAST_YEAR, "12-28");
                int currentYearAnchor = applyCompletionAnchor(7L, CURRENT_YEAR, "06-15");
                log.info("演示通关锚点：game 9 上一年 {} 行，game 7 当前年 {} 行", lastYearAnchor, currentYearAnchor);

                int backfilled = jdbcTemplate.update(
                        "UPDATE user_games SET completed_at = COALESCE(last_played, created_at) " +
                                "WHERE status = ? AND completed_at IS NULL",
                        STATUS_COMPLETED);
                log.info("历史完成时间回填：COALESCE(last_played, created_at)，影响 {} 行", backfilled);

                int achievementAnchors = 0;
                achievementAnchors += insertAchievementAnchorIfAbsent(10L, LAST_YEAR, "12-05");
                achievementAnchors += insertAchievementAnchorIfAbsent(21L, LAST_YEAR, "12-18");
                achievementAnchors += insertAchievementAnchorIfAbsent(22L, CURRENT_YEAR, "05-20");
                achievementAnchors += insertAchievementAnchorIfAbsent(37L, CURRENT_YEAR, "07-08");
                log.info("演示成就锚点：新增 {} 条", achievementAnchors);

                markSeedApplied(SEED_DEMO_ANCHORS);
                return lastYearAnchor + currentYearAnchor + backfilled + achievementAnchors;
            });
            return affected == null ? 0 : affected;
        } catch (Exception e) {
            log.error("演示锚点迁移失败，本次事务已回滚，下次启动可重试", e);
            return 0;
        }
    }

    /**
     * 步骤 9-10（同一事务）：演示标签种子 3 条 + 写一次性标记。
     * 标签种子不得写入 data.sql，否则用户删除标签后重启会复活。
     */
    private int applyTagSeedIfNeeded(TransactionTemplate transactionTemplate) {
        try {
            Integer affected = transactionTemplate.execute(txStatus -> {
                if (isSeedApplied(SEED_USER_GAME_TAGS)) {
                    log.info("演示标签种子已执行（{}），跳过", SEED_USER_GAME_TAGS);
                    return 0;
                }

                int rows = 0;
                rows += insertTagSeedIfUserGameExists(1L, "神作");
                rows += insertTagSeedIfUserGameExists(1L, "年度最爱");
                rows += insertTagSeedIfUserGameExists(5L, "CRPG");
                log.info("演示标签种子：新增 {} 条", rows);

                markSeedApplied(SEED_USER_GAME_TAGS);
                return rows;
            });
            return affected == null ? 0 : affected;
        } catch (Exception e) {
            log.error("演示标签种子迁移失败，本次事务已回滚，下次启动可重试", e);
            return 0;
        }
    }

    private int applyCompletionAnchor(Long gameId, String yearExpression, String monthDay) {
        String sql = "UPDATE user_games SET completed_at = " +
                "STR_TO_DATE(CONCAT(" + yearExpression + ", '-" + monthDay + " 20:00:00'), '%Y-%m-%d %H:%i:%s') " +
                "WHERE user_id = 1 AND game_id = ? AND status = ? AND completed_at IS NULL";
        return jdbcTemplate.update(sql, gameId, STATUS_COMPLETED);
    }

    private int insertAchievementAnchorIfAbsent(Long achievementId, String yearExpression, String monthDay) {
        String sql = "INSERT INTO user_achievements (user_id, achievement_id, unlocked_at) " +
                "SELECT 1, ?, STR_TO_DATE(CONCAT(" + yearExpression + ", '-" + monthDay + " 20:00:00'), '%Y-%m-%d %H:%i:%s') " +
                "FROM DUAL WHERE NOT EXISTS (" +
                "SELECT 1 FROM user_achievements WHERE user_id = 1 AND achievement_id = ?)";
        return jdbcTemplate.update(sql, achievementId, achievementId);
    }

    private int insertTagSeedIfUserGameExists(Long gameId, String tag) {
        String sql = "INSERT INTO user_game_tags (user_id, game_id, tag) " +
                "SELECT 1, ?, ? FROM DUAL WHERE EXISTS (" +
                "SELECT 1 FROM user_games WHERE user_id = 1 AND game_id = ?) " +
                "ON DUPLICATE KEY UPDATE id = id";
        return jdbcTemplate.update(sql, gameId, tag, gameId);
    }

    private boolean isSeedApplied(String seedName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_seed_log WHERE seed_name = ?", Integer.class, seedName);
        return count != null && count > 0;
    }

    private void markSeedApplied(String seedName) {
        jdbcTemplate.update(
                "INSERT INTO app_seed_log (seed_name, executed_at) VALUES (?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE seed_name = seed_name",
                seedName);
        log.info("已写入一次性种子标记：{}", seedName);
    }

    private void upsertDemoUser() {
        String sql = "INSERT INTO users (id, username, avatar, email, phone, password_hash, bio, level, join_date, " +
                "platform, platform_id, custom_tags, gender, region, preferred_language) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE id = id";
        try {
            jdbcTemplate.update(sql,
                    1L,
                    "GameMaster",
                    "https://picsum.photos/seed/avatar/200/200",
                    "master@gamestats.com",
                    "13800138000",
                    "e10adc3949ba59abbe56e057f20f883e",
                    "热爱游戏，享受生活",
                    42,
                    java.time.LocalDate.of(2023, 1, 15),
                    "demo",
                    "13800138000",
                    "[\"硬核玩家\",\"成就猎人\",\"RPG爱好者\",\"深夜玩家\"]",
                    "male",
                    "北京",
                    "zh-CN"
            );
            log.info("演示账号已就绪");
        } catch (Exception e) {
            log.error("演示账号初始化失败", e);
        }
    }

    private boolean addColumnIfNotExists(String tableName, String columnName, String ddl) {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("已新增列 {}.{}", tableName, columnName);
            return true;
        }
        log.debug("{}.{} 已存在，跳过迁移", tableName, columnName);
        return false;
    }
}