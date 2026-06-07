# AgenticCPS MCP Server 接入说明

本文说明外部 AI Agent 如何接入 AgenticCPS 暴露的 MCP Server，并记录 Codex 的本地接入配置。

## 1. 当前服务形态

AgenticCPS 使用 Spring AI MCP Server 暴露 CPS / CPX 工具。以当前代码配置为准：

- Server 名称：`cps-mcp-server`
- 本地后端端口：`48080`
- MCP SSE 连接地址：`http://127.0.0.1:48080/sse`
- MCP 消息端点：`http://127.0.0.1:48080/mcp/message`
- 配置来源：`backend/qiji-server/src/main/resources/application.yaml`
- 本地端口来源：`backend/qiji-server/src/main/resources/application-local.yaml`

旧需求文档中曾出现 `/mcp/cps` 作为 Streamable HTTP 入口；当前可运行配置没有声明该路径，外部 Agent 接入时优先使用 `/sse`。

## 2. 启动 AgenticCPS 后端

在项目根目录执行：

```powershell
cd backend
mvn spring-boot:run -pl qiji-server -Dspring-boot.run.profiles=local
```

依赖服务：

- MySQL：`127.0.0.1:3306/cps`
- Redis：`127.0.0.1:6379`
- 本地后端：`http://127.0.0.1:48080`

启动成功后，外部 Agent 应连接：

```text
http://127.0.0.1:48080/sse
```

如果部署到远程环境，将 host 替换为实际域名或内网地址，例如：

```text
https://cps.example.com/sse
```

## 3. 外部 Agent 接入方式

### 3.1 Codex

本仓库已提供项目级 Codex 配置：

```toml
[mcp_servers.agenticcps]
type = "stdio"
command = "cmd"
args = ["/c", "npx", "-y", "mcp-remote@latest", "http://127.0.0.1:48080/sse", "--transport", "sse-only"]
startup_timeout_sec = 30
enabled = true
```

说明：

- Codex 通过 `mcp-remote` 把本地 SSE MCP Server 转成 stdio MCP Server。
- 使用前必须先启动 AgenticCPS 后端，否则 Codex 启动该 MCP server 时会连接失败。
- 如果 Codex 只读取用户全局配置，可将上述片段复制到 `C:\Users\<用户名>\.codex\config.toml` 的 `[mcp_servers]` 区域下。

### 3.2 Claude Desktop / 其他 stdio-only Agent

如果 Agent 只支持 stdio MCP，也可以使用 `mcp-remote` 桥接：

```json
{
  "mcpServers": {
    "agenticcps": {
      "command": "cmd",
      "args": [
        "/c",
        "npx",
        "-y",
        "mcp-remote@latest",
        "http://127.0.0.1:48080/sse",
        "--transport",
        "sse-only"
      ]
    }
  }
}
```

远程部署时替换 URL：

```json
"https://cps.example.com/sse"
```

### 3.3 Spring AI MCP Client

如果外部 Java / Spring AI 应用直接作为 MCP Client，可按 Spring AI SSE client 配置：

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: external-agent
        sse:
          connections:
            agenticcps:
              url: http://127.0.0.1:48080
              sse-endpoint: /sse
```

## 4. 可用工具

当前 `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/tool` 下注册的工具包括：

| 工具名 | 用途 |
|---|---|
| `cps_search_goods` | 多平台商品搜索 |
| `cps_compare_prices` | 跨平台比价，返回低价、高返利、综合最优候选 |
| `cps_generate_link` | 生成 CPS 推广 / 返利链接 |
| `cps_query_orders` | 查询会员 CPS 订单 |
| `cps_get_rebate_summary` | 查询会员返利账户汇总 |
| `cps_recommend_by_scene` | 按 AIoT / 场景需求推荐商品 |
| `cps_purchase_decision` | 基于 CPS 候选商品和海纳证据生成购买决策建议 |
| `cps_list_selection_themes` | 查询已发布选品主题 |
| `cps_recommend_from_selection_theme` | 从选品主题中推荐商品 |
| `cps_get_rebate_balance` | 查询可兑换返利余额 |
| `cps_create_token_exchange` | 创建返利兑换 Token 订单 |
| `cps_query_exchange_status` | 查询返利兑换 Token 状态 |
| `cpx_list_tasks` | 查询 CPX 任务列表 |
| `cpx_get_task_detail` | 查询 CPX 任务详情 |
| `cpx_generate_tracking_link` | 生成 CPX 跟踪链接 |
| `cpx_query_conversions` | 查询 CPX 转化记录 |
| `cpx_recommend_tasks_by_scene` | 按场景推荐 CPX 任务 |
| `cpx_search_articles` | 搜索 CPX 内容 / 资讯 |

## 5. 调用建议

### 搜索商品

适合用户自然语言需求，如“帮我找一款 300 元以内的无线鼠标”：

```json
{
  "keyword": "无线鼠标",
  "platformCode": "jd",
  "minPrice": 0,
  "maxPrice": 30000,
  "pageNo": 1,
  "pageSize": 10
}
```

金额字段在业务中通常按分或平台原始金额处理，外部 Agent 展示给用户前应确认返回字段单位。

### 生成推广链接

转链会影响订单归因和返利归属。外部 Agent 不应让用户输入任意 `memberId` 来覆盖可信上下文；应优先使用 MCP ToolContext、登录态、服务端签名或网关注入的可信会员身份。

### 返利兑换 Token

兑换属于资金链路，只能处理 `AVAILABLE` 可用返利。外部 Agent 在调用兑换前应先查询余额，并向用户明确兑换金额、目标资产、幂等键和失败处理策略。

## 6. 鉴权与生产部署

当前 Spring Security 配置对 MCP SSE / message 端点执行 `permitAll`，便于本地开发接入。

生产环境建议：

- 在网关层增加 API Key、Bearer Token、mTLS 或内网访问控制。
- 如果启用项目内 `cps_mcp_api_key` 表，应校验 key 状态、过期时间、权限级别和租户。
- 对所有 Tool 调用保留审计日志：工具名、参数摘要、成员上下文、耗时、状态、错误原因、客户端 IP。
- 对转链、订单、返利、Token 兑换等会员资产相关工具，只信任登录上下文或服务签名，不信任请求体里的 `memberId` / `userId`。

## 7. 排障清单

- Codex / Agent 报连接失败：确认后端已启动，且 `http://127.0.0.1:48080/sse` 可访问。
- 工具列表为空：确认 `spring.ai.mcp.server.enabled=true`，并检查 CPS Tool Bean 是否被 Spring 扫描。
- 本地能连，远程不能连：确认防火墙、反向代理是否允许 SSE 长连接。
- 工具调用返回业务失败：先查后端日志和 `cps_mcp_access_log`，再确认平台适配器、供应商配置、会员上下文和租户上下文。
- Codex 启动慢：`mcp-remote@latest` 首次会通过 `npx` 下载依赖，可在联网环境预热一次。
