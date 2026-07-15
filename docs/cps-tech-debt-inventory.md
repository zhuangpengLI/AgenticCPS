# CPS AI 技术债盘点

> 盘点范围：`backend/qiji-module-cps`。本文件只记录技术债和证据，不包含修复实现。

## 盘点摘要

本次聚焦 CPS 平台适配器、订单同步链路、返利计算链路、Mapper 查询、MCP 工具层五类风险，并补充 AI 规则文档可读性与真实路径漂移两类工程治理风险。总体看，模块已经具备平台策略接口、供应商双维路由、订单同步 Job、返利结算服务和 5 个 MCP Tool，但在资金/订单安全边界、平台实现完整性、返利优先级一致性、统计 SQL 性能、MCP 审计闭环上存在需要优先治理的技术债。

关键结论：

- P0 风险集中在订单幂等键、MCP 转链 memberId 信任、返利优先级、账户并发更新，以及 AI 规则文档的 UTF-8 读取/写入治理。
- P1 风险集中在官方平台适配器待实现、Mapper 统计 SQL 索引失效、MCP 访问日志未接入、真实模块路径漂移。
- P2 风险集中在测试覆盖、金额类型规则漂移和局部 N+1 查询。

编码核验补充：`agent_improvement/memory/MEMORY.md` 与 `docs/e2e-agent-issue-workflow.md` 当前可按 UTF-8 解码，且未发现常见替换字符；但 AGENTS 规则明确记录 PowerShell/GBK 导致中文文件腐化的历史风险，因此仍按 P0 工程治理处理，要求后续文档读写必须显式 UTF-8 并验证。

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
| CPS-TD-014 | AI 规则文档存在被错误编码读取/写入后腐化的治理风险（代码治理已完成，待 CI 首次成功后正式关闭） | P0 | AI 规则读取、代码生成约束、Pre-PR 自查 | `script/check_utf8_integrity.py`；`script/test/test_check_utf8_integrity.py`；`.github/workflows/utf8-integrity.yml`；`.github/pull_request_template.md`；`docs/cps-pre-pr-template.md` | 已建立无第三方依赖的 UTF-8/BOM/U+FFFD/mojibake 扫描器、19 项自动化测试和 PR/push CI 闸门；代码完成时间：2026-07-13 16:35:22 +08:00。CI 尚未在远端首次成功，不能提前标记正式关闭 | CI 首次成功后记录运行链接和时间，并将本项正式关闭 | 是 | `python script/check_utf8_integrity.py`；`python -m pytest script/test/test_check_utf8_integrity.py`；确认 GitHub Actions `UTF-8 Integrity` 首次成功且无非法豁免 |
| CPS-TD-015 | README/AGENTS 中仍有 `yudao-*` 模块名，和真实 `qiji-*` 目录漂移 | P1 | AI 定位、命令选择、CPS 模块开发入口 | `README.md:348`；`README.md:389`；`README.md:400`；`AGENTS.md:127`；`AGENTS.md:128`；`AGENTS.md:219`；`docs/project-map.md:65` | 新 Agent 容易按旧路径查找 `yudao-module-cps` 或运行旧 Maven module，导致误判“文件不存在”或改错位置；`docs/project-map.md` 已记录真实路径但 README/AGENTS 仍混有旧名 | 立即修正活跃入口文档；历史需求文档可加“历史命名”说明 | 部分可；活跃入口文档不建议等业务需求 | 运行 `rg "yudao-module-cps|yudao-server|yudao-framework" README.md AGENTS.md docs agent_improvement/memory` 并逐项确认 |
| CPS-TD-016 | 冻结记录解冻只改记录状态，未同步返利账户可用/冻结余额 | P0 | 冻结解冻、返利账户、兑换失败回滚、提现可用余额 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/freeze/CpsFreezeServiceImpl.java:95`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/freeze/CpsFreezeServiceImpl.java:101`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/freeze/CpsFreezeServiceImpl.java:126`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/rebate/CpsRebateAccountMapper.java:28` | `batchUnfreeze` 有明确 TODO，`manualUnfreeze` 同样只更新 freeze record；这会造成记录显示已解冻但账户 `frozen_balance` 未减少、`available_balance` 未恢复 | 立即处理；涉及冻结/兑换/提现时必须先修 | 否 | 构造冻结账户 + 到期记录，执行自动/手动解冻后断言账户余额和记录状态同时变化 |
| CPS-TD-017 | 已入账订单后续退款/失效不会自动扣回返利 | P0 | 订单同步、退款/失效、返利扣回、账户余额 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java:116`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java:120`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:152`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:187` | 订单同步能把订单标记为退款，但未看到同步链路调用 `reverseRebate`；已到账返利可能在平台退款后仍留在会员可用余额 | 下一次改订单同步、退款或返利结算时必须处理 | 否 | 已到账订单收到退款状态后，断言生成扣回流水、账户余额扣减、原返利记录状态变化 |
| CPS-TD-018 | 返利结算“查记录后插入”缺少唯一键/锁，存在并发重复入账窗口 | P1 | 返利结算、重复流水、账户余额 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:77`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java:111`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/order/CpsOrderMapper.java:45`；`backend/sql/mysql/cps-all-in-one.sql:176`；`backend/sql/mysql/cps-all-in-one.sql:178` | 当前仅先查已有返利记录再插入，SQL 只见普通 `idx_order_id`，未见 `(tenant_id, order_id, rebate_type)` 唯一约束；并发 Job/手动结算可能双写流水并重复加余额 | 下一次修返利结算或补偿 Job 时处理 | 否 | 并发调用同一订单结算，断言只生成一条 REBATE 流水且账户只加一次 |
| CPS-TD-019 | 激活供应商配置查询不校验 vendor `status`，禁用供应商仍可能被使用 | P1 | 平台适配器、供应商切换、搜索/转链/订单同步 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClientFactory.java:179`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClientFactory.java:188`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/vendor/CpsApiVendorMapper.java:31`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/vendor/CpsApiVendorMapper.java:37` | `selectByVendorAndPlatform` 只按 vendor/platform 查，和启用列表的 `status = 1` 语义不一致；后台禁用供应商可能不生效 | 下一次改供应商管理或平台路由时处理 | 是 | 禁用 active vendor 后搜索/转链/订单同步应失败或回退到明确配置的可用供应商 |
| CPS-TD-020 | 单平台搜索/转链只检查注册适配器，不检查平台启用状态 | P2 | App 搜索/转链、MCP 搜索/转链、平台开关 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClientFactory.java:104`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/CpsPlatformClientFactory.java:126`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsServiceImpl.java:46`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsServiceImpl.java:81` | 全平台搜索会过滤启用平台，但显式传 platformCode 的单平台路径只看 Bean 是否存在；如果禁用语义是“不可用”，这里会绕过开关 | 下一次改平台开关或商品入口时处理 | 是 | 禁用平台后，显式 platformCode 搜索/转链应返回统一禁用错误 |
| CPS-TD-021 | 供应商配置字段语义复用，缺少平台/供应商维度显式校验 | P2 | 大淘客、好单库配置、后台供应商管理 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/dataoke/DtkJdVendorClient.java:86`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/dataoke/DtkJdVendorClient.java:117`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/haodanku/HdkTaobaoVendorClient.java:91` | `authToken` 在不同供应商/平台中代表 unionId、order key、tb_name 等不同语义，后台配置容易填错且测试难定位 | 下一次改供应商配置 UI 或新增供应商时处理 | 是 | 为 vendor/platform 增加配置 schema 或 `extraConfig` 必填校验，并补连接测试 |
| CPS-TD-022 | MCP API Key 表和访问日志表存在，但 Tool 层未看到鉴权/审计使用路径 | P1 | MCP 鉴权、访问审计、安全风控 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/mcp/CpsMcpApiKeyMapper.java:15`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/mcp/CpsMcpAccessLogMapper.java:13`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java:36`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java:56` | 数据结构具备 apiKeyId、toolName、requestParams、status、errorMessage、durationMs，但 5 个 Tool 未注入/调用日志 Mapper；API Key 是否由框架外层拦截也未在 CPS 作用域内形成可验证证据 | 下一次改 MCP 网关、Tool 或 API Key 管理时处理 | 是 | 调用成功/失败 Tool，断言鉴权生效、日志落库、耗时和脱敏参数完整 |
| CPS-TD-023 | MCP API Key 查询未内置启用/过期校验 | P1 | MCP 鉴权、API Key 生命周期 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/mcp/CpsMcpApiKeyMapper.java:15`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java:44`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java:48`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java:58` | Mapper 只按 keyValue 查询，DO 有 `status`、`expireTime`、`lastUseTime`、`useCount`；如果调用方忘记额外校验，禁用/过期 Key 可能继续通过 | 下一次补 MCP 鉴权时处理 | 是 | disabled/expired/key not found 三类用例都应失败并写审计日志 |
| CPS-TD-024 | MCP Tool 直接返回原始异常消息，错误结构不统一 | P1 | MCP Tool、AI 客户端兼容、安全脱敏 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsSearchGoodsToolFunction.java:171`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsComparePricesToolFunction.java:170`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsGenerateLinkToolFunction.java:136`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsQueryOrdersToolFunction.java:154`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsGetRebateSummaryToolFunction.java:147` | `e.getMessage()` 直接进入 Tool 响应，可能暴露供应商、SQL、签名、内部状态等信息；不同 Tool 混用 success payload + error 字符串，客户端难以稳定处理 | 下一次改任一 MCP Tool 时处理 | 是 | 建立统一 `code/message/details` 或等价结构，details 脱敏且可配置 |
| CPS-TD-025 | MCP 分页/数量参数只做上限截断，缺少下界和默认值统一校验 | P2 | MCP 搜索、比价、订单查询、返利摘要 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsSearchGoodsToolFunction.java:128`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsComparePricesToolFunction.java:119`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsQueryOrdersToolFunction.java:132`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool/CpsGetRebateSummaryToolFunction.java:120` | 多处 `Math.min(x, 20)` 只限制上限，0 或负数可能进入分页/limit 逻辑，行为依赖下游实现 | 下一次改 MCP 参数校验时处理 | 是 | pageNo/pageSize/recentCount 为 null、0、负数、超大值时均返回稳定结果 |
| CPS-TD-026 | 后台订单等大表查询存在前置通配 `LIKE` 扫描风险 | P2 | Admin 订单列表、转链记录、供应商列表 | `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/order/CpsOrderMapper.java:29`；`backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/order/CpsOrderMapper.java:30` | `itemTitle`、`platformOrderId` 使用 `likeIfPresent`，数据量增长后容易退化为扫描；是否改成精确、前缀或全文检索需要产品查询语义确认 | 后台列表/搜索体验迭代时处理 | 是 | 对常用筛选建立索引友好查询，必要时保留高级模糊搜索并限制范围 |

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

1. 第零批工程治理：`CPS-TD-014`、`CPS-TD-015`。先保证 AI 读到的规则和路径是对的，否则后续治理会反复失焦。
2. 第一批 P0：`CPS-TD-001`、`CPS-TD-003`、`CPS-TD-004`、`CPS-TD-005`、`CPS-TD-016`、`CPS-TD-017`。这些直接影响订单归属、返利金额、账户余额、冻结余额和越权风险。
3. 第二批 P1：`CPS-TD-006`、`CPS-TD-007`、`CPS-TD-009`、`CPS-TD-010`、`CPS-TD-018`、`CPS-TD-019`、`CPS-TD-022`、`CPS-TD-023`、`CPS-TD-024`。这些影响平台能力稳定性、MCP 可观测性、MCP 安全和并发幂等。
4. 第三批性能与质量：`CPS-TD-008`、`CPS-TD-011`、`CPS-TD-012`、`CPS-TD-013`、`CPS-TD-020`、`CPS-TD-021`、`CPS-TD-025`、`CPS-TD-026`。建议随统计、转链、测试体系和资金模型演进渐进消化。

## 后续落地方式

- 每次 CPS 需求进入开发前，先检查是否触碰本清单中的影响链路。
- 若触碰 P0/P1 技术债，PR 描述必须说明“本次是否消化”，不消化需给出延期原因。
- 每修复一项技术债，至少补一条验证用例，并在本文件中把状态改为“已处理”或迁移到变更记录。
