<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.9-blue.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.x-brightgreen.svg" alt="Vue">
  <img src="https://img.shields.io/badge/Java-17%2F21-orange.svg" alt="Java">
  <img src="https://img.shields.io/badge/MCP-Model%20Context%20Protocol-blueviolet.svg" alt="MCP">
  <img src="https://img.shields.io/badge/Vibe%20Coding-AI%20First-ff69b4.svg" alt="Vibe Coding">
  <img src="https://img.shields.io/badge/License-AGPL--3.0-green.svg" alt="License">
</p>

<h1 align="center">AgenticCPS — 一人公司的智能返利赚钱机器</h1>

<p align="center">
  <b>Vibe Coding + 低代码 + AI 自主编程</b> —— 让一个人拥有一支技术团队的战斗力<br/>
  零代码启动、对话式开发、全自动运营的 CPS 联盟返利与导购平台
</p>

> gitee: [AgenticCPS](https://gitee.com/zhuangpengli/AgenticCPS.git)

> gitcode: [AgenticCPS](https://gitcode.com/lizhuangpeng/AgenticCPS)

> github: [AgenticCPS](https://github.com/zhuangpengLI/AgenticCPS)

<p align="center">
  <a href="#-30-秒了解-agenticcps">30 秒了解</a> &bull;
  <a href="#-为什么选择-agenticcps">为什么选择</a> &bull;
  <a href="#-vibe-coding--ai-自主编程">Vibe Coding</a> &bull;
  <a href="#-核心功能">核心功能</a> &bull;
  <a href="#-技术栈">技术栈</a> &bull;
  <a href="#-快速开始">快速开始</a>
</p>

---

## 30 秒了解 AgenticCPS

**你是否属于以下人群？**

- 想做电商返利副业，但不会写代码
- 一人公司（OPC）创业者，团队只有你自己
- 自由职业者 / 数字游民，想打造被动收入管道
- 个人开发者，想快速搭建一套完整的返利 SaaS
- 小型工作室，3 人团队想干 30 人的活

**AgenticCPS 就是为你打造的。** 它是一套**开箱即用**的智能 CPS 联盟返利平台，深度融合 **Vibe Coding**、**低代码** 与 **AI 自主编程**三大核心理念：

> **用自然语言告诉 AI 你想要什么，AI 自己写代码、自己测试、自己部署。**
> 
> 一个人 = 产品经理 + 架构师 + 全栈开发 + 测试工程师 + 运维工程师。

---

## Agentic 生态闭环定位

AgenticCPS 不再只是单独的 CPS 导购项目，而是 Agentic 生态中的“返利资产与商品推荐服务”。当前三个项目的基础能力与关系必须这样理解：

- `AgenticTokenHub` 已经具备多模型网关、Token 计费、会员套餐、积分互转和支付能力，是生态里的 AI 能力与 Token 结算底座。
- `AgenticCPS` 已经规划并落地活动中心、返利工具箱、选品库、榜单型选品、商品搜索、比价、批量转链、订单追踪、返利汇总和 MCP 工具，是生态里的 CPS 返利资产与商品推荐服务。
- `AgenticAIoT` 已经定位为设备接入、数据流转、规则引擎、AI 运维和多协议 IoT 平台，是生态里的企业设备数据与 AI 运维场景入口。

生态融合要补齐的不是把三套系统揉成一个大单体，而是三者之间的 **账户互通、资产互转、场景联动和 Agent 调用接口**。所有后续新功能都要先判断自己属于哪个项目的边界，再通过开放接口、MCP 工具或事件流水完成跨系统协作。

三者形成以下职责关系：

| 项目 | 生态定位 | 核心职责 | 边界约束 |
|------|----------|----------|----------|
| `AgenticCPS` | CPS 返利与商品推荐服务 | 活动中心运营配置、返利工具箱、选品库主题与商品快照、爆款榜/主播榜/9.9 包邮/博主橱窗等榜单货架、商品搜索、比价、批量转链、口令解析、订单追踪、返利结算、返利冻结/扣减、AIoT 场景商品推荐、CPS MCP Tools | 不负责大模型网关、Token 余额主账本、设备接入与设备规则引擎 |
| `AgenticTokenHub` | AI Token 与模型调用结算底座 | 多模型网关、Token 钱包、Token 计费、会员套餐、外部返利兑换 Token、API Key 额度、AI 调用成本统计、Token MCP Tools | 不负责 CPS 订单、返利结算、商品推荐和设备业务 |
| `AgenticAIoT` | 企业级设备数据与 AI 运维场景入口 | 设备接入、指标采集、告警、规则引擎、AI 分析任务、采购需求生成、触发 CPS 推荐、AIoT MCP Tools | 不直接沉淀 Token 钱包，不直接实现 CPS 返利和电商平台适配 |

三套系统融合的长期方向是：

```text
统一用户、统一账户、统一权益、统一 API 鉴权、统一 MCP 工具、统一事件流水
```

最小商业闭环已经按 P0 方向落地：

```text
AgenticCPS 可用返利
        ↓
冻结 / 扣减 / 幂等 / 对账
        ↓
AgenticTokenHub 发放 AI Token
        ↓
AI Token 可被 AgenticAIoT、CPS AI 导购和其他 Agent 场景消耗
```

### P0：CPS 返利兑换 AI Token

P0 的目标是先让“返利”变成生态内可消耗的 AI Token 燃料：

- AgenticCPS 提供返利余额、冻结、解冻、确认扣减和本地兑换订单。
- AgenticTokenHub 提供兑换预估、兑换提交、Token 入账和兑换订单查询。
- 两边统一使用 `X-App-Id`、`X-Tenant-Id`、`X-Timestamp`、`X-Nonce`、`X-Signature`、`X-Idempotency-Key` 做服务间鉴权与幂等。
- 只有 `AVAILABLE` 可用返利允许兑换；待结算、退款、失效、冻结中返利不得兑换。
- 兑换链路必须遵循：创建本地兑换单 → 冻结返利 → 调用 aitoken 发放 Token → 成功确认扣减 → 失败解冻 → 超时进入补偿状态。

### 后续路线图

| 阶段 | 目标 | 交付效果 |
|------|------|----------|
| P0 | CPS 返利兑换 aitoken Token | CPS 返利成为 AI Token 燃料 |
| P1 | 强化活动中心、返利工具箱、选品库、榜单货架、CPS MCP 工具和 AI 导购能力 | 运营可配置跨平台活动卡片、批量转链工作台、主题选品库、爆款榜、主播销量榜、9.9 包邮专区和博主橱窗热推，AI 可稳定搜索、比价、转链、查返利、按主题或榜单推荐商品 |
| P2 | AgenticAIoT 统一消耗 aitoken Token | 设备告警、巡检、能耗分析统一计费 |
| P3 | AIoT 数据驱动 CPS 商品推荐 | 设备问题 → AI 分析 → 商品推荐 → CPS 返利 |
| P4 | 生态中台化 | 统一用户、账户、鉴权、事件、MCP Gateway、数据看板 |

详细协议与 P0 落地说明见：[docs/agentic-ecosystem-p0-rebate-token-exchange.md](./docs/agentic-ecosystem-p0-rebate-token-exchange.md)

---

## 为什么选择 AgenticCPS？

### 传统模式 vs AgenticCPS

| 维度 | 传统 CPS 系统开发 | AgenticCPS |
|------|------------------|------------|
| **团队规模** | 5~10 人技术团队 | **1 人即可** |
| **开发周期** | 3~6 个月 | **开箱即用，AI 扩展按天计** |
| **技术门槛** | 需要全栈工程师 | **自然语言描述需求，AI 自动实现** |
| **平台对接** | 每个平台单独开发 | **淘宝/京东/拼多多/抖音已内置** |
| **日常运维** | 专职运维团队 | **定时任务自动运行，异常自动告警** |
| **功能迭代** | 排期 → 开发 → 测试 → 上线 | **Vibe Coding：说一句话就上线** |
| **成本投入** | 人力 30~100 万/年 | **服务器 + 域名，年成本千元级** |

### 一人公司（OPC）最佳实践

```
你说：「帮我接入抖音联盟平台」

AI 自动完成：
  ✅ 分析抖音联盟 API 文档
  ✅ 生成抖音平台适配器代码
  ✅ 创建数据库配置表
  ✅ 注册 MCP Tool 供 AI Agent 调用
  ✅ 编写单元测试并验证通过
  ✅ 生成 API 接口文档

用时：30 分钟。传统开发：2 周。
```

---

## Vibe Coding + AI 自主编程

### 什么是 Vibe Coding？

**Vibe Coding（氛围编程）** 是一种全新的软件开发范式：

> **你不写代码，你描述 Vibe（氛围/意图/感觉），AI 把它变成可运行的软件。**

不同于传统的「写代码 → 编译 → 调试」循环，Vibe Coding 是：

```
描述意图 → AI 理解 → AI 编码 → AI 测试 → AI 交付
   你             你审核                        你验收
```

在 AgenticCPS 中，这不是概念，而是**已经落地的工作方式**——项目的 CPS 核心模块（20,000+ 行代码）**100% 由 AI 自主编程完成**，从数据库设计到 API 接口，从业务逻辑到单元测试，从定时任务到 MCP AI 接口层，全部由 AI 自主编写。

### Qoder AI 编码助手

平台集成 **Qoder AI 编码助手**，作为你的全栈 AI 程序员：

| 你说什么 | AI 做什么 |
|---------|----------|
| 「加一个商品收藏功能」 | 自动生成 Controller → Service → Mapper → 数据库表 → 前端页面 |
| 「接入唯品会联盟」 | 分析 API → 生成适配器 → 注册平台 → 编写测试 → 更新文档 |
| 「返利规则加一个阶梯奖励」 | 设计方案 → 修改配置表 → 更新计算引擎 → 回归测试 |
| 「给我看看昨天的运营数据」 | 调用 MCP Tool → 查询统计表 → 格式化输出运营报告 |
| 「把搜索性能优化一下」 | 分析慢查询 → 添加缓存策略 → 优化索引 → 压测验证 |

### 基于 Specs / Plans 的规范化 AI 编程

不同于「让 AI 随便写」的粗放模式，AgenticCPS 引入了 **规范化 AI 编程工作流**：

```
.qoder/
├── specs/      # 编码规范：技术标准、架构约束、代码风格
├── plans/      # 实施计划：任务分解、验收标准、交付清单
├── agents/     # AI 代理：角色定义、职责边界、协作流程
└── skills/     # 可复用技能：代码模板、最佳实践、经验沉淀
```

**工作流程**：

```
 需求对齐           方案设计          自主编码           验收交付
┌─────────┐    ┌─────────┐    ┌──────────┐    ┌─────────┐
│ 读取 Specs │ → │ 设计方案  │ → │ AI 自主编码 │ → │ 自动测试  │
│ 解析 Plans │    │ 生成计划  │    │ 生成测试代码 │    │ 验收报告  │
│ 用户确认   │    │ 用户确认  │    │ 规范遵循   │    │ 文档输出  │
└─────────┘    └─────────┘    └──────────┘    └─────────┘
      你参与             你参与          AI 自动完成          你验收
```

| 核心优势 | 说明 |
|---------|------|
| **需求精准对齐** | Specs/Plans 确保 AI 理解无偏差，告别「AI 乱写代码」 |
| **方案先行** | 先设计 → 再确认 → 后编码，零返工 |
| **纯 AI 自主编程** | 需求到代码全流程 AI 化，效率提升 10 倍以上 |
| **质量可保障** | 自动测试 + 规范约束 + 验收标准，代码质量可控 |
| **持续自进化** | 每次项目反馈自动优化 Specs/Plans，越用越聪明 |

---

## Codex Agentic 开发方式

AgenticCPS 的新默认开发方式是 **Codex + Superpowers + oh-my-codex + TDD + E2E 证据闭环**。目标不是让 AI 更快“堆代码”，而是让每一次改动都有需求来源、计划、失败测试、实现、验证证据和文档回写。

```text
需求 / Issue
  -> Codex 读取 AGENTS.md、README.md、docs/project-map.md
  -> Superpowers brainstorming / writing-plans 对齐设计与计划
  -> TDD 写出最小失败测试
  -> Codex / OMX / 子代理实现独立任务
  -> Playwright / Midscene.js / Maven / TypeScript 验证
  -> README、AGENTS、项目地图和测试规范同步更新
```

### 开发者快速开始：Codex 工作流

后端 CPS 改动优先运行：

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsRebateTokenExchangeServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

管理后台前端改动优先运行：

```bash
cd frontend/admin-vue3
pnpm ts:check
pnpm e2e
```

Midscene.js 只作为视觉/语义辅助 E2E。运行前在本机配置模型环境变量，不要提交密钥：

```bash
cd frontend/admin-vue3
set MIDSCENE_MODEL_BASE_URL=https://your-model-endpoint/v1
set MIDSCENE_MODEL_API_KEY=your-api-key
set MIDSCENE_MODEL_NAME=your-vl-model
set MIDSCENE_MODEL_FAMILY=qwen2.5-vl
pnpm e2e:midscene
```

### CPS 平台配置中心

新平台接入和已有平台重配统一从管理后台菜单 **联盟配置 → 平台配置中心** 进入，前端路由为 `/cps-config/platform-onboarding`，后端接口根路径为 `/admin-api/cps/platform-onboarding`。平台列表仍提供配置、检测、启停、删除以及供应商、推广位和返利规则的增删改入口。

工作台按“平台信息 → API 供应商 → 推广位 → 返利配置 → 检测与启用”五步组织配置。保存只写当前租户的加密草稿；检测针对草稿版本和配置指纹执行，失败时可以保留为待完善/禁用状态。已有平台重配期间运行流量继续读取旧配置，只有检测通过且版本、指纹一致的草稿才能在一个事务中切换 `cps_platform`、`cps_api_vendor`、`cps_adzone` 和 `cps_rebate_config`，事务提交后才清理缓存。

四张运行表仍是搜索、转链、订单同步和返利结算的运行时事实来源；`cps_platform_onboarding_draft` 只保存待发布编辑态，供应商凭证等敏感字段使用项目统一加密处理，响应和日志均不返回明文。旧的 API 供应商、平台、推广位、返利菜单已隐藏，但其后端 CRUD 接口和权限暂时保留，便于兼容与回滚。备用供应商目前只参与配置、优先级和检测，**尚未实现运行时自动故障切换**。

平台配置中心的目标验证命令：

```bash
# 后端 CPS 配置中心目标测试
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsPlatformOnboardingDraftMapperTest,CpsPlatformOnboardingFingerprintTest,CpsPlatformOnboardingDraftServiceImplTest,CpsPlatformOnboardingValidatorTest,CpsPlatformOnboardingConnectionTesterTest,CpsPlatformOnboardingPublishDbTest,CpsPlatformOnboardingCacheInvalidatorTest,CpsPlatformOnboardingLifecycleServiceTest,CpsPlatformOnboardingControllerTest,CpsPlatformServiceImplTest,CpsApiVendorServiceImplTest,CpsAdzoneServiceImplTest,CpsRebateConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"

# 前端定向检查与确定性 E2E
cd ../frontend/admin-vue3
pnpm exec eslint src/api/cps/platformOnboarding.ts src/views/cps/platformOnboarding src/views/cps/components/adzoneRules.ts
pnpm exec playwright test e2e/cps-platform-onboarding.spec.ts

# UI 契约、UTF-8 与差异检查
cd ../..
python -m pytest script/test/test_admin_cps_platform_onboarding_ui_contract.py -q
python script/check_utf8_integrity.py README.md docs/project-map.md backend/sql/module/cps-all-in-one.sql backend/sql/module/cps-update.sql
git diff --check
```

完整仓库的 `pnpm ts:check` 或真实供应商凭证冒烟测试若受既有类型错误、外部账号或网络环境影响，应单独记录，不得把这类环境结果当作平台配置中心已实现自动故障切换的证据。

### 质量门禁

| 场景 | 必跑验证 | 证据要求 |
|------|----------|----------|
| CPS 资金、订单、返利、提现、OpenAPI | 先写后端失败测试，再跑对应 Maven 测试 | 失败/通过输出、涉及幂等和租户边界的断言 |
| MCP Tool 或 AI Agent 接口 | 单元测试 + 参数/权限/审计日志断言 | 工具名、参数摘要、耗时、错误原因可追踪 |
| admin-vue3 页面或 issue 修复 | `pnpm ts:check` + Playwright 复现/回归 | `test-results`、trace、截图或 HTML report |
| 视觉语义检查 | `pnpm e2e:midscene` | Midscene report 作为辅助证据，不能替代确定性断言 |
| 文档/工作流改动 | UTF-8 解码检查 + 命令路径核对 | 文档命令与 `package.json` / Maven 模块一致 |

更多细节见：[docs/codex-agentic-development-workflow.md](./docs/codex-agentic-development-workflow.md) 与 [docs/e2e-agent-issue-workflow.md](./docs/e2e-agent-issue-workflow.md)。

---

## 低代码：不只是少写代码，而是不写代码

AgenticCPS 的低代码能力体现在系统的每个层面：

### 1. 代码生成器 —— 一键生成 CRUD

输入一张数据库表，**一键生成**完整的前后端代码：

```
输入：数据库表结构
输出：
  ✅ Java Controller / Service / Mapper / DO / VO
  ✅ Vue3 前端页面（列表 + 表单 + 详情）
  ✅ SQL 建表脚本
  ✅ Swagger API 文档
  ✅ 单元测试代码
```

支持**单表、树表、主子表**三种模式，覆盖 80% 的管理后台开发场景。

### 2. 可视化工作流 —— 拖拽设计业务流程

基于 Flowable 工作流引擎，在线拖拽设计审批流程：

- 提现审核流程
- 返利结算审批
- 平台接入流程
- 任何自定义业务流程

### 3. 报表 & 大屏 —— 拖拽生成数据可视化

| 能力 | 说明 |
|------|------|
| 数据报表设计器 | 拖拽字段生成数据报表，支持导出 Excel、PDF |
| 图形报表设计器 | 柱状图、折线图、饼图等数十种图表组件 |
| 大屏设计器 | 全屏数据大屏，内置几十种可视化组件 |
| 打印设计器 | 拖拽设计打印模板，支持条形码、二维码 |

### 4. MCP 协议 —— AI Agent 零代码接入

通过 MCP（Model Context Protocol）协议，任何 AI Agent **无需写一行代码**即可接入 CPS 系统：

```json
// AI Agent 直接调用，无需任何开发
{
  "method": "tools/call",
  "params": {
    "name": "cps_search_goods",
    "arguments": { "keyword": "iPhone 16 手机壳", "priceMax": 50 }
  }
}
```

**11 个 AI Tools 开箱即用**：

| Tool | 功能 | 一句话说明 |
|------|------|-----------|
| `cps_search_goods` | 商品搜索 | 帮用户在淘宝/京东/拼多多/抖音搜商品，支持平台筛选与价格区间 |
| `cps_compare_prices` | 多平台比价 | 自动比较各平台价格，输出最便宜/返利最高/综合最优方案 |
| `cps_generate_link` | 推广链接生成 | 生成带返利追踪的购买链接（短链/长链/口令/移动端） |
| `cps_query_orders` | 订单查询 | 查看用户的返利订单列表与全链路状态 |
| `cps_get_rebate_summary` | 返利汇总 | 查看余额、待结算、累计返利与最近记录 |
| `cps_recommend_by_scene` | 场景推荐 | 面向 AIoT 场景按采购需求推荐 CPS 商品 |
| `cps_list_selection_themes` | 选品库主题查询 | 查询已发布主题库，供 AI Agent 选择活动主题 |
| `cps_recommend_from_selection_theme` | 主题选品推荐 | 按主题返回商品快照、推荐理由、券后价、佣金，可选生成推广链接 |
| `cps_get_rebate_balance` | 可兑换返利查询 | 查询会员可兑换 Token 的返利余额 |
| `cps_create_token_exchange` | 返利兑换 Token | 创建 CPS 返利兑换 aitoken Token 订单 |
| `cps_query_exchange_status` | 兑换状态查询 | 查询返利兑换 Token 订单状态 |

---

## 核心功能

### CPS 联盟返利系统

一站式聚合淘宝、京东、拼多多等主流电商平台，实现从搜索到返利提现的完整闭环：

| 功能模块 | 描述 | 一人公司价值 |
|---------|------|------------|
| 多平台 CPS 接入 | 淘宝/京东/拼多多/抖音联盟统一接入 | 一套系统管所有平台 |
| 商品搜索与比价 | 关键词搜索、链接解析、跨平台比价 | 帮用户找到最省钱的方案 |
| 活动中心 | 管理后台配置跨平台活动卡片，支持 CPS/CPA、热门/最新、搜索和跳转商品广场 | 运营能快速搭建外卖、本地生活、票券等活动入口 |
| 返利工具箱 | 集成万能转链、口令解析、归属检测、优惠券查询、商品广场、淘礼金计划、推广文案编辑和批量复制 | 运营在一个工作台完成选品、解析、转链、查券、补贴玩法配置和社群分发 |
| 选品库 | 运营自定义主题、AI 推荐、第三方拉取、大促模板、商品快照沉淀 | 把一次性选品沉淀成可复用的主题货架 |
| 榜单型选品 | 基于主题规则、商品快照、销量、佣金、优惠券和活动标签组织爆款商品推荐、主播销量排行榜、9.9 包邮专区、博主橱窗热门推荐等榜单/专区 | 把运营选品转成可复用、可分发、可被 AI 调用的榜单货架 |
| 会员返利体系 | 等级 + 平台 + 个人多维度返利配置 | 灵活设定利润空间 |
| 订单全链路追踪 | 查询 → 转链 → 下单 → 结算 → 入账 | 每一分钱都追踪到位 |
| 提现管理 | 支付宝/微信提现，自动/人工审核 | 自动化资金流转 |
| MCP AI 接口 | 11 个 AI Tools，AI Agent 直接调用 | 接入 ChatGPT、Claude 等 AI 助手 |
| 运营数据看板 | 订单/佣金/返利/利润实时统计 | 一个人掌控全局 |
| 风控管理 | 异常行为检测、黑名单、退款率预警 | 自动守护资金安全 |

大淘客淘宝高效转链的订单归因规则已整理到 [`docs/dataoke-high-efficiency-link-attribution.md`](docs/dataoke-high-efficiency-link-attribution.md)。后续优化转链、订单同步、MCP 链接生成和推广位管理时，必须区分 `externalId`、`relationId/channelId`、`specialId` 与专属 PID：只有带可信上下文或已验证映射的订单才能进入会员返利资产链路。

大淘客搜索页与商品广场实现方式已整理到 [`docs/dataoke-search-page-implementation.md`](docs/dataoke-search-page-implementation.md)。后续优化商品搜索页、热搜榜、搜索联想词、超级搜索/联盟搜索补量、商品广场筛选和搜索结果转链时，按“热搜首屏 → 输入联想 → 搜索召回 → 商品统一字段 → 明确转链”的链路实现，搜索结果不得直接进入返利结算或 Token 兑换链路。

### 技术架构总览

```
qiji-module-cps/
├── qiji-module-cps-api/           # API 定义层
│   ├── enums/                     # 枚举（平台编码、订单状态…）
│   └── api/                       # 远程服务接口
│
└── qiji-module-cps-biz/           # 业务实现层
    ├── controller/admin/          # 管理后台接口（含活动中心、返利工具箱、选品库、商品广场、平台/订单等）
    ├── controller/app/            # 会员端接口
    ├── service/                   # 业务服务（goods/toolbox/order/rebate/activity/selection/exchange 等）
    ├── client/                    # 平台适配器（策略模式，可插拔扩展）
    │   ├── taobao/                # 淘宝联盟
    │   ├── jingdong/              # 京东联盟
    │   ├── pinduoduo/             # 拼多多联盟
    │   └── douyin/                # 抖音联盟
    ├── dal/                       # 数据访问层（CPS 核心业务表）
    ├── job/                       # 定时任务（订单同步、状态更新）
    └── mcp/                       # MCP AI 接口层
```

### 全模块能力矩阵

| 模块 | 核心能力 | 低代码支持 |
|------|---------|-----------|
| 系统管理 | 用户、角色、菜单、部门、字典、日志 | 代码生成器 + 拖拽配置 |
| 会员中心 | 会员管理、等级体系、积分签到、标签分组 | 代码生成器 |
| 支付系统 | 支付宝/微信支付、退款、钱包、转账 | 已集成，开箱即用 |
| 工作流 | Flowable 流程引擎，在线设计审批流 | 可视化流程设计器 |
| 数据报表 | 报表设计器、大屏设计器 | 纯拖拽，零代码 |
| AI 大模型 | 聊天、图像生成、知识库、工作流 | MCP 协议对接 |
| 微信公众号 | 粉丝管理、消息推送、自动回复 | 可视化配置 |
| 商城系统 | 商品、促销、订单、售后 | 代码生成器 |
| 基础设施 | 定时任务、文件服务、消息队列、监控 | 在线管理界面 |

---

## 技术栈

### 项目模块

| 项目 | 说明 |
|------|------|
| `qiji-dependencies` | Maven 依赖版本管理 |
| `qiji-framework` | Java 框架扩展（安全、缓存、权限、多租户…） |
| `qiji-server` | 主服务端（所有模块的容器） |
| `qiji-module-system` | 系统管理模块 |
| `qiji-module-member` | 会员中心模块 |
| `qiji-module-infra` | 基础设施模块 |
| `qiji-module-pay` | 支付系统模块 |
| `qiji-module-mall` | 商城系统模块 |
| `qiji-module-ai` | AI 大模型模块 |
| `qiji-module-mp` | 微信公众号模块 |
| `qiji-module-report` | 报表与大屏模块 |
| `qiji-module-cps` | **CPS 联盟返利系统模块** |

### 核心框架

| 框架 | 说明 | 版本 |
|------|------|------|
| [Spring Boot](https://spring.io/projects/spring-boot) | 应用开发框架 | 3.5.9 |
| [Spring Security](https://spring.io/projects/spring-security) | 安全框架 | 6.5.2 |
| [Spring AI](https://spring.io/projects/spring-ai) | AI 集成框架（MCP 支持） | 1.1.2 |
| [MyBatis Plus](https://mp.baomidou.com/) | ORM 增强 | 3.5.15 |
| [Redis](https://redis.io/) / [Redisson](https://github.com/redisson/redisson) | 缓存 & 分布式锁 | 7.0 / 3.35.0 |
| [Flowable](https://www.flowable.com/) | 工作流引擎 | 7.2.0 |
| [Vue 3](https://vuejs.org/) + Element Plus | 管理后台前端 | 3.5.12 / 2.11.1 |
| [UniApp](https://uniapp.dcloud.net.cn/) | 移动端多端适配 | Latest |
| [MySQL](https://www.mysql.com/) | 数据库（支持 Oracle/PG/SQLServer/达梦/人大金仓/GaussDB/openGauss） | 5.7 / 8.0+ |
| [MapStruct](https://mapstruct.org/) | Bean 转换 | 1.6.3 |
| [Quartz](https://www.quartz-scheduler.org/) | 任务调度 | 2.5.0 |
| [SkyWalking](https://skywalking.apache.org/) | 链路追踪 & 日志中心 | 9.5.0 |

---

## 快速开始

### 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 17 或 21（推荐 21） |
| MySQL | 5.7 或 8.0+ |
| Redis | 5.0+ |
| Maven | 3.8+ |
| Node.js | 16+（admin-vue3）/ 20+（admin-uniapp） |
| pnpm | 8.6+（admin-vue3）/ 9+（admin-uniapp） |

### 三步启动

```bash
# 1. 克隆项目
git clone https://github.com/zhuangpengLI/AgenticCPS.git
cd AgenticCPS/backend

# 2. 初始化数据库
#    先导入 sql/mysql/ruoyi-vue-pro.sql（主库表结构）
#    新库再导入 sql/module/cps-all-in-one.sql（CPS 全量表、种子数据、菜单和权限）
#    现有库升级执行 sql/module/cps-update.sql（每段更新含修改时间记录）
#    配置 application-local.yaml 中的数据库连接信息

# 3. 启动后端
mvn clean compile
# 运行 YudaoServerApplication 主类（端口：48080）
```

### Docker 一键部署

```bash
cd backend/script/docker

# 源码机器：构建并归集 JAR、前端页面和初始化 SQL
bash build-package.sh

# 将整个 docker 目录复制到部署服务器后，一键启动
bash deploy.sh

# 查看状态、日志或停止服务
bash deploy.sh status
bash deploy.sh logs server
bash deploy.sh down
```

端口映射：后端 48080 → 48080，MySQL 3306 → 3306，Redis 6379 → 6379，前端 80 → 8080
完整说明见 `backend/script/docker/Docker-HOWTO.md`。

Docker 后端使用 `application-prod.yaml`，MySQL 应用账号和 Redis 密码均从 `docker.env` 统一传入容器与 Java 配置；首次生产部署前请修改其中所有 `ChangeMe_` 默认密码。

### 前端启动

```bash
# 管理后台（admin-vue3）
cd frontend/admin-vue3
pnpm install   # 需要 pnpm >= 8.6, Node.js >= 16
pnpm dev

# 管理后台（admin-uniapp）
cd frontend/admin-uniapp
pnpm install   # 需要 pnpm >= 9, Node.js >= 20
pnpm dev:h5    # H5 端

# 生产打包
pnpm build:prod
```

### 性能指标

| 指标 | 要求 |
|------|------|
| 单平台搜索 | < 2 秒（P99） |
| 多平台比价 | < 5 秒（P99） |
| 转链生成 | < 1 秒 |
| 订单同步延迟 | < 30 分钟 |
| 返利入账 | 平台结算后 24 小时内 |
| MCP Tool 调用 | < 3 秒（搜索类）/ < 1 秒（查询类） |

---

## 谁在使用？典型场景

### 场景 1：一人公司 CPS 创业

> **小张**，95 后自由职业者，一个人运营返利公众号。
> 
> 以前：用 Excel 手动记录订单、手动计算返利、手动转账给用户。
> 现在：AgenticCPS 自动同步订单、自动计算返利、用户自助提现。
> **每天多出 4 小时做推广，月收入翻 3 倍。**

### 场景 2：AI 导购助手

> **小李**，独立开发者，想做一个 AI 购物助手。
> 
> 以前：需要自己对接淘宝/京东/拼多多 API，写搜索、比价、转链逻辑。
> 现在：接入 AgenticCPS 的 MCP 接口，11 个 AI Tools 开箱即用，支持搜索、比价、转链、选品库推荐和返利兑换。
> **1 天完成原来 2 个月的工作量。**

### 场景 3：Vibe Coding 快速扩展

> **小王**，返利平台运营者，想接入唯品会联盟。
> 
> 以前：找外包开发，报价 3 万，工期 3 周。
> 现在：对 AI 说「帮我接入唯品会联盟」，30 分钟搞定。
> **开发成本从 3 万降到 0。**

---

## 项目进展

- ✅ Phase 1：基础框架搭建（已完成）
- ✅ Phase 2：核心功能开发（已完成）
- ✅ Phase 3：订单与结算（已完成）
- ✅ Phase 4：会员与提现（已完成）
- ✅ Phase 5：数据统计（已完成）
- ✅ Phase 6：MCP AI 接口（已完成）
- ✅ Phase 7：管理后台活动中心与商品广场联动（已完成）
- ✅ Phase 8：管理后台选品库、主题商品快照、爆款榜、主播销量榜、9.9 包邮专区和博主橱窗热推榜单货架（已完成）
- ✅ Phase 9：返利工具箱 P0/P1/P2（万能转链、口令解析、归属检测、优惠券查询、商品广场嵌入、淘礼金计划、推广文案编辑）（已完成）
- ✅ Phase 10：文档与优化（持续更新）

---

## 演示图

### 系统功能

| 模块 | 截图 | 截图 | 截图 |
|------|------|------|------|
| 登录 & 首页 | ![登录](/.image/登录.jpg) | ![首页](/.image/首页.jpg) | ![个人中心](/.image/个人中心.jpg) |
| 用户 & 应用 | ![用户管理](/.image/用户管理.jpg) | ![令牌管理](/.image/令牌管理.jpg) | ![应用管理](/.image/应用管理.jpg) |
| 租户 & 套餐 | ![租户管理](/.image/租户管理.jpg) | ![租户套餐](/.image/租户套餐.png) | - |
| 菜单 & 角色 | ![菜单管理](/.image/菜单管理.jpg) | ![角色管理](/.image/角色管理.jpg) | - |

### 基础设施

| 模块 | 截图 | 截图 | 截图 |
|------|------|------|------|
| 代码生成 | ![代码生成](/.image/代码生成.jpg) | ![生成效果](/.image/生成效果.jpg) | - |
| 定时任务 | ![定时任务](/.image/定时任务.jpg) | ![任务日志](/.image/任务日志.jpg) | - |
| 监控平台 | ![Java监控](/.image/Java监控.jpg) | ![链路追踪](/.image/链路追踪.jpg) | ![日志中心](/.image/日志中心.jpg) |

### 报表 & 大屏

| 模块 | 截图 | 截图 | 截图 |
|------|------|------|------|
| 报表设计器 | ![数据报表](/.image/报表设计器-数据报表.jpg) | ![图形报表](/.image/报表设计器-图形报表.jpg) | ![打印设计](/.image/报表设计器-打印设计.jpg) |
| 大屏设计器 | ![大屏列表](/.image/大屏设计器-列表.jpg) | ![大屏预览](/.image/大屏设计器-预览.jpg) | ![大屏编辑](/.image/大屏设计器-编辑.jpg) |

---

## 交流社区

欢迎加入 AgenticCPS 交流社区，与志同道合的开发者、创业者一起探索 Vibe Coding 与 CPS 赚钱的无限可能！

| 渠道 |            二维码            |
|:---------:|:-------------------------:|
| 加入知识星球，获取深度教程、源码解析、运营经验分享| ![知识星球](/.image/知识星球.jpg) |
| 添加群主，备注：进技术交流群 |   ![微信](/.image/微信.png)   |
| 扫码加入微信群，获取最新动态、技术答疑、部署支持 |  ![微信群](/.image/微信群.jpg)  |



> **微信群**：技术交流、Bug 反馈、功能建议，欢迎扫码加入。
>
> **知识星球**：付费精品社区，内含完整部署教程、Vibe Coding 实战案例、一人公司 CPS 创业经验分享，以及专属答疑服务。

---

## 开源协议

本项目采用 **GNU Affero General Public License v3.0 (AGPL-3.0)** 开源协议。

| 使用场景 | 是否允许 |
|---------|---------|
| 个人学习、研究 | ✅ 允许 |
| 内部企业使用 | ✅ 允许 |
| 商业二次开发（需开源） | ✅ 允许 |
| 对外提供 SaaS 服务 | ✅ 允许（需开源修改部分） |
| 闭源商业化分发 | ❌ 禁止 |

详见 [LICENSE](./LICENSE)

---

## 💝 赞助与支持

开源项目的发展离不开社区的支持。如果您觉得本项目对您有帮助，欢迎赞助支持持续开发！

### 为什么需要赞助？

| 用途 | 说明 |
|------|------|
| 🖥️ 服务器部署 | 测试环境、演示环境、CI/CD 服务器租赁费用 |
| 🤖 AI Token 费用 | 大模型 API 调用费用（通义千问、DeepSeek、OpenAI 等） |
| 🔧 持续开发 | 新平台对接开发、功能迭代、Bug 修复 |
| 📚 文档完善 | 技术文档、API 文档、视频教程制作 |

### 赞助方式
#### 微信支付 / 支付宝

<p align="center">
  <img src="./.image/微信收款.jpg" alt="微信支付">
  <img src="./.image/支付宝收款.jpg" alt="支付宝">
</p>

> 💡 请在备注中留下您的 GitHub ID 或联系方式，我们将列在赞助者名单中（如愿意公开）


### 企业赞助

欢迎企业用户进行商业赞助，我们将提供以下回报：

| 赞助等级 | 回报 |
|---------|------|
| 🥉 青铜赞助商 | README 中显示企业 Logo |
| 🥈 白银赞助商 | 优先 Issue 处理 + Logo 展示 |
| 🥇 黄金赞助商 | 专属技术支持 + 优先功能开发 + Logo 展示 |
| 💎 钻石赞助商 | 定制开发支持 + 专属技术顾问 + 首页显著展示 |

---

## 🎁 功能悬赏

为了加速项目功能开发，我们推出**功能悬赏计划**。您可以悬赏特定功能的开发，开发者完成后可获得赏金。

### 当前悬赏列表

| 功能 | 悬赏金额 | 状态 | 说明 |
|------|--------|------|------|
| 智能推送 | ¥1,000 | 🔴 待开发 | 基于用户行为的智能商品推送 |

### 悬赏规则

1. **认领任务**：在 Issue 中评论认领，确认后开始开发
2. **开发周期**：根据功能复杂度协商，一般 2-4 周
3. **代码审核**：提交 PR 后进行代码审核，通过后合并
4. **发放赏金**：合并后 3 个工作日内发放至指定账户

### 如何发起悬赏？

如果您需要特定功能但不在列表中，可以：

1. 在 [Issues](../../issues) 中创建功能请求，标注 `💰 悬赏` 标签
2. 说明功能需求和悬赏金额
3. 等待开发者认领或我们评估后添加到悬赏列表

---

### 🙏 感谢所有赞助者

感谢以下赞助者的慷慨支持（按时间排序）：

<!-- 赞助者名单将在此处更新 -->

> 成为第一个赞助者，让开源走得更远！

<p align="center">
  <b>AgenticCPS —— 让每一个有想法的人，都能拥有自己的返利帝国。</b><br/>
  <sub>一人公司 &bull; Vibe Coding &bull; AI 自主编程 &bull; 低代码 &bull; 开箱即用</sub>
</p>
