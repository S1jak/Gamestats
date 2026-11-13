-- ============================================================
-- game_stats 种子数据
-- ON DUPLICATE KEY UPDATE id=id：已存在则空操作，不覆盖用户后续修改
-- 状态值使用中文：未玩 / 在玩 / 已完成 / 搁置 / 弃坑
-- ============================================================

-- ============================================================
-- 0. 数据库列迁移已迁移到 Java 侧 DatabaseMigrationRunner，
--    避免 Spring ScriptUtils 不支持 DELIMITER / 存储过程的问题
-- ============================================================

-- ============================================================
-- 1. 用户（演示账号：13800138000 / 123456）
--    已迁移到 Java 侧 DatabaseMigrationRunner，避免 data.sql 在列迁移前执行插入失败
-- ============================================================

-- ============================================================
-- 2. 游戏目录（全局）
-- ============================================================
INSERT INTO games (id, name, cover, description, release_date, developer, rating,
                   platform, platform_game_id)
VALUES
    (1, '艾尔登法环', 'https://picsum.photos/seed/game1/400/500',
     '由FromSoftware开发的一款黑暗幻想风开放世界动作RPG。',
     '2022-02-25', 'FromSoftware', 9.5, 'demo', 'game1'),
    (2, '赛博朋克 2077', 'https://picsum.photos/seed/game2/400/500',
     'CD Projekt Red开发的开放世界科幻RPG。',
     '2020-12-10', 'CD Projekt Red', 8.5, 'demo', 'game2'),
    (3, '荒野大镖客：救赎 2', 'https://picsum.photos/seed/game3/400/500',
     'Rockstar开发的西部开放世界动作冒险游戏。',
     '2018-10-26', 'Rockstar', 9.8, 'demo', 'game3'),
    (4, '哈迪斯', 'https://picsum.photos/seed/game4/400/500',
     'Supergiant Games开发的Roguelike动作游戏。',
     '2020-09-17', 'Supergiant Games', 9.3, 'demo', 'game4')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 3. 游戏类型字典
-- ============================================================
INSERT INTO game_genres (id, name) VALUES
    (1, '魂系'),
    (2, '开放世界'),
    (3, '硬核'),
    (4, '年度最佳'),
    (5, 'RPG'),
    (6, '科幻'),
    (7, '剧情'),
    (8, '动作'),
    (9, '西部'),
    (10, 'Roguelike'),
    (11, '独立')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 4. 游戏-类型关联
-- ============================================================
INSERT INTO game_genre_relations (id, game_id, genre_id) VALUES
    -- 艾尔登法环: 魂系, 开放世界, 硬核, 年度最佳
    (1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4),
    -- 赛博朋克 2077: 开放世界, 科幻, 剧情
    (5, 2, 2), (6, 2, 6), (7, 2, 7),
    -- 荒野大镖客：救赎 2: 开放世界, 剧情, 西部
    (8, 3, 2), (9, 3, 7), (10, 3, 9),
    -- 哈迪斯: Roguelike, 动作, 独立
    (11, 4, 10), (12, 4, 8), (13, 4, 11)
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 5. 成就定义（game_id = 1 艾尔登法环）
-- ============================================================
INSERT INTO achievements (id, game_id, name, description, icon, platform_achievement_id) VALUES
    (1, 1, '艾尔登之王', '获得所有结局',     'https://picsum.photos/seed/a1/64/64', 'ach1'),
    (2, 1, '碎星将军',   '击败拉塔恩',       'https://picsum.photos/seed/a2/64/64', 'ach2'),
    (3, 1, '月光大剑',   '获得暗月大剑',     'https://picsum.photos/seed/a3/64/64', 'ach3'),
    (4, 1, '黄金律法',   '修复黄金律法',     'https://picsum.photos/seed/a4/64/64', 'ach4'),
    (5, 1, '群星时代',   '进入群星时代',     'https://picsum.photos/seed/a5/64/64', 'ach5'),
    (6, 1, '癫火之王',   '成为癫火之王',     'https://picsum.photos/seed/a6/64/64', 'ach6')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 6. 用户游戏库（user_id = 1，中文状态）
-- ============================================================
INSERT INTO user_games (id, user_id, game_id, play_time, last_played, status, notes) VALUES
    (1, 1, 1, 18650, '2024-01-15 20:30:00', '已完成',
     '这是一款令人难以忘怀的游戏。开放世界的设计、战斗系统、Boss设计都非常出色。'),
    (2, 1, 2,  9800, '2024-03-02 21:00:00', '在玩',
     '夜之城的故事很迷人，资料片表现更佳。'),
    (3, 1, 3, 24000, '2024-02-18 19:15:00', '已完成',
     '亚瑟的故事令人动容。'),
    (4, 1, 4,  5600, '2024-01-30 22:00:00', '在玩',
     '爽快利落的战斗，每次死亡都有收获。')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 7. 用户成就解锁（user_id = 1，艾尔登法环 6 个全解锁）
-- ============================================================
INSERT INTO user_achievements (id, user_id, achievement_id, unlocked_at) VALUES
    (1, 1, 1, '2024-01-10 18:00:00'),
    (2, 1, 2, '2023-12-15 21:30:00'),
    (3, 1, 3, '2023-11-20 20:00:00'),
    (4, 1, 4, '2024-01-10 22:00:00'),
    (5, 1, 5, '2024-01-12 19:45:00'),
    (6, 1, 6, '2024-01-14 20:15:00')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 8. 用户游戏平台拥有（user_id = 1，game_id = 1）
-- ============================================================
INSERT INTO user_game_platforms (id, user_id, game_id, name, owned) VALUES
    (1, 1, 1, 'Steam',  1),
    (2, 1, 1, 'PS5',    0),
    (3, 1, 1, 'Xbox',   0),
    (4, 1, 1, 'Switch', 0)
ON DUPLICATE KEY UPDATE id = id;


-- ============================================================
-- 9. 追加游戏目录（id 5-10）
-- ============================================================
INSERT INTO games (id, name, cover, description, release_date, developer, rating,
                   platform, platform_game_id) VALUES
    (5, '博德之门3', 'https://picsum.photos/seed/game5/400/500',
     'Larian Studios开发的回合制CRPG，基于D&D 5e规则。',
     '2023-08-03', 'Larian Studios', 9.8, 'demo', 'game5'),

    (7, '霍格沃茨之遗', 'https://picsum.photos/seed/game7/400/500',
     'Avalanche Software开发的开放世界RPG，沉浸式魔法世界体验。',
     '2023-02-10', 'Avalanche Software', 8.2, 'demo', 'game7'),

    (9, '只狼：影逝二度', 'https://picsum.photos/seed/game9/400/500',
     'FromSoftware开发的忍者动作游戏，以"架势"为核心的硬核战斗。',
     '2019-03-22', 'FromSoftware', 9.2, 'demo', 'game9'),
    (10, '巫师3：狂猎', 'https://picsum.photos/seed/game10/400/500',
     'CD Projekt Red开发的开放世界RPG，杰洛特的最终冒险。',
     '2015-05-19', 'CD Projekt Red', 9.6, 'demo', 'game10')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 10. 追加游戏-类型关联（id 14-30）
--     复用已有 game_genres：1魂系 2开放世界 3硬核 4年度最佳
--                          5RPG 6科幻 7剧情 8动作 9西部 10Roguelike 11独立
-- ============================================================
INSERT INTO game_genre_relations (id, game_id, genre_id) VALUES
    -- 博德之门3: RPG, 剧情, 年度最佳
    (14, 5, 5), (15, 5, 7), (16, 5, 4),
    -- 霍格沃茨之遗: RPG, 开放世界
    (20, 7, 5), (21, 7, 2),
    -- 只狼：影逝二度: 魂系, 硬核, 动作
    (25, 9, 1), (26, 9, 3), (27, 9, 8),
    -- 巫师3：狂猎: RPG, 开放世界, 剧情
    (28, 10, 5), (29, 10, 2), (30, 10, 7)
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 11. 追加成就定义（id 7-40，共 34 个）
-- ============================================================
INSERT INTO achievements (id, game_id, name, description, icon, platform_achievement_id) VALUES
    -- 博德之门3 (game_id = 5)
    (7,  5, '英雄崛起',   '完成第一章',       'https://picsum.photos/seed/a7/64/64',  'ach7'),
    (8,  5, '星界旅行',   '首次进入星界',     'https://picsum.photos/seed/a8/64/64',  'ach8'),
    (9,  5, '真魂者',     '获得真魂力量',     'https://picsum.photos/seed/a9/64/64',  'ach9'),
    (10, 5, '龙之血脉',   '与巨龙结盟',       'https://picsum.photos/seed/a10/64/64', 'ach10'),
    (11, 5, '终极抉择',   '完成游戏主线',     'https://picsum.photos/seed/a11/64/64', 'ach11'),
    (12, 5, '队友羁绊',   '完成所有队友任务', 'https://picsum.photos/seed/a12/64/64', 'ach12'),
    -- 霍格沃茨之遗 (game_id = 7)
    (19, 7, '魔法初学者', '学会第一个咒语',   'https://picsum.photos/seed/a19/64/64', 'ach19'),
    (20, 7, '学院杯',     '赢得学院杯',       'https://picsum.photos/seed/a20/64/64', 'ach20'),
    (21, 7, '神器收藏',   '收集所有神器',     'https://picsum.photos/seed/a21/64/64', 'ach21'),
    (22, 7, '黑魔法师',   '击败黑魔法师',     'https://picsum.photos/seed/a22/64/64', 'ach22'),
    (23, 7, '霍格沃茨传奇', '完成游戏',       'https://picsum.photos/seed/a23/64/64', 'ach23'),
    -- 只狼：影逝二度 (game_id = 9)
    (30, 9, '忍者之刃',   '获得忍义手',       'https://picsum.photos/seed/a30/64/64', 'ach30'),
    (31, 9, '剑圣之死',   '击败剑圣苇名一心', 'https://picsum.photos/seed/a31/64/64', 'ach31'),
    (32, 9, '不死之身',   '获得不死之力',     'https://picsum.photos/seed/a32/64/64', 'ach32'),
    (33, 9, '复仇完成',   '完成复仇',         'https://picsum.photos/seed/a33/64/64', 'ach33'),
    (34, 9, '断绝不死',   '完成游戏',         'https://picsum.photos/seed/a34/64/64', 'ach34'),
    -- 巫师3：狂猎 (game_id = 10)
    (35, 10, '猎魔人之道', '完成白果园章节',   'https://picsum.photos/seed/a35/64/64', 'ach35'),
    (36, 10, '维吉玛之主', '完成维吉玛任务线', 'https://picsum.photos/seed/a36/64/64', 'ach36'),
    (37, 10, '史凯利杰之王', '完成史凯利杰线', 'https://picsum.photos/seed/a37/64/64', 'ach37'),
    (38, 10, '狂猎之死',   '击败狂猎首领',     'https://picsum.photos/seed/a38/64/64', 'ach38'),
    (39, 10, '希里之命运', '决定希里的命运',   'https://picsum.photos/seed/a39/64/64', 'ach39'),
    (40, 10, '模糊时间',   '完成游戏主线',     'https://picsum.photos/seed/a40/64/64', 'ach40')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 12. 追加用户游戏库（user_id = 1，id 5-10，中文状态）
-- ============================================================
INSERT INTO user_games (id, user_id, game_id, play_time, last_played, status, notes) VALUES
    (5,  1, 5,  12340, '2024-01-20 21:30:00', '在玩',
     'CRPG的巅峰之作，选择与后果系统令人惊叹。'),
    (7,  1, 7,   5430, '2024-01-05 19:45:00', '已完成',
     '魔法世界的沉浸感很强，但后期略显重复。'),
    (9,  1, 9,   3500, '2023-11-20 21:00:00', '已完成',
     '架势系统独树一帜，硬核但极具成就感。'),
    (10, 1, 10,  8500, '2023-10-15 20:30:00', '已完成',
     '杰洛特的故事完美落幕，剧情与世界观顶级。')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 13. 追加用户成就解锁（user_id = 1，id 7-30）
--     战神/只狼全解锁，其余部分解锁
-- ============================================================
INSERT INTO user_achievements (id, user_id, achievement_id, unlocked_at) VALUES
    -- 博德之门3：4/6 解锁
    (7,  1, 7,  '2023-09-15 10:00:00'),
    (8,  1, 8,  '2023-10-20 14:30:00'),
    (9,  1, 9,  '2023-11-10 21:00:00'),
    (10, 1, 11, '2024-01-15 20:00:00'),
    -- 霍格沃茨：2/5 解锁
    (14, 1, 19, '2023-03-05 20:00:00'),
    (15, 1, 20, '2023-04-10 22:00:00'),
    -- 只狼：5/5 全解锁
    (22, 1, 30, '2019-04-10 20:00:00'),
    (23, 1, 31, '2019-05-05 21:30:00'),
    (24, 1, 32, '2019-04-20 19:00:00'),
    (25, 1, 33, '2019-06-01 22:00:00'),
    (26, 1, 34, '2019-06-15 20:45:00'),
    -- 巫师3：4/6 解锁
    (27, 1, 35, '2015-06-10 20:00:00'),
    (28, 1, 36, '2015-07-05 21:00:00'),
    (29, 1, 38, '2015-08-20 22:30:00'),
    (30, 1, 40, '2015-09-15 19:30:00')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 14. 追加用户游戏平台拥有（user_id = 1，每款 Steam+PS5）
-- ============================================================
INSERT INTO user_game_platforms (id, user_id, game_id, name, owned) VALUES
    -- 博德之门3
    (5,  1, 5,  'Steam', 1),
    (6,  1, 5,  'PS5',   1),
    -- 霍格沃茨之遗
    (9,  1, 7,  'Steam', 1),
    (10, 1, 7,  'PS5',   1),
    -- 只狼：影逝二度
    (13, 1, 9,  'Steam', 1),
    (14, 1, 9,  'PS5',   0),
    -- 巫师3：狂猎
    (15, 1, 10, 'Steam', 1),
    (16, 1, 10, 'PS5',   0)
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================
-- 15. 游玩会话记录（user_id = 1，最近 360 天每日一条）
--     使用 MySQL 8 递归 CTE 生成日期序列：从今天往前 359 天到今天
--     game_id 在「已存在的 games」中按 id 顺序轮换（JOIN ROW_NUMBER 子查询，
--     自动跳过已删除的游戏，如塞尔达/战神）；duration 随 n 与周末变化（周末 +120 分钟）
--     DELETE + INSERT 保证重启幂等：每次重置为干净的 360 条最新数据
--     cte_max_recursion_depth 默认 1000，360 < 1000 无需调整
-- ============================================================
DELETE FROM play_records WHERE user_id = 1;

INSERT INTO play_records (user_id, game_id, start_time, end_time, duration)
WITH RECURSIVE date_seq(d, n) AS (
    SELECT DATE_SUB(CURDATE(), INTERVAL 359 DAY), 1
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY), n + 1 FROM date_seq WHERE n < 360
)
SELECT
    1 AS user_id,
    g.id AS game_id,
    CONCAT(ds.d, ' 20:00:00') AS start_time,
    DATE_ADD(
        CONCAT(ds.d, ' 20:00:00'),
        INTERVAL (60 + (ds.n % 4) * 60 + CASE WHEN DAYOFWEEK(ds.d) IN (1, 7) THEN 120 ELSE 0 END) MINUTE
    ) AS end_time,
    60 + (ds.n % 4) * 60 + CASE WHEN DAYOFWEEK(ds.d) IN (1, 7) THEN 120 ELSE 0 END AS duration
FROM date_seq ds
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM games
) g ON g.rn = ((ds.n - 1) % (SELECT COUNT(*) FROM games)) + 1;

-- ============================================================
-- 16. 年度报告快照（user_id = 1，2025 年）
--     首次访问 /api/report/yearly?year=2025 时会被 upsert 覆盖为实时聚合值
-- ============================================================
INSERT INTO yearly_reports (user_id, year, favorite_game_id, total_play_time, play_style, achievements_count) VALUES
    (1, 2025, 1, 20340, '资深玩家', 30)
ON DUPLICATE KEY UPDATE id = id;
