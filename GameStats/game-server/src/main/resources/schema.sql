-- ============================================================
-- game_stats 数据库表结构 (DDL)
-- 由 application.yml 的 spring.sql.init.mode=always 自动执行
-- CREATE TABLE IF NOT EXISTS 保证幂等，重启不会破坏数据
--
-- 注意：若 game_stats 库已存在旧表，需先 DROP DATABASE game_stats
--       让应用重新创建，或手动 ALTER TABLE 补齐字段。
--
-- 迁移说明：本脚本只负责"新库建表"与自定义标签/种子标记两张新表；
--           旧库缺失的 user_games.completed_at、yearly_reports.snapshot_json/
--           generated_at 由 DatabaseMigrationRunner 在应用启动时幂等补列。
--           本阶段不手工执行任何 DDL/DML，需在 IDE 启动后端后由迁移自动执行。
-- ============================================================

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username         VARCHAR(255) NOT NULL COMMENT '用户名',
    avatar           VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    email            VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
    phone            VARCHAR(20)  DEFAULT NULL COMMENT '手机号(本地登录用)',
    password_hash    VARCHAR(255) DEFAULT NULL COMMENT '密码(MD5)',
    bio              VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    level            INT          NOT NULL DEFAULT 1 COMMENT '等级',
    join_date        DATE         DEFAULT NULL COMMENT '加入日期',
    custom_tags      VARCHAR(500) DEFAULT NULL COMMENT '自定义标签(JSON数组)',
    platform         VARCHAR(50)  NOT NULL COMMENT '第三方登录提供商',
    platform_id      VARCHAR(255) NOT NULL COMMENT '第三方平台ID / 手机号',
    access_token     VARCHAR(500) DEFAULT NULL COMMENT '第三方access_token',
    refresh_token    VARCHAR(500) DEFAULT NULL COMMENT '第三方refresh_token',
    token_expires_at DATETIME     DEFAULT NULL COMMENT 'token过期时间',
    gender           VARCHAR(16)  DEFAULT 'other' COMMENT '性别',
    birthday         DATE         DEFAULT NULL COMMENT '生日',
    region           VARCHAR(32)  DEFAULT NULL COMMENT '地区',
    preferred_language VARCHAR(8) DEFAULT 'zh-CN' COMMENT '偏好语言',
    profile_visibility VARCHAR(16) DEFAULT 'public' COMMENT '资料可见性',
    allow_comments   TINYINT(1)   DEFAULT 1 COMMENT '允许评论',
    email_notify     TINYINT(1)   DEFAULT 1 COMMENT '邮件通知',
    site_notify      TINYINT(1)   DEFAULT 1 COMMENT '站内通知',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_id (platform, platform_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 游戏目录表（全局游戏信息，与用户无关）
-- ============================================================
CREATE TABLE IF NOT EXISTS games (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name             VARCHAR(255) NOT NULL COMMENT '游戏名称',
    cover            VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    description      TEXT         COMMENT '游戏描述',
    release_date     DATE         DEFAULT NULL COMMENT '发行日期',
    developer        VARCHAR(128) DEFAULT NULL COMMENT '开发商',
    rating           DECIMAL(3,1) DEFAULT NULL COMMENT '评分',
    platform         VARCHAR(50)  NOT NULL COMMENT '来源平台',
    platform_game_id VARCHAR(255) NOT NULL COMMENT '平台内游戏ID',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_game_id (platform, platform_game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏目录表';

-- ============================================================
-- 3. 游戏类型字典表
-- ============================================================
CREATE TABLE IF NOT EXISTS game_genres (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(100) NOT NULL COMMENT '类型名称',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_genre_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏类型字典表';

-- ============================================================
-- 4. 游戏-类型关联表（多对多）
-- ============================================================
CREATE TABLE IF NOT EXISTS game_genre_relations (
    id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    game_id  BIGINT NOT NULL COMMENT '游戏ID',
    genre_id BIGINT NOT NULL COMMENT '类型ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_genre (game_id, genre_id),
    KEY idx_genre_id (genre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏类型关联表';

-- ============================================================
-- 5. 成就定义表（全局，属于某款游戏）
-- ============================================================
CREATE TABLE IF NOT EXISTS achievements (
    id                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    game_id                BIGINT       NOT NULL COMMENT '游戏ID',
    name                   VARCHAR(255) NOT NULL COMMENT '成就名称',
    description            TEXT         COMMENT '成就描述',
    icon                   VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
    platform_achievement_id VARCHAR(255) NOT NULL COMMENT '平台内成就ID',
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_achievement (game_id, platform_achievement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就定义表';

-- ============================================================
-- 6. 用户游戏库表（用户拥有的游戏记录）
-- ============================================================
CREATE TABLE IF NOT EXISTS user_games (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    game_id    BIGINT      NOT NULL COMMENT '游戏ID',
    play_time  INT         NOT NULL DEFAULT 0 COMMENT '游玩时长(分钟)',
    last_played DATETIME   DEFAULT NULL COMMENT '最后游玩时间',
    status     VARCHAR(50) NOT NULL DEFAULT '未玩' COMMENT '状态：未玩/在玩/已完成/搁置/弃坑',
    completed_at DATETIME  DEFAULT NULL COMMENT '完成时间',
    notes      TEXT        COMMENT '游戏笔记',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_game (user_id, game_id),
    KEY idx_game_id (game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户游戏库表';

-- ============================================================
-- 7. 用户成就解锁表
-- ============================================================
CREATE TABLE IF NOT EXISTS user_achievements (
    id              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT   NOT NULL COMMENT '用户ID',
    achievement_id  BIGINT   NOT NULL COMMENT '成就ID',
    unlocked_at     DATETIME NOT NULL COMMENT '解锁时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_achievement (user_id, achievement_id),
    KEY idx_achievement_id (achievement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成就解锁表';

-- ============================================================
-- 8. 游玩记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS play_records (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT   NOT NULL COMMENT '用户ID',
    game_id    BIGINT   NOT NULL COMMENT '游戏ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time   DATETIME NOT NULL COMMENT '结束时间',
    duration   INT      NOT NULL COMMENT '时长(分钟)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_game_id (game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游玩记录表';

-- ============================================================
-- 9. 年度报告表
-- ============================================================
CREATE TABLE IF NOT EXISTS yearly_reports (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id            BIGINT       NOT NULL COMMENT '用户ID',
    year               INT          NOT NULL COMMENT '年份',
    favorite_game_id   BIGINT       DEFAULT NULL COMMENT '年度最爱游戏ID',
    total_play_time    INT          NOT NULL DEFAULT 0 COMMENT '总游玩时长(分钟)',
    play_style         VARCHAR(100) DEFAULT NULL COMMENT '游玩风格',
    achievements_count INT          NOT NULL DEFAULT 0 COMMENT '成就解锁数',
    snapshot_json      JSON         DEFAULT NULL COMMENT '完整年度报告快照(JSON)',
    generated_at       DATETIME     DEFAULT NULL COMMENT '快照生成时间',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_year (user_id, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度报告表';

-- ============================================================
-- 10. 用户游戏平台拥有表（补齐前端 platforms 字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS user_game_platforms (
    id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT      NOT NULL COMMENT '用户ID',
    game_id BIGINT      NOT NULL COMMENT '游戏ID',
    name    VARCHAR(32) NOT NULL COMMENT '平台名称',
    owned   TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否拥有: 0否 1是',
    PRIMARY KEY (id),
    KEY idx_user_game (user_id, game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户游戏平台拥有表';

-- ============================================================
-- 补充字段：users.custom_tags（幂等添加，兼容已存在的旧数据库）
-- CREATE TABLE IF NOT EXISTS 不会为已存在的表自动追加新列，
-- 故在此用 information_schema 判断后 ALTER，保证每次启动幂等。
-- ============================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'custom_tags');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE users ADD COLUMN custom_tags VARCHAR(500) DEFAULT NULL COMMENT ''自定义标签(JSON数组)''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- ============================================================
-- 11. 用户游戏自定义标签表（user × game × tag，幂等建表）
--     标签与游戏类型解耦：类型仍由 game_genre_relations 提供。
--     唯一键保证同一用户同一游戏的标签不重复；不建外键，归属由应用层校验。
-- ============================================================
CREATE TABLE IF NOT EXISTS user_game_tags (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    game_id    BIGINT      NOT NULL COMMENT '游戏ID',
    tag        VARCHAR(50) NOT NULL COMMENT '用户自定义标签',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_game_tag (user_id, game_id, tag),
    KEY idx_user_game (user_id, game_id),
    KEY idx_user_tag (user_id, tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户游戏自定义标签表';

-- ============================================================
-- 12. 一次性种子执行记录表
--     每个一次性种子执行前先查 seed_name；已存在则整段跳过。
--     业务数据与标记在同一事务内提交，避免"数据已插、标记未写"导致重复。
--     用户删除业务数据不改变标记，重启不复活演示种子。
-- ============================================================
CREATE TABLE IF NOT EXISTS app_seed_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    seed_name   VARCHAR(64) NOT NULL COMMENT '种子名称',
    executed_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seed_name (seed_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性种子执行记录表';
