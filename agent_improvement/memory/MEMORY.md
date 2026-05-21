# Claude Memory Index

本目录存储 Claude Code 的规则和记忆文件。

## 文件

- [codegen-rules.md](codegen-rules.md) - 代码生成器模板规则与 CPS AI Coding 约束
- [cps-ai-coding-rules.md](cps-ai-coding-rules.md) - CPS always 级 AI 可执行约束
- [testing-specification.md](testing-specification.md) - 单元测试、集成测试与 CPS Human-in-the-loop 测试规范

## 规则说明

### codegen-rules.md
基于 `yudao-module-infra/src/main/resources/codegen` Velocity 模板库总结的业务系统代码生成规范。

包含：
- 后端：DO/Mapper/Service/Controller/VO 分层结构
- 前端：Vue3 Element Plus、Vue3 Vben Admin、Vben5 Antd、UniApp 移动端模板
- 命名约定（PascalCase/camelCase/kebab-case）
- 模板类型：通用(1)、树表(2)、ERP主表(11)
- 主子表处理逻辑
- VO 类型：PageReqVO/ListReqVO/SaveReqVO/RespVO

### cps-ai-coding-rules.md
CPS 模块专用 always 级规则，适用于 `backend/qiji-module-cps` 的平台适配、订单同步、返利、冻结、Mapper、MCP Tool 和前端 CRUD 改动。

包含：
- 真实 `qiji-*` 模块路径和旧 `yudao-*` 命名漂移处理
- UTF-8 文档读写与乱码防护
- 金额单位、租户隔离、软删除、memberId 信任边界
- 订单状态机、幂等、退款/失效、冻结/扣减规则
- MCP Tool 参数校验、访问日志、异常结构规则
