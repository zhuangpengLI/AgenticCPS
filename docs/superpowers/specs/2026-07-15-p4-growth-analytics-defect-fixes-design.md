# P4 增长分析三个缺陷修复设计

**日期：** 2026-07-15
**范围：** `CpsGrowthAnalyticsService`、P4 增长分析 Admin API 及其回归测试

## 背景

P4 在线验收确认了三个缺陷：

1. 已超过处理时限的 CPS `PROCESSING` 事件没有通过 `/token-reconciliation` 输出 `PROCESSING_TIMEOUT`。
2. TokenHub 已成功入账、CPS 侧没有对应成功事件时，没有输出反向单边成功差异。
3. billing 边界校验对未知动作默认放行，未遵循“只允许消费已确认 CPS 资产事件”的安全边界。

现有服务单元测试覆盖了超时判断方法，但控制器测试仅验证 Mockito 委托，没有覆盖真实 JSON 中 `LocalDateTime`、`Duration` 和事件列表经过 HTTP/Jackson 反序列化后进入领域逻辑的路径。因此现有测试不能证明线上请求契约可用。

## 目标

- 保持现有 Admin API 路径和 `TokenReconciliationSummary` JSON 结构兼容。
- 对账逻辑同时识别 CPS 单边成功和 TokenHub 单边成功。
- 对已到达或超过阈值、且仍处于 `PROCESSING` 的 CPS 提交事件输出 `PROCESSING_TIMEOUT`。
- billing 边界改为 fail-closed：只有明确白名单动作允许通过。
- 用服务层测试和真实 JSON 控制器回归测试锁定三个缺陷。

## 非目标

- 不接入真实 TokenHub 日切任务、BI 看板或生产告警平台。
- 不修改返利账户、订单、结算、Token 主账本或数据库结构。
- 不改变 `diffCodesByOrderNo` 的响应字段和键结构。
- 不在本次修复中扩展跨租户同业务单号的复合明细响应。

## 设计

### 1. PROCESSING 超时判定

对每个由 `tenantId + businessOrderNo + idempotencyKey` 组成的事件组，找到 CPS 侧最新的 `SUBMIT` 事件。只有该事件当前仍为 `PROCESSING`，并且：

```text
eventTime + processingTimeout <= now
```

时输出 `PROCESSING_TIMEOUT`。

该规则避免两个边界问题：

- 恰好到达超时阈值时漏报。
- 较早的 `PROCESSING` 后已有 `SUCCESS`，却仍被旧事件误报超时。

控制器回归测试使用与线上一致的 ISO-8601 JSON：`LocalDateTime` 使用日期时间字符串，`Duration` 使用 `PT...` 字符串，验证反序列化后的值能够触发领域超时判断。

### 2. TokenHub 反向单边成功

保留现有差异编码，并新增：

| 条件 | 差异编码 |
|---|---|
| CPS `SUBMIT/SUCCESS` 存在，TokenHub `CREDIT/SUCCESS` 不存在 | `TOKENHUB_MISSING_SUCCESS` |
| TokenHub `CREDIT/SUCCESS` 存在，CPS `SUBMIT/SUCCESS` 不存在 | `CPS_MISSING_SUCCESS` |

重复 TokenHub 入账仍同时输出 `TOKENHUB_DUPLICATE_CREDIT`。因此 TokenHub-only 的两次成功既能表达“CPS 缺失”，也能表达“TokenHub 重复入账”，不会互相覆盖。

只有 CPS 与 TokenHub 各恰好一次成功、没有回滚且没有任何差异时，才计入 `matchedSuccessCount`。

### 3. billing 白名单边界

允许条件必须全部满足：

- `serviceName` 严格等于 `billing-service`；
- `action` 严格等于 `CONSUME_CONFIRMED_ASSET_EVENT`；
- 不写 CPS 返利账户；
- 不读取/计算 CPS 返利规则；
- 不修改归因。

判断顺序：

1. 一旦命中资产写入、返利计算、冻结或归因修改等禁止条件，拒绝并保留原因码 `BILLING_MUST_NOT_WRITE_CPS_REBATE_ASSET`。
2. 其余不满足完整白名单的命令统一拒绝，原因码为 `ONLY_CONFIRMED_ASSET_EVENT_CONSUMPTION_ALLOWED`。
3. 仅完整匹配白名单时返回 `ALLOWED_CONFIRMED_ASSET_EVENT_CONSUMPTION`。

这使未知动作、拼写错误、错误服务名和未来新增但尚未审查的动作全部默认拒绝。

## 测试策略

按 TDD 顺序补充最小失败测试：

1. 服务测试：TokenHub-only 成功必须包含 `CPS_MISSING_SUCCESS`。
2. 服务测试：未知 billing 动作必须拒绝，且确认事件消费仍允许。
3. 服务测试：恰好达到阈值算超时；后续已有 CPS 成功则不再报超时。
4. HTTP/Jackson 回归测试：使用真实 JSON 请求验证 `PT30M` 和日期时间反序列化后能返回 `PROCESSING_TIMEOUT`。
5. 运行 P4 Python 合同测试，确保 API 路径、权限和只读边界未漂移。

目标验证命令：

```powershell
& 'C:\Users\zhuangpengli\.codex\tools\apache-maven-3.9.9\bin\mvn.cmd' test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=CpsGrowthAnalyticsServiceTest,CpsGrowthAnalyticsControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
python -m pytest script/test/test_stage_four_growth_analytics_contract.py -q
```

构建完成后重启本地后端，并使用用户提供的新管理员 Token 复测三个在线请求。Token 仅用于本地验证，不写入源码、测试、日志或文档。

## 兼容性与风险

- API 路径、请求结构和响应结构不变；只新增一个可能出现的差异编码和一个拒绝原因码。
- billing 从默认允许改为白名单允许，属于预期的安全收紧；依赖未知动作被放行的调用方会被明确拒绝。
- 超时判断改为“最新 CPS 提交状态”，减少已成功订单被旧 `PROCESSING` 事件误报的风险。
- 本次不解决 `diffCodesByOrderNo` 在跨租户同业务单号场景下可能覆盖的问题，避免扩大已批准的方案 A 范围。
