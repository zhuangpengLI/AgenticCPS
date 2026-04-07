# AI 自主编程平台

<cite>
**本文引用的文件**
- [README.md](file://README.md)
- [backend/README.md](file://backend/README.md)
- [docs/CPS系统PRD文档.md](file://docs/CPS系统PRD文档.md)
- [openspec/config.yaml](file://openspec/config.yaml)
- [agent_improvement/memory/MEMORY.md](file://agent_improvement/memory/MEMORY.md)
- [agent_improvement/memory/codegen-rules.md](file://agent_improvement/memory/codegen-rules.md)
- [backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java](file://backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java)
- [backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java](file://backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java)
- [.claude/skills/openspec-apply-change/openspec-apply-change.md](file://.claude/skills/openspec-apply-change/openspec-apply-change.md)
- [.claude/commands/opsx/apply.md](file://.claude/commands/opsx/apply.md)
</cite>

## 更新摘要
**所做更改**
- 新增 MCP（Model Context Protocol）协议集成指南章节
- 完善 Vibe Coding 开发范式的理论基础和实践指导
- 增强 AI 代理协作工作流的详细说明
- 补充代码生成器的实现机制和扩展指南
- 新增 MCP 工具权限管理和访问控制机制
- 完善性能指标和故障排查指南

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [MCP 协议集成指南](#mcp-协议集成指南)
7. [Vibe Coding 开发范式](#vibe-coding-开发范式)
8. [AI 代理协作工作流](#ai-代理协作工作流)
9. [代码生成器实现机制](#代码生成器实现机制)
10. [依赖关系分析](#依赖关系分析)
11. [性能考量](#性能考量)
12. [故障排查指南](#故障排查指南)
13. [结论](#结论)
14. [附录](#附录)

## 简介
本项目是一个"AI 自主编程平台"，以 Vibe Coding（氛围编程）为核心开发范式，融合"低代码 + AI 自主编程"，目标是让一个人也能拥有"产品经理 + 架构师 + 全栈开发 + 测试工程师 + 运维工程师"的综合能力。平台通过规范化 AI 编程工作流（Specs/Plans）、AI 代理协作、MCP（Model Context Protocol）协议集成，实现"用自然语言描述需求，AI 自动完成从编码到测试再到交付"的闭环。

- 平台特性
  - Vibe Coding：以"意图"驱动，AI 自动理解 → 编码 → 测试 → 交付
  - 规范化 AI 编程：Specs/Plans/Agents/Skills 四大要素，确保质量与一致性
  - MCP 零代码接入：AI Agent 无需写代码即可调用平台工具
  - 低代码：代码生成器、可视化工作流、报表与大屏设计器
  - CPS 联盟返利系统：聚合淘宝、京东、拼多多、抖音等平台，提供搜索、比价、推广链接生成、订单追踪与结算

- 适用人群
  - 想做电商返利副业但不会写代码的人
  - 一人公司（OPC）创业者
  - 自由职业者/数字游民
  - 个人开发者/小型工作室

- 项目定位
  - 开箱即用的智能 CPS 联盟返利与导购平台
  - 100% 由 AI 自主编程完成的 CPS 核心模块（20,000+ 行代码）

**章节来源**
- [README.md:1-523](file://README.md#L1-L523)
- [backend/README.md:39-129](file://backend/README.md#L39-L129)

## 项目结构
项目采用多模块分层架构，后端基于 Spring Boot 3.x，前端包含 Vue3 管理后台与 UniApp 移动端，基础设施模块提供缓存、消息队列、定时任务等能力。CPS 模块是平台的核心业务域，包含 API 定义层、业务实现层、平台适配器、数据访问层、定时任务与 MCP 接口层。

- 后端模块概览
  - yudao-dependencies：Maven 依赖版本管理
  - yudao-framework：框架扩展（安全、缓存、权限、多租户等）
  - yudao-server：主服务容器
  - yudao-module-*：各业务模块（系统管理、会员中心、支付、商城、AI、报表、CPS 等）
  - sql：各模块数据库脚本
- 前端模块概览
  - admin-vue3：Vue3 管理后台
  - admin-uniapp：uni-app 移动端管理
  - mall-uniapp：电商移动端应用
- AI 与规范
  - openspec：规范驱动的项目上下文与规则
  - agent_improvement/memory：代码生成规则与 Claude 记忆

```mermaid
graph TB
subgraph "后端"
server["yudao-server"]
framework["yudao-framework"]
cps_api["yudao-module-cps-api"]
cps_biz["yudao-module-cps-biz"]
ai_mod["yudao-module-ai"]
member["yudao-module-member"]
pay["yudao-module-pay"]
mall["yudao-module-mall"]
infra["yudao-module-infra"]
system["yudao-module-system"]
report["yudao-module-report"]
sql["sql/*"]
end
subgraph "前端"
admin_vue3["admin-vue3"]
admin_uniapp["admin-uniapp"]
mall_uniapp["mall-uniapp"]
end
subgraph "AI 与规范"
openspec["openspec/config.yaml"]
memory["agent_improvement/memory/*"]
end
server --> framework
server --> cps_api
server --> cps_biz
server --> ai_mod
server --> member
server --> pay
server --> mall
server --> infra
server --> system
server --> report
server --> sql
admin_vue3 --> server
admin_uniapp --> server
mall_uniapp --> server
openspec --> memory
memory --> server
```

**图表来源**
- [README.md:229-249](file://README.md#L229-L249)
- [backend/README.md:11-57](file://backend/README.md#L11-L57)

**章节来源**
- [README.md:229-249](file://README.md#L229-L249)
- [backend/README.md:11-57](file://backend/README.md#L11-L57)

## 核心组件
- 规范化 AI 编程工作流
  - Specs：技术标准、架构约束、代码风格
  - Plans：任务分解、验收标准、交付清单
  - Agents：角色定义、职责边界、协作流程
  - Skills：可复用技能、代码模板、最佳实践
- AI 代理协作
  - 基于角色与职责的协作流程，确保 AI 在理解需求后按计划执行
- MCP（Model Context Protocol）集成
  - 通过 JSON-RPC 2.0 over Streamable HTTP 在 /mcp/cps 提供 AI 工具与资源
  - 提供搜索、比价、推广链接生成、订单查询、返利汇总等工具
- 低代码能力
  - 代码生成器：一键生成前后端代码、SQL、Swagger 文档、单元测试
  - 可视化工作流：基于 Flowable 在线设计审批流程
  - 报表与大屏：拖拽生成数据报表、图形报表、大屏与打印模板
- CPS 联盟返利系统
  - 多平台接入：淘宝、京东、拼多多、抖音
  - 核心能力：商品搜索与比价、会员返利体系、订单全链路追踪、提现管理、风控管理
  - 定时任务：订单同步、状态更新、结算入账
  - MCP 接口：AI Agent 直接调用

**章节来源**
- [README.md:113-210](file://README.md#L113-L210)
- [backend/README.md:179-205](file://backend/README.md#L179-L205)

## 架构总览
平台整体采用分层架构，后端以 Spring Boot 为核心，前端采用 Vue3 + Element Plus 和 UniApp，基础设施模块提供缓存、消息队列、定时任务、监控等能力。CPS 模块通过策略模式接入多个平台适配器，并通过 MCP 提供 AI 工具与资源。

```mermaid
graph TB
user["用户/AI Agent"]
frontend["前端应用<br/>admin-vue3 / admin-uniapp / mall-uniapp"]
server["后端服务<br/>yudao-server"]
framework["框架扩展<br/>yudao-framework"]
cps_api["CPS API 定义层"]
cps_biz["CPS 业务实现层<br/>controller/admin + controller/app + service + client + dal + job + mcp"]
ai_mod["AI 模块<br/>Spring AI + MCP 支持"]
infra["基础设施模块<br/>Redis/消息队列/定时任务/监控"]
db["数据库"]
mq["消息队列"]
redis["缓存"]
user --> frontend
frontend --> server
server --> framework
server --> cps_api
server --> cps_biz
server --> ai_mod
server --> infra
server --> db
server --> mq
server --> redis
cps_biz --> db
cps_biz --> mq
cps_biz --> redis
ai_mod --> server
```

**图表来源**
- [README.md:229-249](file://README.md#L229-L249)
- [backend/README.md:11-57](file://backend/README.md#L11-L57)

**章节来源**
- [README.md:229-249](file://README.md#L229-L249)
- [backend/README.md:11-57](file://backend/README.md#L11-L57)

## 详细组件分析

### Vibe Coding 开发范式
- 核心理念
  - 以"意图"驱动，AI 自动完成从理解 → 编码 → 测试 → 交付的全过程
  - 通过 Specs/Plans/Agents/Skills 四大要素，确保 AI 编程的质量与一致性
- 工作流程
  - 需求对齐 → 方案设计 → 自主编码 → 验收交付
  - 用户参与需求与方案确认，AI 自动完成编码与测试，最终输出文档与可运行代码

```mermaid
flowchart TD
Start(["开始"]) --> ReadSpecs["读取 Specs<br/>技术标准/架构约束/代码风格"]
ReadSpecs --> Plan["设计 Plans<br/>任务分解/验收标准/交付清单"]
Plan --> Agent["AI 代理协作<br/>角色定义/职责边界/协作流程"]
Agent --> CodeGen["AI 自主编码<br/>生成控制器/服务/映射/前端页面/测试"]
CodeGen --> AutoTest["自动测试<br/>单元测试/集成测试"]
AutoTest --> Docs["文档输出<br/>API 文档/变更记录"]
Docs --> Review["用户验收"]
Review --> End(["结束"])
```

**图表来源**
- [backend/README.md:80-144](file://backend/README.md#L80-L144)

**章节来源**
- [backend/README.md:80-144](file://backend/README.md#L80-L144)

### 规范化 AI 编程工作流（Specs/Plans）
- Specs：定义技术标准、架构约束与代码风格，确保 AI 输出符合项目规范
- Plans：明确任务分解、验收标准与交付清单，避免"AI 乱写代码"
- 优势
  - 需求精准对齐：避免偏差
  - 方案先行：先设计再编码，零返工
  - 纯 AI 自主编程：效率提升 10 倍以上
  - 质量可保障：自动测试 + 规范约束 + 验收标准
  - 持续自进化：每次项目反馈优化 Specs/Plans

**章节来源**
- [backend/README.md:107-144](file://backend/README.md#L107-L144)

### AI 代理协作流程
- 角色与职责
  - 不同 AI 代理负责不同领域（如编码、测试、文档、运维），通过明确的职责边界协作
- 协作机制
  - 基于 Plans 的任务分解与验收标准，确保各代理按计划协同
  - 通过规范化的工具与接口，降低协作成本

**章节来源**
- [backend/README.md:107-144](file://backend/README.md#L107-L144)

### AI 编码助手与代码生成器
- AI 编码助手
  - 全栈 AI 程序员，支持"加一个商品收藏功能"、"接入唯品会联盟"、"优化搜索性能"等场景
- 代码生成器
  - 输入数据库表结构，一键生成：Java 控制器/服务/映射/DO/VO、Vue3 前端页面、SQL 建表脚本、Swagger 文档、单元测试代码
  - 支持单表、树表、主子表三种模式
- 代码生成规则
  - 基于 Velocity 模板库，定义分层结构、命名约定、DO/Mapper/Service/Controller/VO 规范、模板类型（通用/树表/ERP 主表）等
  - 前端支持 Vue3 Element Plus、Vben Admin、Vben5 Antd、UniApp 移动端模板

```mermaid
flowchart TD
Table["输入：数据库表结构"] --> Gen["代码生成器<br/>生成后端/前端/文档/测试"]
Gen --> Backend["后端：Controller/Service/Mapper/DO/VO"]
Gen --> Frontend["前端：列表/表单/详情页面"]
Gen --> SQL["SQL 建表脚本"]
Gen --> Swagger["Swagger API 文档"]
Gen --> UnitTest["单元测试代码"]
```

**图表来源**
- [agent_improvement/memory/codegen-rules.md:1-788](file://agent_improvement/memory/codegen-rules.md#L1-L788)

**章节来源**
- [agent_improvement/memory/codegen-rules.md:1-788](file://agent_improvement/memory/codegen-rules.md#L1-L788)

### CPS 联盟返利系统
- 多平台接入
  - 淘宝、京东、拼多多、抖音联盟适配器，策略模式可插拔扩展
- 核心能力
  - 商品搜索与比价、推广链接生成、订单全链路追踪、会员返利体系、提现管理、风控管理
- 定时任务
  - 订单同步、状态更新、结算入账
- MCP 工具
  - cps_search_goods、cps_compare_prices、cps_generate_link、cps_query_orders、cps_get_rebate_summary

```mermaid
classDiagram
class CpsPlatformClient {
+getPlatformCode()
+searchGoods(request)
+getGoodsDetail(request)
+parseContent(content)
+generatePromotionLink(request)
+queryOrders(request)
+testConnection()
}
class TaobaoAdapter
class JingDongAdapter
class PinduoduoAdapter
class DouYinAdapter
CpsPlatformClient <|.. TaobaoAdapter
CpsPlatformClient <|.. JingDongAdapter
CpsPlatformClient <|.. PinduoduoAdapter
CpsPlatformClient <|.. DouYinAdapter
```

**图表来源**
- [backend/README.md:143-159](file://backend/README.md#L143-L159)

**章节来源**
- [backend/README.md:143-159](file://backend/README.md#L143-L159)

## MCP 协议集成指南

### 协议概述
MCP（Model Context Protocol）协议为 AI Agent 提供了标准化的工具调用接口，实现真正的零代码接入。平台通过 JSON-RPC 2.0 over Streamable HTTP 在 /mcp/cps 端点提供 AI 工具与资源。

### 工具与资源分类
- Tools（工具）
  - 可调用函数：cps_search_goods、cps_compare_prices、cps_generate_link、cps_query_orders、cps_get_rebate_summary
  - 支持并发调用和参数验证
- Resources（资源）
  - 只读数据源：平台配置、返利规则、统计数据
  - 支持权限控制和访问统计
- Prompts（提示词）
  - 预定义交互模板，提升对话质量

### 访问权限管理
平台提供完善的 API Key 管理机制：

```mermaid
flowchart TD
APIKey["API Key 管理"] --> Create["创建 API Key"]
Create --> Config["配置权限级别"]
Config --> Public["public<br/>仅查询权限"]
Config --> Member["member<br/>会员级操作权限"]
Config --> Admin["admin<br/>管理权限"]
Public --> RateLimit["配置限流规则"]
Member --> RateLimit
Admin --> RateLimit
RateLimit --> Monitor["使用统计与监控"]
```

**图表来源**
- [backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java:1-61](file://backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java#L1-L61)

### MCP 工具调用流程
```mermaid
sequenceDiagram
participant Agent as "AI Agent"
participant MCP as "MCP 服务 (/mcp/cps)"
participant Biz as "CPS 业务层"
Agent->>MCP : "tools/call"<br/>{"name" : "...", "arguments" : {...}}
MCP->>Biz : "路由到对应工具方法"
Biz-->>MCP : "返回结果"
MCP-->>Agent : "JSON-RPC 响应"
```

**图表来源**
- [backend/README.md:179-205](file://backend/README.md#L179-L205)

### 访问日志与监控
平台提供完整的访问日志记录机制：

| 字段名称 | 数据类型 | 说明 |
|---------|---------|------|
| apiKeyId | Long | API Key ID（NULL=匿名访问） |
| toolName | String | 调用的Tool名称 |
| requestParams | String | 请求参数（JSON） |
| responseData | String | 响应数据摘要 |
| status | Integer | 调用状态（0失败 1成功） |
| errorMessage | String | 错误信息（失败时记录） |
| durationMs | Integer | 耗时（毫秒） |
| clientIp | String | 客户端IP |

**章节来源**
- [backend/README.md:179-205](file://backend/README.md#L179-L205)
- [backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java:1-61](file://backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpApiKeyDO.java#L1-L61)
- [backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java:1-63](file://backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/cn/iocoder/yudao/module/cps/dal/dataobject/mcp/CpsMcpAccessLogDO.java#L1-L63)

## Vibe Coding 开发范式

### 理念阐述
Vibe Coding（氛围编程）是一种革命性的软件开发范式，其核心理念是"你不写代码，你描述 Vibe（氛围/意图/感觉），AI 把它变成可运行的软件"。

### 工作流程详解
```mermaid
flowchart TD
Intent["描述意图"] --> AIUnderstand["AI 理解需求"]
AIUnderstand --> AICode["AI 编码实现"]
AICode --> AITest["AI 自动测试"]
AITest --> AIDelivery["AI 交付成果"]
AIUnderstand -.-> UserReview["用户审核"]
AITest -.-> UserAcceptance["用户验收"]
UserReview --> AIDelivery
UserAcceptance --> Complete["项目完成"]
```

### 规范化工作流
平台引入了基于 Specs/Plans 的规范化 AI 编程工作流：

```
.qoder/
├── specs/      # 编码规范：技术标准、架构约束、代码风格
├── plans/      # 实施计划：任务分解、验收标准、交付清单
├── agents/     # AI 代理：角色定义、职责边界、协作流程
└── skills/     # 可复用技能：代码模板、最佳实践、经验沉淀
```

**工作流程**：
```
需求对齐           方案设计          自主编码           验收交付
┌─────────┐    ┌─────────┐    ┌──────────┐    ┌─────────┐
│ 读取 Specs │ → │ 设计方案  │ → │ AI 自主编码 │ → │ 自动测试  │
│ 解析 Plans │    │ 生成计划  │    │ 生成测试代码 │    │ 验收报告  │
│ 用户确认   │    │ 用户确认  │    │ 规范遵循   │    │ 文档输出  │
└─────────┘    └─────────┘    └──────────┘    └─────────┘
      你参与             你参与          AI 自动完成          你验收
```

### 核心优势
- **需求精准对齐**：Specs/Plans 确保 AI 理解无偏差，告别"AI 乱写代码"
- **方案先行**：先设计 → 再确认 → 后编码，零返工
- **纯 AI 自主编程**：需求到代码全流程 AI 化，效率提升 10 倍以上
- **质量可保障**：自动测试 + 规范约束 + 验收标准，代码质量可控
- **持续自进化**：每次项目反馈自动优化 Specs/Plans，越用越聪明

**章节来源**
- [backend/README.md:78-144](file://backend/README.md#L78-L144)

## AI 代理协作工作流

### 代理角色定义
平台采用多代理协作模式，每个 AI 代理都有明确的角色和职责边界：

- **需求分析师**：负责需求收集、分析和规格化
- **架构师**：负责系统设计、技术选型和架构决策
- **全栈程序员**：负责代码实现、单元测试和集成测试
- **运维专家**：负责部署、监控和故障处理
- **文档工程师**：负责技术文档、API 文档和用户手册

### 协作流程
```mermaid
sequenceDiagram
participant User as "用户"
participant Analyst as "需求分析师"
participant Architect as "架构师"
participant Developer as "全栈程序员"
participant QA as "测试工程师"
participant Ops as "运维专家"
User->>Analyst : "描述业务需求"
Analyst->>Architect : "需求分析结果"
Architect->>Developer : "技术方案和实现计划"
Developer->>QA : "完成的功能进行测试"
QA->>Ops : "准备部署环境"
Ops->>User : "交付可运行的应用"
```

### 工具链集成
平台提供完整的 AI 代理工具链：

| 工具类别 | 具体工具 | 功能描述 |
|---------|---------|---------|
| 需求分析 | Qoder AI Assistant | 自然语言需求解析 |
| 架构设计 | OpenSpec 规范引擎 | 规范驱动的设计 |
| 代码生成 | 代码生成器 | 一键生成 CRUD 代码 |
| 测试执行 | 自动化测试框架 | 单元测试和集成测试 |
| 部署运维 | CI/CD 流水线 | 自动化部署和监控 |

**章节来源**
- [backend/README.md:101-144](file://backend/README.md#L101-L144)
- [.claude/skills/openspec-apply-change/openspec-apply-change.md:153-157](file://.claude/skills/openspec-apply-change/openspec-apply-change.md#L153-L157)
- [.claude/commands/opsx/apply.md:149-153](file://.claude/commands/opsx/apply.md#L149-L153)

## 代码生成器实现机制

### 技术架构
代码生成器基于 Velocity 模板引擎构建，支持多种前端框架和后端技术栈：

```mermaid
flowchart TD
Input["数据库表结构"] --> TemplateEngine["Velocity 模板引擎"]
TemplateEngine --> BackendTemplates["后端模板<br/>Controller/Service/Mapper/DO/VO"]
TemplateEngine --> FrontendTemplates["前端模板<br/>Vue3 Element Plus/Vben Admin"]
TemplateEngine --> SQLTemplates["SQL 模板<br/>建表脚本/索引"]
TemplateEngine --> APITemplates["API 模板<br/>Swagger 文档"]
TemplateEngine --> TestTemplates["测试模板<br/>单元测试代码"]
BackendTemplates --> Output["生成完整代码"]
FrontendTemplates --> Output
SQLTemplates --> Output
APITemplates --> Output
TestTemplates --> Output
```

### 模板类型支持
- **通用模板**（类型 1）：适用于标准 CRUD 场景
- **树表模板**（类型 2）：适用于树形结构数据
- **ERP 主表模板**（类型 11）：适用于复杂业务场景

### 前端框架支持
- Vue3 Element Plus：现代化桌面应用界面
- Vue3 Vben Admin：企业级管理后台
- Vben5 Antd：React 生态系统集成
- UniApp 移动端：跨平台移动应用

### 代码生成规则
- **命名约定**：PascalCase（类名）、camelCase（变量）、kebab-case（文件名）
- **分层结构**：DO/Mapper/Service/Controller/VO 标准分层
- **VO 类型**：PageReqVO、ListReqVO、SaveReqVO、RespVO 统一规范
- **主子表处理**：支持复杂关联关系的数据处理

**章节来源**
- [agent_improvement/memory/codegen-rules.md:1-788](file://agent_improvement/memory/codegen-rules.md#L1-L788)
- [agent_improvement/memory/MEMORY.md:1-21](file://agent_improvement/memory/MEMORY.md#L1-L21)

## 依赖关系分析
- 模块耦合
  - yudao-server 作为容器，聚合各业务模块；CPS 模块通过 API 定义层与业务实现层解耦
  - 基础设施模块（缓存、消息队列、定时任务、监控）被各模块复用
- 外部依赖
  - Spring Boot 3.5.9、Spring Security 6.5.2、MyBatis Plus 3.5.12、Redis/Redisson、Flowable、Vue3/UniApp、MySQL、Quartz、SkyWalking
- 规范与规则
  - openspec/config.yaml 定义规范驱动的项目上下文与规则
  - agent_improvement/memory 提供代码生成规则与 Claude 记忆

```mermaid
graph TB
server["yudao-server"]
framework["yudao-framework"]
cps_api["yudao-module-cps-api"]
cps_biz["yudao-module-cps-biz"]
ai_mod["yudao-module-ai"]
infra["yudao-module-infra"]
openspec["openspec/config.yaml"]
memory["agent_improvement/memory/*"]
server --> framework
server --> cps_api
server --> cps_biz
server --> ai_mod
server --> infra
openspec --> memory
memory --> server
```

**图表来源**
- [README.md:229-249](file://README.md#L229-L249)
- [openspec/config.yaml:1-21](file://openspec/config.yaml#L1-L21)

**章节来源**
- [README.md:229-249](file://README.md#L229-L249)
- [openspec/config.yaml:1-21](file://openspec/config.yaml#L1-L21)

## 性能考量
- 性能指标
  - 单平台搜索：<2 秒（P99）
  - 多平台比价：<5 秒（P99）
  - 转链生成：<1 秒
  - 订单同步延迟：<30 分钟
  - 返利入账：平台结算后 24 小时内
  - MCP Tool 调用：<3 秒（搜索类）/<1 秒（查询类）
- 优化方向
  - 缓存策略：热点数据缓存、分布式锁控制并发
  - 异步处理：订单同步、结算入账使用消息队列异步化
  - 压测与监控：结合 SkyWalking 与压测工具，持续优化关键路径

**章节来源**
- [README.md:332-342](file://README.md#L332-L342)
- [backend/README.md:326-336](file://backend/README.md#L326-L336)

## 故障排查指南
- 常见问题
  - 数据库连接：检查 application-local.yaml 中的 MySQL 连接配置
  - 缓存连接：检查 Redis 连接配置
  - MCP 工具不可用：确认 /mcp/cps 端点可达，工具注册正常
  - 平台 API 密钥：确保各平台（淘宝/京东/拼多多/抖音）密钥正确配置
- 排查步骤
  - 启动日志：查看 yudao-server 启动日志，确认模块加载成功
  - 定时任务：检查 Quartz 任务是否正常触发
  - 监控平台：通过 SkyWalking 查看链路与日志中心定位问题
- 配置参考
  - application-local.yaml：本地开发配置（数据库、Redis、MCP、平台密钥等）
  - Docker 环境：通过环境变量配置服务端口与连接信息

**章节来源**
- [README.md:305-342](file://README.md#L305-L342)

## 结论
AgenticCPS 以 Vibe Coding 为核心，结合规范化 AI 编程工作流、AI 代理协作与 MCP 协议，实现了"自然语言描述 → AI 自动编码 → 测试 → 交付"的完整闭环。平台不仅具备强大的低代码能力（代码生成器、可视化工作流、报表与大屏），还在 CPS 联盟返利系统上实现了多平台接入与自动化运营。通过规范驱动与持续自进化，平台能够以极低的成本快速扩展新功能，适合个人开发者、自由职业者与小型工作室使用。

## 附录
- 快速开始
  - 环境要求：JDK 17/21、MySQL 5.7/8.0+、Redis 5.0+、Maven 3.8+、Node.js 16+
  - 三步启动：克隆项目 → 初始化数据库 → 启动后端主类
- 使用示例
  - 接入新平台：实现 CpsPlatformClient 接口并注册为 Spring Bean
  - 调用 MCP 工具：通过 /mcp/cps 的 JSON-RPC 调用工具方法
  - 生成代码：输入数据库表结构，一键生成前后端代码与测试
- 扩展指南
  - 新增平台适配器：遵循策略模式，实现接口并注册 Bean
  - 新增 MCP 工具：在 mcp 层注册工具，定义参数与返回结构
  - 优化代码生成：基于 Velocity 模板库扩展模板类型与前端框架支持

**章节来源**
- [README.md:305-342](file://README.md#L305-L342)
- [backend/README.md:179-205](file://backend/README.md#L179-L205)
- [agent_improvement/memory/codegen-rules.md:1-788](file://agent_improvement/memory/codegen-rules.md#L1-L788)