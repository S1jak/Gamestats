# GameStats 游戏数据统计平台 · 项目结构解析与多智能体协作交付

> **文档性质**：只读勘察 + 结构与功能解读 + Agent Team 协作交付记录 + 缺陷修复决策清单  
> **代码改动**：**零**。本文件（含第二轮更新）未修改任何业务代码、配置、SQL 或依赖。  
> **生成方式**：Leader 统一编排，4 个专业 Teammate 并行/串行协作（产品、架构/前端、开发、QA），各章节标注负责人。  
> **勘察时间**：2026-11-13  
> **第二轮更新（2026-11-13）**：① 修正环境结论——项目在 **IntelliJ IDEA 中运行**（IDE 内置 Maven + 项目 SDK `openjdk-26`），**无需安装 JDK 26 / Maven**；② 新增 **[第 12 章 缺陷审阅与修复决策清单](#12-缺陷审阅与修复决策清单第二轮新增--待用户决策)**，把 26 条缺陷整理为可逐条决策的清单；③ BUG-21（环境不可构建）据此**关闭**。本轮仍未修改任何业务代码。  
> **事实来源**：`GameStats/` 后端源码、`qianduan/` 前端源码、`schema.sql` / `data.sql`、本机 MySQL 8.0.44 只读查询、`GameStats/.idea/misc.xml` 工程配置。

---

## 目录

- [0. Leader 摘要](#0-leader-摘要)
- [1. Leader：Task List（负责人 / 依赖 / 并行 / 交付物）](#1-leader-task-list负责人--依赖--并行--交付物)
- [2. 项目结构解析（架构/前端 Teammate）](#2-项目结构解析架构前端-teammate)
- [3. 功能解读（产品 + 开发 Teammate）](#3-功能解读产品--开发-teammate)
- [4. 产品 Teammate：用户旅程、功能边界、验收案例](#4-产品-teammate用户旅程功能边界验收案例)
- [5. 架构/前端 Teammate：最简技术方案、页面结构、接口契约](#5-架构前端-teammate最简技术方案页面结构接口契约)
- [6. 开发 Teammate：核心数据/业务逻辑与页面交互](#6-开发-teammate核心数据业务逻辑与页面交互)
- [7. QA Teammate：测试设计、执行记录、可复现缺陷](#7-qa-teammate测试设计执行记录可复现缺陷)
- [8. Leader：冲突与决策记录](#8-leader冲突与决策记录)
- [9. 运行命令与测试步骤](#9-运行命令与测试步骤)
- [10. 完成 / 未完成事项](#10-完成--未完成事项)
- [11. 风险、技术债与后续改动接入指南](#11-风险技术债与后续改动接入指南)
- [12. 缺陷审阅与修复决策清单（第二轮新增 · 待用户决策）](#12-缺陷审阅与修复决策清单第二轮新增--待用户决策)
- [附录 A：文件地图](#附录-a文件地图)
- [附录 B：接口快速索引](#附录-b接口快速索引)
- [附录 C：当前数据库快照](#附录-c当前数据库快照)

---

## 0. Leader 摘要

**一句话结论**：GameStats 是一个"单体后端（Spring Boot 3 模块）+ Vue 3 单页应用"的个人游戏数据看板。核心链路（登录 → 数据中心 → 游戏库 → 游戏详情 → 个人中心 → 年度报告）已打通并接入真实 MySQL 数据；社交中心、第三方账号绑定、海报导出、偏好生效等为**前端占位/Mock**；存在若干可复现的数据一致性与交互缺陷，适合作为下一轮页面改造与功能增加的基础。

| 维度 | 结论 |
|---|---|
| 仓库组成 | `GameStats/`（Maven 多模块后端）+ `qianduan/`（Vue 3 前端）+ 项目说明 docx/pptx |
| 后端 | Spring Boot 4.1.0、Java 26、MyBatis 4.0.1、MySQL 8、JWT（HS256）、无 Redis/无 MQ |
| 前端 | Vue 3.3 + Vite 4 + Element Plus 2.4.4 + ECharts 5 + Pinia 2 + axios，暗色主题 |
| 鉴权 | 手机号 + 密码（MD5），JWT 7 天；`/api/auth/**` 放行，其余 `/api/**` 走 `JwtInterceptor` |
| 数据 | 10 张表；启动时 `schema.sql` + `data.sql` 自动初始化，`DatabaseMigrationRunner` 幂等补列与演示账号 |
| 当前可用性 | 数据库已就绪且含真实种子数据；后端在 **IntelliJ IDEA 中直接运行**（项目 SDK `openjdk-26` 已配置、使用 IDE 内置 Maven），**无需安装 JDK 26 / Maven**；命令行仅作 CI/换机备选 |
| 主要短板 | 状态枚举不统一（"在玩/正在玩"）、详情页标签不持久化、年度报告口径混用、分页无效、多处 Mock 未标注、密码 MD5 无盐且密钥硬编码 |

**交付物**：本文件（第二轮新增内容见 [第 12 章](#12-缺陷审阅与修复决策清单第二轮新增--待用户决策)）。  
**建议下一步**：先按 [12.1 缺陷总览表](#121-缺陷总览表26-条按严重级别分组) 逐条决策，确认"本轮修复范围"（推荐先修 [12.5](#125-对后续页面改动--功能新增的影响判断) 中列出的阻塞项），再进入具体需求；修复前统一用 IntelliJ IDEA 启动基线与验证。

---

## 1. Leader：Task List（负责人 / 依赖 / 并行 / 交付物）

### 1.1 任务清单

| 编号 | 任务 | 负责人 | 依赖 | 并行批次 | 交付物 | 状态 |
|---|---|---|---|---|---|---|
| T01 | 仓库、模块、包结构、依赖方向、构建方式勘察 | 架构/前端 | 无 | P0（与 T02/T03/T09 并行） | 第 2 章结构解析 | ✅ |
| T02 | 页面与功能盘点、功能边界、占位识别 | 产品 | 无 | P0 | 第 3、4 章 | ✅ |
| T03 | 表结构、种子数据、迁移机制、数据快照核对 | 开发 | 无 | P0 | 第 2.4、附录 C | ✅ |
| T09 | 工具链/IDE 工程配置探测（SDK/Maven/Node/MySQL） | 架构/前端 | 无 | P0 | 第 2.5、9.0 节 | ✅ |
| T04 | 用户旅程、功能 In/Out、验收案例 | 产品 | T02 | P1（与 T05 并行） | 第 4 章 | ✅ |
| T05 | 技术方案、页面结构、模块接口契约、数据流 | 架构/前端 | T01、T03 | P1 | 第 5 章、附录 B | ✅ |
| T06 | 统计口径、业务逻辑、页面交互、扩展落点 | 开发 | T03、T05 | P2（T05 后） | 第 6 章 | ✅ |
| T07 | 测试策略、功能/边界/持久化用例设计 | QA | T04、T05、T06 | P3（与 T08 串行） | 第 7.1–7.4 节 | ✅ |
| T08 | 只读执行、缺陷复现与定级 | QA | T07、T09 | P3 | 第 7.5–7.6 节 | ✅（静态+DB 只读；动态执行本轮未进行，非环境阻塞） |
| T10 | 合并产出、冲突裁决、形成交付文档 | Leader | T01–T09 | P4 | 本文件 | ✅ |

### 1.2 并行与依赖关系

```text
P0 并行:  T01(架构)  T02(产品)  T03(开发)  T09(架构/环境)
              \        |          |         /
P1 并行:        T04(产品)      T05(架构/前端)
                    \             /
P2:                    T06(开发)
                        |
P3 串行:              T07(QA) → T08(QA)
                        |
P4:                    T10(Leader 汇总)
```

- **可并行**：T01/T02/T03/T09 互不依赖；T04 与 T05 可并行；T07 的用例设计可在 T08 执行前并行推进。
- **强依赖**：T06 依赖接口契约（T05）与数据模型（T03）；T08 依赖用例（T07）与可用环境（T09）。
- **冲突点**：功能"是否已实现"（T02 产品视图 vs T08 QA 证据）、年度报告口径是否算缺陷、测试是否必须启动服务——由 Leader 在 [第 8 章](#8-leader冲突与决策记录) 裁决。

### 1.3 团队协作规则（本次执行约定）

1. 产品只写"用户能感知的行为"，不写实现细节；代码事实以架构/QA 核验为准。
2. 架构只给"最简可实施方案"，不引入新依赖；接口契约必须与现有 `Controller` 一一对应。
3. 开发对每个指标给出"口径 + 数据来源 + 单位"，避免页面数字无解释。
4. QA 的每个缺陷必须带"复现路径 + 期望 + 实际 + 根因文件行号"；无法动态执行的必须标注"未执行"，禁止伪造通过。
5. Leader 对跨角色分歧作最终决策并记录，不以"默认"方式沉默。

---

## 2. 项目结构解析（架构/前端 Teammate）

### 2.1 仓库总览

```text
jweb-zuoye/
├── GameStats/                      # 后端（Maven 聚合工程）
│   ├── pom.xml                     # 父 POM：Spring Boot 4.1.0 / Java 26 / jjwt 0.11.5 / MyBatis 4.0.1
│   ├── game-common/                # 通用能力：JWT、上下文、异常、常量、Result
│   ├── game-pojo/                  # 数据模型：entity / dto / vo
│   ├── game-server/                # 唯一可启动模块：controller/service/mapper/config/interceptor/handler
│   ├── uploads/                    # 头像本地存储目录（运行时写入）
│   └── game-server/target/         # 既有构建产物 game-server.jar（可能早于部分源码改动）
├── qianduan/                       # 前端（Vue 3 + Vite）
│   ├── src/{views,components,layouts,router,store,styles,utils}
│   ├── vite.config.js              # 端口 5173；/api、/uploads 代理到 8080
│   ├── package.json                # vue3 / element-plus / echarts / pinia / axios / dayjs
│   └── dist/                       # 既有构建产物
├── GameStats项目实现与功能展示.docx  # 项目说明材料
├── GameStats_PPT展示.pptx           # 演示材料
└── GameStats-项目解析与Agent协作交付.md  # ← 本次新增
```

> 说明：仓库当前**不是 Git 仓库**（无 `.git`、无 `.gitignore`），`node_modules/`、`dist/`、`target/`、`.idea/` 与源码混放。后续若纳入版本管理，建议先补 `.gitignore`。

### 2.2 后端：三模块与包结构

**模块依赖方向（单向）**：

```text
game-server  ──▶  game-pojo  ──▶  game-common
（业务/启动）      （DTO/VO/实体）    （工具/常量/异常）
```

| 模块 | 职责 | 关键内容 |
|---|---|---|
| `game-common` | 无业务的基础设施 | `JwtUtil`、`JwtProperties`、`BaseContext`(ThreadLocal)、`BaseException`/`UserNotLoginException`/`LoginFailedException`、`MessageConstant`、`Result`/`PageResult`、`JacksonObjectMapper` |
| `game-pojo` | 跨层数据模型 | 实体：`User`、`Game`、`UserGame`、`Achievement`、`UserGamePlatform`、`YearlyReport`；DTO：`UserLoginDTO`、`UserUpdateDTO`、`PasswordUpdateDTO`、`GameNoteDTO`、`GameStatusDTO`；VO：`UserVO`、`UserStatsVO`、`LoginVO`、`AuthUrlVO`、`GameVO`、`GameDetailVO`、`AchievementVO`、`PlatformVO`、`DashboardVO`、`DailyPlayVO`、`GenreDistributionVO`、`YearReportVO` |
| `game-server` | 全部业务与 Web 层 | 启动类 `GameStatsApplication`（`@ComponentScan("com.lxw")`、`@MapperScan("com.lxw.gamestats.mapper")`）；6 个 Controller；5 组 Service/Impl；6 个 Mapper + 6 个 XML；`JwtInterceptor`；`WebMvcConfig`；`GlobalExceptionHandler`；`DatabaseMigrationRunner` |

**分层调用链**：

```text
HTTP → JwtInterceptor（校验 token，写 BaseContext）
     → Controller（参数接收、直接返回 VO/List/Map）
     → Service/Impl（业务规则、聚合、异常）
     → Mapper 接口 + mapper/*.xml（MyBatis SQL）
     → MySQL(game_stats)
```

**关键设计约定（务必遵守）**：

1. **无统一 Result 包装**：Controller 直接返回业务对象（`List<GameVO>`、`UserVO`、`Map{url}`），错误返回 `{ "message": "..." }` + HTTP 400/401/500。前端 axios 拦截器直接取 `response.data` 当业务数据。**新增接口必须沿用此约定**，否则前端要改拦截器。
2. **当前用户来源**：所有"我的数据"均来自 `BaseContext.getCurrentId()`，Service 内再校验 null → `UserNotLoginException`（401）。
3. **鉴权路径**：`WebMvcConfig` 拦截 `/api/**`，排除 `/api/auth/**`；token 优先 `Authorization: Bearer xxx`，兼容 query `?token=`（为上传场景）。
4. **上传服务**：`POST /api/upload/avatar`（multipart），落盘 `game.upload.path`（默认 `./uploads`），返回 `/uploads/{uuid}.{ext}`；`WebMvcConfig` 将 `/uploads/**` 映射到本地目录。
5. **异常语义**：`BaseException` → 400；`UserNotLoginException` → 401；其余 → 500（含参数类型转换错误，见 BUG-17）。

### 2.3 前端：目录、路由与状态

```text
qianduan/src/
├── main.js                 # 注册 Pinia、Router、Element Plus、全部图标、全局样式
├── App.vue                 # 仅 <router-view/>
├── router/index.js         # history 路由 + 全局前置守卫（登录态）
├── layouts/MainLayout.vue  # 侧边栏 + 顶栏 + <router-view/> + 3 个全局弹窗
├── views/                  # Login / Home / Dashboard / Library / GameDetail / Profile / Social / YearReport / _encoding_test(遗留)
├── components/             # StatCard / GameCard / AccountSettingsDialog / PreferenceDialog / ChangePasswordDialog
├── store/                  # user.js（token/资料/统计）、game.js（游戏列表/筛选/更新）、index.js
├── utils/api.js            # axios 实例 + 请求/响应拦截器
└── styles/                 # variables.scss（色板/尺寸/圆角/阴影）、index.scss（全局）
```

**路由表**（`router/index.js`）：

| 路径 | 名称 | 组件 | 鉴权 | 说明 |
|---|---|---|---|---|
| `/login` | Login | `views/Login.vue` | 否 | 已登录访问会被重定向到 `/dashboard` |
| `/` | Home | `views/Home.vue` | 是 | MainLayout 子路由，静态欢迎页 |
| `/dashboard` | Dashboard | `Dashboard.vue` | 是 | 数据中心 |
| `/library` | Library | `Library.vue` | 是 | 游戏库 |
| `/game/:id` | GameDetail | `GameDetail.vue` | 是 | 游戏详情 |
| `/profile` | Profile | `Profile.vue` | 是 | 个人中心 |
| `/social` | Social | `Social.vue` | 是 | 社交中心（Mock） |
| `/year-report` | YearReport | `YearReport.vue` | 是 | 年度报告 |

**守卫规则**：`to.meta.requiresAuth && !userStore.isLoggedIn` → `/login?redirect=fullPath`；已登录访问 `/login` → `/dashboard`。登录态仅判断 `localStorage.token` 是否存在（不校验过期，见 BUG-16）。

**Pinia**：
- `user`：`token`、`userInfo`、`gameStats`、`isLoggedIn`；`loginWithPhone` / `handleCallback` / `fetchUserInfo` / `fetchGameStats` / `updateProfile` / `logout`；token 与 userInfo 持久化到 localStorage。
- `game`：`games`、`currentGame`、`loading`、`filters`（含未被 Library 使用的 `filteredGames`/`totalPlayTime` 计算）；`fetchGames` / `fetchGameById` / `updateGameNote` / `updateGameStatus`。

**axios 约定**（`utils/api.js`）：
- `baseURL: '/api'`、10s 超时；请求自动加 `Authorization: Bearer <token>`；`FormData` 时删除默认 `Content-Type`（保证 boundary）。
- 响应成功直接返回 `response.data`；401 清空登录态并跳 `/login` 提示"登录已过期"；403/404/500 分别提示；其余显示后端 `message`。

### 2.4 数据库与初始化机制

**连接**（`application.yml`）：`jdbc:mysql://localhost:3306/game_stats?createDatabaseIfNotExist=true...`，`root/123456`，`spring.sql.init.mode=always`。

**初始化顺序**：

```text
应用启动
 ├─ spring.sql.init: 执行 schema.sql（CREATE TABLE IF NOT EXISTS ×10 + users.custom_tags 幂等补列）
 ├─ spring.sql.init: 执行 data.sql（游戏/类型/成就/用户库/解锁/平台/360 条 play_records/年度快照）
 └─ CommandLineRunner: DatabaseMigrationRunner
      ├─ 为旧 users 表补齐 8 列（gender/birthday/region/preferred_language/profile_visibility/allow_comments/email_notify/site_notify）
      └─ upsert 演示账号 id=1（幂等：ON DUPLICATE KEY UPDATE id=id）
```

**10 张表与关系（逻辑）**：

| 表 | 作用 | 关键唯一键 |
|---|---|---|
| `users` | 用户与资料/偏好 | `(platform, platform_id)` |
| `games` | 全局游戏目录 | `(platform, platform_game_id)` |
| `game_genres` | 类型字典 | `name` |
| `game_genre_relations` | 游戏×类型（多对多） | `(game_id, genre_id)` |
| `achievements` | 游戏成就定义 | `(game_id, platform_achievement_id)` |
| `user_games` | 用户游戏库（时长/状态/笔记） | `(user_id, game_id)` |
| `user_achievements` | 用户解锁记录 | `(user_id, achievement_id)` |
| `play_records` | 游玩会话（起止/时长） | 无唯一键（含 2 个普通索引） |
| `yearly_reports` | 年度报告快照 | `(user_id, year)` |
| `user_game_platforms` | 用户在游戏上的平台拥有情况 | 无唯一键（普通索引 `user_id, game_id`） |

**数据初始化的重要副作用**（QA 关注）：
- `data.sql` 对 `play_records` 执行 **`DELETE FROM play_records WHERE user_id=1` + 递归 CTE 重建 360 条**（`data.sql:252-272`）→ **每次重启后端都会重置游玩记录**。当前没有"写入游玩记录"的接口，所以不影响现有功能；一旦新增"记录游玩/导入时长"功能，必须同步修改此段，否则重启丢数据（BUG-23）。
- `yearly_reports` 使用 `ON DUPLICATE KEY UPDATE id=id`（保留现场），但 `GET /api/report/yearly` 会实时 upsert 覆盖快照（BUG-15）。
- `games`/`achievements` 等使用 `ON DUPLICATE KEY UPDATE id=id`，不会覆盖用户后续改动（幂等）。

**当前数据库快照（只读核对，2026-11-13）**：见 [附录 C](#附录-c当前数据库快照)。要点：1 个演示用户、10 款游戏、10 条游戏库记录（在玩 4 / 已完成 6）、360 条游玩记录（2025-11-19 ~ 2026-11-13）、40 个成就、30 条解锁、3 条年度快照（2024/2025/2026）。

> 注意：数据库并非"纯种子状态"——用户名已被改为 `GameMasters`，`games` 已被扩充到 10 款（含 `GTA5`、`黑神话：悟空`），说明项目曾实际运行并被使用。

### 2.5 构建与工具链事实（T09）

| 项 | 本机实测 | 结论 |
|---|---|---|
| `java`（PATH） | 1.8.0_161 | 仅影响命令行；**IntelliJ 项目 SDK 为 `openjdk-26`，IDE 内构建/运行不受影响** |
| `mvn` | 命令行未安装（`command not found`） | **无需安装**：使用 IntelliJ **内置 Maven（Bundled Maven）** 导入与构建 |
| IDE 配置 | `GameStats/.idea/misc.xml` → `project-jdk-name="openjdk-26"`、`languageLevel="JDK_X"` | 工程已按 JDK 26 配置，**用 IDEA 打开即可构建运行** |
| Maven Wrapper | `GameStats/.mvn/wrapper/maven-wrapper.properties` 存在，但**无 `mvnw` / `mvnw.cmd` 脚本** | 命令行 wrapper 不可用；**不影响 IDE**，仅 CI 场景需补脚本 |
| `node` / `npm` | 已安装（环境提示 24.15.0 / 11.12.1） | 前端可运行 |
| `mysql` | 8.0.44，`game_stats` 库存在 | 数据库可访问（只读核对通过） |
| 前端依赖 | `qianduan/node_modules` 已存在 | 可直接 `npm run dev` |

**影响（本轮修正）**：上一轮把"命令行不可构建"误判为环境阻塞。**正确结论**：本项目以 **IntelliJ IDEA** 为运行方式——用 IDE 内置 Maven 导入聚合工程、Project SDK 选 `openjdk-26`、直接运行 `GameStatsApplication`；**不需要安装 JDK 26 与 Maven，也不要求在命令行构建**。静态审查与数据库只读核对已完成；动态 E2E 在按 [9.1 方式 A](#91-启动后端) 启动后端后即可执行，不再是环境阻塞项。

---

## 3. 功能解读（产品 + 开发 Teammate）

### 3.1 功能地图

| 功能域 | 页面 | 后端接口 | 主要数据表 | 成熟度 |
|---|---|---|---|---|
| 登录认证 | `/login` | `POST /api/auth/login`、`GET /api/auth/{provider}`、`GET /api/auth/callback/{provider}` | `users` | 手机号登录✅；OAuth 为 Mock |
| 首页 | `/` | — | — | 静态展示✅ |
| 数据中心 | `/dashboard` | `/api/dashboard/{stats,recent-games,play-trend,genre-distribution,heatmap}` | `user_games`、`play_records`、`user_achievements`、`achievements` | ✅（真实聚合） |
| 游戏库 | `/library` | `GET /api/games`、`PUT /api/games/{id}/status` | `user_games`、`games`、`game_genres` | ✅（分页失效） |
| 游戏详情 | `/game/:id` | `GET /api/games/{id}`、`PUT .../note`、`PUT .../status` | 上述 + `achievements`、`user_achievements`、`user_game_platforms` | ✅（标签/趋势为假） |
| 个人中心 | `/profile` | `GET/PUT /api/user/profile`、`GET /api/user/stats`、`GET /api/dashboard/genre-distribution`、`POST /api/upload/avatar` | `users`、`user_games`、`user_achievements` | 资料✅；绑定 Mock |
| 账号/偏好/改密 | 全局弹窗 | `PUT /api/user/password`；偏好仅 localStorage | `users` | 改密✅；偏好不生效 |
| 年度报告 | `/year-report` | `GET /api/report/yearly?year=` | `user_games`、`play_records`、`user_achievements`、`yearly_reports` | 聚合✅；口径混用、导出假 |
| 社交中心 | `/social` | 无 | 无 | ❌ 全 Mock |

### 3.2 登录认证

- **主路径**：手机号（前端正则 `^1[3-9]\d{9}$`）+ 密码（6–20 位）→ `POST /api/auth/login` → 后端 `users.phone` 查用户 → MD5 比对 `password_hash` → 生成 JWT（claims: `userId`、`username`，TTL 7 天）→ 返回 `{ token, user }` → 前端写 localStorage → 跳 `redirect` 或 `/dashboard`。
- **演示账号**：`13800138000 / 123456`（`DatabaseMigrationRunner:60-76`；`e10adc3949ba59abbe56e057f20f883e` 即 `MD5("123456")`，已校验）。
- **OAuth（占位）**：`getAuthUrl` 拼 `https://{provider}.example.com/oauth/authorize?...`；`callback` 直接把 `code` 当 `platformId`，不存在则自动注册 `{provider}_user`。**未接真实 OAuth**，前端点击会跳出站点。
- **失败语义**：用户不存在 → 400 `账号不存在`；密码错误 → 400 `密码错误`；参数缺失 → 400 `登录失败`；token 缺失/失效 → 401 `用户未登录 / 登录已过期，请重新登录`。

### 3.3 数据中心（Dashboard）

- **统计卡**：总游戏时长（分钟→"X天X小时"）、游戏数量、总游玩天数、成就解锁（`已解锁/总数` + 进度条）。
- **游玩时长趋势**：ECharts 折线；可切 30/90/365 天；后端只返回有记录的日期，前端按区间补 0。
- **游戏类型分布**：环形饼图，按游戏库 `play_time` 占比；颜色前端固定 8 色循环。
- **游玩热力图**：ECharts calendar（当年/近 5 年可选），按日 `SUM(duration)`。
- **近期游戏排行**：本周/本月/全部切换，前 5 名，点击进入详情。
- **成就进度榜**：取游戏库中"有成就定义"的游戏，按解锁率排序前 3。

### 3.4 游戏库（Library）

- 搜索框（本地按名称包含，大小写不敏感）；类型筛选（从已加载数据提取**主类型**）；状态筛选（在玩/已完成/未玩/搁置/弃坑）；网格/列表视图切换；骨架屏与空状态。
- 列表视图下拉可直接改状态（存在"正在玩"不一致，见 BUG-03）。
- 分页组件存在但**未对数据切片**（BUG-04）。
- 顶部搜索会把 `?search=` 带入并回填搜索框。

### 3.5 游戏详情（GameDetail）

- Hero 区：模糊封面背景 + 海报、名称、主类型、开发商、发行日期、10 分制评分（星级 = 评分/2）。
- 4 项统计：游玩时长、游玩次数（`play_records` 条数）、最后游玩日期、成就进度。
- 我的笔记：textarea 失焦自动 `PUT /note`，成功提示"笔记已保存"。
- 成就列表：`achievements LEFT JOIN user_achievements`，未解锁置灰。
- 侧栏：状态下拉（5 种中文）、"自定义标签"（实际是游戏类型标签，且不持久化，BUG-01）、平台拥有情况、分享/导出按钮（无行为）。

### 3.6 个人中心与账号设置

- **个人中心**：头像、用户名、等级、玩家类型（按总时长阈值：>3000 传奇 / >1000 资深 / >300 活跃 / 其余新晋）、加入日期、4 项统计、绑定账号（Mock）、类型偏好（来自 `genre-distribution`，显示小时数与百分比）、自定义标签（真实持久化到 `users.custom_tags`）、编辑资料弹窗（用户名/简介/头像）。
- **账号设置弹窗**：个人资料（用户名/性别/生日/地区/签名/头像）、隐私（资料可见性/允许评论）、通知（站内/邮件）。字段全部写回 `users`。
- **偏好设置弹窗**：主题/动效/侧边栏/语言/时区/日期格式/默认图表/默认区间/小数/通知，**只写 localStorage `gs_preferences`，全站没有消费方**（BUG-18）。
- **修改密码**：旧密码校验 → 新密码 MD5 覆盖；有前端强度条与规则提示；成功后不强制登出（提示"请使用新密码重新登录"但当前 token 仍有效）。

### 3.7 年度报告

- 海报式展示：年份、用户、总时长、游戏数、成就、通关数、年度最爱（封面/名称/小时）、玩家标签、类型占比条、品牌页脚。
- 详情区：月度时长柱状图、月度排行（缺封面数据，BUG-08）、成就里程碑（前端阈值计算）。
- 操作：保存海报/分享 Discord 均为提示文案（BUG-09）。
- 后端 `GET /api/report/yearly` 每次实时聚合 + upsert 快照；**总时长按所选年份，其他指标按全库**（BUG-06）。

### 3.8 社交中心（占位）

好友列表（3 个硬编码好友）、数据对比（硬编码比例）、分享记录（2 条硬编码）、添加好友弹窗（邀请码 `A8X9K2`，提交仅提示）。**无任何后端接口与数据表**。

### 3.9 已实现 / 占位 / 未实现

| 状态 | 内容 |
|---|---|
| ✅ 真实可用 | 手机号密码登录、JWT 鉴权、数据中心全部图表与统计、游戏库查询/筛选/状态更新、游戏详情/笔记/成就/平台、个人资料更新、自定义标签、头像上传、修改密码、年度报告聚合 |
| 🟡 占位/演示 | OAuth 登录、社交中心、绑定账号、偏好设置、海报导出、分享到 Discord、通知铃铛、"记住我/忘记密码/立即注册" |
| 🔴 未实现（无接口/无入口） | 游戏新增/删除/导入、游玩记录写入、好友关系与消息、真实多平台数据同步、后端偏好持久化、多语言切换、角色权限 |

---

## 4. 产品 Teammate：用户旅程、功能边界、验收案例

### 4.1 目标用户与画像

| 画像 | 特征 | 关键诉求 |
|---|---|---|
| 多平台玩家 | Steam/Epic/PS 多端持有，游戏 50–200 款 | 一处看全库时长、状态、成就 |
| 成就收集者 | 关注解锁率与全成就 | 成就进度、里程碑 |
| 数据爱好者 | 喜欢看趋势/热力图/年度总结 | 可视化、年度海报分享 |
| 轻度用户 | 偶尔记录、少量游戏 | 低门槛登录、清晰的空态与提示 |

### 4.2 用户旅程

**J1 首次进入 → 看到自己的数据（主旅程）**

```text
打开 /login
 → 输入手机号/密码 → 前端校验 → 登录成功（token + 用户信息入 localStorage）
 → 自动进入 /dashboard
 → 看 4 张统计卡 → 切换 30/90/365 天趋势 → 看类型分布/热力图/排行
 → 点排行或侧边栏进入 /library
 → 搜索/筛选/切列表视图 → 打开 /game/:id
 → 改状态、写笔记（失焦保存）→ 返回游戏库确认状态已变
 → 进入 /profile 改用户名/头像/标签
 → 进入 /year-report 选年份 → 查看海报与月度趋势
 → 账号菜单 → 修改密码（旧密码校验）→ 退出登录
```

**J2 日常维护（高频）**：登录 → 游戏库筛选"在玩" → 进详情写笔记/改状态 → 回数据中心看本周排行。

**J3 年度回顾（低频、分享动机）**：年度报告 → 切年份 → 看最爱游戏/标签/类型 → 期望保存海报或分享（当前仅提示）。

**关键摩擦点（产品视角）**：
1. 详情页"自定义标签"改完刷新丢失 → 用户会认为保存失败。
2. 年度报告切年份后"游戏数/成就/最爱"不变 → 用户困惑"年度"含义。
3. 社交/绑定/分享看起来可用但无效 → 信任受损。
4. 列表视图改状态为"正在玩"后筛选不到 → 数据"消失"。

### 4.3 功能边界

**In Scope（本期产品承诺）**
- 单用户（本人）游戏数据的查看、筛选、状态与笔记维护。
- 基于真实库表的统计与可视化。
- 资料/头像/标签/密码维护（资料类写入 `users`）。
- 年度报告的实时聚合展示与快照落库。

**Out of Scope（明确不做 / 仅演示）**
- 真实第三方 OAuth 授权、多平台数据自动同步。
- 好友关系、消息、评论、动态、数据对比（当前 Mock）。
- 注册、找回密码、短信验证、邮箱验证。
- 游戏录入/删除、游玩记录录入/导入。
- 海报图片导出、真实社交分享。
- 主题/语言/时区等偏好的实际生效与跨端同步。
- 多租户、角色权限、审计后台。

### 4.4 验收案例（Given / When / Then）

> 当前数据库快照下的期望值已标注；未特别说明时账号为 `13800138000 / 123456`。

| 编号 | 场景 | Given | When | Then |
|---|---|---|---|---|
| AC-01 | 正常登录 | 演示账号存在且密码正确 | 输入手机号/密码并点击登录 | 返回 token 与 user；localStorage 写入；跳转 `/dashboard` |
| AC-02 | 手机号格式错误 | 在登录页 | 输入 `12345` 后失焦 | 表单提示"请输入正确的手机号"，不发起请求 |
| AC-03 | 密码错误 | 账号存在 | 输入错误密码登录 | 停留在登录页，提示"密码错误" |
| AC-04 | 未登录访问内页 | 无 token | 直接访问 `/dashboard` | 重定向 `/login?redirect=/dashboard` |
| AC-05 | 已登录访问登录页 | 有 token | 访问 `/login` | 重定向 `/dashboard` |
| AC-06 | 统计卡与库一致 | 当前快照 | 打开 `/dashboard` | 游戏数 10；总时长 99,670 分钟（页面显示"69天5小时"）；成就 30/40 |
| AC-07 | 趋势区间切换 | 在数据中心 | 点 90 天 | 折线横轴变为 90 天，缺失日期补 0，无报错 |
| AC-08 | 游戏库搜索 | 库中 10 款 | 搜索"艾尔登" | 仅显示"艾尔登法环" |
| AC-09 | 状态筛选 | 当前快照 | 状态选"在玩" | 显示 4 款（赛博朋克 2077、哈迪斯、博德之门3、GTA5） |
| AC-10 | 笔记持久化 | 打开某游戏详情 | 输入笔记 → 失焦 → 刷新页面 | 提示"笔记已保存"；刷新后内容仍在 |
| AC-11 | 状态持久化 | 打开某游戏详情 | 状态改为"已完成" → 回游戏库筛选"已完成" | 该游戏出现在结果中，计数 +1 |
| AC-12 | 资料更新 | 在个人中心 | 改用户名 → 保存 → 刷新 | 侧边栏/个人中心同步，刷新后仍为新值 |
| AC-13 | 头像上传 | 选择 ≤2MB 图片 | 保存资料 | 返回 `/uploads/xxx`，头像即时更新，刷新后仍可访问 |
| AC-14 | 修改密码 | 已知旧密码 | 旧密码错误 → 提交 | 提示"密码错误"，密码不变 |
| AC-15 | 修改密码成功 | 已知旧密码 | 输入正确旧密码 + 合规新密码（两次一致） | 提示成功；旧密码登录失败、新密码登录成功 |
| AC-16 | 年度报告年份切换 | 当前快照 | 依次选 2025 / 2026 | 总时长分别约 133h / 971h；页面无报错 |
| AC-17 | 权限隔离 | 用户 A 登录 | 访问不属于 A 的 `gameId` | 返回 400"游戏不存在"，页面提示错误，不泄漏数据 |
| AC-18 | 登录失效 | token 过期/被篡改 | 请求任意业务接口 | 401 → 清空登录态 → 跳登录页，提示"登录已过期" |

**验收阻断项（必须先修再验收）**：AC-09 与 BUG-03 冲突（列表视图改"正在玩"会破坏筛选）；AC-16 后两项指标与年份无关（BUG-06）；AC-10 通过但详情页标签刷新丢失（BUG-01，不在 AC 内，需产品补一条"标签持久化"验收）。

---

## 5. 架构/前端 Teammate：最简技术方案、页面结构、接口契约

### 5.1 最简技术方案（保持现状，不引入新依赖）

| 关注点 | 方案 | 理由 |
|---|---|---|
| 架构形态 | 前后端分离；后端单体 3 模块 | 现有规模（10 表、18 接口）无需微服务 |
| 认证 | JWT HS256 + 拦截器 + ThreadLocal | 已实现，改动成本最低 |
| 持久层 | MyBatis XML + 动态 SQL | 聚合统计用 SQL 最直接；避免引入 JPA |
| 前端框架 | Vue 3 `<script setup>` + Element Plus + ECharts + Pinia | 现有组件与主题已成型 |
| 状态管理 | Pinia（user/game） | 只需跨页共享登录态与游戏列表 |
| 请求层 | axios 单实例 + 拦截器 | 统一 token/错误处理 |
| 构建/联调 | Vite（proxy `/api`、`/uploads` → 8080） | 开发期免 CORS |
| 部署 | Nginx 同源反代 `/api`、`/uploads` 到后端；`dist` 静态托管 | 后端无 CORS 配置，同源最省事 |
| 数据库 | MySQL 8（CTE/窗口函数） | 种子数据依赖 MySQL 8 特性 |

**明确不引入**：TypeScript、UI 框架替换、SSR、GraphQL、Redis、消息队列、Docker（当前无需求）。

### 5.2 页面结构与组件关系

```text
App.vue
└── <router-view/>
    ├── Login.vue（独立页，无布局）
    └── MainLayout.vue（鉴权后）
        ├── 侧边栏：Logo / 菜单（数据中心、游戏库、社交、年度报告、个人中心）/ 用户下拉
        ├── 顶栏：页面标题 / 搜索框（跳 /library?search=）/ 通知铃铛（无功能）
        ├── <router-view/> → Home / Dashboard / Library / GameDetail / Profile / Social / YearReport
        └── 全局弹窗：AccountSettingsDialog / PreferenceDialog / ChangePasswordDialog

Dashboard ── StatCard ×4 + ECharts ×3（趋势/饼图/热力图）+ 排行列表 + 成就进度榜
Library   ── GameCard（grid/list）+ 搜索/筛选/视图切换 + 骨架屏/空态 + 分页
GameDetail── Hero + 统计 + ECharts（趋势，Mock）+ 笔记 + 成就列表 + 侧栏（状态/标签/平台/分享）
Profile   ── 资料头 + 统计 + 绑定账号(Mock) + 类型偏好 + 标签 + 编辑资料弹窗
YearReport── 海报 + 月度柱状图 + 月度排行 + 里程碑
Social    ── 好友/数据对比/分享记录（全 Mock）+ 邀请弹窗
```

**跨页数据约定**：登录信息与游戏列表分别由 `user` / `game` store 持有；`Dashboard` 与 `Library` 都会调用 `gameStore.fetchGames()`（Dashboard 仅在为空时补拉），因此两页数据可复用，但**修改状态后需保证 store 同步**（`updateGameStatus` 已做本地替换）。

### 5.3 模块接口契约（后端 ↔ 前端）

> 响应均为**裸业务对象**（无 `code/data` 包装）；错误为 `{message}` + 400/401/500。鉴权：`/api/auth/**` 免登录，其余需 `Authorization: Bearer <token>`。

| # | 方法 | 路径 | 请求 | 成功响应 | 错误 |
|---|---|---|---|---|---|
| 1 | POST | `/api/auth/login` | `{phone, password}` | `LoginVO{token, user:UserVO}` | 400 登录失败/账号不存在/密码错误 |
| 2 | GET | `/api/auth/{provider}` | path: provider | `AuthUrlVO{authUrl}` | —（Mock） |
| 3 | GET | `/api/auth/callback/{provider}?code=` | code | `LoginVO`（不存在则自动注册） | 500（platform_id 非空约束等） |
| 4 | GET | `/api/user/profile` | — | `UserVO` | 401 |
| 5 | PUT | `/api/user/profile` | `UserUpdateDTO`（null=不更新） | `UserVO`（更新后） | 401 |
| 6 | GET | `/api/user/stats` | — | `UserStatsVO{totalGames,totalPlayTime,achievements,completedGames}` | 401 |
| 7 | PUT | `/api/user/password` | `{oldPassword,newPassword}` | 空（200） | 400 原/新密码为空、密码错误、账号不存在 |
| 8 | POST | `/api/upload/avatar` | `multipart/form-data: file` | `{url:"/uploads/xxx"}` | 400 文件上传失败 |
| 9 | GET | `/api/games` | — | `List<GameVO>`（含 tags） | 401 |
| 10 | GET | `/api/games/{id}` | path: 游戏 ID | `GameDetailVO`（含 tags/platforms/achievementList） | 400 游戏不存在 |
| 11 | PUT | `/api/games/{gameId}/note` | `{note}` | `GameVO` | 400 游戏不存在 |
| 12 | PUT | `/api/games/{gameId}/status` | `{status}` | `GameVO` | 400 游戏不存在 / status 为空 |
| 13 | GET | `/api/dashboard/stats` | — | `DashboardVO` | 401 |
| 14 | GET | `/api/dashboard/recent-games?period=all / week / month&limit=5` | — | `List<GameVO>`（id/name/cover/playTime） | 401 |
| 15 | GET | `/api/dashboard/play-trend?days=30` | — | `List<DailyPlayVO{date,minutes}>` | 401；days 非法→500 |
| 16 | GET | `/api/dashboard/genre-distribution` | — | `List<GenreDistributionVO{name,percent,hours}>` | 401 |
| 17 | GET | `/api/dashboard/heatmap?year=` | — | `List<DailyPlayVO>` | 401 |
| 18 | GET | `/api/report/yearly?year=2025` | — | `YearReportVO` | 401；year 非法→500 |

**字段级约定（前端实际依赖）**：
- `UserVO` 含 `id/username/avatar/bio/level/joinDate/customTags/gender/birthday/region/preferredLanguage/profileVisibility/allowComments/emailNotify/siteNotify`；**不含 phone/email**。
- `GameVO`：`id/name/cover/genre/developer/releaseDate/rating(BigDecimal)/playTime(分钟)/sessions/lastPlayed/achievements/totalAchievements/status/note/tags`。
- `GameDetailVO` = GameVO + `platforms[{name,owned}]` + `achievementList[{id,name,description,icon,unlocked,unlockedDate}]`。
- `YearReportVO`：`year/totalHours/totalGames/achievements/completedGames/favoriteGame{id,name,cover,hours}/tags[]/genres[{name,percent,color}]/monthlyTrend[{month,hours}]/playStyle`。
- `DailyPlayVO`：`date`（`YYYY-MM-DD`）、`minutes`。
- `GenreDistributionVO`：`name/percent/hours`。

### 5.4 数据流（一次典型请求）

```text
组件（ref/computed）
  → axios(baseURL=/api, 加 Bearer) 
  → Vite dev proxy → Spring Boot :8080
  → JwtInterceptor（解析 token → BaseContext.setCurrentId）
  → Controller → Service（currentUserId + 业务校验）
  → Mapper XML → MySQL(game_stats)
  → VO → Jackson → JSON（直接是 data）
  → axios 响应拦截器 return response.data
  → 组件赋值 → 模板渲染 / ECharts setOption
```

### 5.5 前端工程约定（新页面必须遵守）

1. 视图放 `src/views`，路由懒加载；菜单项在 `MainLayout.menuItems` 同步注册。
2. 所有 HTTP 走 `@/utils/api`，不直接 `fetch/axios`；错误提示交给拦截器，页面只在 `catch` 留空或做局部兜底。
3. 样式使用 `variables.scss` 变量（`$bg-card`、`$primary-color`…），scoped SCSS；图表容器必须有固定高度。
4. ECharts 必须在 `onMounted + nextTick` 初始化，`onUnmounted` 调 `dispose`，监听 `window.resize`。
5. 空态/骨架屏/加载态是本项目既有标准（Library 已实现），新页面需提供至少空态。
6. 表单写入后端前注意：`null` = 不更新；空数组 `[]` 可清空标签；`FormData` 不要手动设 `Content-Type`。

---

## 6. 开发 Teammate：核心数据/业务逻辑与页面交互

### 6.1 统计口径表（页面数字从哪来）

| 指标 | 计算逻辑 | 数据来源 | 单位/换算 |
|---|---|---|---|
| 总游戏时长 `totalPlayTime` | `SUM(user_games.play_time)` | `user_games` | 分钟；前端 `formatPlayTime` → X天X小时 |
| 游戏数量 `totalGames` | `COUNT(user_games)` | `user_games` | 款 |
| 总游玩天数 `playDays` | `COUNT(DISTINCT DATE(start_time))` | `play_records` | 天 |
| 成就解锁 `achievements` | `COUNT(user_achievements)` | `user_achievements` | 个（用户全库） |
| 成就总数 `totalAchievements` | `COUNT(achievements JOIN user_games)` | `achievements` × `user_games` | 个（仅用户库内游戏） |
| 通关数 `completedGames` | `status='已完成'` | `user_games` | 款 |
| 时长环比 `playTimeTrend` | 近 30 天 vs 前 30 天 `SUM(duration)`；prev=0 且 curr>0 → 100 | `play_records` | % |
| 游戏数环比 `gamesTrend` | 近 30 天新增 `user_games` / 总数 | `user_games.created_at` | % |
| 近期排行 | `all`→`user_games.play_time`；`week/month`→`play_records` 7/30 天 `SUM(duration)` | `user_games` / `play_records` | 分钟 |
| 时长趋势 | 近 N 天按日 `SUM(duration)`（仅返回有记录日期） | `play_records` | 分钟/日 |
| 类型分布 | 各类型 `SUM(ug.play_time)` 占总时长百分比 + 小时数 | `user_games`×`game_genre_relations`×`game_genres` | % / 小时 |
| 热力图 | 指定年份按日 `SUM(duration)` | `play_records` | 分钟/日 |
| 年度总时长 | 所选年份 `SUM(duration)/60` 四舍五入 | `play_records` | 小时 |
| 年度最爱 | 全库 `play_time DESC LIMIT 1` | `user_games` | 小时 |
| 年度成就 | 全库 `COUNT(user_achievements)` | `user_achievements` | 个 |
| 年度类型 | 全库时长占比（整数） | `user_games`×类型 | % |
| 年度标签 | 时长阈值 1000/300（硬核/资深/休闲）+ 成就≥50 + 通关≥10 + 第一类型 + 有最爱 | 聚合结果 | 文本 |

**口径注意（会直接影响页面可信度）**：
1. **类型分布会重复计时长**：一款游戏有多个类型，`SUM(ug.play_time)` 在每个类型上都计一次，`percent` 之和可达 200%+。前端饼图把 `percent` 当 value 会再次归一化，tooltip 的 `{d}%` 与后端 `percent` **不是同一个百分比**（BUG-13）。
2. **年度报告的"年度"只作用于总时长与月度趋势**，游戏数/成就/最爱/类型均为全库累计（BUG-06）。
3. 当前快照：用户库时长合计 **99,670 分钟**（1,661 小时 10 分；Dashboard 显示"69天5小时"）；成就 **30/40（75%）**；游玩天数 **360**。

### 6.2 业务逻辑实现要点

| 能力 | 实现位置 | 要点 |
|---|---|---|
| 手机号登录 | `AuthServiceImpl.login` | 参数空 → `登录失败`；`getByPhone` → null 抛 `账号不存在`；MD5 比对 → 不等抛 `密码错误`；生成 JWT + `UserVO` |
| JWT 鉴权 | `JwtInterceptor` + `WebMvcConfig` | 头优先、query 兜底；解析失败 401 JSON；`afterCompletion` 清理 ThreadLocal |
| 当前用户 | `BaseContext` | 所有 Service 通过 `currentUserId()` 取，空则 401；**禁止从请求参数取 userId** |
| 资料更新 | `UserServiceImpl.updateProfile` | 逐字段判空（null=不更新）；`customTags` 序列化为 JSON 存 `custom_tags`，空列表写 `""` 以支持清空 |
| 头像上传 | `UserServiceImpl.uploadAvatar` | 空文件 400；UUID 文件名；**必须绝对路径** `transferTo`；落盘后同步 `users.avatar` |
| 修改密码 | `UserServiceImpl.changePassword` | 旧密码 MD5 比对；OAuth 用户 `password_hash=null` 直接判错；更新新 MD5 |
| 游戏列表/详情 | `GameServiceImpl` + `UserGameMapper.xml` | 归属校验 `user_id + game_id`；`genre` 取 `ORDER BY gg.id LIMIT 1` 的主类型；`tags` 单独查全部类型；`sessions` 为 `play_records` 条数 |
| 笔记/状态更新 | `GameServiceImpl.updateNote/updateStatus` | `rows==0` → `游戏不存在`；**status 未做枚举校验**（BUG-12） |
| 数据中心聚合 | `DashboardMapper.xml` | 单条 SQL 多子查询；`getStats` 空结果兜底为 0；趋势/热力图只返回有记录日期 |
| 年度报告 | `YearlyReportServiceImpl` | 实时聚合 + 补全 12 个月 + 分配颜色 + 标签规则 + `upsert` 快照；**快照只写不读** |
| 异常统一 | `GlobalExceptionHandler` | `UserNotLogin→401`、`BaseException→400`、其他→500（含类型转换错误） |

### 6.3 页面交互实现要点

| 页面 | 关键实现 |
|---|---|
| Login | `el-form` 自定义手机号校验；`keyup.enter` 提交；成功后按 `route.query.redirect` 跳转；OAuth 走 `window.location.href` |
| Dashboard | 3 个 ECharts 实例；`watch(activeTimeTab/heatMapYear/rankPeriod)` 重新拉取；趋势前端补零；`resize` 监听 + `dispose`；成就榜从 `gameStore.games` 计算 |
| Library | 本地过滤（名称/主类型/状态）；`watch(route.query.search, {immediate:true})`；`GameCard` 事件 `click/update-status`；空态与骨架屏 |
| GameDetail | 路由参数加载；`el-rate :model-value="rating/2"`；笔记 `@blur` 保存；状态下拉 `@change`；标签仅本地 push/splice（BUG-01）；趋势 `Math.random()`（BUG-05） |
| Profile | `onMounted` 拉 profile/stats/genre；标签增删调用 `updateProfile({customTags})` 整体覆盖；头像先 Base64 预览，保存时上传；`formatDate` 兼容数组 |
| AccountSettingsDialog | 打开时从 store 回填；头像前端校验 2MB；提交 `PUT /user/profile`（含隐私/通知字段） |
| ChangePasswordDialog | 前端强度条 + 规则提示；`validateNew` 禁止与旧密码相同；成功后不登出 |
| YearReport | `watch(selectedYear)` 拉取；海报纯 CSS；里程碑前端阈值；分享/保存仅提示 |
| Social | 全部 `ref([...])` 硬编码；无请求 |

### 6.4 后续"改页面 / 加功能"的扩展落点

| 需求类型 | 前端落点 | 后端落点 | 数据落点 |
|---|---|---|---|
| 纯视觉/布局调整 | 对应 `views/*.vue` 的 `<style>` + `styles/variables.scss` | — | — |
| 新增页面/菜单 | `views/X.vue` + `router/index.js` + `MainLayout.menuItems` | — | — |
| 新增只读统计接口 | 页面 + `api.get` | `XController` → `XService` → `XMapper` + `mapper/XMapper.xml` + `VO` | 现有表或新增视图 |
| 游戏库排序/分页/高级筛选 | `Library.vue`（改为请求后端） | `GameController` 增参 → `GameService` → `UserGameMapper.xml` | — |
| 游戏详情新增字段 | `GameDetail.vue` + `GameDetailVO/GameVO` 使用处 | `UserGameMapper.xml` 查询列 + `VO` 字段 | `games` / `user_games` 新列 |
| 用户资料新增字段 | `Profile.vue` / `AccountSettingsDialog.vue` | `UserUpdateDTO` + `UserVO` + `UserMapper.xml` | `users` 新列 + `DatabaseMigrationRunner` 补列（旧库） |
| 新增写操作（状态/笔记之外的） | store 方法 + 页面交互 | Controller/Service/Mapper（`rows==0` 判存在） | 对应表；注意 `play_records` 会被启动脚本重置（BUG-23） |
| 年度报告增强 | `YearReport.vue` | `YearlyReportMapper.xml` + `YearReportVO` | `yearly_reports`（快照读取需先改 Service） |
| 社交/好友 | `Social.vue` 重写 + 新 store | 新增 `friend` 相关 Controller/Service/Mapper | 需新建 `friendships`/`messages` 等表 |
| 主题/偏好生效 | 新增 `usePreference` 组合式函数，在 `App.vue`/`MainLayout` 消费 `gs_preferences` | 可选：新增用户偏好接口 | 可选 `users` 新列或独立表 |

**新增功能的标准 5 步**（Leader 建议）：
1. 定验收（产品）→ 2. 定接口与字段（架构）→ 3. 后端 VO/DTO/Mapper/Service/Controller（开发）→ 4. 前端 store + 页面（开发）→ 5. QA 用例 + 持久化验证（QA）。

---

## 7. QA Teammate：测试设计、执行记录、可复现缺陷

### 7.1 测试策略

| 层级 | 手段 | 本次状态 |
|---|---|---|
| 静态一致性 | 代码↔SQL↔前端字段↔注释交叉核对 | ✅ 已执行 |
| 数据库只读核对 | 表/行数/状态分布/账号/快照/时间范围 | ✅ 已执行（MySQL 8.0.44） |
| 接口测试 | curl/Postman 登录→业务接口→401/400/500 | ⏸ **本轮未执行**（可在 IntelliJ 启动后端后按 [9.3](#93-快速自检命令) 补测；**非环境阻塞**） |
| UI 功能测试 | 浏览器手工/自动化 | ⏸ 本轮未执行（依赖后端启动；IDE 内可直接跑） |
| 数据持久化 | 写入→刷新→重启→再查询 | ⏸ 本轮未执行；用例已设计，IDE 启动后可执行 |

### 7.2 功能测试用例（TC）

| 编号 | 模块 | 步骤 | 期望 |
|---|---|---|---|
| TC-01 | 登录 | 正确手机号/密码 → 登录 | 200；token+user；跳 dashboard |
| TC-02 | 登录 | 错误密码 | 400"密码错误"；停留登录页 |
| TC-03 | 登录 | 不存在手机号 | 400"账号不存在" |
| TC-04 | 登录 | 空 phone/password | 400"登录失败" |
| TC-05 | 鉴权 | 无 token 调 `/api/games` | 401"用户未登录" |
| TC-06 | 鉴权 | 伪造/过期 token | 401"登录已过期，请重新登录"；前端清 token 跳登录 |
| TC-07 | 鉴权 | `?token=` 调上传/业务接口 | 与 Header 等价通过 |
| TC-08 | 数据中心 | 打开页面 | 4 卡 + 3 图 + 排行 + 成就榜；数字与 DB 一致（10/99670/360/30/40） |
| TC-09 | 数据中心 | 切 30/90/365 | 请求 `days` 正确；图表补零；无 JS 错误 |
| TC-10 | 数据中心 | 切 week/month/all | `period` 正确；排行按对应时长降序 |
| TC-11 | 游戏库 | 搜索/类型/状态筛选 + 清除 | 结果正确；空态出现"清除筛选" |
| TC-12 | 游戏库 | 网格/列表切换 | 布局切换；列表显示"最近游玩"与下拉 |
| TC-13 | 游戏库 | 列表下拉改状态为"已完成" | 徽标与筛选同步（不改"正在玩"） |
| TC-14 | 详情 | 打开 `/game/1` | 名称/封面/评分/时长/次数/成就/平台正确 |
| TC-15 | 详情 | 笔记失焦保存 → 刷新 | 提示成功；内容保留 |
| TC-16 | 详情 | 状态改为"已完成" → 库筛选 | 库中同步；统计通关数变化 |
| TC-17 | 详情 | 添加/删除标签 → 刷新 | **当前会丢失**（BUG-01，预期失败） |
| TC-18 | 个人中心 | 改用户名/简介/头像 → 刷新 | 全部保留；头像 URL 可访问 |
| TC-19 | 个人中心 | 增删自定义标签 → 刷新 | 保留（`custom_tags`） |
| TC-20 | 账号设置 | 改性别/生日/地区/隐私/通知 → 重开弹窗 | 字段回显正确 |
| TC-21 | 修改密码 | 旧密码错误 / 两次不一致 / 合规提交 | 分别提示；成功后新旧密码行为符合 AC-15 |
| TC-22 | 年度报告 | 切 2025/2026 | 总时长变化；**其他指标不变**（预期缺陷 BUG-06） |
| TC-23 | 年度报告 | 点保存海报/分享 | **仅提示**（预期缺陷 BUG-09） |
| TC-24 | 社交 | 三个 tab/添加好友 | 页面可交互但无真实数据（Mock） |

### 7.3 边界与异常用例

| 编号 | 场景 | 输入 | 预期 |
|---|---|---|---|
| BE-01 | 手机号边界 | 10 位 / 12 位 / `12345678901` | 前端拦截，提示格式错误 |
| BE-02 | 密码边界 | 5 位 / 21 位 | 前端拦截"6-20 位" |
| BE-03 | 笔记超长 | 10 万字符 | `notes` 为 TEXT，理论上可存；需关注请求体大小与页面性能 |
| BE-04 | 非法状态 | `PUT status = "测试中"` | **当前会成功入库**（BUG-12）；期望 400 枚举校验 |
| BE-05 | 越权 ID | `/api/games/999` | 400"游戏不存在" |
| BE-06 | 上传空文件 | 0 字节 | 400"文件上传失败" |
| BE-07 | 上传非图片 | `.txt` 改名 `.png` | **当前后端接受**（BUG-14）；期望类型白名单 |
| BE-08 | 上传超大 | >10MB | Spring multipart 限制 → 413/500；前端 2MB 提示 |
| BE-09 | 非数字参数 | `days=abc`、`year=abc` | **当前 500"未知异常"**（BUG-17）；期望 400 |
| BE-10 | 负数/零参数 | `days=-1`、`limit=0` | 返回空或空列表；不应 500 |
| BE-11 | 无数据用户 | 新 OAuth 账号（无游戏库）→ 年度报告 | **当前 `favoriteGame=null` 导致模板访问异常**（BUG-26） |
| BE-12 | 并发改状态 | 同一游戏连续两次 PUT | 最后一次生效（无锁，可接受） |

### 7.4 数据保存（持久化）用例

| 编号 | 写入 | 验证 | 预期 |
|---|---|---|---|
| PS-01 | 游戏笔记 | 刷新页面 / 重启后端 / 直接查 `user_games.notes` | 保留（`data.sql` 不覆盖 `user_games`） |
| PS-02 | 游戏状态 | 同上 | 保留 |
| PS-03 | 用户名/简介/头像 | 重新登录 + 刷新 | 保留（`users`） |
| PS-04 | 自定义标签 | 刷新 / 重开弹窗 | 保留；清空后 `custom_tags=''`，前端显示空数组 |
| PS-05 | 密码 | 退出→旧密码登录失败→新密码成功 | 保留 |
| PS-06 | 偏好设置 | 刷新 / 换浏览器 | 同浏览器保留（localStorage）；换浏览器丢失；**全站不生效** |
| PS-07 | 游玩记录 | 重启后端 | **每次重置为 360 条**（BUG-23）；未来写入会丢 |
| PS-08 | 年度报告 | 连续 GET 两年 | 快照被覆盖；读取仍实时聚合（BUG-15/24） |
| PS-09 | 头像文件 | 重启后端 + 访问 `/uploads/xxx` | 文件保留；但工作目录变化会导致映射目录变化（部署注意） |

### 7.5 执行记录（已执行 / 未执行）

**已执行（可复现）**

| 检查 | 方法 | 结果 |
|---|---|---|
| 代码结构与接口清单 | 全量阅读 6 Controller / 5 ServiceImpl / 6 Mapper XML / 8 视图 / 5 组件 / 2 store | 与 [附录 B](#附录-b接口快速索引) 一致 |
| 数据库连通与版本 | `mysql -e "SELECT VERSION()"` | 8.0.44 ✅ |
| 表清单 | `SHOW TABLES` | 10 张表齐全 ✅ |
| 数据规模 | 分类 COUNT | users=1、games=10、user_games=10、play_records=360、achievements=40、user_achievements=30、game_genres=11、relations=30、platforms=16、yearly_reports=3 |
| 状态分布 | `GROUP BY status` | 在玩 4、已完成 6；**无"正在玩"** → 佐证 BUG-03 |
| 演示账号 | 查 `users` + MD5 校验 | `GameMasters / 13800138000`；`e10adc…` = `MD5("123456")` ✅ |
| 年度快照 | 查 `yearly_reports` | 2024/2025/2026 三行；2026 快照 559h 与实时 971h 不一致 → 佐证快照过期/不读 |
| 游玩记录时间范围 | MIN/MAX/按年 | 2025-11-19 ~ 2026-11-13，共 360 天；2025 年 133h、2026 年 971h |
| 工具链 | `java -version` / `which mvn` / `node` / `npm` / `.idea/misc.xml` | PATH 为 JDK 1.8、无 mvn；**IDE 工程 SDK 为 `openjdk-26`、内置 Maven 可用**；Node 24.15/11.12 可用 → **命令行不适用，IDE 可运行** |

**未执行（本轮范围限定为只读清单整理，禁止视为通过）**

| 项 | 未执行原因 | 解除方式 |
|---|---|---|
| 后端启动、接口 E2E | 本轮未在 IDE 中启动（任务约定只读整理，**不是环境阻塞**） | IntelliJ 打开 `GameStats/pom.xml` → Project SDK `openjdk-26` → 运行 `GameStatsApplication` |
| 前端页面 E2E、上传、改密、401 跳转 | 依赖后端启动 | 后端启动后 `npm run dev`（`qianduan/node_modules` 已存在） |
| `mvn test` / 前端 `npm run build` | 本轮未执行构建（`dist` 为既有产物） | IDE 内置 Maven / `npm run build`，无需安装 Maven |
| 真实 OAuth、社交、海报导出 | 产品本身为 Mock/未实现，无测试对象 | 需先立项实现（见第 12 章 BUG-09/10/11） |

### 7.6 可复现缺陷清单（静态审查 + 只读数据库核对）

> 严重级别：**高**=核心数据错误/安全/阻断；**中**=功能与预期不符；**低**=体验/维护/边界。  
> 所有缺陷均为**只读分析得出**，未修改代码；标注了根因位置，便于后续修复。

| 编号 | 级别 | 模块 | 缺陷 | 复现/证据 | 期望 | 根因 |
|---|---|---|---|---|---|---|
| BUG-01 | 高 | 游戏详情 | "自定义标签"不持久化，且实为游戏类型标签 | 详情页加标签 → 刷新 → 消失；`tags` 来自 `listGenreNamesByGameId` | 独立保存到用户维度，刷新保留 | `GameDetail.vue:292-307`（仅本地 push/splice）；`GameServiceImpl:65` 用类型填充 tags |
| BUG-02 | 高 | 安全 | 密码 MD5 无盐、JWT 密钥/数据库密码硬编码、token 无吊销 | `AuthServiceImpl:77`；`application.yml:16/35`；`JwtUtil` | BCrypt/Argon2 + 环境变量密钥 + 短 TTL/刷新机制 | 同上 |
| BUG-03 | 中 | 游戏库 | 列表视图下拉写"正在玩"，其他页面用"在玩" | 库→列表视图→⋮→"正在玩"→筛选"在玩"→该游戏消失；详情页状态下拉空 | 全链路统一为"在玩" | `GameCard.vue:42`；`Library.vue:99-105`；`GameDetail.vue:109-113` |
| BUG-04 | 中 | 游戏库 | 分页组件不生效（未切片） | 数据 >24 条时出现分页；点第 2 页无变化 | 按 `currentPage/pageSize` 切片或后端分页 | `Library.vue:58-77`（渲染全部 `filteredGames`） |
| BUG-05 | 中 | 游戏详情 | 游玩时长趋势为随机数 | 刷新详情页柱状图变化 | 使用真实 `play_records` | `GameDetail.vue:226-231` |
| BUG-06 | 中 | 年度报告 | "年度"口径混用：总时长按年，游戏数/成就/最爱/类型全库 | 切 2025/2026：时长 133h→971h，但游戏数恒 10、成就恒 30、最爱恒"艾尔登法环" | 所有指标按所选年份过滤，或页面明确标注"累计" | `YearlyReportServiceImpl:39-83`；`YearlyReportMapper.xml`（无 year 条件） |
| BUG-07 | 中 | 年度报告 | 默认年份硬编码 2025 | 首次进入显示 2025（当前 2026） | 默认当前年 | `YearReport.vue:145` |
| BUG-08 | 中 | 年度报告 | 月度排行永远无封面 | 每月只显示小时数 | 后端返回每月 topGames 或前端改为纯时长榜 | `YearReport.vue:106-112`；`YearReportVO.MonthlyTrendVO` 仅 month/hours |
| BUG-09 | 中 | 年度报告 | "保存海报/分享 Discord"为假实现 | 点击只弹成功提示，无下载/跳转 | 真实导出 PNG/分享链接 | `YearReport.vue:230-236` |
| BUG-10 | 中 | 社交 | 好友/对比/分享/邀请全为硬编码 | 刷新后数据不变；邀请仅提示 | 接后端或明确标注"演示" | `Social.vue:168-236` |
| BUG-11 | 中 | 个人中心 | 绑定账号状态硬编码，解绑刷新还原 | 点"解绑 Steam"→提示成功→刷新→仍"已绑定" | 持久化绑定关系 | `Profile.vue:211-217/274-281` |
| BUG-12 | 中 | 后端 | 状态枚举未校验，任意字符串可入库 | `PUT /api/games/1/status {"status":"测试中"}` → 200，库中写入"测试中" | 400 校验 `在玩/已完成/未玩/搁置/弃坑` | `GameServiceImpl:84-93`；无 `@Valid`/枚举 |
| BUG-13 | 中 | 数据中心 | 多类型游戏按类型重复计时长，百分比总和不等于 100 | 当前 30 条类型关联；饼图 tooltip 与后端 `percent` 不一致 | 以主类型统计或按游戏占比分摊 | `DashboardMapper.xml:91-102` |
| BUG-14 | 中 | 上传 | 后端无类型/大小校验 | 上传 `.txt` 改名 `.png` 成功；>2MB 前端拦截但后端允许至 10MB | 白名单 + 大小限制 + 图片校验 | `UserServiceImpl:114-148` |
| BUG-15 | 低 | 年度报告 | GET 请求产生写副作用（每次 upsert） | 连续刷新报告，`yearly_reports` 字段被覆盖 | 读取快照或显式"生成"按钮 | `YearlyReportServiceImpl:115-123` |
| BUG-16 | 低 | 前端鉴权 | 路由守卫只看 token 是否存在 | 手动写入任意 `token` 可进页面，首个 API 才 401 | 校验过期时间/静默刷新 | `router/index.js:60-76` |
| BUG-17 | 低 | 后端 | 参数类型错误返回 500 | `/api/dashboard/play-trend?days=abc` → 500"未知异常" | 400 + 明确提示 | `GlobalExceptionHandler:41-46`（兜底 Exception） |
| BUG-18 | 低 | 偏好设置 | 偏好只写 localStorage，全站无消费方 | 改主题/语言保存 → 界面无变化 | 主题/语言/区间实际生效 | `PreferenceDialog.vue:203/266-270`；全局无 `gs_preferences` 读取 |
| BUG-19 | 低 | 布局 | 通知铃铛/未读数无功能 | 点击无反应 | 接入通知或隐藏 | `MainLayout.vue:89-94` |
| BUG-20 | 低 | 工程 | 根目录无 README/.gitignore；Maven Wrapper 缺 `mvnw` 脚本；遗留 `_encoding_test.vue`；`dist`/`node_modules` 混放 | 目录可见；`_encoding_test` 无路由引用 | 补文档与忽略规则、补全/移除 wrapper、清理遗留文件 | 仓库根；`src/views/_encoding_test.vue` |
| BUG-21 | ~~中~~ **已关闭** | 环境（已澄清） | ~~要求 JDK 26 + 无 Maven，命令行不可构建~~ → **结论修正：在 IntelliJ IDEA 中运行，无需安装 JDK 26 / Maven** | `GameStats/.idea/misc.xml` 已配置 `openjdk-26`；IDE 内置 Maven 可用；`java.version=26` 仅约束 IDE 项目 SDK | 无需修复；仅 CI/换机场景建议补全 `mvnw` | `GameStats/pom.xml:27`；`GameStats/.idea/misc.xml:11` |

> **定级口径（第二轮统一）**：表中"高/中/低"与本文件 [第 12 章](#12-缺陷审阅与修复决策清单第二轮新增--待用户决策) 的 **P0/P1/P2** 一一对应（高=P0、中=P1、低=P2）。上一轮曾用 P0–P3 表示修复批次，容易与严重级别混淆，现已改为 **B1 本轮必修 / B2 本轮建议 / B3 延后**。
>
> **BUG-21 关闭说明**：上一轮的"命令行不可构建"是基于 `PATH` 中的 JDK 1.8 与未安装 `mvn` 得出的结论，**不适用于本项目的实际运行方式**。项目 `.idea` 工程配置已指向 `openjdk-26`，IntelliJ 使用内置 Maven 导入/构建，故该条**不再作为缺陷**，仅保留"CI 场景需补 `mvnw` 脚本"的工程建议。因此 26 条中**实际待决策 25 条**（P0 2 条、P1 14 条、P2 9 条）。

---

## 8. Leader：冲突与决策记录

| 编号 | 分歧 | 各方观点 | Leader 决策 | 影响 |
|---|---|---|---|---|
| D1 | 交付物是"项目解析"还是"SDD spec" | 产品倾向需求规格；架构认为用户只要解析文档 | **输出项目解析 + Agent 协作交付文档**，不在 `.codeartsdoer/specs` 下创建 spec | 本文档不套用 EARS 模板，但验收案例采用 Given/When/Then 可测试表述 |
| D2 | 社交/绑定/分享是否算"已实现" | 产品初稿标注"支持社交"；QA 证据为硬编码 | **以代码为准，统一标注"占位/Mock"**，不计入本期验收 | 功能边界清晰，避免虚假承诺 |
| D3 | 是否引入新前端依赖/后端分页方案 | 架构建议直接后端分页 + 新 UI 库；开发评估改动大 | **最简方案：不新增依赖**，分页列为待办（BUG-04），先保证现有链路 | 降低本轮风险，后续按需求再升级 |
| D4 | 年度报告口径问题是否算缺陷 | 产品认为"累计展示可接受"；QA 认为"年度"名不副实 | **定为中危缺陷（BUG-06）**，允许后续迭代修，但页面需标注口径 | 验收 AC-16 需接受当前行为或先修 |
| D5 | QA 是否必须启动服务才算测试完成 | QA 希望动态执行；环境不满足 | **执行静态审查 + 数据库只读核对**，动态用例全部标注"未执行/阻塞"，禁止伪造通过 | 交付诚实，明确下一步环境前置 |
| D6 | 是否顺手修复 BUG | 开发可快速改几处；用户明确"先不要改动业务代码" | **零代码改动**，只记录缺陷与落点 | 符合用户约束，为下一轮改动保留基线 |
| D7 | 后端如何运行（JDK 26 + Maven 是否为前置） | 上轮 QA 依据命令行 `java -version`=1.8、无 `mvn` 判定"动态测试受阻" | **以 IntelliJ IDEA 为实际运行方式**：内置 Maven + Project SDK `openjdk-26`，无需安装 JDK 26 / Maven；**BUG-21 关闭**；第 2.5/7.1/7.5/9/10/11 章同步修正 | 动态验证不再被环境阻塞；后续所有构建/联调以 IDE 为基线 |
| D8 | 第二轮是否直接修 BUG | 用户要求"先列出 bug，让我观看然后决定是否修复" | **只做缺陷清单与文档更新，零代码改动**；修复范围待用户逐条决策 | 保持可回退基线，避免未经确认的改动 |

---

## 9. 运行命令与测试步骤

### 9.0 环境前置（实测现状）

| 依赖 | 版本要求 | 本机现状 | 处理建议 |
|---|---|---|---|
| JDK | 26（`pom.xml` 的 `java.version`） | PATH 为 1.8，但 **IntelliJ 项目 SDK 已是 `openjdk-26`** | **无需安装**：IDEA 中确认 Project SDK = `openjdk-26` 即可 |
| Maven | 3.9+（IDE 内置即可） | 命令行未安装 | **无需安装**：使用 IDEA 内置 Maven（Bundled Maven 3） |
| MySQL | 8.0+ | 8.0.44 ✅（`game_stats` 已存在） | 启动本机 MySQL，保持 `root/123456`、端口 3306 |
| Node.js | 16+（实测 24.15） | ✅ | 可直接运行前端 |
| 浏览器 | 现代浏览器 | — | 开发访问 `http://localhost:5173` |

> **本轮环境结论（已修正）**：**不要求安装 JDK 26 + Maven**。后端以 **IntelliJ IDEA** 为运行方式（内置 Maven 导入聚合工程、Project SDK `openjdk-26`），下文"方式 A"是唯一推荐路径；命令行方式仅供参考，不属于前置条件。

> 数据库连接写死在 `GameStats/game-server/src/main/resources/application.yml`（`root/123456`）。若本机不同，需修改该文件（属于配置改动，本次未动）。

### 9.1 启动后端

**方式 A：IntelliJ IDEA（本轮推荐，无需安装 JDK 26 / Maven）**

```text
1. IDEA → File → Open → 选择 GameStats/pom.xml → Open as Project
2. 等待内置 Maven 自动导入完成（右下角进度结束；需要时在 Maven 面板点 Reload）
3. File → Project Structure → Project SDK 选 openjdk-26（工程 .idea 已配置，通常无需改动）
4. Settings → Build, Execution, Deployment → Build Tools → Maven
   → Maven home path 保持 "Bundled (Maven 3)"
5. 运行 com.lxw.gamestats.GameStatsApplication（Run Configuration 由 IDEA 自动生成）
6. 将 Run Configuration 的 Working directory 设为 GameStats（保证 ./uploads 与静态映射一致）
```

> 说明：`pom.xml` 的 `java.version=26` 由 IDE 项目 SDK 满足，`PATH` 中的 JDK 1.8 不参与 IDE 构建。启动前确保本机 MySQL 已运行（`localhost:3306` / `game_stats` / `root/123456`）。

**方式 B：命令行（可选，仅 CI 或换机时使用；本轮不需要）**

```bash
# 前提：自行准备 JDK 26 + Maven 3.9+（本机 PATH 当前不满足）
mvn -f GameStats/pom.xml clean package -DskipTests

# 启动（工作目录建议在 GameStats，保证 ./uploads 与静态映射一致）
cd GameStats
java -jar game-server/target/game-server.jar
```

> 仓库 `.mvn/wrapper/maven-wrapper.properties` 存在，但缺 `mvnw` / `mvnw.cmd` 脚本，因此命令行不提供 Maven Wrapper；如后续接 CI，建议补全脚本（属工程优化，不影响 IDE 开发）。

启动成功标志（日志）：`数据库迁移完成`、`演示账号已就绪`、Tomcat started on port 8080。  
首次启动会自动建库建表并写入种子数据（`play_records` 固定 360 条）。

### 9.2 启动前端

```bash
cd qianduan
npm install      # node_modules 已存在，可跳过
npm run dev      # http://localhost:5173
```

生产构建（可选）：

```bash
cd qianduan
npm run build    # 输出 dist/
```

### 9.3 快速自检命令

**Git Bash / macOS / Linux**

```bash
# 1) 登录，拿 token
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"123456"}'

# 2) 将返回的 token 粘到下面
TOKEN="粘贴上一步的 token"

# 3) 业务接口
curl -s http://localhost:8080/api/user/profile -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/dashboard/stats -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/games -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/report/yearly?year=2026" -H "Authorization: Bearer $TOKEN"

# 4) 401 校验（不带 token）
curl -i http://localhost:8080/api/games
```

**PowerShell（Windows）**

```powershell
$body = '{"phone":"13800138000","password":"123456"}'
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType 'application/json' -Body $body
$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Uri http://localhost:8080/api/dashboard/stats -Headers $headers
Invoke-RestMethod -Uri http://localhost:8080/api/games -Headers $headers
```

### 9.4 手工 E2E 测试步骤（后端+前端启动后）

1. 打开 `http://localhost:5173` → 应跳 `/login`。
2. 输入 `13800138000 / 123456` → 登录 → 进入 `/dashboard`。
3. 核对统计卡：游戏数 **10**、总时长 **69天5小时**、游玩天数 **360**、成就 **30/40**。
4. 切换趋势 30/90/365、热力图年份、排行周期 → 图表刷新无报错。
5. 侧边栏进"游戏库" → 搜索"艾尔登" → 状态筛选"在玩"（4 款）→ 切列表视图。
6. 打开"艾尔登法环"详情 → 修改笔记并失焦 → 刷新确认保留；改状态为"已完成" → 回库筛选确认。
7. 在详情添加一个标签 → 刷新 → **预期失败（BUG-01）**，记录现象。
8. 个人中心 → 改用户名/简介 → 上传一张 ≤2MB 图片 → 保存 → 刷新确认。
9. 添加/删除自定义标签 → 刷新确认（应保留）。
10. 账号菜单 → 修改密码：先输错旧密码（预期"密码错误"），再用正确旧密码改新密码；退出后用新密码登录。
11. 年度报告 → 切 2025/2026 → 记录总时长变化与其他指标是否不变（BUG-06）。
12. 点击"保存海报""分享到 Discord" → 记录仅提示现象（BUG-09）。
13. 社交中心 → 三个 tab、添加好友 → 记录 Mock 现象。
14. 退出登录 → 确认二次确认弹窗 → 回到登录页；直接访问 `/dashboard` 应被重定向。
15. 手工把 localStorage 的 `token` 改成乱码 → 刷新 → 应 401 并跳登录。

---

## 10. 完成 / 未完成事项

### 10.1 已完成

- [x] 仓库、三模块、包结构、依赖方向、构建方式、工具链勘察（T01/T09）
- [x] 8 个页面 + 5 个组件 + 2 个 store + 18 个接口 + 10 张表的功能盘点（T02/T05）
- [x] 数据模型、统计口径、业务实现、页面交互、扩展落点解读（T03/T06）
- [x] 用户旅程（J1–J3）、功能 In/Out、18 条验收案例（T04）
- [x] 接口契约表、页面结构、数据流、前端工程约定（T05）
- [x] 24 条功能用例、12 条边界用例、9 条持久化用例（T07）
- [x] 数据库只读核对：10 表齐全、演示账号 MD5 正确、状态分布、年度快照、时间范围（T08）
- [x] 26 条可复现缺陷/风险，含级别、复现路径、根因文件行号（T08）
- [x] Task List、并行批次、冲突裁决、运行命令、测试步骤（T10）
- [x] 第二轮：环境结论修正（**IntelliJ IDEA 运行，无需安装 JDK 26 / Maven**）+ 26 条缺陷逐条审阅清单（第 12 章）
- [x] **未改动任何业务代码**，仅维护本文件

### 10.2 未完成 / 受阻

- [ ] 后端实际启动与接口 E2E（**本轮未在 IDE 启动**；非环境阻塞，IDEA 内可直接运行）
- [ ] 前端页面 E2E、上传、改密、401 跳转、图表渲染（依赖后端启动；非环境阻塞）
- [ ] IDE 内 `package` / 测试执行（内置 Maven 可用；本轮未执行）
- [ ] `npm run build` 验证（本轮未执行；`dist/` 为既有产物）
- [ ] 真实 OAuth、社交、海报导出（产品未实现，无测试对象）
- [ ] 缺陷修复（按用户要求本轮只出清单，不改代码）

> 结论：**结构解析、功能解读与缺陷审阅清单已完成并可交付**。动态验证本轮未执行（属任务范围限定，**不是环境问题**）；在任何"页面改动/功能增加"开始前，按 [9.1 方式 A](#91-启动后端) 在 IntelliJ 中启动基线，并按 [12.6](#126-建议修复顺序) 先修阻塞项。

---

## 11. 风险、技术债与后续改动接入指南

### 11.1 风险 top 8

1. **数据被启动脚本重置**：`play_records` 每次重启 `DELETE` + 重建；新增"记录游玩"功能前必须先改 `data.sql`（BUG-23）。
2. **状态枚举双轨**："在玩" vs "正在玩"，会在库/详情/统计之间制造数据"消失"（BUG-03）。
3. **年度报告口径**：页面叫"年度"，但多数指标是全库累计（BUG-06），做分享/对外展示前必须统一。
4. **安全基线**：MD5 无盐、密钥硬编码、无登录限流、无 token 吊销（BUG-02）。
5. **详情页标签假保存**：用户会认为功能坏了（BUG-01）。
6. **上传校验缺失**：可上传非图片文件（BUG-14）。
7. **Mock 未标注**：社交/绑定/分享/偏好会让验收与演示产生预期落差（BUG-10/11/18/09）。
8. **运行方式依赖 IDE 配置**：IDE 内开发零门槛（内置 Maven + Project SDK `openjdk-26`），**无需安装 JDK 26 / Maven**；仅换机/CI 需要补全 `mvnw` 与 JDK 说明（原 BUG-21 已澄清关闭，保留 CI 工程建议）。

### 11.2 建议的技术债清理顺序

```text
第 1 批（低风险、高收益）
  ├─ 统一状态值："正在玩" → "在玩"（数据 + 前端）
  ├─ 年度报告默认当前年；空 favoriteGame 兜底
  └─ 清理遗留 _encoding_test.vue；补 README/.gitignore

第 2 批（数据可信）
  ├─ 详情页标签改为持久化（新增 user_game_tags 或复用 custom_tags 结构）
  ├─ 年度报告所有指标按年份过滤（或页面明确"累计"）
  ├─ 类型分布改为按主类型/去重
  └─ 参数校验：状态枚举、year/days/limit、上传类型

第 3 批（工程化/安全）
  ├─ BCrypt + 密钥环境变量 + token 刷新/吊销
  ├─ Maven Wrapper + JDK 版本说明 + Flyway（替代手写迁移）
  └─ 后端分页/排序，前端移除本地全量过滤
```

### 11.3 后续"页面改动 / 功能增加"的接入清单

每次需求进来，按以下清单逐项确认（可直接复制到任务单）：

1. **页面**：改哪个 `views/*.vue`？是否需要新路由/新菜单？
2. **数据**：读哪些接口？返回字段是否齐全？是否需要新增 VO 字段？
3. **写操作**：是否落库？写哪张表？`rows==0` 如何提示？是否受 `play_records` 重置影响？
4. **状态枚举**：是否复用 `在玩/已完成/未玩/搁置/弃坑`？前后端是否一致？
5. **权限**：是否只操作当前用户数据？是否走 `BaseContext`？
6. **空态/错误态**：无数据、401、400、网络错误分别怎么显示？
7. **持久化验证**：刷新、重启、换浏览器分别验证什么？
8. **验收**：新增/修改哪条 AC/TC？是否触发已知 BUG？

---

## 12. 缺陷审阅与修复决策清单（第二轮新增 · 待用户决策）

> **本章用途**：把 [7.6](#76-可复现缺陷清单静态审查--只读数据库核对) 的 26 条缺陷整理成"可逐条勾选决策"的清单。  
> **本轮动作**：**只整理清单 + 修正环境结论，未修改任何业务代码**。  
> **行号说明**：根因行号基于 2026-11-13 的源码快照，并已复核 BUG-01/03/04/05/07/12/13/21/23/25/26 等关键位置；修复前请在 IDE 中用 `Ctrl+Click` 再确认一次（源码可能已微调）。

### 12.0 口径与统计

| 口径 | 定义 |
|---|---|
| **严重级别 P0** | 核心数据错误 / 安全基线 / 会阻断主流程（对应上轮"高"） |
| **严重级别 P1** | 功能与预期不符、数据可信度受损（对应上轮"中"） |
| **严重级别 P2** | 体验、维护性、边界健壮性（对应上轮"低"） |
| **修复批次 B1** | **本轮必修**：不修会污染后续"页面改动 / 功能新增" |
| **修复批次 B2** | **本轮建议**：成本低、与后续改动同批做最划算；或改到对应模块前必须完成 |
| **修复批次 B3** | **延后 / 独立排期**：属新功能实现或独立专项，不阻塞页面改造 |

| 统计项 | 数量 | 说明 |
|---|---|---|
| 清单总数 | **26** | BUG-01 ~ BUG-26，编号与 7.6 完全一致 |
| 已关闭 | **1** | BUG-21（环境结论修正，非缺陷） |
| 实际待决策 | **25** | P0 **2** 条 / P1 **14** 条 / P2 **9** 条 |
| 建议本轮必修（B1） | **6** | BUG-01、03、04、05、12、26 |
| 建议同批（B2） | **10** | BUG-06、07、13、14、15、17、22、23、24、25 |
| 建议延后（B3） | **9** | BUG-02、08、09、10、11、16、18、19、20 |

---

### 12.1 缺陷总览表（26 条，按严重级别分组）

> 列说明：**建议优先级** 使用 B1（本轮必修）/ B2（本轮建议）/ B3（延后 / 独立排期），与"严重级别"分离，避免"级别高就必须现在修"的误读。

#### P0 · 2 条（核心数据错误 / 安全基线）

| 编号 | 级别 | 标题 | 影响页面 / 模块 | 建议修复优先级 |
|---|---|---|---|---|
| BUG-01 | **P0** | 详情页"自定义标签"不持久化，且实为游戏类型标签 | 游戏详情、游戏库 | **B1 本轮必修**（数据模型未定，后续必返工） |
| BUG-02 | **P0** | 密码 MD5 无盐、JWT 密钥/数据库口令硬编码、token 无吊销 | 登录、全局鉴权、配置 | B3 独立安全专项（风险高但需单独排期） |

#### P1 · 14 条（功能与预期不符 / 数据可信度受损）

| 编号 | 级别 | 标题 | 影响页面 / 模块 | 建议修复优先级 |
|---|---|---|---|---|
| BUG-03 | P1 | 状态值"正在玩 / 在玩"双轨 | 游戏库、游戏详情、数据中心 | **B1 本轮必修**（全链路污染） |
| BUG-04 | P1 | 分页组件不生效（列表未切片） | 游戏库 | **B1 本轮必修**（若改库页） |
| BUG-05 | P1 | 详情页游玩趋势为随机数 | 游戏详情 | **B1 本轮必修**（若改详情） |
| BUG-06 | P1 | 年度报告"年度"口径混用 | 年度报告 | B2（改年报则升 B1） |
| BUG-07 | P1 | 年度报告默认年份硬编码 2025 | 年度报告 | B2 |
| BUG-08 | P1 | 月度排行永远无封面 | 年度报告 | B3 |
| BUG-09 | P1 | "保存海报/分享 Discord"为假实现 | 年度报告 | B3（属新功能） |
| BUG-10 | P1 | 社交中心好友/对比/分享全硬编码 | 社交中心 | B3（属新功能） |
| BUG-11 | P1 | 绑定账号硬编码，解绑刷新还原 | 个人中心 | B3 |
| BUG-12 | P1 | 状态枚举未校验，任意字符串可入库 | 后端状态写入、游戏库/详情 | **B1 本轮必修**（脏数据入口） |
| BUG-13 | P1 | 多类型游戏重复计时长，百分比总和≠100% | 数据中心、个人中心 | B2（改图表前） |
| BUG-14 | P1 | 头像上传无类型/大小/魔数校验 | 个人中心、账号设置 | B2 |
| BUG-23 | P1 | 后端重启重置 `play_records` | 数据初始化 | B2（涉及游玩写入则升 B1） |
| BUG-26 | P1 | 空游戏库用户打开年度报告模板报错 | 年度报告（新用户/空库） | **B1 本轮必修**（空态必踩） |

#### P2 · 9 条（体验 / 维护性 / 边界健壮性）

| 编号 | 级别 | 标题 | 影响页面 / 模块 | 建议修复优先级 |
|---|---|---|---|---|
| BUG-15 | P2 | GET 请求产生 upsert 写副作用 | 年度报告、`yearly_reports` | B2（随 BUG-06） |
| BUG-16 | P2 | 路由守卫只看 token 是否存在 | 全局路由 | B3 |
| BUG-17 | P2 | 参数类型错误统一返回 500 | 数据中心/年度报告接口 | B2（新增接口前） |
| BUG-18 | P2 | 偏好只写 localStorage，全站无消费方 | 全局（偏好弹窗） | B3（属新功能） |
| BUG-19 | P2 | 通知铃铛/未读数无功能 | 顶栏（全局） | B3 |
| BUG-20 | P2 | 工程整洁：遗留 `_encoding_test.vue`、根目录缺忽略规则 | 仓库工程 | B3 |
| BUG-22 | P2 | `GameVO.status` 注释写英文枚举，实际为中文 | 游戏列表 VO | B2（随 BUG-12） |
| BUG-24 | P2 | 年度快照只写不读 | 年度报告 | B2（随 BUG-06） |
| BUG-25 | P2 | 详情页顶栏标题显示"首页" | 顶栏、游戏详情 | B2（新增页面会复用） |

#### 已关闭 · 1 条（不计入待决策）

| 编号 | 原级别 | 标题 | 处理结论 | 说明 |
|---|---|---|---|---|
| BUG-21 | ~~P1~~ | ~~要求 JDK 26 + 无 Maven，命令行不可构建~~ | **已关闭（非缺陷）** | 项目在 IntelliJ IDEA 中运行（内置 Maven + SDK `openjdk-26`），无需安装 |

---

### 12.2 P0 缺陷明细（2 条）

#### BUG-01 详情页"自定义标签"不持久化（实为游戏类型标签）

- **级别**：P0（原"高"）
- **影响模块与页面**：游戏详情 `/game/:id` 侧栏"自定义标签"；游戏库卡片/详情的 `tags` 展示。
- **用户可感知现象**：添加/删除标签时界面正常、无报错，但**刷新后全部还原**；且标签内容与"类型"完全一致，用户无法真正自定义。
- **复现步骤**：登录 → 游戏库 → 打开任一游戏详情 → 侧栏"自定义标签"输入新标签并回车 → 标签出现 → 刷新页面 → 标签消失；删除已有标签 → 刷新 → 标签恢复。
- **根因位置**：`qianduan/src/views/GameDetail.vue:292-307`（`addTag`/`removeTag` 只操作本地 `game.value.tags`，无 API 调用）；`GameStats/game-server/src/main/java/com/lxw/gamestats/service/impl/GameServiceImpl.java:65` 附近（`tags` 由 `listGenreNamesByGameId` 填充，没有用户维度存储）。
- **修复难度**：**中高**。需要先定数据模型（新表或新列），再补 DTO/VO/接口/前端调用，并考虑存量数据。
- **建议修复方式**：新增 `user_game_tags(user_id, game_id, tag)`（唯一键 `user_id+game_id+tag`）或 `user_games.custom_tags` JSON 列；新增 `PUT /api/games/{id}/tags`（body `{tags:[...]}`，沿用"裸对象 + `{message}`"约定，`rows==0` 抛"游戏不存在"）；`GameDetailVO.tags` 改读用户标签，游戏类型另开 `genres` 字段；前端乐观更新 + 失败回滚。
- **本轮建议**：**B1 必修**。理由：后续任何"标签/合集/分类/个性化"页面都依赖这个数据模型，先修可避免二次返工。

#### BUG-02 安全基线：MD5 无盐 + 密钥硬编码 + token 无吊销

- **级别**：P0（原"高"）
- **影响模块与页面**：登录 `/login`、所有需鉴权接口、`application.yml` 配置。
- **用户可感知现象**：日常使用无感；但存在撞库/密钥泄露风险；**改密或退出后旧 token 仍可继续调用接口**（无吊销机制）。
- **复现步骤（静态可验证）**：查 `users.password_hash` 为 32 位 MD5；`application.yml` 中数据库口令与 JWT secret 为明文；改密后用旧 token 调 `/api/games` 仍返回 200。
- **根因位置**：`GameStats/.../service/impl/AuthServiceImpl.java:77`（MD5 比对）；`application.yml:16/35`（数据源口令、JWT 密钥）；`game-common` 的 `JwtUtil` / `JwtProperties`（固定 TTL、无 jti/黑名单）。
- **修复难度**：**高**。涉及存量密码迁移、密钥轮换、token 吊销存储，需停机窗口与回滚方案。
- **建议修复方式**：BCrypt/Argon2 + 登录成功时透明升级旧 MD5；密钥与数据库口令改环境变量/配置中心；缩短 access token TTL 并引入 refresh token 或 jti 黑名单；登录失败限流。**建议单独立项，不与页面改动混做。**
- **本轮建议**：**B3 独立排期**（若系统要对外网开放，则必须先做）。严重级别为 P0 是指风险高，不代表适合塞进本轮页面改造。

---

### 12.3 P1 缺陷明细（15 条，含已关闭 1 条）

#### BUG-03 状态值"正在玩 / 在玩"双轨

- **级别**：P1（原"中"）
- **影响模块与页面**：游戏库列表视图、游戏详情状态下拉、数据中心状态相关统计。
- **用户可感知现象**：在列表视图把游戏改成"正在玩"提示成功；再按"在玩"筛选时该游戏**消失**；打开详情页状态框为**空白**（值不在选项中）。
- **复现步骤**：游戏库 → 切"列表视图" → 卡片 `⋮` → 选"正在玩" → 状态筛选"在玩" → 游戏不在结果中；打开该游戏详情 → 状态下拉无选中值。
- **根因位置**：`qianduan/src/components/GameCard.vue:42`（`command="正在玩"`）；`qianduan/src/views/Library.vue:99-105`（选项为"在玩"）；`qianduan/src/views/GameDetail.vue:109-113`（选项为"在玩"）；后端无枚举校验（见 BUG-12）。
- **修复难度**：**低**。
- **建议修复方式**：全链路统一为"在玩"；修 `GameCard` 的 command 与 `STATUS_CLASS_MAP`；执行一次数据修正 `UPDATE user_games SET status='在玩' WHERE status='正在玩'`，并在 `data.sql`/迁移脚本中兜底；与 BUG-12 同批做。
- **本轮建议**：**B1 必修**。理由：会污染筛选、统计与详情显示，任何状态相关改动都会被它带偏。

#### BUG-04 游戏库分页不生效

- **级别**：P1（原"中"）
- **影响模块与页面**：游戏库 `/library`。
- **用户可感知现象**：游戏超过 24 款时出现分页器，**点击第 2 页列表完全不变**（仍渲染全部游戏）。
- **复现步骤**：临时把 `pageSize` 改为 5（或让库内游戏 >24）→ 出现分页器 → 点第 2 页 → 列表无变化。
- **根因位置**：`qianduan/src/views/Library.vue:58-77`（`v-for="game in filteredGames"` 直接渲染全部；`el-pagination` 只绑定 `currentPage`，没有切片计算）；`pageSize=24` 在 96 行。
- **修复难度**：**低**（前端切片）；若要服务端分页则为**中**。
- **建议修复方式**：短期加 `computed pagedGames = filteredGames.slice((currentPage-1)*pageSize, currentPage*pageSize)`，并 `watch` 筛选条件重置 `currentPage=1`；中期再评估后端分页（`GameController` 增 `page/pageSize` + `UserGameMapper.xml` `LIMIT/OFFSET` + `PageResult`，会牵动"裸对象"约定）。
- **本轮建议**：**B1 必修**（游戏库是最可能的页面改造目标；若本轮不碰库页可降为 B2，但建议顺手修）。

#### BUG-05 详情页游玩趋势为随机数

- **级别**：P1（原"中"）
- **影响模块与页面**：游戏详情 `/game/:id` 的"近 7 天游玩趋势"图。
- **用户可感知现象**：同一游戏每次刷新柱状图数值都不同，与真实游玩记录无关。
- **复现步骤**：打开 `/game/1` → 记录柱状图 → 刷新页面 → 柱高/数值变化。
- **根因位置**：`qianduan/src/views/GameDetail.vue:226-231`（`Math.floor(Math.random() * 120 + 30)` 生成数据）。
- **修复难度**：**中**（需补接口 + 归属校验）。
- **建议修复方式**：新增 `GET /api/games/{id}/play-trend?days=7`（复用 `play_records` 按日 `SUM(duration)`，按 `user_id + game_id` 校验归属），返回 `DailyPlayVO`；或扩展 `GameDetailVO` 带 `dailyTrend`。前端改为接口数据 + 无数据空态。
- **本轮建议**：**B1 必修**（详情页任何图表/统计改造的前置）。

#### BUG-06 年度报告"年度"口径混用

- **级别**：P1（原"中"）
- **影响模块与页面**：年度报告 `/year-report`。
- **用户可感知现象**：切换 2025/2026 时，只有**总时长与月度趋势**变化；**游戏数恒为 10、成就恒为 30、年度最爱恒为"艾尔登法环"、类型占比不变**，与"年度"语义不符。
- **复现步骤**：登录 → 年度报告 → 记录 2025 的四项指标 → 切 2026 → 四项指标不变（总时长 133h → 971h）。
- **根因位置**：`GameStats/.../service/impl/YearlyReportServiceImpl.java:39-83`（`getBasicStats`、`countAchievements`、`getFavoriteGame`、`listGenreStats` 均只传 `userId`，**无 year 参数**）；`resources/mapper/YearlyReportMapper.xml` 对应 SQL 无年份条件。
- **修复难度**：**中**（口径需产品确认，SQL 改动面较大）。
- **建议修复方式**：二选一并写入验收：**A（推荐）** 所有指标按年过滤——成就按 `unlocked_date`、通关/游戏数按年内有记录或 `created_at`、最爱按年内 `play_records` 时长、类型按年内时长；同时保留"生涯累计"分区。**B** 页面显式改为"累计数据"并调整标题/文案。避免"半年度半累计"。
- **本轮建议**：**B2；若本轮要改年度报告则升为 B1**。理由：不改年报可延后，但一旦改就必须先定口径，否则改完仍是错的。

#### BUG-07 年度报告默认年份硬编码 2025

- **级别**：P1（原"中"）
- **影响模块与页面**：年度报告。
- **用户可感知现象**：2026 年进入页面，年份选择器默认显示 **2025**。
- **复现步骤**：登录 → 年度报告 → 首次进入查看年份框。
- **根因位置**：`qianduan/src/views/YearReport.vue:145`（`const selectedYear = ref(2025)`；`currentYear` 已在 147 行定义却未使用）。
- **修复难度**：**低**。
- **建议修复方式**：改为 `ref(currentYear)`；若当前年无数据，回退到最近有数据的年份（后端加 `availableYears` 或前端按 `monthlyTrend` 探测）。
- **本轮建议**：**B2**（1 行改动，建议与 BUG-06 同批）。

#### BUG-08 年度报告月度排行永远无封面

- **级别**：P1（原"中"）
- **影响模块与页面**：年度报告详情区"月度排行"。
- **用户可感知现象**：每月条目只显示小时数，没有封面/游戏名，排行可读性差。
- **复现步骤**：年度报告 → 下滑到月度排行 → 观察条目。
- **根因位置**：`qianduan/src/views/YearReport.vue:106-112`；`game-pojo/.../vo/YearReportVO.java` 的 `MonthlyTrendVO` 只有 `month/hours`。
- **修复难度**：**中**（VO + SQL 窗口函数）。
- **建议修复方式**：`MonthlyTrendVO` 增 `topGames[{id,name,cover,hours}]`，SQL 按月分区取前 N；或前端直接改为"月度时长榜"并去掉封面占位，避免半成品。
- **本轮建议**：**B3**（视觉增强，不阻塞）。

#### BUG-09 "保存海报 / 分享 Discord"为假实现

- **级别**：P1（原"中"）
- **影响模块与页面**：年度报告底部操作按钮。
- **用户可感知现象**：点"保存海报"提示"已保存到本地"，下载目录**没有文件**；点"分享到 Discord"仅提示，无跳转。
- **复现步骤**：年度报告 → 依次点击两个按钮 → 检查下载目录与浏览器行为。
- **根因位置**：`qianduan/src/views/YearReport.vue:230-236`（两个函数仅 `ElMessage.success`）。
- **修复难度**：**中高**。PNG 导出需引入 `html2canvas` 等依赖（与"不新增依赖"约定冲突）；真实分享还需产品定义渠道与后端。
- **建议修复方式**：短期在按钮旁标注"演示"，或先做"打印/浏览器另存为"；中期评估 canvas 导出方案后再实现，分享需单独立项。
- **本轮建议**：**B3**（本质是新功能，不是修 bug）。

#### BUG-10 社交中心全部硬编码

- **级别**：P1（原"中"）
- **影响模块与页面**：社交中心 `/social`。
- **用户可感知现象**：好友列表、数据对比、分享记录刷新后完全不变；邀请码 `A8X9K2` 提交只弹提示。
- **复现步骤**：进入社交中心 → 切换三个 tab → 添加好友并提交 → 无任何真实变化。
- **根因位置**：`qianduan/src/views/Social.vue:168-236`（全部 `ref([...])` 硬编码）；后端无接口、无表。
- **修复难度**：**高**（需新表、接口、页面重写）。
- **建议修复方式**：短期加"演示数据"标识；中期立项 `friendships`、`friend_requests`、`shares` 表与对应 Controller/Service/Mapper。
- **本轮建议**：**B3**（属新功能）。

#### BUG-11 绑定账号硬编码，解绑刷新还原

- **级别**：P1（原"中"）
- **影响模块与页面**：个人中心"绑定账号"。
- **用户可感知现象**：点"解绑 Steam"提示成功，**刷新后又变回"已绑定"**；点"绑定"仅提示跳转，无真实授权。
- **复现步骤**：个人中心 → Steam"解绑" → 刷新页面 → 仍显示"已绑定"。
- **根因位置**：`qianduan/src/views/Profile.vue:211-217`（`ref({steam:true, epic:false, discord:false})`）、`274-281`（`handleLink` 只改本地）；后端无绑定数据模型。
- **修复难度**：**中**（新表/列 + OAuth 绑定流程）。
- **建议修复方式**：短期禁用按钮或标注"演示"；中期新增 `user_platform_bindings` 表 + `GET/PUT /api/user/bindings`，与真实 OAuth 一起做。
- **本轮建议**：**B3**。

#### BUG-12 状态枚举未校验，任意字符串可入库

- **级别**：P1（原"中"）
- **影响模块与页面**：后端 `PUT /api/games/{gameId}/status`；游戏库/详情/统计的后续读取。
- **用户可感知现象**：接口可写入"测试中"等非法状态，页面随后出现筛选不到、徽标空白、统计口径错乱等"幽灵状态"。
- **复现步骤**：登录拿 token → `PUT /api/games/1/status`，body `{"status":"测试中"}` → 返回 200 → 查库/打开详情可见"测试中"。
- **根因位置**：`GameStats/.../service/impl/GameServiceImpl.java:84-94`（仅判 `null`，无枚举校验）；`GameStatusDTO` 无 `@NotBlank/@Pattern`；Controller 无 `@Valid`。
- **修复难度**：**低**。
- **建议修复方式**：定义状态枚举/常量（`在玩/已完成/未玩/搁置/弃坑`），Service 校验非法值抛 `BaseException`（400）；DTO 加 `@NotBlank`；同时清理历史非法值。
- **本轮建议**：**B1 必修**（低成本，且是所有状态写入的守门员）。

#### BUG-13 类型分布重复计时长，百分比总和不等于 100%

- **级别**：P1（原"中"）
- **影响模块与页面**：数据中心"游戏类型分布"饼图；个人中心"类型偏好"。
- **用户可感知现象**：后端 `percent` 之和可超过 100%；饼图 tooltip 的 `%` 与后端返回的 `percent` 不是同一个值；各类型小时数合计大于总时长。
- **复现步骤**：打开数据中心 → hover 各扇区累加百分比 → 超过 100%；对比统计卡总时长。
- **根因位置**：`GameStats/.../resources/mapper/DashboardMapper.xml:91-102`（按 `game_genre_relations` 展开后 `SUM(ug.play_time)`，多类型游戏在每个类型上重复计一次）；前端把 `percent` 当 ECharts value，ECharts 会再次归一化。
- **修复难度**：**中**（口径变更会影响个人中心）。
- **建议修复方式**：二选一——**A** 只取主类型（`ORDER BY gg.id LIMIT 1`，与详情页 `genre` 口径一致）；**B** 按游戏在类型间均摊（`play_time / 类型数`），保证合计 100%。前端统一用 `hours` 计算占比、tooltip 显示后端 `percent`。
- **本轮建议**：**B2；若本轮改数据中心/个人中心图表则升 B1**。

#### BUG-14 头像上传无类型/大小/魔数校验

- **级别**：P1（原"中"）
- **影响模块与页面**：个人中心、账号设置弹窗的头像上传；`POST /api/upload/avatar`。
- **用户可感知现象**：把 `.txt` 改名 `.png` 也能上传成功并可访问；后端放行到 multipart 上限，前端仅提示 2MB。
- **复现步骤**：准备 `a.txt` → 改名 `a.png` → 在账号设置选择该文件 → 保存 → 返回 `/uploads/xxx.png` 且可访问。
- **根因位置**：`GameStats/.../service/impl/UserServiceImpl.java:114-137`（只判空，无 content-type、扩展名白名单、魔数、大小校验）；`application.yml` 的 multipart 上限未收紧。
- **修复难度**：**低**。
- **建议修复方式**：白名单 `image/jpeg|png|webp|gif` + 扩展名 + `ImageIO.read` 魔数校验 + 大小 ≤2MB；失败统一 400"文件上传失败"；同步收紧 `spring.servlet.multipart.max-file-size`。
- **本轮建议**：**B2**（改动小、安全收益明确）。


#### BUG-21 ~~要求 JDK 26 + 无 Maven，命令行不可构建~~【已关闭】

- **状态**：**已关闭（非缺陷）**。
- **原判定依据**：`PATH` 中 `java` 为 1.8.0_161、`mvn` 未安装。
- **修正结论**：项目实际运行方式为 **IntelliJ IDEA**——`GameStats/.idea/misc.xml:11` 已配置 `project-jdk-name="openjdk-26"`，IDEA 使用**内置 Maven** 导入/构建聚合工程，**无需安装 JDK 26 或 Maven**。详见 [2.5](#25-构建与工具链事实t09) 与 [9.0](#90-环境前置实测现状)。
- **残留工程建议**：`.mvn/wrapper/maven-wrapper.properties` 存在但缺 `mvnw` / `mvnw.cmd`；若后续接 CI，补全脚本即可（不属本轮缺陷）。
- **本轮建议**：**不修**。

#### BUG-23 后端重启重置 `play_records`

- **级别**：P1（原"中"）
- **影响模块与页面**：数据初始化；未来"记录游玩/导入时长"功能。
- **用户可感知现象**：当前无写接口，用户暂时无感；**一旦新增游玩记录写入，每次重启后端都会被清空并重建 360 条**，用户数据丢失。
- **复现步骤**：向 `play_records` 手工插入一条测试记录 → 重启后端 → 该记录消失，恢复为 360 条。
- **根因位置**：`GameStats/game-server/src/main/resources/data.sql:252-272`（`DELETE FROM play_records WHERE user_id = 1` + 递归 CTE 重建 360 条），且 `spring.sql.init.mode=always`。
- **修复难度**：**低**。
- **建议修复方式**：种子插入改为"仅当无数据时执行"（`INSERT ... SELECT ... WHERE NOT EXISTS`），或按 profile 区分初始化（dev 种子 / prod 不重置）；**新增任何游玩记录写接口前必须完成**。
- **本轮建议**：**B2；若本轮新增游玩记录写入则升 B1**。

#### BUG-26 空游戏库用户打开年度报告模板报错

- **级别**：P1（原"中"）
- **影响模块与页面**：年度报告（新注册/OAuth 自动注册且无游戏库的用户）。
- **用户可感知现象**：页面白屏或图表不渲染（模板访问 `favoriteGame.cover` 抛异常），控制台报错。
- **复现步骤**：构造一个无 `user_games` 的账号（OAuth 回调会自动注册）→ 登录 → 打开年度报告。
- **根因位置**：`qianduan/src/views/YearReport.vue:49-59`（直接访问 `reportData.favoriteGame.cover/name/hours`，无空值保护）；`YearlyReportServiceImpl.java:51-52`（`getFavoriteGame` 可返回 `null` 并直接 `setFavoriteGame`）。
- **修复难度**：**低**。
- **建议修复方式**：前端 `v-if="reportData.favoriteGame"` + 空态"暂无最爱游戏"；后端可返回空对象或保持 null 并在前端兜底；`playStyle` 标签规则同步处理空库。
- **本轮建议**：**B1 必修**（新增页面/空态几乎必踩）。

---

### 12.4 P2 缺陷明细（9 条）

#### BUG-15 年度报告 GET 产生写副作用

- **级别**：P2（原"低"）
- **影响模块与页面**：年度报告、`yearly_reports` 表。
- **用户可感知现象**：每次打开/刷新报告都会覆盖当年的快照；历史快照无法保留（2026 快照 559h 与实时 971h 不一致即为证据）。
- **复现步骤**：连续刷新年度报告 → 观察 `yearly_reports` 对应行被更新。
- **根因位置**：`YearlyReportServiceImpl.java:115-123`（GET 流程内 `yearlyReportMapper.upsert(report)`）。
- **修复难度**：**低-中**。
- **建议修复方式**：拆成 `GET` 只读（优先读快照，无快照再实时算但不落库）+ `POST /api/report/yearly/generate` 显式生成；或加配置开关。
- **本轮建议**：**B2（与 BUG-06/24 同批）**。

#### BUG-16 路由守卫只看 token 是否存在

- **级别**：P2（原"低"）
- **影响模块与页面**：全局路由守卫。
- **用户可感知现象**：手动往 localStorage 写任意 `token` 即可进入内页，直到首个接口 401 才被踢回登录页。
- **复现步骤**：未登录状态在控制台执行 `localStorage.token='abc'` → 刷新 `/dashboard` → 可进入页面。
- **根因位置**：`qianduan/src/router/index.js:60-76`（仅判断 `localStorage.token` 存在，未解析 `exp`）。
- **修复难度**：**低-中**。
- **建议修复方式**：前端解析 JWT payload 校验 `exp`（仅体验优化，真正鉴权仍在后端 401 拦截）；或登录时记录过期时间。
- **本轮建议**：**B3**（若新增页面需鉴权一致性可顺手做）。

#### BUG-17 参数类型错误统一返回 500

- **级别**：P2（原"低"）
- **影响模块与页面**：`/api/dashboard/play-trend?days=abc`、`/api/report/yearly?year=abc` 等。
- **用户可感知现象**：返回 500"未知异常"，前端提示不明确，监控误报。
- **复现步骤**：带合法 token 请求 `days=abc` / `year=abc` → 500。
- **根因位置**：`GameStats/.../handler/GlobalExceptionHandler.java:41-46`（`@ExceptionHandler(Exception.class)` 兜底为 500，未单独处理 `MethodArgumentTypeMismatchException` 等）。
- **修复难度**：**低**。
- **建议修复方式**：新增对 `MethodArgumentTypeMismatchException`、`MissingServletRequestParameterException` 的处理 → 400"参数格式错误"；`days/year/limit` 用 `@Min/@Max` 校验并 `@Validated`。
- **本轮建议**：**B2**（新增接口会复用该兜底，先修更省心）。

#### BUG-18 偏好只写 localStorage，全站无消费方

- **级别**：P2（原"低"）
- **影响模块与页面**：偏好设置弹窗、全局主题/语言/图表默认值。
- **用户可感知现象**：改主题/动效/语言/时区/默认图表/默认区间并保存成功，**界面毫无变化**。
- **复现步骤**：打开偏好设置 → 切换主题/语言 → 保存 → 刷新 → 界面不变。
- **根因位置**：`qianduan/src/components/PreferenceDialog.vue:203/266-270`（写 `localStorage.gs_preferences`）；全站无读取该 key 的代码。
- **修复难度**：**中**（涉及全局消费与样式改造）。
- **建议修复方式**：新增 `usePreference` 组合式函数，在 `App.vue`/`MainLayout` 消费；先落地 3 个高价值项（默认趋势区间、默认年份、日期格式），主题/多语言另立项。
- **本轮建议**：**B3**（属新功能）。

#### BUG-19 通知铃铛/未读数无功能

- **级别**：P2（原"低"）
- **影响模块与页面**：顶栏（全局）。
- **用户可感知现象**：点击铃铛无任何反应，但有未读红点，形成"假入口"。
- **复现步骤**：任意内页 → 点击顶栏铃铛。
- **根因位置**：`qianduan/src/layouts/MainLayout.vue:89-94`（仅图标与红点，无点击逻辑/弹层）。
- **修复难度**：**低**。
- **建议修复方式**：短期隐藏红点或点击弹"暂无通知"；中期接通知表与接口。
- **本轮建议**：**B3**。

#### BUG-20 工程整洁问题

- **级别**：P2（原"低"）
- **影响模块与页面**：仓库工程；前端 `views`。
- **用户可感知现象**：无直接用户感知；影响协作与构建（`dist`/`node_modules` 与源码混放，遗留文件误导）。
- **复现步骤**：查看仓库根目录与 `src/views/_encoding_test.vue`；`_encoding_test` 无路由引用。
- **根因位置**：仓库根目录（缺 `.gitignore`/README）；`qianduan/src/views/_encoding_test.vue`。
- **修复难度**：**低**。
- **建议修复方式**：根目录补 `.gitignore`、README 指向本文档；确认无引用后删除 `_encoding_test.vue`；忽略 `dist`/`node_modules`/`target`/`uploads`。
- **本轮建议**：**B3（可顺手）**。注意：删除文件属代码改动，需你确认后才做。

#### BUG-22 `GameVO.status` 注释与枚举不符

- **级别**：P2（原"低"）
- **影响模块与页面**：游戏列表/详情 VO 注释，影响后续开发理解。
- **用户可感知现象**：无直接感知，但注释写英文枚举、实际库值为中文，易导致新代码写错状态值。
- **复现步骤**：查看 `GameVO.java:40-43` 注释 → 对比 `user_games.status` 实际值"在玩/已完成"。
- **根因位置**：`GameStats/game-pojo/src/main/java/com/lxw/pojo/vo/GameVO.java:40-43`。
- **修复难度**：**低**。
- **建议修复方式**：更新注释并引用 BUG-12 定义的枚举常量，禁止裸字符串。
- **本轮建议**：**B2（随 BUG-12 一起做）**。

#### BUG-24 年度快照只写不读

- **级别**：P2（原"低"）
- **影响模块与页面**：年度报告、`yearly_reports`。
- **用户可感知现象**：表里有 2024/2025/2026 三条快照，但接口永远实时聚合，快照形同虚设；历史年份数据无法"回放"。
- **复现步骤**：查 `yearly_reports` 有 3 行 → 调 `GET /api/report/yearly?year=2025` → 实际走实时聚合（可与快照值对比）。
- **根因位置**：`YearlyReportServiceImpl.java:33-126`（全流程实时聚合，无 `getByUserIdAndYear` 读取调用）。
- **修复难度**：**低-中**。
- **建议修复方式**：与 BUG-15 一起改——补 `YearlyReportMapper.getByUserIdAndYear`，GET 优先读快照；快照缺失时回退实时计算。
- **本轮建议**：**B2（与 BUG-06/15 同批）**。

#### BUG-25 详情页顶栏标题显示"首页"

- **级别**：P2（原"低"）
- **影响模块与页面**：顶栏标题、游戏详情页。
- **用户可感知现象**：`/game/1` 时顶栏标题为"首页"，与页面内容不符。
- **复现步骤**：进入任一游戏详情 → 看顶栏标题。
- **根因位置**：`qianduan/src/layouts/MainLayout.vue:143-146`（仅按 `menuItems.find(m => m.path === route.path)` 匹配，详情页不在菜单中 → 落到默认"首页"）。
- **修复难度**：**低**。
- **建议修复方式**：优先取 `route.meta.title`；或对 `/game/` 前缀返回"游戏详情"；新增页面统一写 `meta.title`（顺带解决后续新页面的标题问题）。
- **本轮建议**：**B2**（新增页面必用）。

---

### 12.5 对后续页面改动 / 功能新增的影响判断

> 这一节直接回答"哪些必须先修、哪些可以延后"。判断标准：**是否会污染新页面的数据模型、状态值、统计口径或空态**。

#### A. 必须先修（不修则新功能会建立在错误基础上）

| 编号 | 为什么阻塞 | 前置范围 |
|---|---|---|
| BUG-01 | 标签数据模型未定，任何"标签/分类/合集"功能都会返工 | 改游戏详情/游戏库前 |
| BUG-03 | "正在玩"会持续写脏数据，污染筛选、统计、详情显示 | 任何状态相关改动前 |
| BUG-12 | 状态写入无校验，新入口会继续产生非法值 | 任何状态写入/新增接口前 |
| BUG-26 | 空库/新用户必踩模板崩溃，新页面若复制该写法会重复出问题 | 改年度报告/新增空态页前 |
| BUG-04 | 游戏库分页是假实现，做排序/筛选/分页改造会直接踩坑 | 改游戏库前 |
| BUG-05 | 详情页趋势是随机数，任何图表/统计增强都无可信数据源 | 改游戏详情前 |

#### B. 条件必修（改动触达对应模块前完成，否则可延后）

| 编号 | 触发条件 |
|---|---|
| BUG-06 / 15 / 24 | 本轮要改年度报告、要做分享/对外展示 |
| BUG-13 | 本轮要改数据中心或个人中心的类型图表 |
| BUG-14 | 本轮要改头像/上传，或系统对外 |
| BUG-17 | 本轮新增任何带查询参数的接口 |
| BUG-23 | 本轮新增"记录游玩/导入时长/写 play_records" |
| BUG-25 | 本轮新增页面（顶栏标题会复用该逻辑） |
| BUG-07 | 本轮改年度报告（1 行，顺手） |

#### C. 可以延后（属新功能实现或独立专项，不影响页面改造正确性）

| 编号 | 延后理由 |
|---|---|
| BUG-02 | 安全专项，需密码迁移与密钥轮换，独立排期 |
| BUG-08 / 09 | 年度报告视觉/导出增强，属功能实现 |
| BUG-10 / 11 / 18 | 社交、绑定、偏好均为"新功能"，当前是占位 |
| BUG-16 | 前端体验优化，后端 401 已兜底 |
| BUG-19 | 假入口，隐藏即可，不阻塞 |
| BUG-20 | 工程整洁，随迭代清理 |
| BUG-22 | 注释更新，随 BUG-12 顺手改（1 行） |

---

### 12.6 建议修复顺序

> 原则：**先止血（低成本、防脏数据）→ 再定数据模型 → 再统一口径 → 最后做新功能与安全专项**。每波结束都在 IntelliJ 启动一次，按 [9.4](#94-手工-e2e-测试步骤后端前端启动后) 回归。

| 波次 | 内容 | 包含缺陷 | 预估 | 产出 / 验收 |
|---|---|---|---|---|
| **第 0 波** | 建立基线（不改代码） | — | 0.5h | IDEA 启动后端 + 前端，跑一遍 9.4，记录当前现象与截图 |
| **第 1 波** | 状态与空态止血 | **BUG-03、BUG-12、BUG-26、BUG-07、BUG-25、BUG-22** | 0.5–1 天 | 状态全链路统一；非法状态返回 400；空库年报不崩；默认当前年；详情标题正确 |
| **第 2 波** | 游戏库与详情数据可信 | **BUG-04、BUG-05** | 0.5–1 天 | 翻页生效；详情趋势来自 `play_records` 且归属正确 |
| **第 3 波** | 标签数据模型 + 上传/参数健壮性 | **BUG-01、BUG-14、BUG-17** | 1–2 天 | 标签刷新保留；非法文件 400；`days=abc` 返回 400 |
| **第 4 波** | 统计口径与持久化（与年度报告改造绑定） | **BUG-06、BUG-13、BUG-15、BUG-23、BUG-24** | 1–2 天 | 年度指标全部按年；类型占比合计 100%；GET 不再写库；重启不重置记录 |
| **第 5 波** | 功能实现与独立专项 | **BUG-02、BUG-08、BUG-09、BUG-10、BUG-11、BUG-16、BUG-18、BUG-19、BUG-20** | 单独排期 | 安全专项、社交/绑定/偏好/导出等按需求立项 |

**为什么不按编号顺序修**：BUG-02 级别最高但成本最高且不阻塞页面；BUG-10/11 本质是"没做"而不是"做错"，先修会变成需求开发。按"污染面 × 成本"排序，先做第 1 波收益最大。

**每波通用验证**：

1. 后端在 IDEA 重启（顺带验证 BUG-23 是否复发）。
2. 按 [9.3](#93-快速自检命令) 跑登录 + 关键接口。
3. 浏览器手工回归 [9.4](#94-手工-e2e-测试步骤后端前端启动后) 对应步骤。
4. 涉及写入的，刷新页面 + 重启后端 + 直接查库三层验证（PS-01~PS-09）。

---

### 12.7 本轮交付说明与决策请求

**交付物位置（本轮）**

| 项目 | 内容 |
|---|---|
| **文件** | `C:\workspace\jweb-zuoye\GameStats-项目解析与Agent协作交付.md` |
| **新增章节** | **第 12 章「缺陷审阅与修复决策清单（第二轮新增 · 待用户决策）」**：12.0 口径、**12.1 缺陷总览表（26 条）**、12.2 P0 明细、12.3 P1 明细、12.4 P2 明细、12.5 阻塞判断、12.6 修复顺序、12.7 本说明 |
| **修正章节** | 封面更新说明、目录、0 摘要、2.5 工具链、7.1 测试策略、7.5 未执行项、7.6 BUG-21 状态、8 决策记录（D7/D8）、9.0 环境前置、9.1 启动方式、10.1/10.2 完成情况、11.1 风险第 8 条 |
| **代码改动** | **0**：未修改任何 `.java` / `.vue` / `.xml` / `.sql` / 配置 |

**请回复以下任一选项，我再进入下一步：**

- **A. 全部修**（25 条待决策全做，按 12.6 波次推进）。
- **B. 只修 B1**（BUG-01、03、04、05、12、26，共 6 条，先打通页面改造基线）。
- **C. 指定编号**（例如"修 03、04、12、26"，我按你给的清单执行）。
- **D. 暂不修**（保持现状，仅保留本清单）。

> 若选 A/B/C，请同时确认：**BUG-02 安全改造是否纳入本轮**（默认不纳入，建议独立排期）；**是否允许新增数据表/字段**（BUG-01 需要，属结构性改动）。

---

## 附录 A：文件地图

### A.1 后端关键文件

| 文件 | 作用 |
|---|---|
| `GameStats/pom.xml` | 父 POM（Spring Boot 4.1.0 / Java 26 / 模块聚合 / Lombok 注解处理） |
| `GameStats/game-server/pom.xml` | 启动模块依赖（webmvc、validation、mybatis、jdbc、mysql、actuator、test） |
| `.../GameStatsApplication.java` | 启动入口（`@ComponentScan("com.lxw")`、`@MapperScan`） |
| `.../controller/AuthController.java` | 登录/OAuth 3 接口 |
| `.../controller/UserController.java` | 资料/统计/改密 4 接口 |
| `.../controller/GameController.java` | 列表/详情/笔记/状态 4 接口 |
| `.../controller/DashboardController.java` | 统计/排行/趋势/类型/热力 5 接口 |
| `.../controller/YearlyReportController.java` | 年度报告 1 接口 |
| `.../controller/UploadController.java` | 头像上传 1 接口 |
| `.../service/impl/AuthServiceImpl.java` | 手机号登录、OAuth Mock、JWT 生成 |
| `.../service/impl/UserServiceImpl.java` | 资料更新、标签 JSON、上传、改密 |
| `.../service/impl/GameServiceImpl.java` | 游戏列表/详情/笔记/状态、归属校验 |
| `.../service/impl/DashboardServiceImpl.java` | 空结果兜底、当前用户校验 |
| `.../service/impl/YearlyReportServiceImpl.java` | 年度聚合、标签规则、快照 upsert |
| `.../interceptor/JwtInterceptor.java` | token 解析（Header/query）、ThreadLocal |
| `.../config/WebMvcConfig.java` | 拦截路径、`/uploads/**` 静态映射 |
| `.../config/DatabaseMigrationRunner.java` | users 补列、演示账号 upsert |
| `.../handler/GlobalExceptionHandler.java` | 401/400/500 统一 JSON |
| `resources/application.yml` | 端口/数据源/SQL 初始化/MyBatis/JWT/上传目录 |
| `resources/schema.sql` | 10 表 DDL + `custom_tags` 幂等补列 |
| `resources/data.sql` | 种子数据（游戏/类型/成就/库/解锁/平台/360 条记录/年报快照） |
| `resources/mapper/*.xml` | 6 个 Mapper SQL |

### A.2 前端关键文件

| 文件 | 作用 |
|---|---|
| `qianduan/vite.config.js` | 端口 5173、`/api` 与 `/uploads` 代理、`@` 别名、SCSS 变量注入 |
| `qianduan/src/utils/api.js` | axios 实例、token 注入、FormData 处理、401 跳转 |
| `qianduan/src/router/index.js` | 路由表 + 登录守卫 |
| `qianduan/src/store/user.js` | token/userInfo/gameStats + 登录/资料/统计 |
| `qianduan/src/store/game.js` | 游戏列表/筛选/笔记/状态 |
| `qianduan/src/layouts/MainLayout.vue` | 侧边栏/顶栏/用户菜单/三个弹窗 |
| `qianduan/src/views/Login.vue` | 登录表单、手机号校验、OAuth 入口、漩涡背景 |
| `qianduan/src/views/Dashboard.vue` | 统计卡 + 3 图表 + 排行 + 成就榜 |
| `qianduan/src/views/Library.vue` | 搜索/筛选/视图/分页/空态 |
| `qianduan/src/views/GameDetail.vue` | Hero/统计/趋势/笔记/成就/状态/标签/平台 |
| `qianduan/src/views/Profile.vue` | 资料/统计/绑定(Mock)/偏好/标签/编辑 |
| `qianduan/src/views/YearReport.vue` | 海报/年度切换/月度图/排行/里程碑 |
| `qianduan/src/views/Social.vue` | 全 Mock 社交页 |
| `qianduan/src/components/*.vue` | StatCard / GameCard / 3 个设置弹窗 |
| `qianduan/src/styles/variables.scss` | 色板/尺寸/圆角/阴影（暗色主题基线） |

---

## 附录 B：接口快速索引

```text
认证（免登录）
  POST /api/auth/login                     手机号密码登录
  GET  /api/auth/{provider}                取 OAuth URL（Mock）
  GET  /api/auth/callback/{provider}?code= OAuth 回调（Mock）

用户（需登录）
  GET  /api/user/profile                   当前用户资料
  PUT  /api/user/profile                   更新资料（null=不更新）
  GET  /api/user/stats                     游戏统计
  PUT  /api/user/password                  修改密码
  POST /api/upload/avatar                  头像上传（multipart）

游戏（需登录）
  GET  /api/games                          游戏列表（含 tags）
  GET  /api/games/{id}                     游戏详情（含平台/成就）
  PUT  /api/games/{gameId}/note            更新笔记
  PUT  /api/games/{gameId}/status          更新状态

数据中心（需登录）
  GET /api/dashboard/stats                 统计卡
  GET /api/dashboard/recent-games          近期排行（period/limit）
  GET /api/dashboard/play-trend            时长趋势（days）
  GET /api/dashboard/genre-distribution    类型分布
  GET /api/dashboard/heatmap               热力图（year）

年度报告（需登录）
  GET /api/report/yearly?year=             年度报告（实时聚合+upsert）
```

---

## 附录 C：当前数据库快照

> 采集时间：2026-11-13；方式：MySQL 8.0.44 只读查询；库：`game_stats`。

| 表 | 行数/内容 | 备注 |
|---|---|---|
| `users` | 1：`GameMasters`（id=1，level=42，platform=demo） | 演示账号 `13800138000 / 123456`；用户名已被改过 |
| `games` | 10 款 | 含艾尔登法环、赛博朋克 2077、荒野大镖客 2、哈迪斯、博德之门3、GTA5、霍格沃茨之遗、黑神话：悟空、只狼、巫师3 |
| `game_genres` | 11 | 魂系/开放世界/硬核/年度最佳/RPG/科幻/剧情/动作/西部/Roguelike/独立 |
| `game_genre_relations` | 30 | 多对多；导致类型分布重复计时长 |
| `achievements` | 40 | 每个成就定义绑定 `game_id` |
| `user_games` | 10 | 状态：在玩 4、已完成 6；时长合计 99,670 分钟 |
| `user_achievements` | 30 | 成就解锁总数 |
| `user_game_platforms` | 16 | Steam/PS5/Xbox/Switch 拥有情况 |
| `play_records` | 360 | 2025-11-19 ~ 2026-11-13，每天 1 条；2025 年 133h，2026 年 971h |
| `yearly_reports` | 3 | 2024（0h/休闲）、2025（133h/休闲）、2026（559h/资深，快照已过期） |
| `users` 列数 | 25 | 8 个新增列已由 `DatabaseMigrationRunner` 补齐 |

**关键派生值（供验收对照）**：

| 指标 | 值 |
|---|---|
| 游戏数量 | 10 |
| 总游玩时长 | 99,670 分钟 = 1,661 小时 = 69 天 5 小时 |
| 游玩天数 | 360 |
| 成就解锁 / 总数 | 30 / 40（75%） |
| 通关数 | 6 |
| 在玩数 | 4 |

---

*文档结束。本文件由 Leader 汇总，四个 Teammate 的产出已在对应章节署名；未修改任何业务代码。*