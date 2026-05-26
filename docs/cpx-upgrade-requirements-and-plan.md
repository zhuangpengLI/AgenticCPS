# AgenticCPS 升级为 CPX 项目需求与改造计划

> 生成日期：2026-05-26  
> 范围：将现有 CPS 返利与导购系统升级为 CPS 主导型 CPX 综合联盟平台，统一承载 CPS、CPA、CPL、CPM、CPC、oCPA、oCPC 等推广方式，并补充资讯发布、平台对接资料库和任务发布能力。  
> 约束：保持 AgenticCPS 在生态中的职责边界，不接管 aitoken-platform 的 Token 主账本，也不接管 AgenticAIoT 的设备数据与规则引擎。

> 当前进展：已落地 CPX 任务中心、资讯中心、平台对接中心和 CPX 看板的后端骨架与后台页面，并在 CPX OpenAPI 接口上补齐 HMAC 签名校验与幂等键透传；后台任务、资讯、平台档案已具备基础创建、查询、更新能力；后续继续补充结算、审核、统计明细和更多前端交互。

## 1. 背景与现状

当前项目已经具备 CPS 返利系统的主干能力：

- 商品搜索、比价、转链、订单同步、返利结算、提现、Token 兑换、MCP Tool。
- 活动中心已经有 `billing_type` 字段，但当前语义主要是 `CPS`、`CPA`、`CPS+CPA`。
- 活动中心、返利工具箱、选品库、MCP 查询、OpenAPI 返利冻结/扣减已经形成可复用基础。
- 资金链路仍以“订单 -> 佣金 -> 返利 -> 可用余额 -> Token 兑换”为核心，适合 CPS，不足以完整表达 CPA、CPL、CPM、CPC、oCPA、oCPC。

关键现状证据：

- `backend/sql/mysql/cps-all-in-one.sql` 中 `cps_rebate_activity.billing_type` 注释为 `CPS/CPA/CPS+CPA`。
- `CpsRebateActivityServiceImpl` 的计费筛选选项写死为 `CPS`、`CPA`、`CPS+CPA`。
- `CpsPlatformClient` 以商品搜索、推广链接、订单查询为核心接口，天然偏 CPS。
- `CpsOrderDO`、`CpsRebateRecordDO`、`CpsRebateSettleServiceImpl` 以订单、佣金、返利金额为核心，不适合直接承载点击、线索、动作类事件。

## 2. CPX 定义

CPX 是统一推广计费模型，不是把所有推广都塞进“订单返利”。本项目建议定义为：

| 推广方式 | 触发事件 | 结算依据 | 典型场景 | 是否进入会员返利 |
|---|---|---|---|---|
| CPS | sale 成交订单 | 订单金额、佣金、退款状态 | 电商商品、外卖、票券、到店核销 | 是，沿用现有返利账户 |
| CPA | action 有效动作 | 平台确认的注册、开卡、下载、首单、激活等动作 | 88VIP、App 拉新、会员开通 | 可配置，默认进入待结算奖励 |
| CPL | lead 有效线索 | 表单、手机号、预约、咨询等线索审核状态 | 本地生活、教育、家装、企业服务 | 可配置，默认进入待审核奖励 |
| CPM | impression 有效曝光 | 有效曝光千次、去重曝光、预算消耗 | 品牌曝光、内容分发、信息流素材测试 | 默认不进入会员余额，优先做曝光成本统计 |
| CPC | click 有效点击 | 有效点击、去重点击、预算消耗 | 活动导流、内容流量分发 | 默认不进入会员余额，先做运营收入/成本统计 |
| oCPA | optimized action 目标动作 | 目标转化事件、优化目标成本、平台确认状态 | App 激活、留资后深度转化、首购优化 | 可配置，按目标转化确认后进入冻结奖励 |
| oCPC | optimized click 有效点击与目标效果 | 有效点击、目标转化效果、优化成本指标 | 搜索广告、信息流广告、落地页转化优化 | 默认不进入会员余额，需任务显式开启奖励 |

统一语言：

- `promotionMethod`：推广方式，枚举 `CPS`、`CPA`、`CPL`、`CPM`、`CPC`、`OCPA`、`OCPC`、`MIXED`，允许组合展示但结算必须拆成单一事件类型。
- `campaign`：推广活动，替代“返利活动”的泛化概念。
- `offer`：可推广对象，可是商品、活动页、表单、App、会员权益、券包。
- `trackingLink`：带归因参数的推广链接。
- `event`：点击、曝光、线索提交、动作回传、订单回传等原始事件。
- `conversion`：可结算转化，必须有幂等键、租户、平台、活动、会员/推广位、状态、金额或计费单价。
- `settlement`：结算记录，负责将有效转化转为平台收入、会员奖励或返利流水。

## 3. 需求描述

### 3.1 运营活动中心升级

活动中心升级为 CPX 活动中心：

- 支持筛选 `全部`、`CPS`、`CPA`、`CPL`、`CPM`、`CPC`、`OCPA`、`OCPC`、`混合`。
- 活动卡片展示推广方式、奖励口径、结算周期、素材数量、有效期、平台、状态。
- `jump_type=search` 继续用于 CPS 商品搜索；CPA/CPL/CPM/CPC/oCPA/oCPC 可以跳转活动落地页、线索表单、外部 URL 或生成追踪链接。
- 后台创建活动时必须声明单一主推广方式；组合活动通过多个结算规则表达，不允许一个结算事件同时按多种方式入账。
- 兼容历史 `CPS+CPA` 展示，但内部建议迁移成两个规则：`CPS` 订单规则 + `CPA` 动作规则。

### 3.2 推广对象与素材

新增 CPX offer/material 概念：

- CPS offer 复用商品广场与选品库商品快照。
- CPA offer 表示注册、开卡、下载、激活等动作任务。
- CPL offer 表示线索表单或预约任务，需要记录线索字段 schema、隐私提示、审核规则。
- CPM offer 表示曝光任务，需要曝光位、有效曝光口径、千次单价、频控和预算。
- CPC offer 表示导流页面或内容素材，需要预算、单价、日限额、去重规则。
- oCPA/oCPC offer 表示带优化目标的动作或点击任务，需要目标事件、目标成本、优化周期和平台效果回传字段。
- 素材支持标题、图片、短链、淘口令/口令、落地页、推荐文案、适用场景。

### 3.3 追踪与归因

CPX 必须新增统一事件账本，而不是让 CPS 订单表承载所有事件：

- 生成追踪链接时写入 `tracking_id`、平台、活动、offer、推广位、会员、渠道、租户。
- 点击事件必须记录 IP、UA、referer、设备指纹摘要、时间、去重键。
- CPL 线索必须记录加密后的联系信息摘要、授权标记、来源、审核状态，敏感明文不进入通用日志。
- CPA/CPS 转化必须支持平台回调、定时拉单、手工导入三种来源。
- 所有回调必须使用 `X-App-Id`、`X-Tenant-Id`、`X-Timestamp`、`X-Nonce`、`X-Signature`、`X-Idempotency-Key` 或等价 HMAC 机制。
- 幂等键建议为 `tenantId + platformCode + promotionMethod + sourceEventId`。

### 3.4 结算与奖励

结算从“订单返利结算”扩展为“CPX 转化结算”：

- CPS：沿用现有订单状态机和返利账户，只有 `AVAILABLE` 可进入 Token 兑换。
- CPA：平台确认动作有效后，根据固定金额、阶梯奖励或活动配置生成奖励记录。
- CPL：线索提交后先进入待审核，审核通过后生成奖励；重复手机号/设备/会员/活动需要风控拦截。
- CPM：按有效曝光千次计费，必须先做曝光去重、频控和预算限制，默认只统计收入/成本。
- CPC：按有效点击计费，优先做平台收入/成本统计；是否给会员点击奖励必须单独配置预算和风控。
- oCPA/oCPC：记录优化目标、目标转化成本和平台确认状态；实际入账仍拆成明确的动作或点击结算规则。
- 所有奖励必须支持冻结、解冻、扣回、失败补偿、审计日志。
- 不允许客户端请求体传入 `memberId` 决定资产归属；必须来自登录上下文、推广位绑定或可信服务签名。

### 3.5 MCP / Agent 能力

现有 `cps_*` Tool 保持兼容，同时新增 CPX Tool：

- `cpx_list_tasks`：按平台、推广方式、场景列出可推广任务。
- `cpx_get_task_detail`：查询任务详情、结算口径、素材和风险提示。
- `cpx_generate_tracking_link`：为任务/offer 生成追踪链接，返回推广文案和可审计 trackingId。
- `cpx_query_conversions`：查询 CPS/CPA/CPL/CPM/CPC/oCPA/oCPC 转化与结算状态。
- `cpx_recommend_tasks_by_scene`：按 AIoT 场景、用户意图或内容场景推荐 CPX 任务。
- `cpx_search_articles`：搜索 CPX 资讯、平台对接指南和任务攻略。

安全要求：

- 所有 Tool 统一写入 MCP 访问日志，包含 toolName、参数摘要、member/context、耗时、状态、错误原因。
- Tool 响应使用统一 `code/message/data` 结构，内部异常脱敏。
- 生成追踪链接时优先使用 ToolContext 或 API Key 绑定身份，不信任请求体 memberId。

### 3.6 统计与运营看板

CPX 看板需要拆分漏斗和结算：

- 曝光数、有效曝光、点击数、有效点击、线索数、有效线索、动作数、订单数、成交金额、佣金、奖励、ROI。
- 按推广方式、平台、活动、offer、渠道、推广位、会员、日期聚合。
- CPM 需要有效曝光千次、去重率、频控命中、预算消耗。
- CPC/oCPC 需要预算消耗、日限额、异常点击占比和优化目标效果。
- CPL 需要审核通过率、重复率、联系方式脱敏质量。
- CPA/oCPA 需要动作回传成功率、目标转化成本和平台确认延迟。
- CPS 继续保留订单状态、退款、返利到账、Token 兑换数据。

## 4. 改造路线选择

### 方案 A：只扩展活动中心枚举

做法：在现有 `billing_type` 中加入 `CPL`、`CPM`、`CPC`、`OCPA`、`OCPC`，前端筛选和卡片展示同步扩展。

优点：最快，改动小。  
缺点：只能展示活动，不能解决归因、事件、结算、风控和统计。  
适用：需要快速对外展示“支持 CPX”的运营页面。

### 方案 B：新增 CPX 核心层，CPS 作为其中一种结算方式

做法：保持现有 CPS 能力兼容，新增 CPX 活动、offer、tracking、event、conversion、settlement 抽象；CPS 订单结算接入 CPX 事件视角但不立即重命名所有旧表。

优点：业务边界清晰，可渐进迁移，风险可控。  
缺点：需要新增表、服务、测试和后台页面。  
推荐：作为主路线。

### 方案 C：全量重命名 CPS 为 CPX

做法：包名、表名、菜单、API、文档全量改名。

优点：命名彻底统一。  
缺点：破坏面大，影响已有 MCP Tool、OpenAPI、菜单权限、SQL、文档和测试；容易引入兼容问题。  
结论：不建议作为第一阶段目标，只适合在 CPX 核心稳定后做兼容别名和长期迁移。

## 5. 推荐总体架构

采用“CPX 新核心 + CPS 兼容适配”的渐进式架构：

```text
Admin / App / MCP
  -> CPX Campaign / Offer / Tracking API
  -> CPX Event Ledger
  -> CPX Conversion Service
  -> CPX Settlement Service
       |-> CPS Order/Rebate Settlement Adapter
       |-> CPA Action Reward Adapter
       |-> CPL Lead Reward Adapter
       |-> CPM Impression Billing Adapter
       |-> CPC/oCPC Click Billing Adapter
       |-> oCPA Optimized Action Adapter
  -> Rebate Account / Settlement Ledger / Statistics
```

边界原则：

- 现有 `cps_order` 和 `cps_rebate_record` 暂不删除，继续承载 CPS 成交返利。
- 新增 CPX 事件和转化表，承载 CPA、CPL、CPM、CPC、oCPA、oCPC，也可以关联 CPS 订单。
- 资金/奖励最终入账前必须经过统一 settlement 记录，保留审计、幂等和扣回能力。
- 菜单可先改为“CPX 联盟”，内部路由和权限短期保留 `cps:*`，后续逐步补 `cpx:*` 别名。

## 6. 数据模型建议

第一阶段新增或演进以下模型：

| 模型 | 建议表 | 说明 |
|---|---|---|
| CPX 活动 | `cpx_campaign` 或演进 `cps_rebate_activity` | 活动主数据，含 promotionMethod、结算周期、预算、状态 |
| CPX 推广对象 | `cpx_offer` | 商品、活动页、表单、App 动作等统一 offer |
| CPX 素材 | `cpx_material` | 推广文案、图片、落地页、短链、口令 |
| 追踪链接 | `cpx_tracking_link` | trackingId、渠道、推广位、会员、活动、offer |
| 原始事件 | `cpx_event` | click、lead_submit、action_callback、order_callback 等 |
| 转化记录 | `cpx_conversion` | 可结算事件，带幂等键、状态、金额、审核结果 |
| 结算记录 | `cpx_settlement_record` | 收入、奖励、返利、扣回、冻结、解冻审计 |
| CPL 线索扩展 | `cpx_lead_detail` | 脱敏线索信息、授权、审核状态，不在通用日志暴露明文 |

金额字段建议优先采用整数分，避免继续扩大 BigDecimal 与“金额用分”规则漂移。对历史 CPS BigDecimal 字段先做兼容，不在 CPX 首轮强行迁移旧表。

## 7. 分阶段升级计划

### P0：定义边界与守住资金安全

目标：先把 CPX 概念、枚举、兼容边界和现有高风险资金问题说清楚。

- 明确定义 `promotionMethod` 枚举：`CPS`、`CPA`、`CPL`、`CPM`、`CPC`、`OCPA`、`OCPC`、`MIXED`。
- 文档标注 `billing_type` 的历史兼容语义，后续新代码使用 `promotionMethod`。
- 梳理现有 P0 技术债：memberId 信任、冻结解冻、返利扣回、返利幂等、账户并发。
- 新增 CPX 开发约束：CPA/CPL/CPM/CPC/oCPA/oCPC 不得直接写入 `cps_order`；CPM/CPC/oCPC 默认不得进入会员可提现余额。
- 输出数据库迁移草案和 API 兼容策略。

验收：

- 有 CPX 术语表、边界说明、数据流说明。
- 现有 CPS 搜索、转链、订单、返利、Token 兑换行为不变。
- P0/P1 资金与 MCP 风险进入改造排期。

### P1：活动中心从 CPS 展示升级为 CPX 展示

目标：让后台可以运营 CPS/CPA/CPL/CPM/CPC/oCPA/oCPC 任务，但不急于上线全量结算。

- 扩展活动中心筛选：`全部`、`CPS`、`CPA`、`CPL`、`CPM`、`CPC`、`OCPA`、`OCPC`、`混合`。
- 后台活动表单增加推广方式、结算说明、预算说明、转化口径。
- 历史 `CPS+CPA` 数据展示为“混合”，内部建议拆规则。
- 菜单文案从“CPS联盟”逐步改为“CPX联盟”，保留旧权限码兼容。
- 活动卡片文案从“返利文案”泛化为“奖励/结算文案”。

验收：

- 后台可以创建和筛选 CPS 主导的多计费模型任务。
- CPS 商品广场跳转逻辑不受影响。
- CPA/CPL/CPM/CPC/oCPA/oCPC 任务不会误触发 CPS 转链或订单返利。

### P2：追踪链接与事件账本

目标：所有推广方式都有统一 trackingId 和事件流水。

- 新增 tracking link 服务，统一生成活动/offer 推广链接。
- 新增 impression/click event 记录接口，用于 CPM/CPC/oCPC 和漏斗统计。
- 新增平台回调接收接口，支持 CPA/CPL/CPM/CPC/oCPA/oCPC/CPS 转化回传。
- 新增幂等、签名、nonce、时间窗和租户隔离校验。
- 所有事件写入统一事件账本，敏感字段脱敏或摘要化。

验收：

- 同一 `X-Idempotency-Key` 重复提交只生成一条事件。
- 非法签名、过期时间戳、重复 nonce、跨租户回调全部失败。
- 点击事件可以关联活动、offer、推广位和会员上下文。

### P3：CPA/CPL/oCPA 转化与奖励结算

目标：让非订单类转化可以被审核、结算、冻结、扣回。

- 新增 CPA/oCPA 动作转化状态机：`PENDING`、`CONFIRMED`、`REJECTED`、`SETTLED`、`REVERSED`。
- 新增 CPL 线索审核状态机：`SUBMITTED`、`VALIDATING`、`APPROVED`、`REJECTED`、`SETTLED`。
- 新增固定金额、阶梯金额、活动预算上限、会员奖励比例配置。
- CPA/CPL/oCPA 奖励进入冻结或待结算，不直接进入可用余额。
- 审核失败、平台撤销、重复线索触发扣回或拒绝。

验收：

- CPA/oCPA 有效动作可以生成奖励记录并按冻结期入账。
- CPL 线索可以审核通过后生成奖励，重复线索不会重复结算。
- 失败、撤销、重复、跨租户用例均有测试覆盖。

### P4：CPM/CPC/oCPC 计费与风控

目标：支持曝光和点击计费，但先保护预算和反作弊。

- 定义有效曝光：同活动、同 offer、同设备/IP/会员在窗口期内按去重规则累计。
- 定义有效点击：同活动、同 offer、同设备/IP/会员在窗口期内只计一次。
- 支持活动预算、日预算、单会员/单设备频控。
- CPM/CPC/oCPC 先进入运营收入/成本统计；会员曝光/点击奖励需单独开关。
- 增加异常点击检测：高频、同 IP、无 referer、异常 UA、短时间重复。

验收：

- CPM/CPC/oCPC 任务能统计曝光、有效曝光、点击、有效点击、预算消耗和优化目标效果。
- 重复曝光、重复点击和异常点击不产生重复计费。
- 超预算后 tracking link 返回明确不可计费状态。

### P5：MCP 与 Agent 推荐升级

目标：让 Agent 能调用 CPX 能力，而不破坏现有 `cps_*` 工具。

- 保留现有 `cps_search_goods`、`cps_compare_prices`、`cps_generate_link`。
- 新增 `cpx_list_tasks`、`cpx_get_task_detail`、`cpx_generate_tracking_link`、`cpx_recommend_tasks_by_scene`、`cpx_query_conversions`、`cpx_search_articles`。
- CPX Tool 统一接入 MCP 审计日志。
- AIoT 场景推荐可以返回 CPS 商品、CPA/oCPA 任务、CPL 表单、CPM 曝光任务或 CPC/oCPC 活动，但必须标明结算方式和风险提示。

验收：

- MCP Tool 成功/失败均写审计日志。
- ToolContext 身份优先于请求体 memberId。
- Agent 能按场景拿到不同 promotionMethod 的 offer。

### P6：统计看板、文档与命名迁移

目标：完成产品心智从 CPS 到 CPX 的迁移。

- 新增 CPX 总览：点击、线索、动作、订单、佣金、奖励、ROI。
- 原 CPS 订单和返利看板保留为 CPX 下的 CPS 子视图。
- README、项目地图、菜单、接口文档统一说明 CPX 定位。
- 长期可补充 `cpx:*` 权限别名和 `/cpx/...` API 别名；旧 `/cps/...` 保持兼容周期。

验收：

- 新用户从 README 能理解项目是 CPX 推广联盟系统。
- 老用户仍能使用原有 CPS API、菜单、MCP Tool。
- 看板可以按推广方式拆解 CPX 漏斗。

## 8. 测试与质量门槛

必须采用 TDD，优先覆盖以下场景：

- `promotionMethod` 枚举和活动筛选：CPS/CPA/CPL/CPM/CPC/OCPA/OCPC/MIXED 展示正确。
- 追踪链接生成：租户、会员、推广位、活动、offer 归因正确。
- 回调签名：合法通过，非法签名/过期时间/重复 nonce/重复幂等键失败。
- CPA 结算：有效动作入账，重复动作不重复结算，撤销后扣回。
- CPL 结算：重复线索识别、审核通过入账、审核拒绝不入账、隐私字段脱敏。
- CPM/CPC/oCPC 计费：有效曝光/点击去重、预算限制、频控和异常流量拦截。
- CPS 回归：商品搜索、转链、订单同步、返利入账、退款扣回、Token 兑换不退化。
- MCP 安全：ToolContext 身份优先、访问日志完整、错误脱敏。

建议验证命令：

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=*Cpx*Test "-Dsurefire.failIfNoSpecifiedTests=false"
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsRebateTokenExchangeServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

```bash
cd frontend/admin-vue3
pnpm ts:check
pnpm lint:eslint
pnpm e2e
```

## 9. 风险与约束

- 不建议第一阶段全量重命名包名、表名和 API，避免破坏既有功能。
- CPA/CPL/CPM/CPC/oCPA/oCPC 不应直接复用 `cps_order`，否则订单状态机和资金语义会失真。
- CPM/CPC/oCPC 风控复杂，不能在没有预算、频控和去重规则时开放会员现金奖励。
- CPL 涉及个人信息，必须脱敏、加密、授权留痕，并限制日志输出。
- CPA/CPL/CPM/CPC/oCPA/oCPC 平台回调必须幂等和可审计，不能依赖“先查后插”的弱幂等。
- CPX 新金额字段应使用整数分；历史 BigDecimal 字段先兼容、后迁移。
- MCP 和 OpenAPI 必须优先修复 memberId 信任边界和审计日志，否则 CPX 会扩大风险面。

## 10. 推荐近期任务顺序

1. 新增 CPX 术语、枚举、活动中心展示规则和文档。
2. 扩展活动中心和任务中心支持 CPA/CPL/CPM/CPC/oCPA/oCPC/混合，不接入结算。
3. 设计并落地 tracking link 与 event ledger。
4. 接入 CPA/oCPA 回调、CPL 审核和非 CPS 结算状态机。
5. 在 CPM/CPC/oCPC 上线前完成曝光/点击去重、预算、频控和异常流量风控。
6. 新增 CPX MCP Tool 与统一审计。
7. 完成 CPX 看板和 README/项目地图迁移。

推荐主线是方案 B：新增 CPX 核心层，保留 CPS 兼容适配。这样可以让项目从“返利导购系统”自然升级为“推广联盟系统”，同时不破坏已经落地的 CPS 资金闭环。
