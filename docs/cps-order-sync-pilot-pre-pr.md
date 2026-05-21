# CPS Order Sync Pilot Pre-PR

> 主 R 打样模块：`backend/qiji-module-cps` 订单同步链路  
> 本次范围：只处理“已到账订单在后续同步中收到退款/失效状态时，自动触发返利扣回”，并补一层最小单元测试。

## 改动目标

- 把 `CpsOrderSyncJob -> CpsOrderServiceImpl -> CpsRebateSettleServiceImpl` 之间缺失的退款扣回动作补上。
- 避免已到账订单在后续同步中被较早的平台状态回滚为 `settled` / `received`。
- 以最小改动方式验证主 R SOP 可执行。

## 涉及模块

- 后端模块：`backend/qiji-module-cps/qiji-module-cps-biz`
- Job：`CpsOrderSyncJob`
- Service：`CpsOrderServiceImpl`、`CpsRebateSettleServiceImpl`
- Mapper：`CpsOrderMapper`
- 测试：`CpsOrderServiceImplTest`

## 影响接口

- 无新增接口。
- 间接受影响链路：
  - 定时任务 `cpsOrderSyncJob`
  - 手动同步 `CpsOrderService.manualSync`
  - 订单后续状态同步
  - 已到账返利的退款/失效扣回

## 风险等级

- 等级：P0
- 原因：涉及会员返利资产扣回和订单状态回写。
- 关联技术债：
  - `CPS-TD-017` 已到账订单退款/失效后未自动扣回返利
  - `CPS-TD-002` 订单状态更新缺少统一状态保护

## 调用链证据

```text
CpsOrderSyncJob.execute
  -> CpsOrderService.batchSaveOrUpdateOrders
  -> CpsOrderService.saveOrUpdateOrder
  -> CpsRebateSettleService.reverseRebate
  -> CpsRebateRecordMapper / CpsRebateAccountMapper
```

关键文件：

- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/job/CpsOrderSyncJob.java`
- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImpl.java`
- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/rebate/CpsRebateSettleServiceImpl.java`
- `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/service/order/CpsOrderServiceImplTest.java`

## 数据库影响

- 无 schema 变更。
- 无新索引。
- 数据写入行为变化：
  - 当同步到退款/失效状态且订单已到账时，会新增一条 `refund` 类型返利记录。
  - 会扣减返利账户 `available_balance` / `total_rebate`。

## 租户 / 权限 / 软删除检查

- 本次未改 Controller，不新增权限入口。
- 本次未新增自定义 SQL。
- 仍沿用现有 Mapper 与多租户/软删除机制。
- 已知存量风险未在本次处理：
  - `selectByPlatformOrderId` 仍缺平台维度/租户维度唯一性保护。

## 金额单位检查

- 本次未新增金额字段。
- 仍沿用现有 CPS 模块 `BigDecimal` 金额模型。
- 该项与 `cps-ai-coding-rules.md` 的“目标态统一 Integer 分”仍存在存量偏差，本次不扩散处理。

## 幂等与并发检查

- 已补行为：
  - 已到账订单收到退款/失效状态时，调用统一的 `reverseRebate(orderId)`。
  - 已到账订单收到更早的平台状态时，不再回滚订单状态。
- 本次未解决：
  - 返利记录缺唯一约束，`reverseRebate` 仍主要依赖业务判断而非数据库约束。
  - `selectByPlatformOrderId` 仍可能在跨平台/跨租户下命中错误记录。

## AI 自查发现的问题与修复记录

| 问题 | 等级 | 是否本次处理 | 证据 |
|------|------|--------------|------|
| 已到账订单后续退款未触发返利扣回 | P0 | 是 | `CpsOrderServiceImpl.saveOrUpdateOrder` |
| 已到账订单可被较早平台状态回滚 | P0 | 是 | `resolveNextOrderStatus` |
| 订单唯一性只看 `platformOrderId` | P0 | 否 | `CpsOrderMapper.selectByPlatformOrderId` |
| 退款扣回依赖业务判断，缺数据库唯一约束 | P1 | 否 | `CpsRebateSettleServiceImpl.reverseRebate` |

## 测试命令与结果

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsOrderServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"
```

结果：

- 覆盖场景 1：已到账订单收到退款状态时触发返利扣回。
- 覆盖场景 2：已到账订单收到较早平台状态时不回滚。

## 需要人工重点 Review 的业务语义

- 退款/失效是否都应该统一走 `reverseRebate(orderId)`，还是需要按平台状态再区分“冻结待确认”和“立即扣回”。
- 已到账后佣金金额变动时，当前仅保护状态不回滚，是否还需要补“差额调账”策略。
- 退款扣回失败时，当前实现会让单条订单更新事务回滚；是否需要改成“订单状态更新 + 补偿任务”模式。

## 剩余风险

- `CPS-TD-016` 冻结解冻不回写账户余额仍未修。
- `CPS-TD-018` 返利结算/扣回仍缺数据库层唯一约束。
- `CPS-TD-001` 订单幂等键设计仍不足。
- `CPS-TD-004` 返利配置优先级仍未统一到会员配置维度。

## 文档同步

- 已对齐 `docs/cps-refactor-sop.md` 的试点方向。
- 已对齐 `docs/cps-pre-pr-template.md` 的输出结构。
- 若本次代码确认保留，建议下一步把该报告的结论回填到 `docs/cps-tech-debt-inventory.md` 的“处理进展”区块。
