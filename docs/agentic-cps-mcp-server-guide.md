# AgenticCPS MCP Server 接入说明

本文说明外部 AI Agent 如何接入 AgenticCPS 暴露的 MCP Server，并记录 Codex 的本地接入配置。

结论：Codex 可以调用本系统 MCP。当前仓库已经有项目级配置 `.codex/config.toml`，本机全局配置 `C:\Users\zhuangpengli\.codex\config.toml` 也已经追加了 `agenticcps` MCP server。使用前只需要先启动 AgenticCPS 后端，然后重启或刷新 Codex 会话，让 Codex 重新加载 MCP server。

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

Codex 官方支持把 MCP server 配到 `config.toml` 中。用户级配置默认在 `~/.codex/config.toml`，可信项目也可以放项目级 `.codex/config.toml`；Codex CLI、IDE 扩展和 Codex App 共用这些 MCP 配置。

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
- 本机当前已经在 `C:\Users\zhuangpengli\.codex\config.toml` 追加了同名配置；新机器或新用户需要自行复制。

#### 3.1.1 Codex 启用步骤

1. 启动后端：

```powershell
cd F:\ai\AgenticCPS\backend
mvn spring-boot:run -pl qiji-server -Dspring-boot.run.profiles=local
```

2. 确认 SSE 端点可连接：

```powershell
curl.exe -N http://127.0.0.1:48080/sse
```

看到 `text/event-stream` 或连接保持不退出，说明 SSE 端点已启动。`/mcp/message` 是 MCP 消息端点，不建议用浏览器 GET 判断；它通常由 MCP client 按协议 POST 调用，浏览器 GET 会返回 405 提示。

3. 重启 Codex App / IDE 扩展，或重新打开 Codex CLI 会话。

4. 在 Codex CLI 的 TUI 中输入：

```text
/mcp
```

确认 `agenticcps` server 处于已连接状态，并且工具列表包含 `cps_search_goods`、`cps_compare_prices`、`cps_generate_link` 等 CPS 工具。

#### 3.1.2 Codex 中的使用方式

在 Codex 里直接用自然语言提出需要即可，例如：

```text
用 AgenticCPS MCP 搜索 300 元以内的无线鼠标，优先京东和淘宝，给我按券后价和返利排序。
```

```text
调用 AgenticCPS MCP 查询我的可兑换返利余额。
```

```text
基于 AgenticCPS MCP 的选品主题，推荐适合办公室节能改造的商品，不要自动转链。
```

Codex 会根据工具名、描述和入参 schema 决定是否调用 `agenticcps` MCP。涉及转链、订单、返利、Token 兑换时，必须确认后端能提供可信会员上下文；不能把用户口头提供的 `memberId` 当作资产归属依据。

#### 3.1.3 当前会话工具不匹配时怎么办

如果 Codex 的 MCP 面板或工具发现结果里出现 `ps_get_all_persons`、`ps_create_person` 这类示例工具，而不是 `cps_*` / `cpx_*` 工具，说明当前会话没有连到正确的 AgenticCPS MCP 工具集，常见原因有：

- 后端启动时加载的不是当前 `qiji-server` 配置；
- Codex 会话在后端启动前已经加载了旧 MCP server；
- `mcp-remote` 连接到了其他测试 SSE server；
- 项目级和全局 `config.toml` 中存在同名 server，但内容不一致；
- Spring AI MCP server 的 Tool Bean 未被扫描或启动失败。

处理顺序：

1. 停掉错误的后端或测试 MCP server。
2. 启动 `F:\ai\AgenticCPS\backend\qiji-server`。
3. 确认 `http://127.0.0.1:48080/sse` 返回 SSE 连接。
4. 检查 `.codex/config.toml` 和 `C:\Users\<用户名>\.codex\config.toml` 中的 `agenticcps` URL 都是 `http://127.0.0.1:48080/sse`。
5. 重启 Codex App / IDE 扩展，或新开 Codex CLI 会话。
6. 再用 `/mcp` 确认工具列表。

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
        toolcallback:
          # 工具列表按需加载，避免远程 SSE 不可用时阻塞 Java 应用启动
          enabled: false
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
- 工具列表变成 `ps_*` 示例工具：当前 Codex 会话连错服务或加载了旧 server；按 3.1.3 重启后端和 Codex 会话。
- 本地能连，远程不能连：确认防火墙、反向代理是否允许 SSE 长连接。
- 工具调用返回业务失败：先查后端日志和 `cps_mcp_access_log`，再确认平台适配器、供应商配置、会员上下文和租户上下文。
- Codex 启动慢：`mcp-remote@latest` 首次会通过 `npx` 下载依赖，可在联网环境预热一次。

## 8. 参考资料

- Codex MCP 配置：<https://developers.openai.com/codex/mcp>
- Codex 配置基础与优先级：<https://developers.openai.com/codex/config-basic>
- Codex `config.toml` 参考：<https://developers.openai.com/codex/config-reference>
