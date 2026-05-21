# E2E Issue Reproduction Workflow

本项目使用 Playwright 作为用户 issue 的 E2E 复现工具。目标是让 Agent 在修复前先把问题变成可重复失败的测试，再基于失败证据读源码、查日志、修复并提交 PR。

## 本地环境

```bash
cd "F:/ai/AgenticCPS/frontend/admin-vue3"
pnpm install
pnpm e2e:install
pnpm e2e
```

如果 `pnpm e2e:install` 因网络失败，Windows 本地默认会优先使用已安装的 Microsoft Edge。也可以通过 `E2E_BROWSER_CHANNEL=chrome` 切换到本机 Chrome。CI 环境仍建议执行 `pnpm e2e:install`，保证浏览器版本可控。

如果本机系统浏览器的 headless 模式启动失败，可以临时使用有界面模式复现：

```bash
E2E_HEADLESS=false pnpm e2e
```

默认会启动 `pnpm dev:e2e`，访问 `http://127.0.0.1:5173`。如果前端服务已经由外部启动：

```bash
E2E_SKIP_WEB_SERVER=true E2E_BASE_URL=http://127.0.0.1:5173 pnpm e2e
```

Midscene.js 辅助视觉/语义检查只在需要时运行。它不会替代 Playwright 的确定性断言：

```bash
set MIDSCENE_MODEL_BASE_URL=https://your-model-endpoint/v1
set MIDSCENE_MODEL_API_KEY=your-api-key
set MIDSCENE_MODEL_NAME=your-vl-model
set MIDSCENE_MODEL_FAMILY=qwen2.5-vl
pnpm e2e:midscene
```

## Agent 处理 Issue 的标准闭环

1. 阅读 issue、截图、日志、requestId、traceId 和用户复现步骤。
2. 在 `frontend/admin-vue3/e2e/issue-repro.spec.ts` 或新增同类 spec 中写出最小复现。
3. 运行 `pnpm e2e`，确认测试先失败，并保留 `test-results`、`playwright-report`、截图、视频和 trace。
4. 读取前端源码、后端日志、接口响应、数据库测试数据，定位 root cause。
5. 做最小修复。
6. 至少运行：
   - `pnpm e2e`
   - `pnpm ts:check`
   - 相关后端 `mvn test -Dtest=...`
7. PR 描述必须包含：复现用例、失败现象、root cause、修复说明、验证命令和剩余风险。

### 用户 Issue 到证据闭环

```text
用户 issue / 截图 / 日志
  -> 提取页面、账号角色、租户、复现步骤、期望和实际
  -> 编写 Playwright 最小失败复现
  -> 运行并保存 trace、截图、video、console/network 线索
  -> 基于失败证据查源码、接口、日志和数据边界
  -> 最小修复
  -> 复跑 Playwright、TypeScript、必要后端测试
  -> PR/最终报告列出证据与剩余风险
```

Midscene.js 可用于“页面是否符合语义预期”的辅助判断，例如登录页、表单布局、关键按钮是否明显可见。它只回答视觉/语义问题；提交前仍必须有 Playwright `expect`、后端单测或类型检查作为硬证据。

## Issue 必填信息

- 环境：local / dev / test / staging / prod
- 页面或入口 URL
- 用户角色、账号类型、租户
- 复现步骤
- 期望结果
- 实际结果
- 截图或录屏
- requestId / traceId / 发生时间
- 是否稳定复现

## 产物目录

- `test-results/`：失败截图、视频、trace
- `playwright-report/`：HTML 报告

这两个目录只作为本地和 CI 产物，不应提交到仓库。

## AI Coding Pre-PR 机制

本项目采用“先 AI 自查，再人工 Review”的 Pre-PR 机制。目标是过滤规范类、低级 Bug、异常处理、一致性、性能和测试覆盖问题，让人工 Review 聚焦业务语义和方案正确性。

### Pre-PR 执行顺序

1. 明确改动目标、业务入口和影响范围。
2. 让 AI 按 `agent_improvement/memory/codegen-rules.md`、`testing-specification.md` 和本文件进行自查。
3. 修复 AI 能明确指出且证据充分的问题。
4. 运行对应验证命令，记录结果。
5. 生成 PR/交付说明，再进入人工 Review。

### Pre-PR 文档模板

```markdown
## 改动目标

## 影响范围
- 后端模块：
- 前端页面：
- MCP Tool：
- 数据表：
- 外部平台：

## 风险等级
- P0/P1/P2：
- 判定原因：

## AI 自查结果
- 规范问题：
- 潜在 Bug：
- 异常处理：
- 性能/索引：
- 安全/权限/租户：
- 测试覆盖：

## 人工重点 Review
- 需要确认的业务语义：
- 需要确认的兼容性：
- 需要确认的资金/订单影响：

## 验证命令
- `mvn test -Dtest=...`
- `pnpm ts:check`
- `pnpm e2e`

## 剩余风险
```

### CPS 专项 Pre-PR 检查

- 是否触碰订单状态、返利计算、提现、冻结、风控等资金链路。
- 是否保留 tenantId、deleted、memberId 等数据边界。
- 是否有平台订单号、平台编码、租户等幂等键校验。
- 是否把平台差异限制在 `client/*`，没有扩散到 Controller/MCP Tool。
- MCP Tool 是否记录访问日志、耗时、异常和必要的参数摘要。
- 第三方平台失败、超时、空结果、限流是否有明确处理。
- 后台列表和统计接口是否检查分页、索引和导出范围。

## 渐进式重构 SOP

当业务需求触碰已有技术债时，优先采用“随需求顺带消化”的渐进式重构方式。

### 适用场景

- 修改订单同步、返利、提现、风控、平台适配、MCP Tool 等核心链路。
- 新需求会继续沿用旧模型、旧状态流转或旧平台字段映射。
- 技术债能在当前需求边界内被小步收口，不需要大规模停工重写。

### 执行步骤

1. 主 R 先圈定本次需求允许消化的技术债边界。
2. AI 扫描调用链，列出入口、Service、Mapper、表、外部平台和测试缺口。
3. 研发确认 P0/P1 风险，不让 AI 自行决定优先级。
4. 先完成一个最复杂或最典型路径的样板改造。
5. 将样板沉淀为可重复步骤，再扩展到同类代码。
6. 每一步都运行最小验证，避免一次性大改后难以定位问题。
7. PR 中明确“业务需求完成了什么”和“顺带消化了哪些技术债”。

### 不适用场景

- 数据库 schema 迁移、资金链路重算、历史订单批量修复等高风险变更。
- 需要改变认证授权、租户模型、结算口径的架构级调整。
- 当前需求时间不足以完成验证闭环，只能先记录债务并阻止继续扩散。
