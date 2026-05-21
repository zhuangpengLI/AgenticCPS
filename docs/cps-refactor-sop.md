# CPS 主 R 打样 SOP

> 适用范围：`backend/qiji-module-cps` 的订单同步、返利计算、冻结/解冻、平台适配器、Mapper、MCP Tool 等高风险链路。

## 目标

用一个高价值模块做可复制样板，让后续 AI 改 CPS 代码时先读链路、先锁行为、再小步重构，避免只修表面代码。

推荐第一批打样模块：

1. 订单同步：`CpsOrderSyncJob` -> `CpsOrderServiceImpl` -> `CpsOrderMapper` -> 返利结算/退款扣回。
2. 返利计算：`CpsRebateSettleJob` -> `CpsRebateSettleServiceImpl` -> `CpsRebateConfigServiceImpl` -> 返利账户/流水 Mapper。

当前更建议先选订单同步，因为它同时触碰平台适配、状态流转、幂等、退款/失效、返利扣回和统计数据源。

## 输入材料

- `docs/project-map.md`
- `docs/cps-tech-debt-inventory.md`
- `agent_improvement/memory/cps-ai-coding-rules.md`
- 需求说明或缺陷描述
- 当前 `git status --short`

## 主 R 执行步骤

### 1. 圈定链路

从入口开始按调用链读取，不跳层：

```text
Controller / Job / MCP Tool
  -> Service
  -> Mapper / Client
  -> DO / DTO / VO
  -> SQL / 外部平台 / 定时任务
```

订单同步样板读取顺序：

1. `CpsOrderSyncJob`
2. `CpsOrderServiceImpl`
3. `CpsOrderMapper` 与 `CpsOrderMapper.xml`
4. `CpsPlatformClient` / `CpsApiVendorClient`
5. 返利相关调用：`CpsRebateSettleServiceImpl`

返利计算样板读取顺序：

1. `CpsRebateSettleJob`
2. `CpsRebateSettleServiceImpl`
3. `CpsRebateConfigServiceImpl`
4. `CpsRebateAccountMapper` / `CpsRebateRecordMapper`
5. 冻结/兑换相关调用：`CpsFreezeServiceImpl`、`CpsRebateTokenExchangeServiceImpl`

### 2. 标记越层泄露

逐项标记：

- PO/DO 是否出现在 Controller 或 MCP 响应中。
- DTO/VO 是否被 Service 当作持久化模型长期传递。
- Mapper 是否承载业务状态判断。
- 平台字段是否进入通用 Service、Controller 或 MCP Tool。
- Controller/MCP 是否绕过 Service 直连 Mapper 或 Client。

输出格式：

```markdown
| 文件 | 泄露类型 | 证据行 | 风险 | 是否本次处理 |
|------|----------|--------|------|--------------|
```

### 3. 标记平台差异

检查平台差异是否只存在于：

- `client/{platform}`
- `client/{vendor}`
- `client/common`
- `client/dto`

若在 Service/Controller/MCP 中出现平台分支，标记为技术债，除非只是平台编码白名单或权限校验。

### 4. 锁定现有行为

先补最小测试，再改实现。

订单同步最小测试建议：

- 同 `platformOrderId`、不同 `platformCode` 不互相覆盖。
- 已到账订单收到退款/失效后触发返利扣回。
- 已结算/已到账不被旧状态回退。
- 平台接口失败不同于空订单结果。

返利计算最小测试建议：

- 返利优先级按个人平台、个人全平台、等级平台、等级全平台、平台默认、全局默认匹配。
- 同一订单并发结算只入账一次。
- 冻结/解冻/扣减同时更新账户和审计记录。
- 余额不足、重复提交、超时补偿不会产生重复流水。

### 5. 小步修改

每次只处理一个 smell：

- 幂等键
- 状态机
- 退款扣回
- 返利优先级
- 账户原子更新
- MCP 访问日志
- 平台失败语义

不要在同一个 PR 同时改 schema、重命名、重写适配器和重排前端页面。

### 6. 验证

优先运行目标模块测试：

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=ClassName "-Dsurefire.failIfNoSpecifiedTests=false"
```

若改动影响公共编译或多个服务：

```bash
cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dsurefire.failIfNoSpecifiedTests=false"
```

文档/规则改动至少运行 UTF-8 校验：

```bash
python -c "from pathlib import Path; [p.read_bytes().decode('utf-8') for p in map(Path, ['docs/cps-tech-debt-inventory.md','agent_improvement/memory/cps-ai-coding-rules.md'])]; print('utf8 ok')"
```

### 7. Pre-PR 报告

每次样板改造输出：

```markdown
## 影响范围

## 调用链证据

## 消化的技术债

## 未消化的技术债与原因

## 测试命令与结果

## 剩余风险

## 需要人工重点 Review 的业务语义
```

## 主 R 判定标准

可进入同类推广的条件：

- 已有一条红绿闭环测试或明确的验证证据。
- 调用链、数据模型、状态转换和异常语义已记录。
- 改动没有扩大平台差异泄露。
- PR 报告能让人工只聚焦业务语义，而不是重新查低级边界。

不可推广的条件：

- 仍依赖口头假设确认资金/订单语义。
- 测试无法区分平台失败与空结果。
- 订单或账户并发行为没有验证。
- 仍存在文档路径漂移或规则读取乱码。
