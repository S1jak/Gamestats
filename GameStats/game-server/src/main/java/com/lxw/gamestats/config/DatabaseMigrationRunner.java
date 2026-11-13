package com.lxw.gamestats.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库版本化迁移：为已存在的 users 表补齐新增列。
 * <p>
 * 因 Spring Boot 的 ScriptUtils 不支持 DELIMITER / 存储过程，
 * 故在应用启动后通过 JDBC Template + information_schema 动态判断并补列。
 * 该 Runner 幂等：列已存在不会重复执行 ALTER。
 */
@Slf4j
@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Map<String, String> USER_COLUMNS = new HashMap<>();

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
        try {
            // 1. 先为旧 users 表补齐新增列（data.sql 中的 INSERT 已迁移到此处）
            for (Map.Entry<String, String> entry : USER_COLUMNS.entrySet()) {
                addColumnIfNotExists(entry.getKey(), entry.getValue());
            }
            // 2. 插入/更新演示账号（幂等）
            upsertDemoUser();
            log.info("数据库迁移完成");
        } catch (Exception e) {
            log.error("数据库迁移失败", e);
        }
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

    private void addColumnIfNotExists(String columnName, String ddl) {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("已新增列 users.{}", columnName);
        } else {
            log.debug("users.{} 已存在，跳过迁移", columnName);
        }
    }
}
