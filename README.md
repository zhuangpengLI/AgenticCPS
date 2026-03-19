<p align="center">
 <img src="https://img.shields.io/badge/Spring%20Boot-3.4.5-blue.svg" alt="Downloads">
 <img src="https://img.shields.io/badge/Vue-3.2-blue.svg" alt="Downloads">
 <img src="https://img.shields.io/github/license/YunaiV/ruoyi-vue-pro" alt="Downloads" />
</p>


## 🐯 平台简介
**AgenticCPS** 是一款**企业级智能 CPS 联盟返利与导购平台**，深度融合 **AI 自主编程**、**多平台 CPS 联盟接入** 与 **智能返利决策** 三大核心能力。平台以"多平台商品聚合 + 智能比价决策 + 返利自动结算 + AI 驱动运营 + 自主编程扩展"为核心理念，提供一站式 CPS 返利查询、跨平台智能比价、AI 驱动商品推荐、返利自动结算、MCP 自主编程接口等核心能力，打造**可搜索、可比价、可追踪、可决策、可自主扩展**的新一代 CPS 智能返利解决方案。


> gitee: [AgenticCPS](https://gitee.com/zhuangpengli/AgenticCPS.git)

> gitcode: [AgenticCPS](https://gitcode.com/lizhuangpeng/AgenticCPS)

> github: [AgenticCPS](https://github.com/zhuangpengLI/AgenticCPS)
> 
> 有任何问题，或者想要的功能，可以在 _Issues_ 中提给我们。
>
> 😜 给项目点点 Star 吧，这对我们真的很重要！

![架构图](/.image/common/ruoyi-vue-pro-architecture.png)

* Java 后端：`master` 分支为 JDK 8 + Spring Boot 2.7，`master-jdk17` 分支为 JDK 17/21 + Spring Boot 3.2
* 管理后台的电脑端：Vue3 提供 `element-plus`、`vben(ant-design-vue)` 两个版本，Vue2 提供 `element-ui` 版本
* 管理后台的移动端：采用 `uni-app` 方案，一份代码多终端适配，同时支持 APP、小程序、H5！
* 后端采用 Spring Boot 多模块架构、MySQL + MyBatis Plus、Redis + Redisson
* 数据库可使用 MySQL、Oracle、PostgreSQL、SQL Server、MariaDB、国产达梦 DM、TiDB 等
* 消息队列可使用 Event、Redis、RabbitMQ、Kafka、RocketMQ 等
* 权限认证使用 Spring Security & Token & Redis，支持多终端、多种用户的认证系统，支持 SSO 单点登录
* 支持加载动态权限菜单，按钮级别权限控制，Redis 缓存提升性能
* 支持 SaaS 多租户，可自定义每个租户的权限，提供透明化的多租户底层封装
* 工作流使用 Flowable，支持动态表单、在线设计流程、会签 / 或签、多种任务分配方式
* 高效率开发，使用代码生成器可以一键生成 Java、Vue 前后端代码、SQL 脚本、接口文档，支持单表、树表、主子表
* 实时通信，采用 Spring WebSocket 实现，内置 Token 身份校验，支持 WebSocket 集群
* 集成微信小程序、微信公众号、企业微信、钉钉等三方登陆，集成支付宝、微信等支付与退款
* 集成阿里云、腾讯云等短信渠道，集成 MinIO、阿里云、腾讯云、七牛云等云存储服务
* 集成报表设计器、大屏设计器，通过拖拽即可生成酷炫的报表与大屏


## 😎 开源协议

**为什么推荐使用本项目？**

① 本项目采用比 Apache 2.0 更宽松的 [MIT License](https://gitee.com/zhijiantianya/ruoyi-vue-pro/blob/master/LICENSE) 开源协议，个人与企业可 100% 免费使用，不用保留类作者、Copyright 信息。

③ 代码整洁、架构整洁，遵循《阿里巴巴 Java 开发手册》规范，代码注释详细，113770 行 Java 代码，42462 行代码注释。


### Qoder 自主编码

平台集成 **Qoder AI 编码助手**，实现 AI 驱动的自主开发能力：

| 能力 | 描述 |
|------|------|
| 智能代码生成 | 基于自然语言描述自动生成业务代码、API 接口、数据库表结构 |
| 协议扩展开发 | 通过 AI 对话快速实现新协议接入（如自定义 TCP/UDP 协议），自动生成编解码器 |
| 物模型生成 | 根据设备描述自动生成物模型定义（属性、服务、事件），减少手动配置 |
| 规则引擎配置 | 自然语言描述业务场景，自动生成场景联动规则和告警配置 |
| 代码审查与优化 | AI 自动审查代码质量，提供优化建议和安全漏洞检测 |
| 文档自动生成 | 自动生成 API 文档、数据库设计文档、协议说明文档 |

### 基于 Specs/Plans 的规范化 AI 编程

平台创新性地引入**规范化 AI 编程工作流**，通过 `.qoder` 目录下的规范文件实现高质量自主编码：

| 文件类型 | 路径 | 作用 |
|---------|------|------|
| **Specs（规范）** | `.qoder/specs/` | 定义编码规范、技术标准、架构约束、代码风格 |
| **Plans（计划）** | `.qoder/plans/` | 定义任务分解、实施步骤、验收标准、交付物清单 |
| **Agents（代理）** | `.qoder/agents/` | 定义 AI 代理角色、职责边界、协作流程 |
| **Skills（技能）** | `.qoder/skills/` | 定义可复用技能、代码模板、最佳实践 |

#### 标准化 AI 编程流程

1. **需求对齐阶段**
   - AI 自动读取 Specs 规范，理解技术栈、编码标准、架构模式
   - AI 解析 Plans 计划，明确需求范围、功能边界、验收标准
   - 生成实施方案，与用户确认后再执行

2. **方案设计阶段**
   - AI 根据规范自动设计技术方案（类图、流程图、数据模型）
   - 生成详细的实施计划（任务分解、依赖关系、优先级）
   - 输出验收标准（功能测试、性能指标、代码质量要求）

3. **自主编码阶段**
   - **无手写代码**：AI 根据方案自主生成完整代码
   - **纯 AI 编程**：从业务逻辑到单元测试，全部由 AI 生成
   - **规范遵循**：自动遵循 Specs 定义的编码规范和架构约束
   - **质量保障**：自动生成单元测试、集成测试、性能测试

4. **验收交付阶段**
   - AI 自动执行测试用例，验证功能是否符合 Plans 中的验收标准
   - 生成验收报告（测试覆盖率、性能指标、代码质量评分）
   - 输出完整文档（API 文档、使用说明、部署指南）

#### 核心优势

| 优势 | 说明 |
|------|------|
| 🎯 **需求精准对齐** | 通过 Specs/Plans 确保 AI 理解无偏差，避免"AI 乱写代码" |
| 📋 **方案先行** | 先设计方案和验收标准，经用户确认后再编码，降低返工风险 |
| 🤖 **纯 AI 自主编程** | 从需求到代码全流程 AI 化，无需手写代码，提升开发效率 10x+ |
| ✅ **质量可保障** | 自动测试 + 规范约束 + 验收标准，确保代码质量可控 |
| 🔄 **持续自进化** | 根据项目反馈自动优化 Specs/Plans，形成正向循环 |

---


### CPS联盟返利系统

CPS联盟返利系统是基于 ruoyi-vue-pro 框架构建的一站式多平台CPS返利查询与导购系统，为消费者提供返利查询、跨平台比价、推广链接生成和返利提现等服务。

#### 核心功能

| 功能模块 | 描述 |
|---------|------|
| 🚀 多平台CPS接入 | 统一接入淘宝联盟、京东联盟、拼多多联盟等主流CPS平台，支持快速扩展新平台 |
| 🚀 商品搜索与比价 | 关键词搜索、链接/口令解析、跨平台比价，帮助用户找到最优价格和最高返利 |
| 🚀 会员返利体系 | 基于现有会员等级体系，支持多维度返利比例配置（个人>等级+平台>等级>平台>全局） |
| 🚀 订单全链路追踪 | 实现从商品查询→链接生成→用户下单→佣金结算→返利入账的完整业务闭环 |
| 🚀 提现管理 | 会员可将返利余额提现到支付宝/微信，支持自动/人工审核、转账失败自动返还 |
| 🚀 MCP AI接口 | 基于MCP协议提供AI Agent接口层，支持5个Tools（搜索/比价/转链/订单/返利） |
| 🚀 运营数据看板 | 实时展示订单量/佣金/返利/利润等核心指标，支持按平台/会员/时间维度统计 |
| 🚀 风控管理 | 异常行为检测、黑名单管理、退款率预警、提现风险控制 |

#### 技术架构

```
yudao-module-cps/
├── yudao-module-cps-api/          # CPS模块API定义
│   ├── enums/                     # 枚举定义（平台编码、订单状态等）
│   └── api/                       # 远程服务接口
│
└── yudao-module-cps-biz/          # CPS模块业务实现
    ├── controller/
    │   ├── admin/                 # 管理后台接口（平台管理、订单、返利、提现、统计、MCP）
    │   └── app/                   # C端会员接口（商品、比价、转链、订单、返利、提现）
    │
    ├── service/
    │   ├── platform/              # CPS平台适配层（策略模式，支持快速扩展）
    │   ├── goods/                 # 商品查询服务（搜索、比价、解析）
    │   ├── link/                  # 推广链接服务（转链、归因）
    │   ├── order/                 # 订单服务（同步、归因、管理）
    │   ├── commission/            # 佣金结算服务（计算、返利、结算）
    │   ├── rebate/                # 返利配置服务（规则管理）
    │   └── withdraw/              # 提现服务（申请、审核、转账）
    │
    ├── client/                    # CPS平台适配器（策略模式）
    │   ├── CpsPlatformClient.java            # 统一接口定义
    │   ├── taobao/                           # 淘宝联盟适配器
    │   ├── jingdong/                         # 京东联盟适配器
    │   ├── pinduoduo/                        # 拼多多联盟适配器
    │   └── douyin/                           # 抖音联盟适配器（扩展）
    │
    ├── dal/
    │   ├── dataobject/            # 数据库实体（9张业务表）
    │   └── mysql/                 # Mapper接口
    ├── convert/                   # DTO转换（MapStruct）
    ├── job/                       # 定时任务（订单同步、状态更新）
    └── mcp/                       # MCP（Model Context Protocol）AI接口层
        ├── server/                # MCP Server主入口与配置
        ├── transport/             # 传输层（HTTP/STDIO）
        ├── tool/                  # MCP Tools（商品搜索/比价/转链/订单/返利）
        ├── resource/              # MCP Resources（平台/规则/画像/统计/热词）
        └── prompt/                # MCP Prompts（找最优价/比价分析/省钱策略）
```

#### 接口概览

**会员端接口（13个）**：
```
POST   /app-api/cps/goods/search          # 商品搜索
POST   /app-api/cps/goods/compare         # 多平台比价
GET    /app-api/cps/goods/detail          # 商品详情
GET    /app-api/cps/goods/recommend       # 商品推荐
POST   /app-api/cps/link/generate         # 生成推广链接
GET    /app-api/cps/order/page            # 我的订单列表
GET    /app-api/cps/order/get             # 订单详情
GET    /app-api/cps/rebate/summary        # 返利汇总（余额/待结算/累计）
GET    /app-api/cps/rebate/page           # 返利明细
POST   /app-api/cps/withdraw/create       # 发起提现
GET    /app-api/cps/withdraw/page         # 提现记录
GET    /app-api/cps/search/history        # 搜索历史
GET    /app-api/cps/search/hot            # 热门搜索
```

**管理端接口（15个）**：
```
POST/PUT/DELETE/GET  /admin-api/cps/platform/*              # 平台配置管理
POST/PUT/DELETE/GET  /admin-api/cps/adzone/*                # 推广位管理
POST/PUT/GET         /admin-api/cps/platform/test           # 平台连通测试
GET/POST             /admin-api/cps/order/*                 # 订单管理（查询/同步/绑定）
POST/PUT/DELETE/GET  /admin-api/cps/rebate-config/*         # 返利配置（等级/个人）
GET                  /admin-api/cps/rebate-record/page      # 返利记录查询
POST/PUT/GET         /admin-api/cps/withdraw/*              # 提现审核（列表/通过/驳回）
GET                  /admin-api/cps/statistics/dashboard    # 运营数据看板
GET                  /admin-api/cps/statistics/platform     # 平台统计
GET                  /admin-api/cps/statistics/trend        # 趋势统计
GET/POST             /admin-api/cps/mcp/api-key/*           # MCP API Key管理
GET                  /admin-api/cps/mcp/access-log          # MCP访问日志
GET                  /admin-api/cps/mcp/statistics          # MCP统计分析
```

#### MCP AI 接口

本模块基于 **MCP（Model Context Protocol）** 协议标准，为 CPS 系统构建一套可供 AI Agent 直接调用的接口层。

**核心 Tools**：

| Tool名称 | 描述 | 参数示例 |
|---------|------|---------|
| `cps_search_goods` | 商品搜索 | keyword, platform_code, price_min, price_max, sort_type, member_id |
| `cps_compare_prices` | 多平台比价 | keyword, member_id |
| `cps_generate_link` | 推广链接生成 | itemId, platformCode, memberId |
| `cps_get_order_status` | 订单状态查询 | memberId |
| `cps_rebate_summary` | 返利汇总查询 | memberId |

**传输层支持**：

| 传输方式 | 适用场景 | 端点 | 说明 |
|----------|----------|------|------|
| Streamable HTTP | 远程AI Agent接入 | `/mcp/cps` | 支持SSE流式响应，适合生产环境 |
| STDIO | 本地开发调试 | 标准输入输出 | 适合本地AI开发工具集成 |

#### 数据库表

```
yudao_cps_platform          # CPS平台配置表
yudao_cps_adzone            # 推广位（PID）管理表
yudao_cps_order             # CPS订单表
yudao_cps_rebate_config     # 返利配置表
yudao_cps_rebate_record     # 返利记录表
yudao_cps_withdraw          # 提现申请表
yudao_cps_statistics        # 统计数据表
yudao_cps_mcp_api_key       # MCP API Key管理表
yudao_cps_mcp_access_log    # MCP访问日志表
```

#### 性能要求

| 指标 | 要求 |
|------|------|
| 单平台搜索 | < 2秒（P99） |
| 多平台比价 | < 5秒（P99） |
| 转链生成 | < 1秒 |
| 订单同步延迟 | < 30分钟 |
| 返利入账延迟 | 平台结算后 24小时内 |

#### 使用说明

1. **接入CPS平台**
   - 在管理后台配置平台AppKey、AppSecret、API地址
   - 配置推广位（PID）
   - 测试平台连通性

2. **配置返利规则**
   - 设置会员等级返利比例
   - 为特定会员设置个人专属返利
   - 配置返利展示规则

3. **配置提现规则**
   - 设置最低/最高提现金额
   - 设置每日提现次数
   - 设置审核方式（自动/人工）

4. **配置MCP AI接口**
   - 启用MCP Server
   - 创建API Key（配置权限级别）
   - 配置限流规则

#### 技术依赖

**复用现有模块**：
- `yudao-module-member` - 复用会员体系（用户、等级、积分）
- `yudao-module-pay` - 复用支付模块（钱包、提现转账）
- `yudao-module-system` - 复用系统模块（权限、字典、通知）
- `yudao-module-infra` - 复用基础设施（定时任务、文件存储）

**技术栈**：
- 后端：Spring Boot 3.x + MyBatis Plus
- 数据库：MySQL 8.0+
- 缓存：Redis 6.0+
- 定时任务：yudao-spring-boot-starter-job
- MCP协议：基于JSON-RPC 2.0

#### 项目进展

- ✅ Phase 1: 基础框架搭建（已完成）
- ✅ Phase 2: 核心功能开发（已完成）
- ✅ Phase 3: 订单与结算（已完成）
- ✅ Phase 4: 会员与提现（已完成）
- ✅ Phase 5: 数据统计（已完成）
- ✅ Phase 6: MCP AI接口（已完成）
- 🚀 Phase 7: 文档与优化（进行中）

### 系统功能

|     | 功能    | 描述                              |
|-----|-------|---------------------------------|
|     | 用户管理  | 用户是系统操作者，该功能主要完成系统用户配置          |
| ⭐️  | 在线用户  | 当前系统中活跃用户状态监控，支持手动踢下线           |
|     | 角色管理  | 角色菜单权限分配、设置角色按机构进行数据范围权限划分      |
|     | 菜单管理  | 配置系统菜单、操作权限、按钮权限标识等，本地缓存提供性能    |
|     | 部门管理  | 配置系统组织机构（公司、部门、小组），树结构展现支持数据权限  |
|     | 岗位管理  | 配置系统用户所属担任职务                    |
| 🚀  | 租户管理  | 配置系统租户，支持 SaaS 场景下的多租户功能        |
| 🚀  | 租户套餐  | 配置租户套餐，自定每个租户的菜单、操作、按钮的权限       |
|     | 字典管理  | 对系统中经常使用的一些较为固定的数据进行维护          |
| 🚀  | 短信管理  | 短信渠道、短息模板、短信日志，对接阿里云、腾讯云等主流短信平台 |
| 🚀  | 邮件管理  | 邮箱账号、邮件模版、邮件发送日志，支持所有邮件平台       |
| 🚀  | 站内信   | 系统内的消息通知，提供站内信模版、站内信消息          |
| 🚀  | 操作日志  | 系统正常操作日志记录和查询，集成 Swagger 生成日志内容 |
| ⭐️  | 登录日志  | 系统登录日志记录查询，包含登录异常               |
| 🚀  | 错误码管理 | 系统所有错误码的管理，可在线修改错误提示，无需重启服务     |
|     | 通知公告  | 系统通知公告信息发布维护                    |
| 🚀  | 敏感词   | 配置系统敏感词，支持标签分组                  |
| 🚀  | 应用管理  | 管理 SSO 单点登录的应用，支持多种 OAuth2 授权方式 |
| 🚀  | 地区管理  | 展示省份、城市、区镇等城市信息，支持 IP 对应城市      |

![功能图](/.image/common/system-feature.png)


### 支付系统

|     | 功能   | 描述                        |
|-----|------|---------------------------|
| 🚀  | 应用信息 | 配置商户的应用信息，对接支付宝、微信等多个支付渠道 |
| 🚀  | 支付订单 | 查看用户发起的支付宝、微信等的【支付】订单     |
| 🚀  | 退款订单 | 查看用户发起的支付宝、微信等的【退款】订单     |
| 🚀  | 回调通知 | 查看支付回调业务的【支付】【退款】的通知结果    |
| 🚀  | 接入示例 | 提供接入支付系统的【支付】【退款】的功能实战    |

### 基础设施

|     | 功能        | 描述                                           |
|-----|-----------|----------------------------------------------|
| 🚀  | 代码生成      | 前后端代码的生成（Java、Vue、SQL、单元测试），支持 CRUD 下载       |
| 🚀  | 系统接口      | 基于 Swagger 自动生成相关的 RESTful API 接口文档          |
| 🚀  | 数据库文档     | 基于 Screw 自动生成数据库文档，支持导出 Word、HTML、MD 格式      |
|     | 表单构建      | 拖动表单元素生成相应的 HTML 代码，支持导出 JSON、Vue 文件         |
| 🚀  | 配置管理      | 对系统动态配置常用参数，支持 SpringBoot 加载                 |
| ⭐️  | 定时任务      | 在线（添加、修改、删除)任务调度包含执行结果日志                     |
| 🚀  | 文件服务      | 支持将文件存储到 S3（MinIO、阿里云、腾讯云、七牛云）、本地、FTP、数据库等   | 
| 🚀  | WebSocket | 提供 WebSocket 接入示例，支持一对一、一对多发送方式              | 
| 🚀  | API 日志    | 包括 RESTful API 访问日志、异常日志两部分，方便排查 API 相关的问题   |
|     | MySQL 监控  | 监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈              |
|     | Redis 监控  | 监控 Redis 数据库的使用情况，使用的 Redis Key 管理           |
| 🚀  | 消息队列      | 基于 Redis 实现消息队列，Stream 提供集群消费，Pub/Sub 提供广播消费 |
| 🚀  | Java 监控   | 基于 Spring Boot Admin 实现 Java 应用的监控           |
| 🚀  | 链路追踪      | 接入 SkyWalking 组件，实现链路追踪                      |
| 🚀  | 日志中心      | 接入 SkyWalking 组件，实现日志中心                      |
| 🚀  | 服务保障      | 基于 Redis 实现分布式锁、幂等、限流功能，满足高并发场景              |
| 🚀  | 日志服务      | 轻量级日志中心，查看远程服务器的日志                           |
| 🚀  | 单元测试      | 基于 JUnit + Mockito 实现单元测试，保证功能的正确性、代码的质量等    |

![功能图](/.image/common/infra-feature.png)

### 数据报表

|     | 功能    | 描述                 |
|-----|-------|--------------------|
| 🚀  | 报表设计器 | 支持数据报表、图形报表、打印设计等  |
| 🚀  | 大屏设计器 | 拖拽生成数据大屏，内置几十种图表组件 |

### 微信公众号

|    | 功能     | 描述                            |
|----|--------|-------------------------------|
| 🚀 | 账号管理   | 配置接入的微信公众号，可支持多个公众号           |
| 🚀 | 数据统计   | 统计公众号的用户增减、累计用户、消息概况、接口分析等数据  |
| 🚀 | 粉丝管理   | 查看已关注、取关的粉丝列表，可对粉丝进行同步、打标签等操作 |
| 🚀 | 消息管理   | 查看粉丝发送的消息列表，可主动回复粉丝消息         |
| 🚀 | 模版消息   | 配置和发送模版消息，用于向粉丝推送通知类消息        |
| 🚀 | 自动回复   | 自动回复粉丝发送的消息，支持关注回复、消息回复、关键字回复 |
| 🚀 | 标签管理   | 对公众号的标签进行创建、查询、修改、删除等操作       |
| 🚀 | 菜单管理   | 自定义公众号的菜单，也可以从公众号同步菜单         |
| 🚀 | 素材管理   | 管理公众号的图片、语音、视频等素材，支持在线播放语音、视频 |
| 🚀 | 图文草稿箱  | 新增常用的图文素材到草稿箱，可发布到公众号         |
| 🚀 | 图文发表记录 | 查看已发布成功的图文素材，支持删除操作           |

### 商城系统

演示地址：<https://doc.iocoder.cn/mall-preview/>

![功能图](/.image/common/mall-feature.png)

![功能图](/.image/common/mall-preview.png)

### 会员中心

|     | 功能   | 描述                               |
|-----|------|----------------------------------|
| 🚀  | 会员管理 | 会员是 C 端的消费者，该功能用于会员的搜索与管理        |
| 🚀  | 会员标签 | 对会员的标签进行创建、查询、修改、删除等操作           |
| 🚀  | 会员等级 | 对会员的等级、成长值进行管理，可用于订单折扣等会员权益      |
| 🚀  | 会员分组 | 对会员进行分组，用于用户画像、内容推送等运营手段         |
| 🚀  | 积分签到 | 回馈给签到、消费等行为的积分，会员可订单抵现、积分兑换等途径消耗 |


### AI 大模型

演示地址：<https://doc.iocoder.cn/ai-preview/>

![功能图](/.image/common/ai-feature.png)

![功能图](/.image/common/ai-preview.gif)

## 🐨 技术栈

### 模块

| 项目                    | 说明                 |
|-----------------------|--------------------|
| `yudao-dependencies`  | Maven 依赖版本管理       |
| `yudao-framework`     | Java 框架拓展          |
| `yudao-server`        | 管理后台 + 用户 APP 的服务端 |
| `yudao-module-system` | 系统功能的 Module 模块    |
| `yudao-module-member` | 会员中心的 Module 模块    |
| `yudao-module-infra`  | 基础设施的 Module 模块    |
| `yudao-module-pay`    | 支付系统的 Module 模块    |
| `yudao-module-mall`   | 商城系统的 Module 模块    |
| `yudao-module-ai`     | AI 大模型的 Module 模块  |
| `yudao-module-mp`     | 微信公众号的 Module 模块   |
| `yudao-module-report` | 大屏报表 Module 模块     |
| `yudao-module-cps`    | CPS联盟返利系统的 Module 模块 |

### 框架

| 框架                                                                                          | 说明               | 版本             | 学习指南                                                           |
|---------------------------------------------------------------------------------------------|------------------|----------------|----------------------------------------------------------------|
| [Spring Boot](https://spring.io/projects/spring-boot)                                       | 应用开发框架           | 3.5.5          | [文档](https://github.com/YunaiV/SpringBoot-Labs)                |
| [MySQL](https://www.mysql.com/cn/)                                                          | 数据库服务器           | 5.7 / 8.0+     |                                                                |
| [Druid](https://github.com/alibaba/druid)                                                   | JDBC 连接池、监控组件    | 1.2.27         | [文档](http://www.iocoder.cn/Spring-Boot/datasource-pool/?yudao) |
| [MyBatis Plus](https://mp.baomidou.com/)                                                    | MyBatis 增强工具包    | 3.5.12         | [文档](http://www.iocoder.cn/Spring-Boot/MyBatis/?yudao)         |
| [Dynamic Datasource](https://dynamic-datasource.com/)                                       | 动态数据源            | 4.3.1          | [文档](http://www.iocoder.cn/Spring-Boot/datasource-pool/?yudao) |
| [Redis](https://redis.io/)                                                                  | key-value 数据库    | 5.0 / 6.0 /7.0 |                                                                |
| [Redisson](https://github.com/redisson/redisson)                                            | Redis 客户端        | 3.35.0         | [文档](http://www.iocoder.cn/Spring-Boot/Redis/?yudao)           |
| [Spring MVC](https://github.com/spring-projects/spring-framework/tree/master/spring-webmvc) | MVC 框架           | 6.2.9          | [文档](http://www.iocoder.cn/SpringMVC/MVC/?yudao)               |
| [Spring Security](https://github.com/spring-projects/spring-security)                       | Spring 安全框架      | 6.5.2          | [文档](http://www.iocoder.cn/Spring-Boot/Spring-Security/?yudao) |
| [Hibernate Validator](https://github.com/hibernate/hibernate-validator)                     | 参数校验组件           | 8.0.2          | [文档](http://www.iocoder.cn/Spring-Boot/Validation/?yudao)      |
| [Flowable](https://github.com/flowable/flowable-engine)                                     | 工作流引擎            | 7.0.0          | [文档](https://doc.iocoder.cn/bpm/)                              |
| [Quartz](https://github.com/quartz-scheduler)                                               | 任务调度组件           | 2.5.0          | [文档](http://www.iocoder.cn/Spring-Boot/Job/?yudao)             |
| [Springdoc](https://springdoc.org/)                                                         | Swagger 文档       | 2.8.9          | [文档](http://www.iocoder.cn/Spring-Boot/Swagger/?yudao)         |
| [SkyWalking](https://skywalking.apache.org/)                                                | 分布式应用追踪系统        | 9.5.0          | [文档](http://www.iocoder.cn/Spring-Boot/SkyWalking/?yudao)      |
| [Spring Boot Admin](https://github.com/codecentric/spring-boot-admin)                       | Spring Boot 监控平台 | 3.5.2          | [文档](http://www.iocoder.cn/Spring-Boot/Admin/?yudao)           |
| [Jackson](https://github.com/FasterXML/jackson)                                             | JSON 工具库         | 2.30.14        |                                                                |
| [MapStruct](https://mapstruct.org/)                                                         | Java Bean 转换     | 1.6.3          | [文档](http://www.iocoder.cn/Spring-Boot/MapStruct/?yudao)       |
| [Lombok](https://projectlombok.org/)                                                        | 消除冗长的 Java 代码    | 1.18.38        | [文档](http://www.iocoder.cn/Spring-Boot/Lombok/?yudao)          |
| [JUnit](https://junit.org/junit5/)                                                          | Java 单元测试框架      | 5.12.2         | -                                                              |
| [Mockito](https://github.com/mockito/mockito)                                               | Java Mock 框架     | 5.17.0         | -                                                              |

## 🐷 演示图

### 系统功能

| 模块       | biu                         | biu                       | biu                      |
|----------|-----------------------------|---------------------------|--------------------------|
| 登录 & 首页  | ![登录](/.image/登录.jpg)       | ![首页](/.image/首页.jpg)     | ![个人中心](/.image/个人中心.jpg) |
| 用户 & 应用  | ![用户管理](/.image/用户管理.jpg)   | ![令牌管理](/.image/令牌管理.jpg) | ![应用管理](/.image/应用管理.jpg) |
| 租户 & 套餐  | ![租户管理](/.image/租户管理.jpg)   | ![租户套餐](/.image/租户套餐.png) | -                        |
| 部门 & 岗位  | ![部门管理](/.image/部门管理.jpg)   | ![岗位管理](/.image/岗位管理.jpg) | -                        |
| 菜单 & 角色  | ![菜单管理](/.image/菜单管理.jpg)   | ![角色管理](/.image/角色管理.jpg) | -                        |
| 审计日志     | ![操作日志](/.image/操作日志.jpg)   | ![登录日志](/.image/登录日志.jpg) | -                        |
| 短信       | ![短信渠道](/.image/短信渠道.jpg)   | ![短信模板](/.image/短信模板.jpg) | ![短信日志](/.image/短信日志.jpg) |
| 字典 & 敏感词 | ![字典类型](/.image/字典类型.jpg)   | ![字典数据](/.image/字典数据.jpg) | ![敏感词](/.image/敏感词.jpg)  |
| 错误码 & 通知 | ![错误码管理](/.image/错误码管理.jpg) | ![通知公告](/.image/通知公告.jpg) | -                        |

### 工作流程

| 模块      | biu                             | biu                             | biu                             |
|---------|---------------------------------|---------------------------------|---------------------------------|
| 流程模型    | ![流程模型-列表](/.image/流程模型-列表.jpg) | ![流程模型-设计](/.image/流程模型-设计.jpg) | ![流程模型-定义](/.image/流程模型-定义.jpg) |
| 表单 & 分组 | ![流程表单](/.image/流程表单.jpg)       | ![用户分组](/.image/用户分组.jpg)       | -                               |
| 我的流程    | ![我的流程-列表](/.image/我的流程-列表.jpg) | ![我的流程-发起](/.image/我的流程-发起.jpg) | ![我的流程-详情](/.image/我的流程-详情.jpg) |
| 待办 & 已办 | ![任务列表-审批](/.image/任务列表-审批.jpg) | ![任务列表-待办](/.image/任务列表-待办.jpg) | ![任务列表-已办](/.image/任务列表-已办.jpg) |
| OA 请假   | ![OA请假-列表](/.image/OA请假-列表.jpg) | ![OA请假-发起](/.image/OA请假-发起.jpg) | ![OA请假-详情](/.image/OA请假-详情.jpg) |

### 基础设施

| 模块            | biu                           | biu                         | biu                       |
|---------------|-------------------------------|-----------------------------|---------------------------|
| 代码生成          | ![代码生成](/.image/代码生成.jpg)     | ![生成效果](/.image/生成效果.jpg)   | -                         |
| 文档            | ![系统接口](/.image/系统接口.jpg)     | ![数据库文档](/.image/数据库文档.jpg) | -                         |
| 文件 & 配置       | ![文件配置](/.image/文件配置.jpg)     | ![文件管理](/.image/文件管理2.jpg)  | ![配置管理](/.image/配置管理.jpg) |
| 定时任务          | ![定时任务](/.image/定时任务.jpg)     | ![任务日志](/.image/任务日志.jpg)   | -                         |
| API 日志        | ![访问日志](/.image/访问日志.jpg)     | ![错误日志](/.image/错误日志.jpg)   | -                         |
| MySQL & Redis | ![MySQL](/.image/MySQL.jpg)   | ![Redis](/.image/Redis.jpg) | -                         |
| 监控平台          | ![Java监控](/.image/Java监控.jpg) | ![链路追踪](/.image/链路追踪.jpg)   | ![日志中心](/.image/日志中心.jpg) |

### 支付系统

| 模块      | biu                       | biu                             | biu                             |
|---------|---------------------------|---------------------------------|---------------------------------|
| 商家 & 应用 | ![商户信息](/.image/商户信息.jpg) | ![应用信息-列表](/.image/应用信息-列表.jpg) | ![应用信息-编辑](/.image/应用信息-编辑.jpg) |
| 支付 & 退款 | ![支付订单](/.image/支付订单.jpg) | ![退款订单](/.image/退款订单.jpg)       | ---                             |
### 数据报表

| 模块    | biu                             | biu                             | biu                                   |
|-------|---------------------------------|---------------------------------|---------------------------------------|
| 报表设计器 | ![数据报表](/.image/报表设计器-数据报表.jpg) | ![图形报表](/.image/报表设计器-图形报表.jpg) | ![报表设计器-打印设计](/.image/报表设计器-打印设计.jpg) |
| 大屏设计器 | ![大屏列表](/.image/大屏设计器-列表.jpg)   | ![大屏预览](/.image/大屏设计器-预览.jpg)   | ![大屏编辑](/.image/大屏设计器-编辑.jpg)         |

### 移动端（管理后台）

| biu                              | biu                              | biu                              |
|----------------------------------|----------------------------------|----------------------------------|
| ![](/.image/admin-uniapp/01.png) | ![](/.image/admin-uniapp/02.png) | ![](/.image/admin-uniapp/03.png) |
| ![](/.image/admin-uniapp/04.png) | ![](/.image/admin-uniapp/05.png) | ![](/.image/admin-uniapp/06.png) |
| ![](/.image/admin-uniapp/07.png) | ![](/.image/admin-uniapp/08.png) | ![](/.image/admin-uniapp/09.png) |

目前已经实现登录、我的、工作台、编辑资料、头像修改、密码修改、常见问题、关于我们等基础功能。


## 开源协议

本项目采用 **GNU Affero General Public License v3.0 (AGPL-3.0)** 开源协议。

### 协议要点

| 要点 | 说明 |
|------|------|
| 📜 开源义务 | 任何使用、修改、分发本项目的代码必须以 AGPL-3.0 协议开源 |
| 🌐 网络服务条款 | 通过网络提供服务（如 SaaS）时，必须向用户提供完整源代码 |
| 🔄 著作权归属 | 修改后的代码需保留原作者著作权声明和协议声明 |
| ⚠️ 无担保声明 | 本软件按"原样"提供，不提供任何形式的担保 |

### 使用场景说明

* ✅ **允许**：个人学习、研究、商业二次开发（需开源）
* ✅ **允许**：内部企业使用（无需开源，除非对外提供服务）
* ⚠️ **需开源**：基于本项目提供 SaaS 服务或对外网络服务
* ❌ **禁止**：闭源商业化分发、移除版权声明

### 完整协议文本

详见 [LICENSE](./LICENSE) 文件或访问 [GNU AGPL-3.0 官方协议文本](https://www.gnu.org/licenses/agpl-3.0.html)

---

## 💝 赞助与支持

开源项目的发展离不开社区的支持。如果您觉得本项目对您有帮助，欢迎赞助支持持续开发！

### 为什么需要赞助？

| 用途 | 说明 |
|------|------|
| 🖥️ 服务器部署 | 测试环境、演示环境、CI/CD 服务器租赁费用 |
| 🤖 AI Token 费用 | 大模型 API 调用费用（通义千问、DeepSeek、OpenAI 等） |
| 🔧 持续开发 | 新平台对接开发、功能迭代、Bug 修复 |
| 📚 文档完善 | 技术文档、API 文档、视频教程制作 |

### 赞助方式

#### 微信支付 / 支付宝

<p align="center">
  <img src="https://img.shields.io/badge/微信支付-green.svg" alt="微信支付">
  <img src="https://img.shields.io/badge/支付宝-blue.svg" alt="支付宝">
</p>

> 💡 请在备注中留下您的 GitHub ID 或联系方式，我们将列在赞助者名单中（如愿意公开）


### 企业赞助

欢迎企业用户进行商业赞助，我们将提供以下回报：

| 赞助等级 | 回报 |
|---------|------|
| 🥉 青铜赞助商 | README 中显示企业 Logo |
| 🥈 白银赞助商 | 优先 Issue 处理 + Logo 展示 |
| 🥇 黄金赞助商 | 专属技术支持 + 优先功能开发 + Logo 展示 |
| 💎 钻石赞助商 | 定制开发支持 + 专属技术顾问 + 首页显著展示 |

---

## 🎁 功能悬赏

为了加速项目功能开发，我们推出**功能悬赏计划**。您可以悬赏特定功能的开发，开发者完成后可获得赏金。

### 当前悬赏列表

| 功能 | 悬赏金额   | 状态 | 说明 |
|------|--------|------|------|
| 智能推送 | ¥1,000 | 🔴 待开发 | 智能推送 |

### 悬赏规则

1. **认领任务**：在 Issue 中评论认领，确认后开始开发
2. **开发周期**：根据功能复杂度协商，一般 2-4 周
3. **代码审核**：提交 PR 后进行代码审核，通过后合并
4. **发放赏金**：合并后 3 个工作日内发放至指定账户

### 如何发起悬赏？

如果您需要特定功能但不在列表中，可以：

1. 在 [Issues](../../issues) 中创建功能请求，标注 `💰 悬赏` 标签
2. 说明功能需求和悬赏金额
3. 等待开发者认领或我们评估后添加到悬赏列表

---

### 🙏 感谢所有赞助者

感谢以下赞助者的慷慨支持（按时间排序）：

<!-- 赞助者名单将在此处更新 -->

> 成为第一个赞助者，让开源走得更远！
