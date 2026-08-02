# CPS 资金与归因安全基线

## 资金状态机

订单返利必须同时满足“确认收货”和“平台结算”两个条件。资格时间取两者较晚值，随后按平台和返利金额分档匹配冻结规则：

```text
PAID -> RECEIVED -> SETTLED
  -> 创建 ORDER_REBATE 冻结
  -> 到达 unfreezeTime
  -> 优先偿还最早欠款
  -> 剩余金额进入 available
```

无平台结算时间、无可信会员归因、会员等级查询失败、无可匹配返利或冻结规则时，订单保持待处理并由任务重试，不得使用代码默认比例直接入账。

结算任务只把批扫描结果当作候选 ID：逐单事务必须使用 `SELECT ... FOR UPDATE` 重读当前订单，并以 `status_version` CAS 写入冻结结果；若退款或失效已并发修改订单，整笔结算事务回滚。待处理或失败订单写入独立的重试次数、下次重试时间和最近错误；扫描过滤未到期重试，并将新单创建时间与重试单到期时间统一作为资格时间进行 FIFO 调度，避免固定低 ID、新单或历史重试单互相造成无限饥饿。

## 唯一资产写入口

`CpsRebateAssetService` 是返利账户 `available / frozen / debt` 的唯一业务写入口。订单结算、退款冲正、自动/手动解冻及 Token 兑换均通过该服务，在同一事务内完成账户锁定、幂等复核、业务记录、账户更新和不可变流水追加。

金额计算统一使用 `CpsMoneyConverter` 完成元与分转换。历史业务表继续兼容两位小数元，新资产流水、欠款和冻结规则以整数分为准。

## 退款与欠款

退款先取消未解冻的订单返利；已解冻返利先扣可用余额，余额不足的差额写入 `cps_rebate_debt`。后续返利按欠款创建顺序偿还，只有剩余金额进入可用余额。管理员只能通过带原因和幂等键的 `WAIVE / INCREASE` 动作调整欠款。

欠款创建后立即进入提醒队列。提醒阈值、站内信间隔、普通/大额提醒持续时间和短信间隔均由 `cps_rebate_asset_policy` 按租户配置；模板缺失或通知通道失败只会安排重试，不影响资金事务。

## 可信归因

自动归因只接受已验证的 specialId、relationId、推广位绑定或唯一有效转链记录。裸数字 externalId 不再解释为 memberId；多候选时拒绝猜测。自动成功、冲突、拒绝、未归因和人工绑定均追加到 `cps_order_attribution_log`。已产生返利记录的订单禁止直接改绑，必须走冲正和重新结算补偿流程。

## 同步与租户

平台适配器使用 `CpsOrderPageResult` 显式声明 PAGE/CURSOR、下一页标记和 hasMore。同步任务逐页持久化，整页成功后才推进 `cps_order_sync_checkpoint`；淘宝场景 1/2/3 使用独立 checkpoint。订单同步、返利结算和解冻任务均使用 `@TenantJob`。

管理后台“资产安全中心”提供会员欠款、不可变资产流水、归因日志、同步水位和租户资产策略。归因日志、同步水位和资产流水只允许查询；租户启用 V2 前资金写入会被拒绝，故障时可打开 `read_only` 熔断，重复幂等请求仍返回原结果。

## 发布步骤

以下步骤是发布门禁，不代表已经在任何生产租户执行。Release B 的唯一键 DDL 在
`backend/sql/module/cps-update.sql` 中保持注释，禁止随全量增量脚本自动执行。

1. **Release A：兼容结构。** 仅执行 `backend/sql/module/cps-update.sql` 中“发布A兼容结构”段，创建新表、新列和普通预检索引。确认 `cps_rebate_asset_policy.v2_enabled=0`、`migration_ready=0`。
2. **预检并归档。** 以目标租户管理员调用 `POST /admin-api/cps/rebate-asset/migration/check`。服务显式按当前 `tenant_id` 核对十类风险：重复账户、重复订单、重复返利主记录、重复资产幂等键、重复冻结幂等键、账户净资产与流水净额不一致、冻结汇总与账户冻结余额不一致、缺失 `OPENING_BALANCE`、无同租户账户的孤儿资产流水、无同租户账户的有效冻结记录。账户金额比较对历史 `NULL` 按零处理，防止 SQL 三值逻辑漏报。任一计数非零时 `ready=false`，立即停止，不自动合并、删除或修复历史资金数据。归档可通过 `GET /admin-api/cps/rebate-asset/migration/check-archives` 查询。
3. **期初流水。** 重复/历史差异完成书面核对后，调用 `POST /admin-api/cps/rebate-asset/migration/opening-balances`。该操作只为当前租户缺失期初流水的账户追加 `OPENING_BALANCE`，不修改账户余额；同租户重跑或并发唯一键冲突均按幂等结果返回。`migration_ready=1` 或 `v2_enabled=1` 后禁止继续回填，避免审批数据集发生变化。
4. **冻结对账。** 再次执行迁移预检，要求全部十类计数均为 `0`。缺少平台结算时间或 `amount_cent` 不可信的历史冻结记录进入人工清单，禁止自动解冻。
5. **人工批准。** 保存最近一次 `ready=true` 的归档批次号、执行人、执行时间、冻结差异清单和审批单号。没有人工批准不得进入 Release B。
6. **Release B：受控唯一键。** 先运行 `cps-update.sql` 发布B段中的 `information_schema.statistics` 索引核对模板，确认旧索引真实名称和列顺序；逐表执行仍保持注释的 ALTER 模板。不得执行整个更新脚本来间接触发 Release B。
7. **标记迁移就绪。** Release B 成功且同一租户最终预检归档仍为 `ready=true` 后，发布变更单才可执行：

   ```sql
   UPDATE cps_rebate_asset_policy policy
   JOIN (
     SELECT tenant_id, batch_no, executed_at, ready
     FROM cps_rebate_asset_migration_check
     WHERE tenant_id = ?
     ORDER BY id DESC
     LIMIT 1
   ) latest ON latest.tenant_id = policy.tenant_id
   SET policy.migration_ready = 1,
       policy.latest_ready_check_batch_no = latest.batch_no,
       policy.ready_check_time = latest.executed_at
   WHERE policy.tenant_id = ?
     AND latest.batch_no = ?
     AND latest.ready = 1
     AND policy.v2_enabled = 0
     AND policy.deleted = b'0';
   ```

8. **启用 V2。** 管理端首次设置 `v2_enabled=1` 时，不只读取布尔值：必须确认 `migration_ready=1` 绑定当前租户最新且 `ready=true` 的归档批次与执行时间，并在同一串行化启用事务内重新运行完整预检。新报告仍为 `ready=true` 才写入 V2 开关和新的批次水位。启用后禁止回退旧写逻辑，只能设置 `read_only=1` 熔断并向前修复；首次启用补齐全平台、全金额、15 天兜底冻结规则。
9. **持续对账。** 每次发布、历史回填和异常恢复后重新生成预检归档；账户余额与不可变流水或冻结汇总不一致时立即切换资产只读。

本地代码门禁命令：

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am \
  "-Dtest=CpsOrderMapperDbTest,CpsRebateAssetMigrationServiceTest,CpsRebateAssetMigrationDbTest,CpsRebateAssetMigrationCheckServiceImplTest,CpsRebateAssetMigrationCheckDbTest" \
  "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 对账断言

```text
available_balance + frozen_balance - debt_balance
= SUM(available_change_cent + frozen_change_cent - debt_change_cent) / 100
```

同时检查同租户同平台订单号唯一、同订单返利主记录唯一、无平台结算时间的订单不存在可用返利，以及所有流水均包含租户、会员、业务单、幂等键和操作主体。
