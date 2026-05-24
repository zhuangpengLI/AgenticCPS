# AgenticCPS：用 Vibe Coding 打造一人公司的智能返利赚钱机器

> 2万行代码、100% AI自主编程、淘宝/京东/拼多多/抖音四平台聚合——这不是概念，而是已经跑在生产环境里的现实。

---

## 一、什么是 AgenticCPS？

如果你曾经想过"做一个电商返利平台"，你大概率会被这些问题劝退：需要对接淘宝联盟 API、京东联盟 API、拼多多 API……每个平台的接口风格、鉴权方式、参数命名截然不同；订单同步、返利结算、提现审核更是一套复杂的业务逻辑链路；再加上前端管理后台、会员移动端……没有一支 5 人以上的技术团队，几乎无法独立完成。

**AgenticCPS** 的出现，就是为了打破这道门槛。

它是一套**开箱即用的智能 CPS 联盟返利平台**，聚合了淘宝、京东、拼多多、抖音等主流电商联盟，从商品搜索、比价、转链，到订单同步、返利计算、提现管理，实现全链路自动化。更关键的是，它深度融合了 **Vibe Coding（氛围编程）**、**低代码**与 **AI 自主编程**三大核心理念，让一个普通人也能运营一套完整的返利 SaaS。

---

## 二、核心功能概览

AgenticCPS 的 CPS 联盟返利模块是整个系统的核心，提供以下关键能力：

| 功能模块 | 描述 |
|---------|------|
| 多平台接入 | 淘宝/京东/拼多多/抖音联盟统一接入，一套代码管理所有平台 |
| 商品搜索与比价 | 关键词搜索、商品链接解析、跨平台实时比价 |
| 活动中心 | 管理后台配置活动卡片，支持 CPS/CPA、热门/最新、平台/场景导航，并可跳转商品广场 |
| 返利工具箱 | 集成万能转链、口令解析、商品广场、推广文案编辑、批量复制和批量转链 |
| 选品库 | 运营自定义主题、AI 推荐、第三方平台拉取、大促模板和商品快照沉淀 |
| 多供应商切换 | 大淘客、好单库等多家供应商 API 无缝切换，数据库一条记录热切换 |
| 推广链接生成 | 带返利追踪的转链（短链/长链/口令/移动端），会员自动归因 |
| 订单全链路追踪 | 从转链到结算，订单状态每 5 分钟增量同步 |
| 返利计算与结算 | 等级 + 平台 + 个人多维度返利配置，冻结 → 解冻 → 入账自动流转 |
| 提现管理 | 支付宝/微信提现，支持自动/人工审核 |
| MCP AI 接口 | 11 个 AI Tools，让 AI Agent 直接调用 CPS 能力 |

---

## 三、技术架构深度解析

### 3.1 双层适配器 + 工厂模式

AgenticCPS 对接多个电商联盟平台，每个平台的 API 差异巨大——淘宝用 `pid` 作为推广位标识，京东叫 `subUnionId`，拼多多叫 `customParameters`。如果把这些差异直接暴露给业务层，代码将充斥着 `if (platform.equals("taobao"))` 这类分支判断，极难维护。

系统采用**策略模式 + 工厂模式**的双层适配器架构彻底解决这个问题：

```
业务层
  │  调用统一接口 CpsPlatformClient
  ▼
CpsPlatformClientFactory（工厂）
  ├── clientMap: platformCode → CpsPlatformClient（平台适配器）
  │     ├── TaobaoPlatformClientAdapter（淘宝）
  │     ├── JdPlatformClientAdapter（京东）
  │     ├── PddPlatformClientAdapter（拼多多）
  │     └── DouyinPlatformClientAdapter（抖音）
  │
  └── vendorClientMap: vendorCode:platformCode → CpsApiVendorClient（供应商客户端）
        ├── dataoke:taobao → DtkTaobaoVendorClient（大淘客-淘宝）
        ├── dataoke:jd     → DtkJdVendorClient（大淘客-京东）
        ├── dataoke:pdd    → DtkPddVendorClient（大淘客-拼多多）
        ├── haodanku:taobao  → HdkTaobaoVendorClient（好单库-淘宝）
        └── haodanku:jd      → HdkJdVendorClient（好单库-京东）
```

**第一层（平台适配器）**：对业务层屏蔽"我在用哪家供应商"，只暴露 `searchGoods()`、`generatePromotionLink()` 等标准接口。

**第二层（供应商客户端）**：对平台适配器屏蔽"我在调哪家 API"，真正执行各家 API 的鉴权、参数构建、响应解析。

这套架构带来了一个极其优雅的能力：**供应商热切换**。`cps_platform` 表中有一个 `active_vendor_code` 字段，只需执行一条 SQL：

```sql
UPDATE cps_platform SET active_vendor_code = 'haodanku' WHERE platform_code = 'taobao';
```

淘宝平台的所有 API 调用立刻从大淘客切换到好单库，无需重启服务，无需修改一行代码。

### 3.2 各平台参数映射机制

统一 DTO `CpsPromotionLinkRequest` 承接业务层的参数，各供应商客户端负责翻译为各自平台的原生参数：

| 统一字段 | 淘宝（大淘客）| 京东（大淘客）| 拼多多（大淘客）|
|---------|------------|------------|--------------|
| `adzoneId` | `pid`（推广位） | `unionId` | `pid` |
| `externalId` | `externalId`（会员归因）| `subUnionId` | `customParameters` |
| `channelId` | `channelId` | `subUnionId` | — |
| `itemLink` | — | `materialId`（商品原始URL）| — |
| `goodsId` / `goodsSign` | `goodsId` | 拼入 materialId URL | `goodsSign` |

以京东为例，`subUnionId` 是追踪订单归因到具体会员的关键参数，系统将 `channelId`（即 memberId）映射到这个字段，确保每笔京东订单都能准确归属到对应会员，从而完成返利计算。

### 3.3 MCP AI 接口层

这是 AgenticCPS 区别于普通 CPS 系统最核心的差异——**MCP（Model Context Protocol）AI 接口层**。

基于 Spring AI 1.1.2，系统暴露了 11 个标准 MCP Tool Function，任何兼容 MCP 协议的 AI Agent 都可以直接调用：

| Tool 名称 | 功能描述 |
|----------|---------|
| `cps_search_goods` | 在各平台搜索商品，支持关键词、平台过滤、价格区间、分页 |
| `cps_compare_prices` | 跨平台比价，返回最低价/最高返利/综合最优三类推荐 |
| `cps_generate_link` | 生成带会员归因的推广转链，支持短链/长链/口令 |
| `cps_query_orders` | 查询会员返利订单状态 |
| `cps_get_rebate_summary` | 查询账户余额、待结算金额、累计返利及近期记录 |
| `cps_recommend_by_scene` | 面向 AIoT 或采购场景推荐 CPS 商品 |
| `cps_list_selection_themes` | 查询已发布选品库主题 |
| `cps_recommend_from_selection_theme` | 按主题返回商品快照、推荐理由、券后价和佣金，可选生成推广链接 |
| `cps_get_rebate_balance` | 查询可兑换 Token 的返利余额 |
| `cps_create_token_exchange` | 创建返利兑换 Token 订单 |
| `cps_query_exchange_status` | 查询返利兑换 Token 状态 |

**比价接口的实现尤为精妙**。`CpsComparePricesToolFunction` 调用 `goodsService.searchGoodsAllPlatforms()`，该方法遍历所有启用平台，异常时静默跳过（不影响其他平台），最终聚合结果并计算三类最优解：

- **cheapest**：券后实付价最低
- **highestRebate**：佣金比例最高（用户获得返利最多）
- **bestValue**：净价（实付 - 返利）最低，综合最划算

这意味着 AI 助手可以直接回答用户："iPhone 16 手机壳，京东券后 ¥19.9，但拼多多返利更高，你实际到手只花 ¥15.3，推荐拼多多。"

---

## 四、完整业务流程：从粘贴链接到收到返利

以用户分享一条淘宝商品链接为例，完整流程如下：

### 第一步：链接识别与平台路由

用户将淘宝商品链接（如 `https://item.taobao.com/item.htm?id=123456`）或淘宝口令粘贴至系统。系统解析 URL 特征，识别为淘宝平台（`platformCode = taobao`），提取商品 ID。

同时，系统读取 `cps_platform` 表中淘宝的 `active_vendor_code`，确定当前激活供应商（假设为 `dataoke`），路由至 `DtkTaobaoVendorClient`。

### 第二步：推广位选择与归因注入

系统按以下优先级确定推广位（PID）：

1. 调用方显式传入的 `adzoneId`
2. 当前会员的专属推广位（从 `cps_adzone` 表按 memberId + platformCode 查询）
3. 平台默认推广位（`cps_platform.default_adzone_id`）

同时将会员 ID 注入为 `externalId`，这是后续订单归因的"身份证"——淘宝联盟会将这个值原样回传在订单中，系统据此完成会员到订单的精准匹配。

### 第三步：调用大淘客 API 生成转链

`DtkTaobaoVendorClient` 调用大淘客 `/tb-service/get-privilege-link` 接口，传入：

```json
{
  "goodsId": "123456",
  "pid": "mm_xxx_yyy_zzz",
  "channelId": "淘宝渠道ID",
  "externalId": "memberId"
}
```

大淘客返回淘宝官方生成的推广链接（含佣金追踪），系统记录到 `cps_transfer_record` 表。

### 第四步：用户下单购买

用户点击转化后的推广链接，在淘宝完成购买。淘宝联盟记录这笔订单，关联至对应推广位，并将 `externalId` 原样回传。

### 第五步：订单同步（Quartz 定时任务）

系统每 5 分钟执行一次增量订单同步任务，调用大淘客的订单查询 API，将新订单写入 `cps_order` 表。订单状态流转如下：

```
已下单 → 已付款 → 已收货（确认收货） → 已结算（平台结算） → 已到账
                   ↓
             已退款 / 已失效
```

### 第六步：返利计算与冻结

订单状态进入"已收货"后，系统按以下优先级查找该会员、该平台的返利比例：

1. 会员个人配置（特定平台）
2. 会员个人配置（全平台）
3. 会员等级 + 特定平台
4. 会员等级（全平台）
5. 平台默认返利率
6. 全局兜底

计算出返利金额后，写入 `cps_rebate_record` 表，状态为"冻结中"，按 `cps_freeze_config` 配置的冻结天数（通常 15 天）等待解冻。

### 第七步：解冻入账与提现

冻结期满后，Quartz 定时任务扫描到期返利记录，将金额转入会员的 `cps_rebate_account` 余额。会员可随时发起提现申请，提现至支付宝或微信，系统根据配置自动审核或触发人工审核流程。

---

## 五、多供应商架构：大淘客 vs 好单库

系统支持同时配置多家供应商，并可按平台分别激活不同供应商，大幅降低单点故障风险。

**大淘客（dataoke）**：国内知名 CPS 聚合供应商，支持淘宝/京东/拼多多三平台，API 文档完善，是系统的默认供应商。

**好单库（haodanku）**：专注返利导购的供应商，支持淘宝/京东/拼多多平台，在某些品类的商品库存量和佣金率上优于大淘客。

两者的切换完全透明。假设某天大淘客 API 不稳定，只需：

```sql
UPDATE cps_platform SET active_vendor_code = 'haodanku' WHERE platform_code = 'taobao';
```

淘宝平台立即切换到好单库，**无需重启、无需改代码、业务零中断**。

---

## 六、Vibe Coding：AI 自主编程的落地实践

AgenticCPS 最令开发者着迷的，是它本身就是 Vibe Coding 的产物与实践平台。

> **Vibe Coding（氛围编程）**：你不写代码，你描述意图（Vibe），AI 把它变成可运行的软件。

CPS 模块的 **20,000+ 行生产代码 100% 由 AI 自主编程完成**，涵盖：

- CPS 核心数据库表的设计与 DDL（含活动中心配置表、选品主题、商品快照表和 CPS 菜单权限脚本）
- 管理后台 REST API Controller（含活动中心、返利工具箱、选品库、商品广场、平台/订单等）
- 会员端 REST API Controller
- 多个核心业务 Service（商品、活动、转链、订单、返利、提现、统计、风控等）
- 6 个 Quartz 定时任务（订单同步、状态更新、返利结算、账单生成等）
- 11 个 MCP Tool Function
- 完整的单元测试套件

AgenticCPS 引入了**规范化 AI 编程工作流**，通过 `.qoder/specs`（编码规范）、`.qoder/plans`（实施计划）、`.qoder/agents`（AI 代理角色）约束 AI 的行为，告别"AI 随便写"的粗放模式：

```
需求对齐 → 方案设计 → AI 自主编码 → 验收交付
  你参与     你确认      AI 自动完成    你审核
```

传统开发一个新平台适配器需要 2 周，Vibe Coding 下：

```
你说：「帮我接入唯品会联盟」

AI 自动完成：
  ✅ 分析 API 文档
  ✅ 生成平台适配器代码（实现 CpsPlatformClient 接口）
  ✅ 生成供应商客户端
  ✅ 注册到 CpsPlatformClientFactory
  ✅ 编写单元测试
  ✅ 更新 API 文档

用时：30 分钟。
```

---

## 七、实际应用场景

### 场景 1：一人公司 CPS 创业者

一个人运营返利公众号，手动记录订单、计算返利的工作量极大。AgenticCPS 接管后，订单自动同步、返利自动计算、用户自助提现——运营者每天多出 4 小时专注推广，而不是繁琐的数据录入。

### 场景 2：AI 购物导购助手

接入 AgenticCPS 的 MCP 接口后，开发者可以让任何 LLM 直接调用 CPS 工具：

> 用户："帮我找一个 50 元以内的无线鼠标，哪个平台最划算？"
>
> AI 自动：调用 `cps_compare_prices` → 比较淘宝/京东/拼多多价格 → 计算返利 → 生成推广链接 → 回复用户

原本需要 2 个月开发的 AI 导购助手，1 天即可完成。

### 场景 3：独立开发者 SaaS 变现

基于 AgenticCPS 的多租户能力，开发者可快速搭建面向多个客户的返利 SaaS 平台，每个客户独立配置自己的推广位和供应商，统一部署，按月收费。

---

## 八、技术栈一览

| 层次 | 技术选型 |
|-----|---------|
| 后端框架 | Spring Boot 3.5.9 + Spring Security 6.5.2 |
| AI 集成 | Spring AI 1.1.2（MCP Streamable HTTP，JSON-RPC 2.0）|
| ORM | MyBatis Plus 3.5.15 |
| 缓存 | Redis 7.0 + Redisson 3.35.0 |
| 数据库 | MySQL 8.0+（兼容 Oracle/PG/达梦/人大金仓等）|
| 任务调度 | Quartz 2.5.0 |
| 工作流 | Flowable 7.2.0 |
| 前端 | Vue 3.5.12 + Element Plus 2.11.1 + TypeScript |
| 移动端 | UniApp（管理端 + 会员端，多平台适配）|
| APM | SkyWalking 9.5.0 |
| 语言 | Java 17/21 |

---

## 九、结语

AgenticCPS 不只是一个返利系统，它是**"一人公司 × Vibe Coding × AI 自主编程"三者结合的最佳实践样本**。

它证明了一件事：在 2025 年，一个普通开发者借助 AI，完全可以独立交付一套原本需要 5 人团队、耗时半年才能完成的复杂业务系统。架构不会因为"只有一个人"而妥协——双层适配器、工厂模式、策略模式、MCP 协议，该有的都有，而且全部由 AI 完成。

如果你想做 CPS 返利副业、想构建 AI 导购产品、或者只是好奇"Vibe Coding 到底能做到什么程度"——AgenticCPS 是最好的答案之一。

---

## 项目信息

### 仓库地址

| 平台 | 地址 |
|------|------|
| GitHub | [https://github.com/zhuangpengLI/AgenticCPS](https://github.com/zhuangpengLI/AgenticCPS) |
| Gitee | [https://gitee.com/zhuangpengli/AgenticCPS](https://gitee.com/zhuangpengli/AgenticCPS) |
| Gitcode | [https://gitcode.com/lizhuangpeng/AgenticCPS](https://gitcode.com/lizhuangpeng/AgenticCPS) |

### 项目简介

**AgenticCPS** 是一套面向一人公司和独立开发者的智能 CPS 联盟返利平台。

**项目定位**：零代码启动、对话式开发、全自动运营的 CPS 联盟返利与导购平台。让一个人拥有一支技术团队的战斗力。

**核心技术栈**：

- 后端：Spring Boot 3.5.9 / Java 17 / MyBatis Plus / Redis / MySQL
- 前端：Vue 3 + Element Plus / UniApp（移动端多平台）
- AI：Spring AI 1.1.2 + MCP Protocol（Streamable HTTP，JSON-RPC 2.0）
- 调度：Quartz 2.5.0 / 工作流：Flowable 7.2.0

**主要功能**：

- 淘宝/京东/拼多多/抖音四平台联盟 CPS 聚合接入
- 管理后台活动中心，支持跨平台活动卡片、CPS/CPA 类型筛选、热门/最新和商品广场联动
- 管理后台返利工具箱，支持万能转链、口令解析、商品广场嵌入、推广文案编辑和批量转链
- 管理后台选品库，支持主题规则、AI 推荐、第三方拉取、大促模板、商品快照和 MCP 查询
- 多供应商架构（大淘客、好单库）+ 热切换，无需重启
- 11 个 MCP AI Tool，支持 AI Agent 零代码接入 CPS 能力
- 跨平台实时比价（最低价/最高返利/综合最优）
- 会员返利体系（多维度返利配置 + 冻结/解冻/入账自动流转）
- Quartz 定时任务全自动订单同步与状态更新
- CPS 核心模块 20,000+ 行代码 100% 由 AI 自主编程生成

**开源协议**：AGPL-3.0
