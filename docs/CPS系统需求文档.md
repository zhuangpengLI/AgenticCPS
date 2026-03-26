# CPS联盟返利系统 - 需求文档

> **项目名称**: CPS联盟返利系统  
> **基础框架**: ruoyi-vue-pro (芋道快速开发平台)  
> **版本**: v1.0  
> **日期**: 2026-03-19  

---

## 一、项目概述

### 1.1 项目背景

CPS（Cost Per Sale，按销售付费）是电商联盟推广的核心模式。用户通过CPS平台查询商品的返利信息，经由推广链接购买商品后，平台获得联盟佣金，并将佣金的一定比例返还给用户，形成"用户省钱、平台盈利、商家获客"的三方共赢模式。

本项目基于 ruoyi-vue-pro 框架，复用其会员模块（`yudao-module-member`）和商城模块（`yudao-module-mall`）的基础能力，构建一套支持多平台联盟的CPS返利系统。

### 1.2 项目目标

1. **多平台聚合**：统一接入淘宝联盟、京东联盟、拼多多联盟等主流CPS平台，提供统一的商品查询和返利查询能力
2. **可扩展架构**：采用平台适配器模式，支持快速接入新的CPS平台（如抖音联盟、唯品会联盟、苏宁联盟等）
3. **会员返利体系**：基于现有会员等级体系，支持按会员等级设置差异化的返利比例
4. **多平台比价**：支持相同关键词跨平台搜索比价，帮助用户找到最优价格和最高返利
5. **订单全链路追踪**：实现从商品查询、链接生成、用户下单到佣金结算的完整业务闭环
6. **复用现有模块**：充分利用 ruoyi-vue-pro 的会员、商品、支付、钱包等已有基础设施

### 1.3 系统定位

| 维度 | 说明 |
|------|------|
| 目标用户 | C端消费者（通过APP/H5/小程序/微信机器人使用） |
| 管理角色 | 运营人员（后台管理返利规则、平台配置、数据统计） |
| 业务模式 | CPS联盟返利、多平台比价导购 |
| 部署方式 | 复用 ruoyi-vue-pro 单体/微服务架构 |

---

## 二、复用模块说明

### 2.1 复用 yudao-module-member（会员模块）

| 已有能力 | 复用方式 |
|----------|----------|
| MemberUserDO（会员用户） | 作为CPS系统的用户主体，关联返利账户 |
| MemberLevelDO（会员等级） | 根据等级设置差异化返利比例 |
| MemberGroupDO（会员分组） | 按分组设置不同的返利策略 |
| MemberPointRecordDO（积分记录） | 可选：将返利转换为积分奖励 |
| MemberSignInConfigDO（签到配置） | 保持现有签到功能，增强用户粘性 |

**扩展点**：在 MemberUserDO 基础上关联 CPS 用户扩展信息（返利余额、累计收益、推广码等）。

### 2.2 复用 yudao-module-mall（商城模块）

| 已有能力 | 复用方式 |
|----------|----------|
| ProductSpuDO / ProductSkuDO（商品） | 参考商品数据结构，用于CPS商品缓存 |
| ProductCategoryDO（商品分类） | 统一分类体系，映射各平台分类 |
| ProductBrandDO（品牌） | 品牌信息管理 |
| ProductFavoriteDO（收藏） | 用户收藏CPS商品 |
| ProductBrowseHistoryDO（浏览记录） | 用户浏览CPS商品的历史记录 |

### 2.3 复用 yudao-module-pay（支付模块）

| 已有能力 | 复用方式 |
|----------|----------|
| PayWalletDO（用户钱包） | 作为CPS返利余额的载体 |
| PayWalletTransactionDO（钱包交易记录） | 记录返利入账、提现等流水 |
| PayTransferDO（转账） | 用户提现时的转账操作 |
| PayChannelDO（支付渠道） | 提现到支付宝/微信的渠道配置 |

### 2.4 复用其他模块

| 模块 | 复用内容 |
|------|----------|
| yudao-module-system | 后台用户管理、角色权限、字典管理、操作日志、消息通知 |
| yudao-module-infra | 定时任务（订单同步）、文件存储（商品图片缓存）、消息队列 |
| yudao-module-mp | 微信公众号消息推送（订单通知、返利到账通知） |

---

## 三、系统架构设计

### 3.1 模块划分

基于 ruoyi-vue-pro 的模块化设计，新增 `yudao-module-cps` 模块：

```
yudao-module-cps/
├── yudao-module-cps-api/              # CPS模块API定义（供其他模块调用）
│   ├── enums/                         # 枚举定义（平台编码、订单状态等）
│   └── api/                           # 远程服务接口
│
└── yudao-module-cps-biz/              # CPS模块业务实现
    ├── controller/
    │   ├── admin/                     # 管理后台接口
    │   │   ├── CpsPlatformController         # 平台配置管理
    │   │   ├── CpsAdzoneController           # 推广位管理
    │   │   ├── CpsOrderController            # 订单管理
    │   │   ├── CpsRebateConfigController     # 返利配置管理
    │   │   ├── CpsWithdrawController         # 提现审核管理
    │   │   └── CpsStatisticsController       # 数据统计
    │   │
    │   └── app/                       # C端会员接口
    │       ├── AppCpsGoodsController         # 商品搜索与比价
    │       ├── AppCpsLinkController          # 转链/口令生成
    │       ├── AppCpsOrderController         # 我的订单
    │       ├── AppCpsRebateController        # 我的返利
    │       └── AppCpsWithdrawController      # 提现
    │
    ├── service/
    │   ├── platform/                  # CPS平台适配层
    │   │   ├── CpsPlatformClientFactory      # 平台客户端工厂
    │   │   └── CpsPlatformConfigService      # 平台配置服务
    │   ├── goods/                     # 商品查询服务
    │   │   ├── CpsGoodsSearchService         # 搜索与比价
    │   │   └── CpsGoodsParseService          # 链接/口令解析
    │   ├── link/                      # 推广链接服务
    │   │   └── CpsPromotionLinkService       # 转链与归因
    │   ├── order/                     # 订单服务
    │   │   ├── CpsOrderSyncService           # 订单同步
    │   │   └── CpsOrderService               # 订单管理
    │   ├── commission/                # 佣金结算服务
    │   │   ├── CpsCommissionCalcService      # 佣金计算
    │   │   └── CpsRebateSettleService        # 返利结算
    │   ├── rebate/                    # 返利配置服务
    │   │   └── CpsRebateConfigService        # 返利规则管理
    │   └── withdraw/                  # 提现服务
    │       └── CpsWithdrawService            # 提现管理
    │
    ├── client/                        # CPS平台适配器（策略模式）
    │   ├── CpsPlatformClient.java            # 统一接口定义
    │   ├── taobao/                           # 淘宝联盟适配器
    │   │   └── TaobaoCpsPlatformClient
    │   ├── jingdong/                         # 京东联盟适配器
    │   │   └── JingdongCpsPlatformClient
    │   ├── pinduoduo/                        # 拼多多联盟适配器
    │   │   └── PinduoduoCpsPlatformClient
    │   └── douyin/                           # 抖音联盟适配器（扩展）
    │       └── DouyinCpsPlatformClient
    │
    ├── dal/
    │   ├── dataobject/                # 数据库实体
    │   └── mysql/                     # Mapper接口
    ├── convert/                       # DTO转换（MapStruct）
    ├── job/                           # 定时任务
    ├── mq/                            # 消息队列
    │
    └── mcp/                           # MCP（Model Context Protocol）AI接口层
        ├── server/
        │   ├── CpsMcpServer.java             # MCP Server主入口（生命周期管理）
        │   └── CpsMcpServerConfig.java       # MCP Server配置类
        ├── transport/
        │   ├── CpsMcpHttpTransport.java      # Streamable HTTP传输层
        │   └── CpsMcpStdioTransport.java     # STDIO传输层（本地开发）
        ├── tool/                             # MCP Tools定义
        │   ├── CpsSearchGoodsTool.java       # 商品搜索工具
        │   ├── CpsComparePricesTool.java     # 多平台比价工具
        │   ├── CpsGenerateLinkTool.java      # 推广链接生成工具
        │   ├── CpsParseContentTool.java      # 链接/口令解析工具
        │   ├── CpsOrderQueryTool.java        # 订单查询工具
        │   ├── CpsRebateSummaryTool.java     # 返利汇总工具
        │   └── CpsRecommendGoodsTool.java    # 商品推荐工具
        ├── resource/                         # MCP Resources定义
        │   ├── CpsPlatformResource.java      # 平台列表及状态
        │   ├── CpsRebateRuleResource.java    # 返利规则配置
        │   ├── CpsMemberProfileResource.java # 会员CPS画像
        │   ├── CpsStatisticsResource.java    # 统计数据
        │   └── CpsHotKeywordResource.java    # 热门搜索词
        └── prompt/                           # MCP Prompts定义
            ├── CpsFindBestDealPrompt.java    # 找最优惠商品
            ├── CpsComparePrompt.java         # 跨平台比价
            ├── CpsOptimizeSavingsPrompt.java # 省钱策略
            └── CpsExplainRebatePrompt.java   # 返利规则解释
```

### 3.2 与现有模块依赖关系

```
yudao-module-cps-biz
  ├── 依赖 yudao-module-member        # 复用会员体系（用户、等级、积分）
  ├── 依赖 yudao-module-pay            # 复用支付模块（钱包、提现转账）
  ├── 依赖 yudao-module-system         # 复用系统模块（权限、字典、通知）
  └── 依赖 yudao-module-infra          # 复用基础设施（定时任务、文件存储）
```

### 3.3 CPS平台适配器架构

采用 **策略模式 + 工厂模式**，实现平台可插拔扩展。新平台只需实现 `CpsPlatformClient` 接口并注册为 Spring Bean，无需修改任何核心业务逻辑。

```java
/**
 * CPS平台统一接口定义
 * 各联盟平台（淘宝、京东、拼多多等）需实现此接口
 */
public interface CpsPlatformClient {

    /** 获取平台编码 */
    String getPlatformCode();

    /** 关键词商品搜索 */
    CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request);

    /** 商品详情查询 */
    CpsGoodsDetail getGoodsDetail(CpsGoodsDetailRequest request);

    /** 解析链接/口令，识别商品 */
    CpsParsedContent parseContent(String content);

    /** 生成推广链接（含用户归因参数） */
    CpsPromotionLink generatePromotionLink(CpsPromotionLinkRequest request);

    /** 增量查询订单 */
    List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request);

    /** 查询单笔订单详情 */
    CpsOrderDTO getOrderDetail(String orderNo);

    /** 测试平台连通性 */
    boolean testConnection();
}
```

---

## 四、功能需求

### 4.1 功能模块总览

```
CPS联盟返利系统
├── 1. 平台管理模块
│   ├── 1.1 CPS平台配置
│   ├── 1.2 推广位（PID）管理
│   └── 1.3 平台连通性测试
│
├── 2. 商品查询模块
│   ├── 2.1 关键词商品搜索
│   ├── 2.2 链接/口令商品解析
│   ├── 2.3 多平台比价
│   ├── 2.4 商品详情查询
│   └── 2.5 商品推荐频道
│
├── 3. 推广链接模块
│   ├── 3.1 推广链接/口令生成
│   ├── 3.2 用户归因参数管理
│   └── 3.3 推广链接记录
│
├── 4. 订单追踪模块
│   ├── 4.1 订单定时同步
│   ├── 4.2 订单归因匹配
│   ├── 4.3 统一订单状态管理
│   └── 4.4 退款订单处理
│
├── 5. 佣金结算模块
│   ├── 5.1 佣金计算引擎
│   ├── 5.2 返利比例配置（多维度）
│   ├── 5.3 返利入账处理
│   └── 5.4 对账管理
│
├── 6. 会员返利模块
│   ├── 6.1 返利账户管理
│   ├── 6.2 会员等级返利比例
│   ├── 6.3 会员个人专属返利设置
│   ├── 6.4 返利展示控制
│   ├── 6.5 返利明细查询
│   ├── 6.6 提现管理
│   └── 6.7 邀请返利（可选）
│
├── 7. 数据统计模块
│   ├── 7.1 运营数据看板
│   ├── 7.2 按平台统计
│   ├── 7.3 会员统计
│   └── 7.4 收益报表
│
├── 8. 系统配置模块
│   ├── 8.1 全局返利规则配置
│   ├── 8.2 提现规则配置
│   ├── 8.3 风控规则配置
│   └── 8.4 消息通知配置
│
└── 9. MCP AI接口模块
    ├── 9.1 MCP Server服务管理
    ├── 9.2 MCP Tools（AI可调用工具）
    ├── 9.3 MCP Resources（AI可访问数据源）
    ├── 9.4 MCP Prompts（预设交互模板）
    └── 9.5 MCP访问控制与日志
```

---

### 4.2 平台管理模块

#### 4.2.1 CPS平台配置

管理各CPS联盟平台的接入配置信息。

| 字段 | 说明 | 示例 |
|------|------|------|
| 平台编码 | 唯一标识 | `taobao`, `jingdong`, `pinduoduo` |
| 平台名称 | 显示名称 | 淘宝联盟、京东联盟、拼多多联盟 |
| 平台图标 | Logo图片URL | - |
| AppKey | 平台分配的应用Key | - |
| AppSecret | 平台分配的应用密钥（加密存储） | - |
| API基础URL | 平台API网关地址 | `https://eco.taobao.com/router/rest` |
| 授权Token | 部分平台需要的访问令牌 | 支持自动刷新 |
| 默认推广位ID | 平台默认PID | `mm_xxx_xxx_xxx` |
| 平台服务费率 | 平台技术服务费比例 | 10% |
| 排序权重 | 比价结果中的展示排序 | 0 |
| 状态 | 启用/禁用 | 启用 |
| 扩展配置 | JSON格式额外配置 | Token刷新地址等 |

**功能要求**：
- 支持增删改查平台配置
- AppSecret 等敏感字段 AES 加密存储，界面脱敏展示
- 支持连通性测试，验证API配置是否正确
- 支持启用/禁用平台，禁用后不参与商品查询和转链

#### 4.2.2 推广位（PID）管理

管理各平台的推广位信息，用于订单归因追踪。

| 字段 | 说明 |
|------|------|
| 所属平台 | 关联CPS平台编码 |
| 推广位ID | 平台分配的PID（淘宝: adzone_id, 京东: positionId, 拼多多: pid） |
| 推广位名称 | 自定义名称 |
| 推广位类型 | 通用 / 渠道专属 / 用户专属 |
| 关联渠道/用户 | 绑定的渠道或用户ID |
| 是否默认 | 是否为该平台默认推广位 |
| 状态 | 启用/禁用 |

**功能要求**：
- 支持从联盟平台API同步推广位列表
- 支持手动创建和管理推广位
- 每个平台至少配置一个默认推广位
- 支持通过API批量创建推广位

---

### 4.3 商品查询与比价模块

#### 4.3.1 商品搜索

会员输入关键词或商品链接/口令，系统查询对应平台的商品信息及返利。

**输入方式**：

1. **关键词搜索**：输入商品名称关键词，默认查询所有已启用平台
2. **链接解析**：粘贴商品URL，自动识别平台并查询
3. **口令解析**：粘贴淘口令等，自动解析商品

**智能识别规则**：

| 输入内容 | 识别平台 | 处理方式 |
|----------|----------|----------|
| 包含 `taobao.com` / `tmall.com` / `淘口令(￥...￥)` | 淘宝 | 调用淘宝联盟API |
| 包含 `jd.com` / `m.jd.com` | 京东 | 调用京东联盟API |
| 包含 `pinduoduo.com` / `yangkeduo.com` | 拼多多 | 调用拼多多联盟API |
| 包含 `douyin.com` | 抖音 | 调用抖音联盟API |
| 纯文本关键词 | 全平台 | 并发查询所有启用平台 |

**搜索请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 是 | 搜索关键词/链接/口令 |
| platformCode | String | 否 | 指定平台（不指定则查询全部已启用平台） |
| priceMin | Long | 否 | 最低价格（分） |
| priceMax | Long | 否 | 最高价格（分） |
| hasCoupon | Boolean | 否 | 是否有优惠券 |
| sortType | String | 否 | 排序方式：综合/价格/销量/佣金/返利 |
| pageNo | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |

**搜索结果（统一商品结构）**：

| 字段 | 说明 |
|------|------|
| platformCode | 所属平台编码 |
| platformName | 所属平台名称 |
| itemId | 平台商品ID |
| title | 商品标题 |
| picUrl | 商品主图URL |
| price | 商品原价（分） |
| finalPrice | 券后价（分） |
| couponAmount | 优惠券金额（分） |
| couponStartTime | 优惠券开始时间 |
| couponEndTime | 优惠券结束时间 |
| commissionRate | 佣金比例（万分比） |
| commissionAmount | 预估佣金金额（分） |
| estimateRebate | 预估用户返利金额（分，根据会员等级动态计算） |
| monthSales | 月销量 |
| shopName | 店铺名称 |
| shopType | 店铺类型（天猫/淘宝/京东自营/POP等） |
| itemUrl | 商品原始链接 |

**技术要求**：
- 并发查询多个平台API，设置超时时间（单平台超时不影响其他平台返回）
- 查询结果按指定排序方式统一排序
- 支持结果缓存（相同关键词短时间内返回缓存结果，TTL 5~10分钟）
- 返利金额根据当前用户的会员等级动态计算
- 使用 `CompletableFuture` 并行调用各平台API

#### 4.3.2 多平台比价

会员输入关键词后，系统并发查询所有已启用CPS平台，将结果聚合展示。

**比价结果展示**：

```
搜索关键词: "iPhone 16 Pro Max 256G"

┌──────────┬──────────┬──────────┬──────────┬──────────────┐
│ 平台     │ 原价     │ 券后价   │ 预估返利  │ 实付（含返利）│
├──────────┼──────────┼──────────┼──────────┼──────────────┤
│ 拼多多   │ ¥9599    │ ¥9599    │ ¥47.99   │ ¥9551.01     │
│ 淘宝     │ ¥9899    │ ¥9699    │ ¥96.99   │ ¥9602.01     │
│ 京东     │ ¥9999    │ ¥9799    │ ¥58.79   │ ¥9740.21     │
└──────────┴──────────┴──────────┴──────────┴──────────────┘
  实付价 = 券后价 - 预估返利    ★ 最优: 拼多多
```

**排序选项**：
- 实付价从低到高（券后价 - 返利，默认）
- 券后价从低到高
- 返利金额从高到低
- 综合销量从高到低

**额外返回字段**：
- `bestPricePlatform`：全网最低价所在平台
- `bestRebatePlatform`：全网最高返利所在平台
- `bestValuePlatform`：综合最优平台（实付最低）

#### 4.3.3 商品推荐频道

基于各平台的精选物料接口，展示推荐商品。

| 频道 | 说明 | 对应接口 |
|------|------|----------|
| 热销爆品 | 各平台销量TOP商品 | 各平台物料精选API |
| 高佣精选 | 佣金比例最高的商品 | 按佣金排序搜索 |
| 大额券 | 优惠券金额较大的商品 | 按优惠券筛选 |
| 9.9包邮 | 低价商品 | 拼多多频道API等 |
| 品牌特卖 | 品牌折扣商品 | 品牌筛选 |

---

### 4.4 推广链接模块

#### 4.4.1 推广链接生成（转链）

将普通商品链接转换为带推广参数的链接，确保用户通过该链接购买后佣金可追踪。

**各平台转链API映射**：

| 平台 | 转链API | 输出格式 |
|------|---------|----------|
| 淘宝联盟 | `taobao.tbk.privilege.get` | 推广链接 + 淘口令 |
| 京东联盟 | `jd.union.open.promotion.common.get` | 推广链接（长链 + 短链） |
| 拼多多联盟 | `pdd.ddk.goods.promotion.url.generate` | 推广链接 + 小程序路径 |

**用户归因参数**（核心）：

| 平台 | 归因方式 | 参数说明 |
|------|----------|----------|
| 淘宝联盟 | adzone_id + external_info | 推广位ID + 外部追踪参数 |
| 京东联盟 | subUnionId | 子联盟标识，传入会员ID的映射值 |
| 拼多多联盟 | custom_parameters | JSON格式，`{"uid":"会员ID"}` |

**功能要求**：
- 转链时自动注入用户归因参数
- 支持生成多种格式：长链接、短链接、淘口令、小程序路径、二维码
- 转链结果缓存（相同用户+相同商品短时间内返回缓存）
- 记录转链日志

---

### 4.5 订单追踪模块

#### 4.5.1 订单定时同步

通过定时任务轮询各平台的订单查询API，将CPS订单同步到本地数据库。

**各平台同步接口**：

| 平台 | 同步接口 | 推荐查询方式 |
|------|----------|--------------|
| 淘宝联盟 | `taobao.tbk.order.details.get` | 按更新时间增量查询 |
| 京东联盟 | `jd.union.open.order.query` | 按更新时间查询（90天内） |
| 拼多多联盟 | `pdd.ddk.order.list.increment.get` | 按最后更新时间增量同步 |

**定时任务配置**：

| 任务 | 频率 | 说明 |
|------|------|------|
| 新订单同步 | 每5分钟 | 查询最近20分钟内新增/更新的订单（重叠保护） |
| 订单状态更新 | 每30分钟 | 更新已有订单的状态变化 |
| 退款订单同步 | 每1小时 | 查询退款订单并更新状态 |
| 历史订单补偿 | 每天凌晨 | 补偿同步可能遗漏的订单 |

**技术要求**：
- 基于 yudao-module-infra 的定时任务框架实现
- 支持手动触发同步
- 同步失败自动重试（最多3次，间隔递增）
- 防重复处理（基于平台+平台订单号去重）
- 记录同步日志

#### 4.5.2 订单归因匹配

将平台返回的CPS订单与本系统的会员关联。

| 平台 | 归因字段 | 匹配方式 |
|------|----------|----------|
| 淘宝 | adzone_id + external_info | 通过推广位 + 外部参数映射用户 |
| 京东 | subUnionId | 直接匹配会员标识 |
| 拼多多 | custom_parameters 中的 uid | 解析JSON提取会员ID |

**归因失败处理**：
- 无法匹配会员的订单标记为"未归因"
- 管理员可在后台手动绑定会员
- 记录归因失败原因日志

#### 4.5.3 统一订单状态机

将各平台的订单状态映射为统一状态：

```
              ┌─────────┐
              │  已下单   │
              └────┬────┘
                   │
              ┌────▼────┐
              │  已付款   │
              └────┬────┘
                   │
        ┌──────────┼──────────┐
        │          │          │
   ┌────▼────┐┌────▼────┐┌────▼────┐
   │  已收货   ││  已退款   ││  已失效   │
   └────┬────┘└─────────┘└─────────┘
        │
   ┌────▼────┐
   │  已结算   │  平台佣金已确认
   └────┬────┘
        │
   ┌────▼────┐
   │  已到账   │  返利已入会员钱包
   └─────────┘
```

**各平台状态映射表**：

| 统一状态 | 淘宝联盟 | 京东联盟 | 拼多多联盟 |
|----------|----------|----------|------------|
| 已付款 | 已付款(12) | 已下单 | 已付款(1) |
| 已收货 | 已确认收货(14) | 已完成 | 已确认收货(2) |
| 已结算 | 已结算(3) | 有效-Valid | 已结算(5) |
| 已退款 | 已退款(13) | 无效-Refund | 退款状态 |
| 已失效 | 无效状态(9) | 无效-其他 | 已失效(8) |

#### 4.5.4 退款订单处理

1. 同步任务检测到订单状态变为"已退款"
2. 查找对应的返利记录
3. 如果返利已入账到会员钱包：
   - 扣减会员钱包余额
   - 余额不足时记录负债，从后续返利中扣除
4. 如果返利尚未入账：取消待结算的返利记录
5. 通知会员退款导致的返利变动

---

### 4.6 佣金结算模块

#### 4.6.1 佣金计算引擎

根据平台佣金和系统配置，计算会员实际应得的返利金额。

**计算公式**：

```
平台佣金 = 商品实付金额 x 商品佣金比例
平台服务费 = 平台佣金 x 平台技术服务费率（如10%）
可分配佣金 = 平台佣金 - 平台服务费
会员返利 = 可分配佣金 x 会员返利比例
平台利润 = 可分配佣金 - 会员返利
```

**示例**：

```
商品售价：¥100，优惠券：¥20，佣金比例：20%
实际成交额 = ¥80
平台佣金 = ¥80 x 20% = ¥16
平台服务费（10%）= ¥16 x 10% = ¥1.6
可分配佣金 = ¥16 - ¥1.6 = ¥14.4

金卡会员返利（70%）= ¥14.4 x 70% = ¥10.08
普通会员返利（50%）= ¥14.4 x 50% = ¥7.20
平台利润（金卡）= ¥14.4 - ¥10.08 = ¥4.32
```

#### 4.6.2 返利比例配置（多维度）

支持多层级的返利比例配置，**优先级从高到低**：

| 层级 | 说明 | 示例 |
|------|------|------|
| 1. 会员个人专属 | 管理员为特定会员设置的专属比例 | 张三返利85% |
| 2. 会员等级 + 平台 | 按会员等级 + CPS平台的组合配置 | 金卡+淘宝=75% |
| 3. 会员等级 | 按会员等级的统一配置 | 金卡=70% |
| 4. 平台级别 | 按CPS平台的统一配置 | 淘宝=65% |
| 5. 全局默认 | 系统全局默认返利比例 | 默认50% |

**会员等级返利比例参考**（复用 MemberLevelDO）：

| 会员等级 | 返利比例 | 升级条件 |
|----------|----------|----------|
| 普通会员 | 50% | 注册即获得 |
| 银卡会员 | 60% | 经验值 >= 1000 |
| 金卡会员 | 70% | 经验值 >= 5000 |
| 铂金会员 | 80% | 经验值 >= 20000 |
| 钻石会员 | 95% | 特邀 / 付费VIP |

**会员个人专属返利设置**：

管理员可为特定会员设置个性化返利比例，覆盖等级规则：

| 字段 | 说明 |
|------|------|
| memberId | 会员ID |
| platformCode | 平台编码（NULL表示全平台生效） |
| rebateRate | 专属返利比例 |
| maxRebateAmount | 单笔最大返利金额（0表示不限） |
| effectiveStartTime | 生效开始时间 |
| effectiveEndTime | 失效时间（NULL表示永久） |
| remark | 备注说明 |

#### 4.6.3 返利展示控制

管理员可配置会员在前端查询时看到的返利信息展示方式。

| 配置项 | 说明 |
|--------|------|
| 展示模式 | REAL（显示真实返利金额）/ DISPLAY（显示自定义展示值） |
| 展示计算基准 | COMMISSION（基于佣金的百分比）/ PRICE（基于商品价格的百分比） |
| 展示精度 | 返利金额保留小数位数（0/1/2位） |
| 是否显示佣金比例 | 控制是否向会员展示商品的佣金比例 |
| 返利文案模板 | 自定义展示文案，如"预估返 ¥{amount}" |
| 未登录展示 | 未登录用户看到的提示（如"登录查看返利"） |

**示例**：商品价格100元，佣金20元，实际返利12元（60%佣金）
- 真实模式：显示"预估返利 ¥12.00"
- 展示模式（基于价格12%）：显示"返利 12%（约 ¥12.00）"

#### 4.6.4 返利入账处理

**入账条件**：
- 订单状态为"已结算"（平台已确认佣金）
- 已超过售后保护期（可配置，默认15天）
- 订单未发生退款

**入账流程**：
1. 定时任务扫描满足入账条件的订单
2. 根据下单时的会员等级计算返利金额
3. 调用 `PayWalletService` 增加会员钱包余额
4. 创建返利记录和钱包交易记录
5. 推送入账通知给会员
6. 更新订单状态为"已到账"

---

### 4.7 会员返利模块

#### 4.7.1 返利账户管理

复用 `PayWalletDO`（用户钱包），记录返利余额和交易明细：

| 信息 | 说明 |
|------|------|
| 可提现余额 | 已结算、可申请提现的金额 |
| 待结算金额 | 已下单但尚未结算的预估返利 |
| 累计收入 | 历史累计返利总额 |
| 累计提现 | 历史累计提现总额 |

#### 4.7.2 返利明细查询

会员可查询自己的返利收支明细。

**明细类型**：

| 类型 | 说明 |
|------|------|
| CPS返利入账 | 订单结算后的返利到账 |
| 返利扣回 | 退款导致的返利扣回 |
| 提现 | 用户发起的提现 |
| 提现退回 | 提现失败退回 |
| 邀请奖励 | 邀请新用户的奖励（可选功能） |
| 系统调整 | 管理员手动调整 |

**查询条件**：时间范围、明细类型、关联平台、金额范围

#### 4.7.3 提现管理

会员将返利余额提现到支付宝或微信，复用 `yudao-module-pay` 的钱包与转账能力。

**提现规则配置**：

| 规则 | 默认值 | 说明 |
|------|--------|------|
| 最低提现金额 | ¥1.00 | 低于此金额不可提现 |
| 单次最高提现 | ¥5,000 | 单次提现上限 |
| 每日提现次数 | 3次 | 每日提现次数限制 |
| 提现手续费 | 0% | 可选收取手续费 |
| 审核方式 | 自动 | 自动/人工/混合（超过阈值人工审核） |
| 人工审核阈值 | ¥500 | 超过此金额需人工审核 |

**提现流程**：

```
会员发起提现 → 校验余额和规则 → 创建提现申请
  → [自动审核 / 人工审核] → 审核通过 → 调用转账API
  → 转账成功 → 扣减余额 → 记录流水 → 通知会员
```

**提现方式**：
- 支付宝转账（企业转账API）
- 微信零钱转账（企业付款API）
- 银行卡转账

#### 4.7.4 邀请返利（可选功能）

支持会员通过推广码邀请新用户，获取额外返利。参考 yudao-module-trade 中的分销模块设计。

| 层级 | 比例 | 说明 |
|------|------|------|
| 一级邀请 | 被邀请人返利的 5%~10% | 直接邀请的用户 |
| 二级邀请 | 被邀请人返利的 1%~3% | 间接邀请的用户 |

> 邀请层级不超过二级，符合合规要求。

---

### 4.8 数据统计模块

#### 4.8.1 运营数据看板（管理后台）

| 指标 | 说明 |
|------|------|
| 今日订单数 | 各平台今日新增订单数 |
| 今日预估佣金 | 今日订单预估佣金总额 |
| 今日预估返利 | 今日需返利给会员的金额 |
| 待结算佣金 | 尚未被平台结算的佣金总额 |
| 已结算佣金 | 已被平台确认的佣金总额 |
| 总返利支出 | 累计返利给会员的金额 |
| 平台利润 | 佣金 - 返利 - 服务费 |
| 活跃会员数 | 近7日/30日有查询或下单的会员数 |

#### 4.8.2 按平台统计

- 各平台订单量趋势（日/周/月）
- 各平台佣金收入趋势
- 各平台转化率（查询 vs 下单）
- 各平台利润贡献占比

#### 4.8.3 会员统计

- 新增会员趋势
- 活跃会员趋势
- 会员等级分布
- TOP返利会员排行

#### 4.8.4 收益报表

- 平台总收入（各平台佣金总额）
- 用户返利支出
- 平台净利润
- 收益趋势图

---

### 4.9 系统配置模块

#### 4.9.1 全局返利规则配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 默认返利比例 | 未配置等级时的默认比例 | 50% |
| 返利计算精度 | 小数点后保留位数 | 2位 |
| 返利取整方式 | 四舍五入/向下取整 | 向下取整 |
| 入账延迟天数 | 订单结算后延迟入账天数 | 0天 |
| 售后保护期 | 确认收货后保护天数 | 15天 |

#### 4.9.2 提现规则配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 最低提现金额 | 单次最低提现额 | ¥1.00 |
| 最高提现金额 | 单次最高提现额 | ¥5,000 |
| 每日提现上限 | 每天最多提现次数 | 3次 |
| 提现手续费率 | 0表示免手续费 | 0% |
| 审核方式 | 自动/人工/混合 | 自动 |

#### 4.9.3 风控规则配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 同设备多账号检测 | 检测同一设备注册多个账号 | 开启 |
| 异常退款率阈值 | 退款率超过阈值预警 | 30% |
| 日查询次数上限 | 单会员每日最大查询次数 | 500次 |
| 黑名单管理 | 异常用户拉黑，冻结返利和提现 | - |

#### 4.9.4 消息通知配置

| 通知场景 | 方式 | 触发条件 |
|----------|------|----------|
| 订单追踪成功 | 站内信/微信 | 检测到新的已付款订单 |
| 返利到账 | 站内信/微信 | 返利结算到钱包 |
| 提现审核结果 | 站内信/微信/短信 | 提现通过或驳回 |
| 提现到账 | 站内信/微信/短信 | 提现打款成功 |
| 订单退款 | 站内信/微信 | 订单退款且返利扣回 |
| 会员升级 | 站内信/微信 | 会员等级提升 |

---

### 4.10 MCP AI接口模块

本模块基于 **MCP（Model Context Protocol）** 协议标准，为CPS系统构建一套可供AI Agent直接调用的接口层。通过MCP Server暴露的 Tools、Resources、Prompts 三类原语，AI应用（如聊天机器人、智能助手、自动化工作流等）能够以标准化方式接入CPS系统，实现定制化商品查询、跨平台比价、推广链接生成等核心功能的AI驱动调用。

#### 4.10.1 MCP协议概述

**MCP（Model Context Protocol）** 是AI应用与外部系统之间的标准化通信协议，基于 JSON-RPC 2.0，定义了三种核心原语：

| 原语 | 说明 | 在CPS系统中的作用 |
|------|------|-------------------|
| **Tools** | AI可调用的可执行函数 | 商品搜索、比价、转链、订单查询等操作 |
| **Resources** | AI可读取的数据源（只读） | 平台配置、返利规则、会员画像、统计数据等 |
| **Prompts** | 预定义的交互模板 | 找最优价、比价分析、省钱策略等场景化提示 |

**MCP Server生命周期**：

```
AI Client                    CPS MCP Server
   │                              │
   │  ── initialize ──────────>   │  能力协商
   │  <── capabilities ────────   │  返回支持的tools/resources/prompts
   │                              │
   │  ── tools/list ──────────>   │  获取可用工具列表
   │  <── tool definitions ────   │
   │                              │
   │  ── tools/call ──────────>   │  调用具体工具
   │  <── result ─────────────    │
   │                              │
   │  ── resources/read ──────>   │  读取数据资源
   │  <── resource content ────   │
   │                              │
   │  ── prompts/get ─────────>   │  获取提示模板
   │  <── prompt messages ─────   │
   │                              │
   │  <── notifications ───────   │  实时通知（如工具列表变更）
   │                              │
```

#### 4.10.2 MCP Server架构设计

**传输层支持**：

| 传输方式 | 适用场景 | 端点 | 说明 |
|----------|----------|------|------|
| Streamable HTTP | 远程AI Agent接入 | `/mcp/cps` | 支持SSE流式响应，适合生产环境 |
| STDIO | 本地开发调试 | 标准输入输出 | 适合本地AI开发工具集成 |

**Server能力声明（Capabilities）**：

```json
{
  "capabilities": {
    "tools": {
      "listChanged": true
    },
    "resources": {
      "subscribe": true,
      "listChanged": true
    },
    "prompts": {
      "listChanged": true
    }
  },
  "serverInfo": {
    "name": "cps-mcp-server",
    "version": "1.0.0"
  }
}
```

**Spring Boot集成方式**：

MCP Server作为 `yudao-module-cps-biz` 的一部分，通过Spring Boot自动配置加载：

```java
@Configuration
@ConditionalOnProperty(prefix = "yudao.cps.mcp", name = "enabled", havingValue = "true")
public class CpsMcpServerConfig {

    @Bean
    public CpsMcpServer cpsMcpServer(
            List<CpsMcpTool> tools,
            List<CpsMcpResource> resources,
            List<CpsMcpPrompt> prompts,
            CpsMcpAuthService authService) {
        return new CpsMcpServer(tools, resources, prompts, authService);
    }
}
```

**配置项**（`application.yaml`）：

```yaml
yudao:
  cps:
    mcp:
      enabled: true                          # 是否启用MCP Server
      transport: http                        # 传输方式: http / stdio
      http:
        endpoint: /mcp/cps                   # HTTP端点路径
        sse-enabled: true                    # 是否启用SSE流式推送
      auth:
        enabled: true                        # 是否启用鉴权
        type: bearer                         # 鉴权方式: bearer / api-key
      rate-limit:
        enabled: true                        # 是否启用限流
        max-requests-per-minute: 60          # 每分钟最大请求数
      logging:
        enabled: true                        # 是否记录MCP请求日志
        log-request-body: true               # 是否记录请求体
        log-response-body: false             # 是否记录响应体
```

#### 4.10.3 MCP Tools 定义

MCP Tools 是AI Agent可直接调用的可执行函数。每个Tool定义包含名称、描述、输入参数Schema和执行逻辑。

##### Tool 1: cps_search_goods（商品搜索）

```json
{
  "name": "cps_search_goods",
  "title": "CPS商品搜索",
  "description": "根据关键词在指定或全部CPS平台搜索商品，返回商品列表及预估返利信息。支持按价格区间、是否有优惠券、排序方式等条件筛选。",
  "inputSchema": {
    "type": "object",
    "properties": {
      "keyword": {
        "type": "string",
        "description": "搜索关键词，支持商品名称、品牌、型号等"
      },
      "platform_code": {
        "type": "string",
        "enum": ["taobao", "jingdong", "pinduoduo"],
        "description": "指定平台编码，不指定则查询全部启用平台"
      },
      "price_min": {
        "type": "integer",
        "description": "最低价格（单位：分）"
      },
      "price_max": {
        "type": "integer",
        "description": "最高价格（单位：分）"
      },
      "has_coupon": {
        "type": "boolean",
        "description": "是否仅查询有优惠券的商品"
      },
      "sort_type": {
        "type": "string",
        "enum": ["comprehensive", "price_asc", "price_desc", "sales_desc", "commission_desc", "rebate_desc"],
        "description": "排序方式"
      },
      "member_id": {
        "type": "integer",
        "description": "会员ID，用于计算个性化返利金额"
      },
      "page_no": {
        "type": "integer",
        "description": "页码，默认1"
      },
      "page_size": {
        "type": "integer",
        "description": "每页数量，默认20，最大50"
      }
    },
    "required": ["keyword"]
  }
}
```

**返回结果**：统一商品列表（含 platformCode, title, price, finalPrice, couponAmount, commissionRate, estimateRebate, monthSales, shopName 等字段），与现有 `/app-api/cps/goods/search` 接口复用同一 Service 层。

##### Tool 2: cps_compare_prices（多平台比价）

```json
{
  "name": "cps_compare_prices",
  "title": "多平台比价",
  "description": "输入关键词或商品链接，同时查询所有已启用CPS平台的价格和返利，返回比价结果。结果中标注最低价平台、最高返利平台和综合最优平台。",
  "inputSchema": {
    "type": "object",
    "properties": {
      "keyword": {
        "type": "string",
        "description": "搜索关键词或商品链接/口令"
      },
      "member_id": {
        "type": "integer",
        "description": "会员ID，用于个性化返利计算"
      },
      "sort_by": {
        "type": "string",
        "enum": ["best_value", "lowest_price", "highest_rebate"],
        "description": "比价排序方式，best_value=实付最低（默认）"
      }
    },
    "required": ["keyword"]
  }
}
```

**返回结果**：各平台比价条目列表 + `bestPricePlatform`、`bestRebatePlatform`、`bestValuePlatform` 标记。

##### Tool 3: cps_generate_link（推广链接生成）

```json
{
  "name": "cps_generate_link",
  "title": "生成推广链接",
  "description": "为指定平台的商品生成带用户归因参数的推广链接。支持生成长链接、短链接、淘口令等格式。用户通过该链接购买商品后，系统可追踪订单并计算返利。",
  "inputSchema": {
    "type": "object",
    "properties": {
      "platform_code": {
        "type": "string",
        "description": "平台编码"
      },
      "item_id": {
        "type": "string",
        "description": "平台商品ID"
      },
      "item_url": {
        "type": "string",
        "description": "商品链接（与item_id二选一）"
      },
      "member_id": {
        "type": "integer",
        "description": "会员ID，用于注入归因参数"
      },
      "format": {
        "type": "string",
        "enum": ["long_url", "short_url", "tpwd", "miniapp"],
        "description": "输出格式：长链接/短链接/淘口令/小程序路径"
      }
    },
    "required": ["member_id"]
  }
}
```

##### Tool 4: cps_parse_content（链接/口令解析）

```json
{
  "name": "cps_parse_content",
  "title": "解析商品链接或口令",
  "description": "输入商品URL、淘口令或其他平台口令，自动识别平台并解析出商品信息（商品ID、标题、价格、佣金、返利等）。",
  "inputSchema": {
    "type": "object",
    "properties": {
      "content": {
        "type": "string",
        "description": "待解析的内容（商品URL、淘口令等）"
      },
      "member_id": {
        "type": "integer",
        "description": "会员ID，用于计算个性化返利"
      }
    },
    "required": ["content"]
  }
}
```

##### Tool 5: cps_query_orders（订单查询）

```json
{
  "name": "cps_query_orders",
  "title": "查询CPS订单",
  "description": "查询指定会员的CPS订单列表，支持按平台、状态、时间范围筛选。返回订单详情及返利状态。",
  "inputSchema": {
    "type": "object",
    "properties": {
      "member_id": {
        "type": "integer",
        "description": "会员ID"
      },
      "platform_code": {
        "type": "string",
        "description": "平台编码筛选"
      },
      "order_status": {
        "type": "integer",
        "description": "订单状态：0已下单 1已付款 2已收货 3已结算 4已到账 -1已退款 -2已失效"
      },
      "start_time": {
        "type": "string",
        "format": "date-time",
        "description": "开始时间"
      },
      "end_time": {
        "type": "string",
        "format": "date-time",
        "description": "结束时间"
      },
      "page_no": {
        "type": "integer",
        "description": "页码"
      },
      "page_size": {
        "type": "integer",
        "description": "每页数量"
      }
    },
    "required": ["member_id"]
  }
}
```

##### Tool 6: cps_get_rebate_summary（返利汇总）

```json
{
  "name": "cps_get_rebate_summary",
  "title": "查询返利汇总",
  "description": "查询指定会员的返利汇总信息，包括可提现余额、待结算金额、累计收入、累计提现等。",
  "inputSchema": {
    "type": "object",
    "properties": {
      "member_id": {
        "type": "integer",
        "description": "会员ID"
      }
    },
    "required": ["member_id"]
  }
}
```

##### Tool 7: cps_get_recommendations（商品推荐）

```json
{
  "name": "cps_get_recommendations",
  "title": "获取推荐商品",
  "description": "获取系统推荐的CPS商品列表，支持按推荐频道筛选（热销爆品、高佣精选、大额券、9.9包邮等）。",
  "inputSchema": {
    "type": "object",
    "properties": {
      "channel": {
        "type": "string",
        "enum": ["hot_sale", "high_commission", "big_coupon", "low_price", "brand_sale"],
        "description": "推荐频道"
      },
      "platform_code": {
        "type": "string",
        "description": "指定平台"
      },
      "member_id": {
        "type": "integer",
        "description": "会员ID"
      },
      "page_size": {
        "type": "integer",
        "description": "返回数量，默认10"
      }
    }
  }
}
```

##### MCP Tools 汇总表

| Tool名称 | 对应CPS功能 | 复用的Service层 | 鉴权要求 |
|----------|-------------|-----------------|----------|
| cps_search_goods | 商品搜索 | CpsGoodsSearchService | 可选会员身份 |
| cps_compare_prices | 多平台比价 | CpsGoodsSearchService | 可选会员身份 |
| cps_generate_link | 推广链接生成 | CpsPromotionLinkService | 必须会员身份 |
| cps_parse_content | 链接/口令解析 | CpsGoodsParseService | 可选会员身份 |
| cps_query_orders | 订单查询 | CpsOrderService | 必须会员身份 |
| cps_get_rebate_summary | 返利汇总 | CpsRebateService（新增） | 必须会员身份 |
| cps_get_recommendations | 商品推荐 | CpsGoodsSearchService | 可选会员身份 |

#### 4.10.4 MCP Resources 定义

MCP Resources 是AI Agent可读取的只读数据源，通过 URI 方式标识和访问。

| Resource URI | 名称 | 描述 | MIME类型 |
|-------------|------|------|----------|
| `cps://platforms` | 平台列表 | 所有已配置CPS平台的基本信息和启用状态 | application/json |
| `cps://platforms/{code}` | 平台详情 | 指定平台的详细配置（脱敏后的公开信息） | application/json |
| `cps://rebate-rules` | 返利规则总览 | 全部返利规则配置（等级维度、平台维度） | application/json |
| `cps://rebate-rules/level/{levelId}` | 等级返利规则 | 指定会员等级的返利规则 | application/json |
| `cps://member/{memberId}/profile` | 会员CPS画像 | 会员的CPS相关信息（等级、返利比例、累计数据） | application/json |
| `cps://statistics/daily` | 今日统计 | 今日运营统计数据（订单量、佣金、返利、利润） | application/json |
| `cps://statistics/daily/{date}` | 历史日统计 | 指定日期的统计快照数据 | application/json |
| `cps://hot-keywords` | 热门搜索词 | 当前热门搜索关键词列表（Top 20） | application/json |
| `cps://member-levels` | 会员等级体系 | 全部会员等级及对应的返利比例信息 | application/json |

**Resource 示例 - `cps://platforms`**：

```json
{
  "uri": "cps://platforms",
  "name": "CPS平台列表",
  "mimeType": "application/json",
  "content": [
    {
      "platformCode": "taobao",
      "platformName": "淘宝联盟",
      "status": "enabled",
      "platformFeeRate": 0.10,
      "supportedFeatures": ["search", "link", "tpwd", "order_sync"]
    },
    {
      "platformCode": "jingdong",
      "platformName": "京东联盟",
      "status": "enabled",
      "platformFeeRate": 0.10,
      "supportedFeatures": ["search", "link", "order_sync"]
    },
    {
      "platformCode": "pinduoduo",
      "platformName": "拼多多联盟",
      "status": "enabled",
      "platformFeeRate": 0.10,
      "supportedFeatures": ["search", "link", "miniapp", "order_sync"]
    }
  ]
}
```

**Resource 示例 - `cps://member/{memberId}/profile`**：

```json
{
  "uri": "cps://member/10001/profile",
  "name": "会员CPS画像",
  "mimeType": "application/json",
  "content": {
    "memberId": 10001,
    "nickname": "张三",
    "levelName": "金卡会员",
    "rebateRate": 0.70,
    "hasCustomRebate": false,
    "walletBalance": 12850,
    "pendingRebate": 5630,
    "totalIncome": 128000,
    "totalWithdraw": 100000,
    "orderCount30d": 25,
    "rebateAmount30d": 8520
  }
}
```

**Resources 订阅机制**：

AI Agent可订阅特定Resource的变更通知，当数据更新时MCP Server主动推送 `notifications/resources/updated` 事件。

| 可订阅Resource | 变更触发条件 |
|---------------|-------------|
| `cps://platforms` | 平台启用/禁用状态变更 |
| `cps://rebate-rules` | 返利规则配置变更 |
| `cps://hot-keywords` | 热词列表每小时自动刷新 |

#### 4.10.5 MCP Prompts 定义

MCP Prompts 是预定义的交互模板，AI Agent可获取并根据上下文填充参数，生成结构化的对话消息序列。

##### Prompt 1: find_best_deal（找最优惠商品）

```json
{
  "name": "find_best_deal",
  "title": "找最优惠商品",
  "description": "根据用户需求，在所有平台中找到价格最低或综合最优的商品，并展示返利信息和购买建议。",
  "arguments": [
    {
      "name": "product_description",
      "description": "用户想买的商品描述",
      "required": true
    },
    {
      "name": "budget",
      "description": "用户预算（元）",
      "required": false
    },
    {
      "name": "member_id",
      "description": "会员ID",
      "required": false
    }
  ]
}
```

**Prompt生成的消息模板**：

```json
{
  "messages": [
    {
      "role": "system",
      "content": "你是CPS返利助手，帮助用户在淘宝、京东、拼多多等平台找到最优惠的商品。你可以调用 cps_search_goods 搜索商品，调用 cps_compare_prices 进行比价，最终给出购买建议。"
    },
    {
      "role": "user",
      "content": "我想买{{product_description}}{{#budget}}，预算{{budget}}元以内{{/budget}}。请帮我找全网最优惠的，并告诉我能返利多少。"
    }
  ]
}
```

##### Prompt 2: compare_across_platforms（跨平台比价分析）

```json
{
  "name": "compare_across_platforms",
  "title": "跨平台比价分析",
  "description": "对指定商品进行全平台比价，输出结构化的比价报告，包括各平台价格、优惠券、返利、实付价对比。",
  "arguments": [
    {
      "name": "product_keyword",
      "description": "商品关键词或链接",
      "required": true
    },
    {
      "name": "member_id",
      "description": "会员ID",
      "required": false
    }
  ]
}
```

##### Prompt 3: optimize_savings（省钱策略推荐）

```json
{
  "name": "optimize_savings",
  "title": "省钱策略推荐",
  "description": "分析用户的购物需求和会员等级，推荐最优的省钱策略，包括选择哪个平台、使用哪些优惠券、预估能省多少钱。",
  "arguments": [
    {
      "name": "shopping_list",
      "description": "用户想购买的商品列表（逗号分隔）",
      "required": true
    },
    {
      "name": "member_id",
      "description": "会员ID",
      "required": true
    }
  ]
}
```

##### Prompt 4: explain_rebate（返利规则解释）

```json
{
  "name": "explain_rebate",
  "title": "返利规则解释",
  "description": "以通俗易懂的方式向用户解释CPS返利的计算规则、会员等级对返利的影响、返利到账时间等。",
  "arguments": [
    {
      "name": "member_id",
      "description": "会员ID（可选，用于展示个性化等级信息）",
      "required": false
    },
    {
      "name": "question_focus",
      "description": "用户关注的问题焦点：如 calculation（计算规则）、timeline（到账时间）、level（等级影响）",
      "required": false
    }
  ]
}
```

##### MCP Prompts 汇总表

| Prompt名称 | 使用场景 | 依赖的Tools | 依赖的Resources |
|-----------|----------|------------|----------------|
| find_best_deal | 用户询问"帮我找XX最便宜的" | cps_search_goods, cps_compare_prices | cps://platforms |
| compare_across_platforms | 用户询问"这个商品哪个平台最划算" | cps_compare_prices | cps://platforms |
| optimize_savings | 用户提供购物清单要求省钱建议 | cps_search_goods, cps_compare_prices, cps_generate_link | cps://member/{id}/profile |
| explain_rebate | 用户询问返利规则如何计算 | cps_get_rebate_summary | cps://rebate-rules, cps://member-levels |

#### 4.10.6 MCP访问控制

**鉴权机制**：

| 鉴权方式 | 说明 | 适用场景 |
|----------|------|----------|
| Bearer Token | HTTP Authorization Header 携带 JWT Token | 标准AI Agent接入 |
| API Key | HTTP Header `X-API-Key` | 简化集成场景 |
| 无鉴权 | 部分只读Resource和不涉及用户数据的Tool | 公开数据查询 |

**权限级别**：

| 级别 | 可访问范围 | 说明 |
|------|-----------|------|
| public | 公开Resources（platforms, hot-keywords, member-levels）+ 不含member_id的搜索 | 无需鉴权 |
| member | 指定会员维度的全部Tools和Resources | 需携带会员Token |
| admin | 全部管理端Tools和Resources（如统计数据） | 需管理员Token |

**MCP鉴权统一接口**：

```java
/**
 * MCP鉴权服务接口
 */
public interface CpsMcpAuthService {

    /**
     * 验证MCP请求的鉴权信息
     * @param authHeader 鉴权头（Bearer Token 或 API Key）
     * @return 鉴权结果，包含权限级别和关联的会员/管理员ID
     */
    McpAuthResult authenticate(String authHeader);

    /**
     * 检查是否有权限调用指定Tool
     */
    boolean canCallTool(McpAuthResult auth, String toolName, Map<String, Object> params);

    /**
     * 检查是否有权限读取指定Resource
     */
    boolean canReadResource(McpAuthResult auth, String resourceUri);
}
```

#### 4.10.7 MCP请求日志

记录所有MCP请求，用于审计、调试和用量统计。

| 字段 | 说明 |
|------|------|
| 请求ID | MCP JSON-RPC请求的唯一标识 |
| 请求方法 | tools/call, resources/read, prompts/get 等 |
| 工具/资源名称 | 调用的Tool名或Resource URI |
| 请求参数 | 输入参数（可配置脱敏） |
| 调用者标识 | Token关联的会员ID或API Key标识 |
| 响应状态 | 成功/失败 |
| 错误信息 | 失败时的错误描述 |
| 耗时 | 请求处理耗时（毫秒） |
| 调用时间 | 请求时间戳 |
| IP地址 | 请求来源IP |

## 五、数据库设计

### 5.1 核心表清单

| 表名 | 说明 |
|------|------|
| `cps_platform_config` | CPS平台配置 |
| `cps_platform_adzone` | 推广位管理 |
| `cps_order` | CPS订单 |
| `cps_rebate_config` | 返利比例配置 |
| `cps_member_rebate_config` | 会员个人返利配置 |
| `cps_rebate_record` | 返利记录 |
| `cps_goods_query_log` | 商品查询日志 |
| `cps_promotion_link_log` | 推广链接日志 |
| `cps_statistics_daily` | 每日统计快照 |
| `cps_mcp_api_key` | MCP接口API Key配置 |
| `cps_mcp_request_log` | MCP请求日志 |

### 5.2 表结构定义

#### cps_platform_config（平台配置表）

```sql
CREATE TABLE cps_platform_config (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    platform_code   varchar(32)  NOT NULL COMMENT '平台编码（taobao/jingdong/pinduoduo等）',
    platform_name   varchar(64)  NOT NULL COMMENT '平台名称',
    platform_icon   varchar(512) DEFAULT NULL COMMENT '平台图标URL',
    app_key         varchar(128) NOT NULL COMMENT 'AppKey',
    app_secret      varchar(256) NOT NULL COMMENT 'AppSecret（加密存储）',
    api_base_url    varchar(256) DEFAULT NULL COMMENT 'API基础地址',
    access_token    varchar(512) DEFAULT NULL COMMENT '访问令牌（加密存储）',
    default_pid     varchar(128) DEFAULT NULL COMMENT '默认推广位ID',
    platform_fee_rate decimal(5,4) NOT NULL DEFAULT 0.1000 COMMENT '平台服务费率',
    sort            int          NOT NULL DEFAULT 0 COMMENT '排序',
    status          tinyint      NOT NULL DEFAULT 0 COMMENT '状态（0启用 1禁用）',
    ext_config      json         DEFAULT NULL COMMENT '扩展配置（JSON）',
    remark          varchar(256) DEFAULT NULL COMMENT '备注',
    creator         varchar(64)  DEFAULT NULL COMMENT '创建者',
    create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater         varchar(64)  DEFAULT NULL COMMENT '更新者',
    update_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id       bigint       NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_code (platform_code, tenant_id, deleted)
) COMMENT = 'CPS平台配置表';
```

#### cps_platform_adzone（推广位表）

```sql
CREATE TABLE cps_platform_adzone (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    platform_code   varchar(32)  NOT NULL COMMENT '平台编码',
    adzone_id       varchar(128) NOT NULL COMMENT '推广位ID',
    adzone_name     varchar(128) DEFAULT NULL COMMENT '推广位名称',
    adzone_type     tinyint      NOT NULL DEFAULT 0 COMMENT '类型（0通用 1渠道专属 2用户专属）',
    bind_id         bigint       DEFAULT NULL COMMENT '绑定的渠道/用户ID',
    is_default      bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否默认推广位',
    status          tinyint      NOT NULL DEFAULT 0 COMMENT '状态（0启用 1禁用）',
    creator         varchar(64)  DEFAULT NULL COMMENT '创建者',
    create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater         varchar(64)  DEFAULT NULL COMMENT '更新者',
    update_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id       bigint       NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_platform_code (platform_code)
) COMMENT = 'CPS推广位表';
```

#### cps_order（CPS订单表）

```sql
CREATE TABLE cps_order (
    id                      bigint          NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id               bigint          DEFAULT NULL COMMENT '会员ID（关联member_user.id）',
    platform_code           varchar(32)     NOT NULL COMMENT '平台编码',
    platform_order_no       varchar(64)     NOT NULL COMMENT '平台订单号',
    platform_parent_order_no varchar(64)    DEFAULT NULL COMMENT '平台父订单号',
    goods_id                varchar(64)     DEFAULT NULL COMMENT '平台商品ID',
    goods_name              varchar(256)    DEFAULT NULL COMMENT '商品名称',
    goods_pic_url           varchar(512)    DEFAULT NULL COMMENT '商品图片URL',
    goods_price             int             NOT NULL DEFAULT 0 COMMENT '商品原价（分）',
    actual_price            int             NOT NULL DEFAULT 0 COMMENT '实际成交价（分）',
    quantity                int             NOT NULL DEFAULT 1 COMMENT '购买数量',
    coupon_amount           int             NOT NULL DEFAULT 0 COMMENT '优惠券金额（分）',
    commission_rate         decimal(8,4)    NOT NULL DEFAULT 0 COMMENT '佣金比例',
    commission_amount       int             NOT NULL DEFAULT 0 COMMENT '预估/实际佣金（分）',
    platform_fee            int             NOT NULL DEFAULT 0 COMMENT '平台服务费（分）',
    distributable_amount    int             NOT NULL DEFAULT 0 COMMENT '可分配佣金（分）',
    rebate_rate             decimal(8,4)    NOT NULL DEFAULT 0 COMMENT '返利比例',
    rebate_amount           int             NOT NULL DEFAULT 0 COMMENT '会员返利金额（分）',
    order_status            tinyint         NOT NULL DEFAULT 0 COMMENT '统一订单状态（0已下单 1已付款 2已收货 3已结算 4已到账 -1已退款 -2已失效）',
    pay_time                datetime        DEFAULT NULL COMMENT '付款时间',
    confirm_time            datetime        DEFAULT NULL COMMENT '确认收货时间',
    settle_time             datetime        DEFAULT NULL COMMENT '平台结算时间',
    rebate_settle_time      datetime        DEFAULT NULL COMMENT '返利到账时间',
    refund_time             datetime        DEFAULT NULL COMMENT '退款时间',
    adzone_id               varchar(128)    DEFAULT NULL COMMENT '推广位ID',
    external_info           varchar(512)    DEFAULT NULL COMMENT '外部追踪参数',
    platform_raw_data       json            DEFAULT NULL COMMENT '平台原始订单数据',
    sync_time               datetime        DEFAULT NULL COMMENT '最后同步时间',
    remark                  varchar(256)    DEFAULT NULL COMMENT '备注',
    creator                 varchar(64)     DEFAULT NULL COMMENT '创建者',
    create_time             datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 varchar(64)     DEFAULT NULL COMMENT '更新者',
    update_time             datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 bit(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id               bigint          NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_order (platform_code, platform_order_no, tenant_id, deleted),
    KEY idx_member_id (member_id),
    KEY idx_order_status (order_status),
    KEY idx_pay_time (pay_time),
    KEY idx_settle_time (settle_time)
) COMMENT = 'CPS订单表';
```

#### cps_rebate_config（返利配置表）

```sql
CREATE TABLE cps_rebate_config (
    id                bigint          NOT NULL AUTO_INCREMENT COMMENT '主键',
    level_id          bigint          DEFAULT NULL COMMENT '会员等级ID（NULL表示不限等级）',
    platform_code     varchar(32)     DEFAULT NULL COMMENT '平台编码（NULL表示全平台）',
    rebate_rate       decimal(5,4)    NOT NULL COMMENT '返利比例（0~1）',
    min_rebate_amount int             NOT NULL DEFAULT 1 COMMENT '最低返利金额（分）',
    max_rebate_amount int             NOT NULL DEFAULT 50000 COMMENT '最高返利金额（分）',
    priority          int             NOT NULL DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
    status            tinyint         NOT NULL DEFAULT 0 COMMENT '状态（0启用 1禁用）',
    remark            varchar(256)    DEFAULT NULL COMMENT '备注',
    creator           varchar(64)     DEFAULT NULL COMMENT '创建者',
    create_time       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater           varchar(64)     DEFAULT NULL COMMENT '更新者',
    update_time       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           bit(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id         bigint          NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_level_platform (level_id, platform_code)
) COMMENT = 'CPS返利配置表';
```

#### cps_member_rebate_config（会员个人返利配置表）

```sql
CREATE TABLE cps_member_rebate_config (
    id                  bigint          NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id           bigint          NOT NULL COMMENT '会员ID',
    platform_code       varchar(32)     DEFAULT NULL COMMENT '平台编码（NULL表示全平台）',
    rebate_rate         decimal(5,4)    NOT NULL COMMENT '返利比例（0~1）',
    max_rebate_amount   int             DEFAULT 0 COMMENT '单笔最大返利（分，0不限）',
    display_rate        decimal(5,4)    DEFAULT NULL COMMENT '展示给会员的比例（NULL用真实值）',
    display_base        tinyint         DEFAULT NULL COMMENT '展示计算基准（1佣金 2商品价格）',
    effective_start     datetime        DEFAULT NULL COMMENT '生效开始时间',
    effective_end       datetime        DEFAULT NULL COMMENT '失效时间（NULL永久）',
    remark              varchar(512)    DEFAULT NULL COMMENT '备注',
    creator             varchar(64)     DEFAULT NULL COMMENT '创建者',
    create_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater             varchar(64)     DEFAULT NULL COMMENT '更新者',
    update_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             bit(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id           bigint          NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_member_id (member_id)
) COMMENT = '会员个人返利配置表';
```

#### cps_rebate_record（返利记录表）

```sql
CREATE TABLE cps_rebate_record (
    id                      bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id               bigint      NOT NULL COMMENT '会员ID',
    order_id                bigint      NOT NULL COMMENT 'CPS订单ID',
    platform_code           varchar(32) NOT NULL COMMENT '平台编码',
    commission_amount       int         NOT NULL DEFAULT 0 COMMENT '佣金金额（分）',
    rebate_rate             decimal(5,4) NOT NULL COMMENT '返利比例',
    rebate_amount           int         NOT NULL DEFAULT 0 COMMENT '返利金额（分）',
    type                    tinyint     NOT NULL COMMENT '类型（1结算入账 2退款扣回 3手动调整）',
    status                  tinyint     NOT NULL DEFAULT 0 COMMENT '状态（0待结算 1已结算 2已退回）',
    settle_time             datetime    DEFAULT NULL COMMENT '结算时间',
    wallet_transaction_id   bigint      DEFAULT NULL COMMENT '关联钱包交易ID',
    remark                  varchar(256) DEFAULT NULL COMMENT '备注',
    creator                 varchar(64) DEFAULT NULL COMMENT '创建者',
    create_time             datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 varchar(64) DEFAULT NULL COMMENT '更新者',
    update_time             datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id               bigint      NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_member_id (member_id),
    KEY idx_order_id (order_id)
) COMMENT = 'CPS返利记录表';
```

#### cps_goods_query_log（查询日志表）

```sql
CREATE TABLE cps_goods_query_log (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id       bigint       DEFAULT NULL COMMENT '会员ID',
    query_type      tinyint      NOT NULL COMMENT '查询类型（1关键词 2链接 3口令）',
    query_content   varchar(512) NOT NULL COMMENT '查询内容',
    platform_code   varchar(32)  DEFAULT NULL COMMENT '命中平台（NULL表示全平台搜索）',
    result_count    int          NOT NULL DEFAULT 0 COMMENT '结果数量',
    ip              varchar(64)  DEFAULT NULL COMMENT '查询IP',
    user_agent      varchar(256) DEFAULT NULL COMMENT 'UserAgent',
    create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    tenant_id       bigint       NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_member_id (member_id),
    KEY idx_create_time (create_time)
) COMMENT = 'CPS商品查询日志表';
```

#### cps_promotion_link_log（推广链接日志表）

```sql
CREATE TABLE cps_promotion_link_log (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id       bigint       NOT NULL COMMENT '会员ID',
    platform_code   varchar(32)  NOT NULL COMMENT '平台编码',
    goods_id        varchar(64)  NOT NULL COMMENT '商品ID',
    goods_name      varchar(256) DEFAULT NULL COMMENT '商品名称',
    promotion_link  varchar(1024) DEFAULT NULL COMMENT '推广链接',
    tpwd            varchar(256) DEFAULT NULL COMMENT '口令（淘口令等）',
    adzone_id       varchar(128) DEFAULT NULL COMMENT '使用的推广位ID',
    create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    tenant_id       bigint       NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_member_id (member_id),
    KEY idx_create_time (create_time)
) COMMENT = 'CPS推广链接日志表';
```

#### cps_statistics_daily（每日统计表）

```sql
CREATE TABLE cps_statistics_daily (
    id                  bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    statistics_date     date        NOT NULL COMMENT '统计日期',
    platform_code       varchar(32) DEFAULT NULL COMMENT '平台编码（NULL表示全平台汇总）',
    order_count         int         NOT NULL DEFAULT 0 COMMENT '订单数',
    order_amount        bigint      NOT NULL DEFAULT 0 COMMENT '订单金额（分）',
    commission_amount   bigint      NOT NULL DEFAULT 0 COMMENT '佣金金额（分）',
    rebate_amount       bigint      NOT NULL DEFAULT 0 COMMENT '返利金额（分）',
    profit_amount       bigint      NOT NULL DEFAULT 0 COMMENT '利润金额（分）',
    query_count         int         NOT NULL DEFAULT 0 COMMENT '查询次数',
    link_count          int         NOT NULL DEFAULT 0 COMMENT '转链次数',
    active_member_count int         NOT NULL DEFAULT 0 COMMENT '活跃会员数',
    new_member_count    int         NOT NULL DEFAULT 0 COMMENT '新增会员数',
    withdraw_amount     bigint      NOT NULL DEFAULT 0 COMMENT '提现金额（分）',
    create_time         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tenant_id           bigint      NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_platform (statistics_date, platform_code, tenant_id)
) COMMENT = 'CPS每日统计表';
```

#### cps_mcp_api_key（MCP接口API Key表）

```sql
CREATE TABLE cps_mcp_api_key (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    api_key         varchar(128) NOT NULL COMMENT 'API Key（加密存储）',
    api_key_prefix  varchar(16)  NOT NULL COMMENT 'API Key前缀（用于展示，如 sk-xxxx）',
    name            varchar(64)  NOT NULL COMMENT 'Key名称（标识用途）',
    auth_level      tinyint      NOT NULL DEFAULT 0 COMMENT '权限级别（0-public 1-member 2-admin）',
    bind_member_id  bigint       DEFAULT NULL COMMENT '绑定会员ID（member级别时必填）',
    allowed_tools   json         DEFAULT NULL COMMENT '允许调用的Tool列表（NULL表示全部）',
    allowed_resources json       DEFAULT NULL COMMENT '允许访问的Resource URI列表（NULL表示全部）',
    rate_limit      int          NOT NULL DEFAULT 60 COMMENT '每分钟请求上限',
    expire_time     datetime     DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
    last_used_time  datetime     DEFAULT NULL COMMENT '最后使用时间',
    total_calls     bigint       NOT NULL DEFAULT 0 COMMENT '累计调用次数',
    status          tinyint      NOT NULL DEFAULT 0 COMMENT '状态（0启用 1禁用）',
    remark          varchar(256) DEFAULT NULL COMMENT '备注',
    creator         varchar(64)  DEFAULT NULL COMMENT '创建者',
    create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater         varchar(64)  DEFAULT NULL COMMENT '更新者',
    update_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id       bigint       NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_key (api_key, tenant_id, deleted),
    KEY idx_bind_member (bind_member_id)
) COMMENT = 'MCP接口API Key表';
```

#### cps_mcp_request_log（MCP请求日志表）

```sql
CREATE TABLE cps_mcp_request_log (
    id              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id      varchar(64)   NOT NULL COMMENT 'JSON-RPC请求ID',
    method          varchar(64)   NOT NULL COMMENT '请求方法（tools/call, resources/read等）',
    tool_name       varchar(64)   DEFAULT NULL COMMENT '工具名称（tools/call时）',
    resource_uri    varchar(256)  DEFAULT NULL COMMENT '资源URI（resources/read时）',
    prompt_name     varchar(64)   DEFAULT NULL COMMENT '提示名称（prompts/get时）',
    request_params  json          DEFAULT NULL COMMENT '请求参数（可配置脱敏）',
    api_key_id      bigint        DEFAULT NULL COMMENT '关联API Key ID',
    member_id       bigint        DEFAULT NULL COMMENT '关联会员ID',
    auth_level      tinyint       DEFAULT NULL COMMENT '鉴权级别',
    response_status tinyint       NOT NULL DEFAULT 0 COMMENT '响应状态（0成功 1失败）',
    error_code      varchar(32)   DEFAULT NULL COMMENT '错误码',
    error_message   varchar(512)  DEFAULT NULL COMMENT '错误信息',
    duration_ms     int           NOT NULL DEFAULT 0 COMMENT '处理耗时（毫秒）',
    ip              varchar(64)   DEFAULT NULL COMMENT '请求来源IP',
    user_agent      varchar(256)  DEFAULT NULL COMMENT 'User-Agent',
    create_time     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    tenant_id       bigint        NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    KEY idx_request_id (request_id),
    KEY idx_api_key_id (api_key_id),
    KEY idx_member_id (member_id),
    KEY idx_method (method),
    KEY idx_create_time (create_time)
) COMMENT = 'MCP请求日志表';
```

---

## 六、接口设计

### 6.1 管理后台接口（Admin）

| 模块 | 接口路径 | 方法 | 说明 |
|------|----------|------|------|
| 平台管理 | `/admin-api/cps/platform/create` | POST | 创建CPS平台配置 |
| 平台管理 | `/admin-api/cps/platform/update` | PUT | 更新CPS平台配置 |
| 平台管理 | `/admin-api/cps/platform/delete` | DELETE | 删除CPS平台配置 |
| 平台管理 | `/admin-api/cps/platform/page` | GET | 分页查询CPS平台 |
| 平台管理 | `/admin-api/cps/platform/get` | GET | 获取CPS平台详情 |
| 平台管理 | `/admin-api/cps/platform/test` | POST | 测试平台连通性 |
| 推广位 | `/admin-api/cps/adzone/create` | POST | 创建推广位 |
| 推广位 | `/admin-api/cps/adzone/update` | PUT | 更新推广位 |
| 推广位 | `/admin-api/cps/adzone/delete` | DELETE | 删除推广位 |
| 推广位 | `/admin-api/cps/adzone/page` | GET | 分页查询推广位 |
| 返利配置 | `/admin-api/cps/rebate-config/create` | POST | 创建返利配置 |
| 返利配置 | `/admin-api/cps/rebate-config/update` | PUT | 更新返利配置 |
| 返利配置 | `/admin-api/cps/rebate-config/delete` | DELETE | 删除返利配置 |
| 返利配置 | `/admin-api/cps/rebate-config/list` | GET | 查询返利配置列表 |
| 会员返利 | `/admin-api/cps/member-rebate/create` | POST | 设置会员个人返利 |
| 会员返利 | `/admin-api/cps/member-rebate/update` | PUT | 更新会员个人返利 |
| 会员返利 | `/admin-api/cps/member-rebate/page` | GET | 查询会员返利配置列表 |
| 订单管理 | `/admin-api/cps/order/page` | GET | 分页查询CPS订单 |
| 订单管理 | `/admin-api/cps/order/get` | GET | 获取CPS订单详情 |
| 订单管理 | `/admin-api/cps/order/sync` | POST | 手动触发订单同步 |
| 订单管理 | `/admin-api/cps/order/bind-member` | POST | 手动绑定订单会员 |
| 返利记录 | `/admin-api/cps/rebate-record/page` | GET | 分页查询返利记录 |
| 提现审核 | `/admin-api/cps/withdraw/page` | GET | 提现申请列表 |
| 提现审核 | `/admin-api/cps/withdraw/approve` | POST | 审核通过 |
| 提现审核 | `/admin-api/cps/withdraw/reject` | POST | 审核驳回 |
| 统计 | `/admin-api/cps/statistics/dashboard` | GET | 运营数据看板 |
| 统计 | `/admin-api/cps/statistics/platform` | GET | 按平台统计 |
| 统计 | `/admin-api/cps/statistics/trend` | GET | 趋势统计 |

### 6.2 会员端接口（App）

| 模块 | 接口路径 | 方法 | 说明 |
|------|----------|------|------|
| 商品搜索 | `/app-api/cps/goods/search` | POST | 商品搜索（关键词/链接/口令） |
| 商品搜索 | `/app-api/cps/goods/compare` | POST | 多平台比价 |
| 商品搜索 | `/app-api/cps/goods/detail` | GET | 商品详情（含返利信息） |
| 商品推荐 | `/app-api/cps/goods/recommend` | GET | 推荐频道商品列表 |
| 推广链接 | `/app-api/cps/link/generate` | POST | 生成推广链接/口令 |
| 我的订单 | `/app-api/cps/order/page` | GET | 我的CPS订单列表 |
| 我的订单 | `/app-api/cps/order/get` | GET | CPS订单详情 |
| 返利信息 | `/app-api/cps/rebate/summary` | GET | 返利汇总（总额/待结算/可提现） |
| 返利信息 | `/app-api/cps/rebate/page` | GET | 返利明细列表 |
| 提现 | `/app-api/cps/withdraw/create` | POST | 发起提现申请 |
| 提现 | `/app-api/cps/withdraw/page` | GET | 提现记录列表 |
| 搜索历史 | `/app-api/cps/search/history` | GET | 搜索历史记录 |
| 搜索历史 | `/app-api/cps/search/hot` | GET | 热门搜索词 |

### 6.3 MCP接口（AI Agent）

MCP接口基于JSON-RPC 2.0协议，通过Streamable HTTP传输层暴露。以下是核心MCP方法：

| 方法 | 端点 | 说明 |
|------|------|------|
| `initialize` | `POST /mcp/cps` | MCP连接初始化，返回Server能力声明 |
| `tools/list` | `POST /mcp/cps` | 获取全部可用Tool列表及Schema定义 |
| `tools/call` | `POST /mcp/cps` | 调用指定Tool（如搜索、比价、转链） |
| `resources/list` | `POST /mcp/cps` | 获取全部可用Resource列表 |
| `resources/read` | `POST /mcp/cps` | 读取指定Resource内容 |
| `resources/subscribe` | `POST /mcp/cps` | 订阅Resource变更通知 |
| `prompts/list` | `POST /mcp/cps` | 获取全部可用Prompt模板列表 |
| `prompts/get` | `POST /mcp/cps` | 获取指定Prompt的消息模板 |

**MCP管理接口（Admin后台管理MCP配置）**：

| 模块 | 接口路径 | 方法 | 说明 |
|------|----------|------|------|
| API Key管理 | `/admin-api/cps/mcp/api-key/create` | POST | 创建API Key |
| API Key管理 | `/admin-api/cps/mcp/api-key/update` | PUT | 更新API Key（权限、限流等） |
| API Key管理 | `/admin-api/cps/mcp/api-key/delete` | DELETE | 删除API Key |
| API Key管理 | `/admin-api/cps/mcp/api-key/page` | GET | 分页查询API Key列表 |
| API Key管理 | `/admin-api/cps/mcp/api-key/regenerate` | POST | 重新生成API Key |
| 请求日志 | `/admin-api/cps/mcp/log/page` | GET | 分页查询MCP请求日志 |
| 请求日志 | `/admin-api/cps/mcp/log/statistics` | GET | MCP调用统计（按Tool/时间维度） |
| MCP状态 | `/admin-api/cps/mcp/status` | GET | MCP Server运行状态和配置信息 |

---

## 七、定时任务设计

| 任务名称 | 执行频率 | 功能说明 |
|----------|----------|----------|
| CpsOrderSyncJob | 每5分钟 | 从各CPS平台增量同步订单数据 |
| CpsRebateSettleJob | 每小时 | 扫描已结算订单，将返利发放到会员钱包 |
| CpsRefundCheckJob | 每30分钟 | 检测退款订单，扣回已发放的返利 |
| CpsStatisticsDailyJob | 每日凌晨1点 | 生成前一天的统计快照数据 |
| CpsAbnormalDetectJob | 每日凌晨2点 | 异常行为检测（刷单、高退款率等） |
| CpsOrderCompensateJob | 每日凌晨3点 | 历史订单补偿同步（防止遗漏） |

---

## 八、各CPS平台接入规范

### 8.1 淘宝联盟（阿里妈妈）

| 项目 | 说明 |
|------|------|
| 接入前提 | 注册淘宝联盟账号，创建应用获取AppKey/AppSecret |
| API协议 | HTTPS + 签名鉴权 (TOP SDK) |
| PID格式 | `mm_{memberid}_{siteid}_{adzoneid}` |
| 核心接口 | `taobao.tbk.dg.material.optional`（搜索）、`taobao.tbk.privilege.get`（转链）、`taobao.tbk.tpwd.create`（口令）、`taobao.tbk.order.details.get`（订单） |
| 归因方式 | adzone_id + external_info |
| 结算方式 | 日结（确认收货后结算） |
| 限流 | 按应用等级，初级约40次/秒 |

### 8.2 京东联盟

| 项目 | 说明 |
|------|------|
| 接入前提 | 注册京东联盟账号，创建应用获取AppKey/AppSecret |
| API协议 | HTTPS + 签名鉴权 |
| PID格式 | unionId + positionId |
| 核心接口 | `jd.union.open.goods.query`（搜索）、`jd.union.open.promotion.common.get`（转链）、`jd.union.open.order.query`（订单） |
| 归因方式 | subUnionId |
| 结算方式 | 月结（每月1-5日提现） |
| 限流 | 按接口类型，一般10-20次/秒 |

### 8.3 拼多多联盟（多多进宝）

| 项目 | 说明 |
|------|------|
| 接入前提 | 注册多多进宝账号，创建应用获取client_id/client_secret |
| API协议 | HTTPS + 签名鉴权 |
| PID格式 | `{appid}_{pid_number}` |
| 核心接口 | `pdd.ddk.goods.search`（搜索）、`pdd.ddk.goods.promotion.url.generate`（转链）、`pdd.ddk.order.list.increment.get`（订单） |
| 归因方式 | custom_parameters（JSON，含uid） |
| 结算方式 | 确认收货后约15个工作日 |
| 限流 | 按应用等级 |

### 8.4 各平台API对照表

| 功能 | 淘宝联盟 | 京东联盟 | 拼多多联盟 |
|------|----------|----------|------------|
| 商品搜索 | `taobao.tbk.dg.material.optional` | `jd.union.open.goods.query` | `pdd.ddk.goods.search` |
| 商品详情 | `taobao.tbk.item.info.get` | `jd.union.open.goods.bigfield.query` | `pdd.ddk.goods.detail` |
| 推广转链 | `taobao.tbk.privilege.get` | `jd.union.open.promotion.common.get` | `pdd.ddk.goods.promotion.url.generate` |
| 口令生成 | `taobao.tbk.tpwd.create` | - | - |
| 口令解析 | `taobao.tbk.tpwd.convert` | - | - |
| 订单查询 | `taobao.tbk.order.details.get` | `jd.union.open.order.query` | `pdd.ddk.order.list.increment.get` |
| 创建推广位 | `taobao.tbk.adzone.create` | `jd.union.open.position.create` | `pdd.ddk.goods.pid.generate` |
| 优惠券查询 | `taobao.tbk.coupon.get` | `jd.union.open.coupon.query` | 搜索接口中包含 |
| 活动链接 | `taobao.tbk.activity.info.get` | - | `pdd.ddk.cms.prom.url.generate` |

---

## 九、非功能需求

### 9.1 性能要求

| 指标 | 要求 |
|------|------|
| 单平台商品搜索响应时间 | < 2秒（P99） |
| 多平台比价响应时间 | < 5秒（P99，并行调用各平台） |
| 订单同步延迟 | < 30分钟 |
| 返利到账延迟 | 平台结算后24小时内 |
| 接口并发 | 支持500+并发查询 |
| MCP Tool调用响应时间 | < 3秒（P99，搜索类）/ < 1秒（P99，查询类） |
| MCP Server并发连接 | 支持100+并发AI Agent连接 |
| MCP SSE推送延迟 | < 500ms |

**优化策略**：
- 多平台搜索使用 `CompletableFuture` 并行调用
- 商品搜索结果 Redis 缓存（TTL 5~10分钟）
- 推广链接生成结果缓存（TTL 24小时）
- 热门搜索词使用 Redis ZSet 统计

### 9.2 安全要求

| 项目 | 措施 |
|------|------|
| API密钥存储 | AES-256 加密存储，运行时解密 |
| 接口防刷 | 基于IP/用户的频率限制（复用 yudao-spring-boot-starter-protection） |
| 数据权限 | 会员只能查看自己的订单和返利数据 |
| 操作审计 | 关键操作记录操作日志（复用系统日志模块） |
| 提现风控 | 异常提现行为检测与拦截 |
| 返利校验 | 返利金额与平台数据二次比对 |
| MCP接口鉴权 | Bearer Token / API Key 鉴权，支持权限分级（public/member/admin） |
| MCP请求限流 | 基于API Key的请求频率限制，防止滥用 |
| MCP参数校验 | 严格校验Tool输入参数，防止注入攻击 |
| MCP日志审计 | 记录全部MCP请求日志，支持追溯和审计 |

### 9.3 可靠性要求

| 指标 | 要求 |
|------|------|
| 订单同步准确率 | 99.99%（不丢单） |
| 返利计算准确率 | 100%（金额精确到分） |
| 系统可用性 | 99.9% |
| 数据备份 | 每日全量 + 实时增量 |

### 9.4 扩展性要求

| 项目 | 措施 |
|------|------|
| 新平台接入 | 实现 `CpsPlatformClient` 接口 + 数据库配置，无需改动核心逻辑 |
| 返利规则 | 支持多维度组合（等级 + 个人 + 平台 + 全局） |
| 通知渠道 | 复用通知模块，易于扩展新的通知方式 |
| 前端适配 | API统一设计，支持H5/小程序/APP多端 |
| MCP扩展 | 新Tool/Resource只需实现接口并注册为Spring Bean，无需修改Server核心逻辑 |
| AI Agent接入 | 标准MCP协议，任何支持MCP的AI客户端可无缝接入 |

---

## 十、技术选型

| 层面 | 技术 | 说明 |
|------|------|------|
| 基础框架 | Spring Boot 3.x | 复用 ruoyi-vue-pro 框架 |
| ORM | MyBatis Plus | 复用现有数据访问模式 |
| 缓存 | Redis | 搜索缓存、API限流、热词统计 |
| 定时任务 | yudao-spring-boot-starter-job | 订单同步、返利结算 |
| 消息队列 | RabbitMQ / Redis Stream | 异步事件通知 |
| HTTP客户端 | OkHttp / RestTemplate | 调用各CPS平台API |
| JSON处理 | Jackson | 平台API数据解析 |
| 加密 | AES-256 | API密钥存储加密 |

---

## 十一、实施计划

### 阶段一：基础架构搭建

- CPS模块工程结构搭建（yudao-module-cps-api + yudao-module-cps-biz）
- 数据库表创建与实体类生成
- CPS平台适配器框架（CpsPlatformClient 接口 + 工厂）
- 平台配置管理CRUD（管理后台）
- 推广位管理CRUD

### 阶段二：核心功能开发

- 淘宝联盟适配器开发（搜索、转链、口令、订单同步）
- 京东联盟适配器开发（搜索、转链、订单同步）
- 拼多多联盟适配器开发（搜索、转链、订单同步）
- 统一商品搜索与多平台比价
- 推广链接生成功能

### 阶段三：返利结算体系

- 返利比例配置管理（多维度：等级/平台/个人）
- 会员返利展示控制
- 订单同步定时任务
- 佣金计算引擎与返利结算
- 退款处理与返利扣回
- 会员钱包集成（复用 PayWallet）

### 阶段四：提现与运营

- 提现申请与审核流程
- 消息通知集成
- 运营数据看板
- 统计报表
- 风控检测
- 搜索历史与热词统计

### 阶段五：扩展与优化

- 抖音联盟适配器（扩展）
- 唯品会联盟适配器（扩展）
- 搜索结果缓存优化
- API限流与熔断
- 邀请返利功能（可选）
- 性能压测与调优

---

## 十二、风险与注意事项

1. **平台API限额**：各平台有调用频率限制，需做好请求限流和熔断（复用 yudao-spring-boot-starter-protection 的限流组件）
2. **佣金核对**：平台佣金结算存在调整，需定期与平台数据对账
3. **合规性**：推广内容需符合各平台运营规范；邀请返利层级不超过二级
4. **反作弊**：防止虚假订单刷返利，需接入风控检测机制
5. **结算周期差异**：淘宝日结、京东月结、拼多多约15工作日，系统需处理不同周期的待结算状态
6. **平台政策变更**：各联盟平台API和规则可能变更，适配器层需具备快速调整能力

---

## 附录：术语表

| 术语 | 说明 |
|------|------|
| CPS | Cost Per Sale，按销售计费的推广模式 |
| 佣金 | CPS平台支付给推广者的销售提成 |
| 返利 | 推广者将佣金的一部分返还给下单用户 |
| PID | 推广位标识，用于追踪订单来源 |
| 转链 | 将普通商品链接转换为包含推广追踪信息的链接 |
| 淘口令 | 淘宝/天猫的商品口令，可在APP中自动解析 |
| adzone_id | 淘宝联盟的广告位ID |
| subUnionId | 京东联盟的子联盟标识 |
| custom_parameters | 拼多多联盟的自定义参数，用于用户归因 |
| 券后价 | 使用优惠券后的商品实付价格 |
| 可分配佣金 | 佣金扣除平台服务费后的可分配金额 |
