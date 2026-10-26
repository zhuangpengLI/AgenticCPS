# 穿山甲电商联盟（抖音电商 CPS）接口接入与资质指南

本文档依据穿山甲开放平台（Pangolin E-commerce Alliance）与抖音电商联盟官方开放接口规范（参考 [穿山甲订单查询接口文档 28733](https://www.csjplatform.com/supportcenter/28733)）编写。旨在为开发者与运营团队提供穿山甲抖音 CPS 渠道的资质准入、接口规范、签名算法实现、后台配置以及与抖店官方百应渠道的双通道对比选型指南。

---

## 一、 平台定位与生态概述

在抖音电商生态中，推广者主要有两种主流的 API 接入路径：
1. **抖店开放平台 / 巨量百应（官方直连）**：面向中大型服务商和工具商，门槛极高（硬性要求软著完全一致、ICP 备案与百应机构认证）。
2. **穿山甲电商联盟（CSJ Pangolin E-commerce）**：字节跳动旗下移动流量变现与聚合联盟，面向 App、小程序、H5 媒体主及返利导购开发者提供抖音电商货源分发能力。

```
                         ┌─────────────────────────────────┐
                         │   穿山甲电商联盟 (CSJ Pangolin)   │
                         │   ecom.pangolin-sdk-toutiao.com │
                         └────────────────┬────────────────┘
                                          │ 统一请求封包 (Envelope POST + MD5)
                                          ▼
                         ┌─────────────────────────────────┐
                         │      AgenticCPS 后端服务        │
                         │    (CsjDouyinVendorClient)      │
                         └────────────────┬────────────────┘
                                          │ 适配标准化 CPS 模型
                                          ▼
 ┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐
 │   穿山甲商品选品检索   │   │   穿山甲高佣转链/口令 │   │ 28733 协议订单状态流转│
 │   (/product/search)   │   │   (/product/link)     │   │ (/order/search)       │
 └───────────────────────┘   └───────────────────────┘   └───────────────────────┘
```

---

## 二、 准入门槛与申请资质要求

相较于抖店开放平台（百应直连）严苛的“软著主体强一致+企业资质”，穿山甲电商联盟在准入范围上对移动开发者更为友好，但对**合规真实性、流量备案与推广质量**具有明确的风控考核规范。

### 1. 主体准入资质要求
* **企业法人或个体工商户**：
  * 支持中国大陆合法注册的**企业法人**或**个体工商户**营业执照（三证合一彩色扫描件）。
  * 经营范围须包含广告、推广、信息技术服务、网络技术或电子商务等相关范畴。
  * 经营异常名录、税收违法黑名单或失信被执行人企业不予准入。
* **法定代表人与经办人实名认证**：
  * 法人身份证正反面彩色扫描件（在有效期内且与执照法人完全一致）。
  * 经办人实名认证与授权委托书（加盖公章）。
* **银行对公账户真实性核验**：
  * 企业主体必须提供与营业执照同名的对公银行账户，穿山甲平台将通过**小额微打款（随机金额并填写回填验证码）**核验账户真实性与主体归属。
  * 个体工商户如无对公账户，支持绑定经营者同名个人银行储蓄卡。

### 2. 媒体与流量备案要求（核心前置）
调用穿山甲电商 API 前，推广载体必须完成平台备案并审核通过：
* **应用载体形式**：支持移动原生 App（Android / iOS）、微信小程序、H5 移动网页、公众号等。
* **流量备案登记**：
  * 须在穿山甲媒体管理平台下载并如实签署《媒体流量备案登记表》。
  * App 须已上架主流应用市场（华为、小米、应用宝、App Store 等），提供包名（Package Name / Bundle ID）及下载链接。
  * 小程序需提供 AppID、主体信息与线上运行截图。
  * 网站/H5 域名需完成工信部 ICP 备案，备案主体须与穿山甲入驻主体一致或提供授权链路证明。

### 3. 开发者等级考核与接口权限（V1-V4 阶梯体系）
穿山甲对电商联盟推手实行等级管理，不同等级享有不同的接口 QPS 频次与佣金权益：
* **准入初级（V1）**：
  * 完成实名认证与媒体备案即可获得基础 API 权限（`/order/search`、`/product/search`、`/product/link`）。
  * 默认接口限频：60 次/分钟（QPS=1~2）。
* **进阶等级（V2~V4）考核指标**：
  * **GMV 门槛**：月度有效核销结算成交额达到平台进阶要求（如 V2 要求月 GMV $\ge$ 10 万元，V3 要求 $\ge$ 50 万元）。
  * **退货/退款率红线**：推手近 30 天退货退款率必须**不得高于 40%**；恶意刷单或违规流量将触发降级、封禁 API 或扣除违约金。
  * **高阶权益**：升级至 V3/V4 后，可申请 QPS 提频至 20~50、获得大促官方额外补贴加成池、以及专属客户经理一对一接入指导。

---

## 三、 接口协议与通讯规范（依据 28733 协议）

### 1. 通讯网关与编码
* **生产网关**：`https://ecom.pangolin-sdk-toutiao.com`（同时支持 HTTP 与 HTTPS）
* **字符编码**：`UTF-8`
* **Content-Type**：`application/json;charset=utf-8`
* **HTTP 请求方式**：统一使用 `POST`

### 2. 公共外层请求报文（Envelope 封包）
所有请求均必须将业务参数封装在 `data` 属性内作为紧凑型 JSON 字符串，并携带统一顶层签名字段：

| 字段名 | 类型 | 是否必选 | 说明 |
| :--- | :--- | :--- | :--- |
| `app_id` | String | 是 | 穿山甲媒体/应用唯一 AppID（例如：`5271412`） |
| `data` | String | 是 | 业务参数紧凑型 JSON 字符串（无多余空格与格式化换行） |
| `req_id` | String | 是 | 客户端生成的请求唯一标识（UUID 或流水号），用于幂等与全链路追踪 |
| `sign_type` | String | 是 | 签名类型，固定为 `"MD5"` |
| `timestamp` | Long | 是 | 当前 Unix 时间戳（秒级或毫秒级） |
| `version` | String | 是 | 协议版本号，例如 `"1.0.0"` 或 `"2.0.0"` |
| `sign` | String | 是 | 报文签名值（32 位小写十六进制字符串） |

### 3. MD5 签名算法与计算规则
1. **参数提取**：取出请求封包中除 `sign` 之外的所有公共字段（`app_id`, `data`, `req_id`, `sign_type`, `timestamp`, `version`）。
2. **字典序排序**：按字段名 ASCII 字典序从小到大升序排序。
3. **键值拼接**：将排序后的参数按 `key=value&` 格式依次拼接成字符串（空值或 null 不参与拼接）。
4. **追加密钥**：在拼接串末尾直接追加配置的保密密钥 `secure_key`（例如：`key1=val1&key2=val2...secure_key`）。
5. **计算 MD5**：使用 UTF-8 编码对最终字符串计算 MD5 摘要，输出 **32 位小写 Hex 字符串**。

**代码参考实现**（已封装于 `CsjOpenApiSigner.java`）：
```java
// 升序排布并追加 secureKey
String signString = "app_id=" + appId
        + "&data=" + compactDataJson
        + "&req_id=" + reqId
        + "&sign_type=MD5"
        + "&timestamp=" + timestamp
        + "&version=" + version
        + secureKey;

String sign = DigestUtil.md5Hex(signString.getBytes(StandardCharsets.UTF_8));
```

---

## 四、 核心 API 接口清单与数据映射

### 1. 订单查询接口（`/order/search` 28733 协议核心）
用于拉取推广渠道在指定时间范围内的抖音电商成交订单明细。

* **接口路径**：`/order/search`
* **请求方式**：`POST`
* **业务请求入参（封装在 data 内部）**：

| 参数名 | 类型 | 是否必选 | 说明 |
| :--- | :--- | :--- | :--- |
| `start_time` | String | 是 | 起始时间（`yyyy-MM-dd HH:mm:ss`） |
| `end_time` | String | 是 | 截止时间（`yyyy-MM-dd HH:mm:ss`） |
| `page` | Integer | 是 | 页码，从 1 开始 |
| `page_size` | Integer | 是 | 每页条数（默认 20，最大 100） |
| `query_order_type` | Integer | 否 | 查询时间类型（1: 支付时间，2: 更新时间，默认为 1） |

* **业务响应字段与状态流转对应**：

| 穿山甲字段名 | 平台字段类型 | AgenticCPS 标准字段 | 换算与说明 |
| :--- | :--- | :--- | :--- |
| `order_id` | String | `platformOrderId` | 抖音订单唯一编号 |
| `item_id` | String | `goodsId` | 商品 ID / 商品编码 |
| `item_title` | String | `goodsName` | 商品标题名称 |
| `item_num` | Integer | `itemNum` | 商品件数 |
| `item_price` | Long | `price` | 单价（单位：分 $\rightarrow$ 转换为元） |
| `total_pay_amount` | Long | `payPrice` | 实际支付总金额（单位：分 $\rightarrow$ 转换为元） |
| `commission` | Long | `commission` | 预估结算推广佣金（单位：分 $\rightarrow$ 转换为元） |
| `commission_rate` | Double | `commissionRate` | 佣金比例（百分比） |
| `custom_info` | String | `externalId` | 推手渠道自定义归因参数（会员 ID 或渠道标记） |
| `flow_point` | String | `status` | 订单状态节点（见下表四态流转映射） |

* **四态订单生命周期映射**：

| 穿山甲 `flow_point` 状态 | 状态含义 | CPS 标准状态码 | 说明 |
| :--- | :--- | :---: | :--- |
| `PAY_SUCC` | 买家付款成功 | **`1`** (已付款) | 订单已成单，产生预估佣金，尚未收货 |
| `CONFIRM` | 买家确认收货 | **`2`** (已收货) | 用户已收货，进入售后结算缓冲期 |
| `SETTLE` | 联盟结算打款 | **`3`** (已结算) | 跨越 15 天无理由退货期，佣金正式入账可提现/兑换 |
| `REFUND` | 退款或失效 | **`-1`** (已失效) | 发生退货退款或被风控判定违规，不可结算 |

---

### 2. 商品搜索接口（`/product/search`）
用于在穿山甲电商货盘中按关键词、分类检索高佣商品。

* **接口路径**：`/product/search`
* **请求方式**：`POST`
* **业务入参**：`keyword`, `page`, `page_size`, `sort_type` (佣金、价格、销量排序)
* **业务响应**：商品列表，包括 `item_id`, `title`, `price` (分), `commission_rate`, `pic_url`, `sales` 等。

---

### 3. 商品转链接口（`/product/link`）
用于将商品转换为带推手标识与自定义追踪信息（`custom_info`）的推广短链、口令与唤起抖音 App 的 DeepLink。

* **接口路径**：`/product/link`
* **请求方式**：`POST`
* **业务入参**：
  * `item_id`: 目标商品 ID
  * `adzone_id`: 推广位 ID（可选）
  * `custom_info`: 渠道追踪信息（用户 ID / 追踪码，长度 $\le 64$）
* **业务响应**：
  * `click_url`: 推广落地短链接
  * `tpwd`: 抖音专属回流口令（如 `8.88 d@u.cl 03/12 ...`）
  * `deep_link`: 唤起抖音 App 原生购买页的协议链接（`snssdk1128://...`）

---

## 五、 AgenticCPS 系统配置与使用指南

### 1. 数据库与元数据配置
系统已在 `cps_api_vendor` 与 `cps_platform` 中完成版本化注册（Schema `v2.1.7`）：
* **平台编码**：`douyin`（抖音电商）
* **供应商编码**：`csj`（穿山甲）
* **供应商类型**：`aggregator`（聚合服务商）

### 2. 管理后台配置（或数据库录入）
在 AgenticCPS 管理后台【CPS 服务管理 $\rightarrow$ 供应商配置】中，选择平台 `douyin` 并配置供应商 `csj`：

| 配置项 | 字段映射 | 示例值 | 填写说明 |
| :--- | :--- | :--- | :--- |
| **AppID** | `app_key` / `appId` | `5271412` | 穿山甲媒体管理平台分配的媒体/应用 AppID |
| **SecureKey** | `app_secret` / `secureKey` | `7d8f3e2a1b9c4...` | 穿山甲电商联盟开放接口安全保密密钥 |
| **API网关** | `api_base_url` | `https://ecom.pangolin-sdk-toutiao.com` | 官方固定网关（支持 http/https） |
| **默认推广位** | `default_adzone_id` | `adzone_dy_default` | 默认兜底推广位编号（可选） |

### 3. 动态切换供应商（双通道路由）
由于 `DouyinPlatformClientAdapter` 采用策略模式与 Spring 依赖查找机制，切换抖音平台数据源只需更改平台激活供应商：
* **切换至穿山甲**：将 `cps_platform` 表中 `douyin` 的 `active_vendor_code` 设为 `'csj'`。
* **切换至官方抖店**：将 `active_vendor_code` 设为 `'official'`。
* **临时覆盖（ThreadLocal）**：支持在单元测试或批处理中通过 `CpsPlatformClientFactory.setVendorOverride("csj")` 指定通道。

---

## 六、 抖音官方百应渠道 vs 穿山甲联盟双通道对比

| 评估维度 | 抖店百应官方 (`official`) | 穿山甲电商联盟 (`csj`) |
| :--- | :--- | :--- |
| **对应开放平台** | 抖店开放平台 (`op.jinritemai.com`) + 巨量百应 | 穿山甲联盟 (`csjplatform.com`) |
| **主体门槛** | 必须为中国大陆**企业法人**，不接受个体工商户 | 支持**企业法人**与**个体工商户** |
| **核心拦截资质** | 必须提供**主体完全一致的软著**与 **ICP 备案** | 仅需应用商店上架截图或流量备案表，无需独立软著 |
| **认证方式** | 严格资质审核 + 百应推手认证 | 对公小额打款核验 + 媒体备案审核 |
| **签名机制** | HMAC-SHA256（`param_json` + Secret） | MD5（紧凑 `data` JSON + ASCII 排序 + `secure_key`） |
| **状态流转字段** | 官方数字状态码（`order_status`） | 语义化事件流转点（`flow_point: PAY_SUCC/CONFIRM/SETTLE/REFUND`） |
| **结算周期** | T+15（买家收货后 15 天自动结算至百应钱包） | 统一月结或周期性打款至媒体公户/经营者储蓄卡 |
| **推荐选型场景** | 大型电商机构、具备独立软著资产的大型服务商工具 | 中小型移动 App、小程序矩阵、个人导购网站、快速上线 MVP 业务 |

---

## 七、 质量工程与测试覆盖

穿山甲适配模块已落地严格的工程测试保护：
1. [`CsjOpenApiSignerTest`](file:///backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/client/csj/CsjOpenApiSignerTest.java)：测试 MD5 签名算法、ASCII 升序排序、紧凑型 JSON 序列化、封包生成。
2. [`CsjDouyinVendorClientTest`](file:///backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/client/csj/CsjDouyinVendorClientTest.java)：涵盖 28733 协议字段解析、四态状态流转、分-元精度计算、敏感配置脱敏、连接测试与异常治理。
3. [`DouyinPlatformClientAdapterTest`](file:///backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/client/douyin/DouyinPlatformClientAdapterTest.java)：测试适配器向 `csj` 供应商的无缝委托与未就绪兜底保护。
4. [`CpsSdkStandardizationContractTest`](file:///backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/client/CpsSdkStandardizationContractTest.java)：确保 `csj` 符合全系统标准供应商规范（能力契约、配置 Schema 校验、元数据导出）。
