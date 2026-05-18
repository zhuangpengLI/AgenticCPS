# CPS AI 技术债盘点

> 盘点范围：`backend/qiji-module-cps`。本文件只记录技术债和证据，不包含修复实现。

## 盘点摘要

本次聚焦 CPS 平台适配器、订单同步链路、返利计算链路、Mapper 查询、MCP 工具层五类风险。总体看，模块已经具备平台策略接口、供应商双维路由、订单同步 Job、返利结算服务和 5 个 MCP Tool，但在资金/订单安全边界、平台实现完整性、返利优先级一致性、统计 SQL 性能、MCP 审计闭环上存在需要优先治理的技术债。

关键结论：

- P0 风险集中在订单幂等键、MCP 转链 memberId 信任、返利优先级与账户并发更新。
- P1 风险集中在官方平台适配器待实现、Mapper 统计 SQL 索引失效、MCP 访问日志未接入。
- P2 风险集中在测试覆盖、金额类型规则漂移、局部 N+1 查询和文档/规则一致性。

## 风险分级规则

| 等级 | 判定规则 |
|------|----------|
| P0 | 资金、返利、提现、订单状态、租户隔离、memberId 越权、重复入账/重复入库 |
| P1 | 平台适配不一致、MCP 参数/异常不完整、Mapper 性能隐患、状态边界不清 |
| P2 | 命名漂移、注释/文档不一致、测试缺口、局部可维护性问题 |

## 技术债清单表

| 编号 | 问题 | 等级 | 影响链路 | 证据文件 | 风险说明 | 建议修复时机 | 是否可随业务需求消化 | 建议验证方式 |
|------|------|------|----------|----------|----------|--------------|----------------------|--------------|
| CPS-TD-001 | 订单幂等查询只按 `platformOrderId`，未显式纳入平台编码/租户维度 | P0 | 订单同步、订单更新、重复入库 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/order/CpsOrderMapper.java:35`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java:81` | 如果不同平台或不同租户出现相同平台订单号，可能误更新其他订单；即使租户插件生效，平台维度仍依赖平台订单号全局唯一这一隐含假设 | 下一次改订单同步、订单表索引或补单逻辑时优先处理 | 是 | 增加同 `platformOrderId`、不同 `platformCode`/tenant 的单元或 DB 测试，验证不会互相覆盖 |
| CPS-TD-002 | 订单状态更新允许从平台状态直接覆盖本地状态，缺少显式状态机/回退保护 | P0 | 订单同步、退款/失效、返利结算 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java:93`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java:252` | 代码按平台状态映射后直接更新，未看到“已到账不可回退”“已退款不可再结算”等保护；退款标记会覆盖状态，但其他回退/乱序同步边界不清 | 下一次改订单同步、结算、退款时同步处理 | 是 | 构造 paid -> received -> settled -> credited 后收到旧 paid/invalid 的乱序同步测试 |
| CPS-TD-003 | MCP 转链允许 request 传入 `memberId`，ToolContext 只在 request 为空时兜底 | P0 | MCP 转链、订单归因、会员返利 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsGenerateLinkToolFunction.java:103`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsGenerateLinkToolFunction.java:119` | 涉及订单归因和返利归属，客户端传入 memberId 的优先级高于可信上下文，存在越权归因风险 | 下一次改 MCP 转链或会员归因时必须优先处理 | 是 | ToolContext 用户 A + request.memberId 用户 B，验证最终 externalId/归因必须为 A |
| CPS-TD-004 | 返利结算实际只按“全等级 + 平台/全平台”匹配，未接入会员个人/会员等级上下文 | P0 | 返利计算、返利入账、会员等级 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:92`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:223`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateConfigServiceImpl.java:77` | 规则要求会员个人平台、会员个人全平台、等级平台、等级全平台、平台默认、全局默认优先级；当前结算调用传入 `null` 等级，且配置对象未看到会员个人维度，可能导致返利比例不符合业务配置 | 下一次改返利配置、会员等级或结算 Job 时处理 | 是 | 增加多级配置优先级测试，覆盖会员等级、平台配置、全平台兜底 |
| CPS-TD-005 | 返利账户“乐观锁”更新使用 `updateById`，未看到版本条件递增/条件更新 | P0 | 返利入账、返利扣回、账户余额 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:260`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:269`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/rebate/CpsRebateAccountDO.java:60` | DO 有 `version` 字段，但更新片段只展示 `updateById`，若没有 MyBatis Plus `@Version` 或条件更新，重试不能真正防止并发丢失更新 | 下一次改返利结算、提现、冻结资金时优先处理 | 是 | 并发执行同一会员多笔入账/扣回，断言余额与流水总和一致 |
| CPS-TD-006 | 官方平台供应商多处核心实现仍为待实现或 `return null` | P1 | 官方 API 接入、平台搜索/转链/订单同步 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/official/taobao/TaobaoOfficialVendorClient.java:71`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/official/taobao/TaobaoOfficialVendorClient.java:93`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/official/pdd/PddOfficialVendorClient.java:55`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/official/douyin/DouyinOfficialVendorClient.java:57` | 当 `active_vendor_code` 指向 official 或默认供应商不可用时，搜索/转链/订单查询可能静默空结果或失败，业务层难以区分“无商品”和“能力未实现” | 下一次接入或切换官方供应商前必须处理 | 是 | 为每个 official client 增加 capability 测试，未实现能力应显式失败而不是静默空 |
| CPS-TD-007 | 平台适配器转链失败多处返回 `null`，缺少统一错误对象/错误码 | P1 | 商品转链、MCP 转链、App 转链 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/common/AbstractApiVendorClient.java:122`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/common/AbstractApiVendorClient.java:130`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/taobao/TaobaoPlatformClientAdapter.java:49`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsGenerateLinkToolFunction.java:122` | null 会把“平台限流/签名失败/商品不可转链/供应商未实现”压扁成同一种失败，影响重试、告警和用户提示 | 下一次改转链或供应商异常处理时处理 | 是 | Mock 不同平台错误响应，断言统一错误码、可观测日志和用户可读提示 |
| CPS-TD-008 | 统计 SQL 对 `create_time` 使用 `DATE(create_time)`，可能导致时间索引失效 | P1 | 统计聚合、实时看板、大表查询 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/resources/mapper/order/CpsOrderMapper.xml:19`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/resources/mapper/order/CpsOrderMapper.xml:35`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/statistics/CpsStatisticsServiceImpl.java:44` | 对列使用函数通常无法利用普通索引；订单表增大后，日统计和实时看板容易退化为全表扫描 | 下一次改统计看板或订单表索引时处理 | 是 | 用时间范围 `create_time >= start and < end` 改造后对比执行计划 |
| CPS-TD-009 | MCP 访问日志和 API Key 数据结构存在，但 5 个 Tool 未写访问日志 | P1 | MCP 审计、问题追踪、安全风控 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java:22`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/mcp/CpsMcpAccessLogMapper.java:13`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsSearchGoodsToolFunction.java:171` | Tool 仅 catch 后返回错误消息，未看到统一记录 toolName、参数摘要、耗时、状态、错误原因；线上排查和滥用审计会缺证据 | 下一次改 MCP 认证、网关或任一 Tool 时处理 | 是 | 调用每个 MCP Tool，断言成功/失败均生成访问日志并包含耗时和脱敏参数 |
| CPS-TD-010 | MCP 搜索/比价缺少严格入参边界，分页和关键词主要依赖调用方自律 | P1 | MCP 搜索、跨平台比价、平台 API 成本 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsSearchGoodsToolFunction.java:39`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsSearchGoodsToolFunction.java:131`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsComparePricesToolFunction.java:125` | 虽然字段标注 required，但运行时代码片段未看到 keyword 为空、pageSize 上限、平台编码白名单等统一校验；比价会跨平台调用，异常/大请求容易放大成本 | 下一次改 MCP 搜索或比价时处理 | 是 | 增加空 keyword、超大 pageSize、非法 platformCode、平台部分失败的 Tool 单测 |
| CPS-TD-011 | 商品转链查会员推广位会先拉取平台全部推广位再内存过滤 | P2 | 转链、会员推广位、Adzone 查询 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsServiceImpl.java:87`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsServiceImpl.java:124`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/adzone/CpsAdzoneMapper.java:36` | 平台推广位数量增长后，按平台全量查询再按 relationType/memberId 内存过滤会增加 DB 与 JVM 压力 | 下一次改转链或推广位管理时处理 | 是 | 增加按 platformCode + relationType + relationId + status 的 Mapper 查询，并验证结果一致 |
| CPS-TD-012 | CPS 模块测试覆盖不均，订单、返利、MCP Tool 缺少直接测试文件 | P2 | 回归测试、AI 生成代码质量闸门 | `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/client/CpsPlatformClientFactoryTest.java:27`；`backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/client/haodanku/AbstractHdkVendorClientTest.java:20`；`backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/client/dataoke/AbstractDtkVendorClientTest.java:19` | 当前可见测试集中在 client factory、好单库/大淘客、vendor/risk/freeze；订单同步、返利优先级、MCP 权限/日志等高风险链路缺少直接测试证据 | 下一次修复任一 P0/P1 技术债时补齐 | 是 | 为 P0/P1 条目逐项补最小单元/DB 测试，纳入 Pre-PR 清单 |
| CPS-TD-013 | 金额字段大量使用 `BigDecimal`，与新规则“金额用 Integer 分”存在规则漂移 | P2 | 订单、返利、提现、统计、MCP 返回 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/order/CpsOrderDO.java:69`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/rebate/CpsRebateAccountDO.java:39`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/withdraw/CpsWithdrawDO.java:56` | 这是规则一致性风险，不建议在本轮直接大规模改 schema；但新增代码若继续混用，会扩大迁移成本 | 新增金额字段或重构资金链路时分阶段处理 | 是 | 先建立金额字段清单和单位说明，后续迁移前补兼容测试 |

## 分模块观察

### 平台适配器

- `CpsPlatformClient` 提供业务层按平台路由接口，`CpsApiVendorClient` 提供供应商 x 平台底层接口，抽象方向清晰。
- `CpsPlatformClientFactory` 会自动注册平台适配器与供应商客户端，并在未配置 `activeVendorCode` 时默认使用 `dataoke`。
- 风险在于官方 vendor 多处能力未实现，同时底层失败大量返回 null 或空集合，业务层难以区分失败类型。

### 订单同步链路

- `CpsOrderSyncJob` 已按平台拉取订单并调用 `batchSaveOrUpdateOrders`，同步日志也会记录平台级成功/失败。
- 最大风险是幂等查询只按 `platformOrderId`，以及状态更新缺少集中状态机。
- `batchSaveOrUpdateOrders` 对单条异常会计入 skip，适合继续保留，但建议同步记录失败订单明细或错误摘要。

### 返利计算链路

- 结算服务已有返利记录幂等检查和账户更新重试思路。
- 当前匹配配置时未传入会员等级或会员个人上下文，与规则要求的优先级不一致。
- 账户并发安全需要确认 `version` 是否真正参与乐观锁；从当前片段看风险较高。

### Mapper 查询

- 多数 DO 继承 `TenantBaseDO`，普通 MyBatis Plus 查询大概率依赖租户插件注入租户条件；自定义 XML 已显式加 `tenant_id` 和 `deleted`。
- 统计 XML 使用 `DATE(create_time)`，这是明确的性能技术债。
- 局部查询存在先全量按平台查再内存过滤的模式，短期影响较低，但应随转链/推广位需求消化。

### MCP 工具层

- 查订单、返利摘要从 `ToolContext` 获取 memberId，方向正确。
- 转链 Tool 仍允许 request memberId 优先，属于需要优先修复的归因风险。
- 访问日志 DO/Mapper 已存在，但 Tool 层未看到日志写入；建议用统一 wrapper/interceptor 收口，而不是每个 Tool 手写。

## 优先修复建议

1. 第一批 P0：`CPS-TD-001`、`CPS-TD-003`、`CPS-TD-004`、`CPS-TD-005`。这些直接影响订单归属、返利金额、账户余额和越权风险。
2. 第二批 P1：`CPS-TD-006`、`CPS-TD-007`、`CPS-TD-009`、`CPS-TD-010`。这些影响平台能力稳定性和 MCP 可观测性。
3. 第三批性能与质量：`CPS-TD-008`、`CPS-TD-011`、`CPS-TD-012`、`CPS-TD-013`。建议随统计、转链、测试体系和资金模型演进渐进消化。

## 后续落地方式

- 每次 CPS 需求进入开发前，先检查是否触碰本清单中的影响链路。
- 若触碰 P0/P1 技术债，PR 描述必须说明“本次是否消化”，不消化需给出延期原因。
- 每修复一项技术债，至少补一条验证用例，并在本文件中把状态改为“已处理”或迁移到变更记录。
