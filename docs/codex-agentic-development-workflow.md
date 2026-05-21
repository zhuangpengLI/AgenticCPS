# Codex Agentic Development Workflow

> 适用范围：AgenticCPS 仓库内所有由 Codex、Superpowers、oh-my-codex、Playwright、Midscene.js 辅助完成的开发、修复、重构和文档更新。

## 1. 默认工作流

```text
读取上下文
  -> 对齐需求与风险
  -> 写计划或确认已有计划
  -> TDD 写失败测试
  -> 最小实现
  -> 分层验证
  -> 文档回写
  -> 最终报告证据和风险
```

- 每轮先确认 `git status --short`，保护已有未提交和未跟踪文件。
- 先读 `AGENTS.md`、`README.md`、`docs/project-map.md`。涉及测试时再读 `agent_improvement/memory/testing-specification.md` 和 `docs/e2e-agent-issue-workflow.md`。
- 需求、创意、行为变化优先走 Superpowers `brainstorming`；多步骤改造用 `writing-plans`；执行已有计划用 `executing-plans` 或直接按计划实施。
- 默认 solo execute。只有任务互相独立、写入范围清楚、验证方式明确时，才使用 Codex native subagents。
- Codex App outside tmux 不假定 OMX `team`、`hud`、`question` 可用；需要互动时使用当前平台可用的原生方式。

## 2. TDD 分层策略

| 改动类型 | 先写的测试 | 最小验证 |
|----------|------------|----------|
| CPS 资金/返利/兑换 | Service 单测或 DB 测试，覆盖幂等、冻结、扣减、失败解冻 | `mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=... "-Dsurefire.failIfNoSpecifiedTests=false"` |
| 订单同步/状态流转 | DB 测试，覆盖重复订单、乱序状态、租户隔离 | 指定订单/结算测试类 |
| MCP Tool | Tool 单测，覆盖参数校验、memberId 上下文、访问日志、错误归一 | 指定 MCP Tool 测试类 |
| 平台适配器 | Mock 响应解析测试，覆盖空结果、限流、签名失败 | 指定 client 测试类 |
| admin-vue3 页面 | Playwright 最小失败复现 | `pnpm ts:check` + `pnpm e2e` |
| 视觉/语义体验 | Midscene smoke 或专项 spec | `pnpm e2e:midscene` + Playwright 硬断言 |

测试必须先证明问题或需求边界，再实现。无法写测试时，要在最终报告中说明原因和替代验证。

## 3. E2E 与 Midscene.js

Playwright 是前端回归的主工具，负责稳定选择器、页面跳转、表单交互、接口 mock、trace、截图和 video。

Midscene.js 是辅助层，用于回答视觉和语义问题，例如：

- 页面是否明显是登录页、列表页、详情页。
- 关键字段、按钮和错误提示是否可见。
- 用户 issue 中的截图现象是否能被语义化复现。

Midscene.js 使用本机环境变量，不提交密钥：

```bash
set MIDSCENE_MODEL_BASE_URL=https://your-model-endpoint/v1
set MIDSCENE_MODEL_API_KEY=your-api-key
set MIDSCENE_MODEL_NAME=your-vl-model
set MIDSCENE_MODEL_FAMILY=qwen2.5-vl
```

运行：

```bash
cd frontend/admin-vue3
pnpm e2e:midscene
```

Midscene 结论只作为辅助证据。最终验收必须保留 Playwright `expect`、TypeScript、Maven 或其他确定性检查。

## 4. 质量门禁

- 后端：优先跑触达模块的最小 Maven 测试；资金/订单/MCP 改动必须覆盖失败、重试、幂等、租户、权限边界。
- 前端：页面改动至少跑 `pnpm ts:check`；用户 issue 修复必须有 Playwright 复现或说明无法自动化的原因。
- 文档：命令必须与实际 `package.json`、POM 模块名一致；中文文件必须保持 UTF-8 可解码。
- 安全：不提交 API Key、Token、Cookie、Midscene 模型密钥或真实平台凭证。
- 报告：最终说明改了什么、验证了什么、未验证什么、剩余风险是什么。

## 5. 微信文章校准入口

原计划引用的微信文章链接在当前环境返回“环境异常，完成验证后即可继续访问”，无法读取原文。当前工作流采用通用 Agentic Coding 最佳实践。后续如果提供文章原文或要点，需要对照本文档补充：

- 是否有新的 Codex prompt / AGENTS 写法建议。
- 是否有新的 Superpowers / OMX 编排建议。
- 是否有新的 TDD、E2E、Midscene 质量门禁建议。
- 是否需要调整 README 面向用户的表达。
