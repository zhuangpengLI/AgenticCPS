# AI 对话接入 CPS MCP 本机回环设计

## 目标

让本机管理端 AI 对话能够通过标准 Spring AI MCP Client 调用同一进程暴露的 CPS MCP Server，并以一次只读商品搜索证明完整链路。

## 范围

- 启用本地 MCP Client。
- 新增名为 `cps` 的 SSE 回环连接，服务地址为 `http://127.0.0.1:48080`、SSE 端点为 `/sse`。
- 新建独立的“CPS MCP 联调测试”AI 角色并绑定 `cps`；不修改已有角色。
- 使用该角色和纯文本模型发送只读商品搜索请求。
- 以 `cps_mcp_access_log` 的新增 `cps_search_goods` 记录作为验收证据。

## 非目标

- 不调用转链、Token 兑换、返利、订单或任何资产写入工具。
- 不修改 CPS MCP Server 的工具实现。
- 不改变既有 AI 角色或生产环境配置。

## 运行流程

1. AI 对话根据角色的 `mcpClientNames` 选择 `cps` MCP Client。
2. MCP Client 经本机 SSE 连接获取 CPS MCP 工具清单。
3. 模型选择 `cps_search_goods` 并发起调用。
4. CPS 工具写入审计记录；对话返回工具结果或基于工具结果的回答。

## 错误处理与验收

- 服务重启后确认 MCP Client 已成功创建。
- 若模型未选择工具，使用更明确的商品搜索提示重试；不执行写操作。
- 成功条件：AI 对话完成，且审计表新增一条本次时间范围内的 `cps_search_goods` 成功记录。
