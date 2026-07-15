# CPS Pre-PR Template

> 用于 AgenticCPS 中所有触碰 CPS 的代码、SQL、配置、MCP Tool、前端页面和规则文档改动。提交前先由 AI 填写，再进入人工 Review。

## 改动目标

- 目标：
- 背景 issue / 需求：
- 本次不做：

## 涉及模块

- 后端模块：
- 前端页面：
- MCP Tool：
- 定时任务：
- 平台适配器：
- 数据表 / 索引：
- 外部系统：

## 影响接口

- Admin API：
- App API：
- OpenAPI：
- MCP Tool：
- 定时任务参数：

## 风险等级

- P0/P1/P2：
- 判定原因：
- 涉及技术债 ID：

## 调用链证据

```text
入口
  -> Service
  -> Mapper / Client
  -> 表 / 外部平台
```

关键文件：

- 

## 数据库影响

- 新增表：
- 修改表：
- 新增/修改索引：
- 数据迁移：
- 回滚方式：

## 租户 / 权限 / 软删除检查

- tenantId 边界：
- deleted 边界：
- memberId / userId 来源：
- Admin 权限：
- App 登录上下文：
- OpenAPI 签名：
- MCP ToolContext：

## 金额单位检查

- 新增金额字段：
- 现有金额字段：
- 单位：分 / 元 / 万分比 / 百分比
- 是否引入 `Double`：
- 是否新增业务模型 `BigDecimal`：
- 兼容/迁移说明：

## 幂等与并发检查

- 幂等键：
- 唯一约束：
- 重复提交：
- 并发结算：
- 超时补偿：
- 退款/失效：
- 状态回退保护：

## 平台适配检查

- 平台差异是否只在 `client/*`：
- unsupported capability 是否显式失败：
- active vendor 是否校验启用状态：
- 平台/API 失败是否区别于空结果：
- 新增/修改测试桩：

## MCP 检查

- 参数校验：
- pageSize / pageNo 下界与上界：
- memberId 是否来自可信上下文：
- API Key 状态/过期校验：
- 访问日志：
- 参数摘要脱敏：
- 耗时：
- 失败原因：
- 错误结构：

## AI 自查发现的问题与修复记录

| 问题 | 等级 | 是否已修 | 证据 |
|------|------|----------|------|
| | | | |

## 测试命令与结果

```bash
# 示例
python script/check_utf8_integrity.py
python -m pytest script/test/test_check_utf8_integrity.py

cd backend
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=ClassName "-Dsurefire.failIfNoSpecifiedTests=false"
```

结果：

- 

## UTF-8 自动治理闸门

- [ ] 本地执行 `python script/check_utf8_integrity.py` 通过
- [ ] 本地执行 `python -m pytest script/test/test_check_utf8_integrity.py` 通过
- [ ] GitHub Actions `UTF-8 Integrity` 通过
- 扫描文件数：
- 本地执行时间：
- CI 运行链接：
- 例外说明（仅允许登记经过审计的 mojibake 误报，不允许豁免非法 UTF-8、BOM 或 U+FFFD）：

## 需要人工重点 Review 的业务语义

- 

## 剩余风险

- 

## 文档与规则同步

- 是否更新 `docs/cps-tech-debt-inventory.md`：
- 是否更新 `agent_improvement/memory/cps-ai-coding-rules.md`：
- 是否检查旧 `yudao-*` 路径：
- 是否完成 UTF-8 自动治理闸门并附证据：
