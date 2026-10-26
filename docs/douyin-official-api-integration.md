# 抖音电商联盟（抖客/百应）官方 API 接入与资质指南

本文档依据抖音电商开放平台（抖店开放平台 `op.jinritemai.com` / 巨量百应开放平台 `buyin.jinritemai.com`）官方开发者规范及 [开发者入驻与资质审核指南 (guide-docs/213/14)](https://op.jinritemai.com/docs/guide-docs/213/14) 编写，涵盖资质申请、API 清单、签名机制、系统实现与后台配置指南。

> [!TIP]
> 如果您暂无独立软件著作权证书（软著）或希望以个体工商户主体快速接入抖音电商 CPS，可选用字节跳动旗下**穿山甲电商联盟渠道**。详细接口规范、资质要求与后台配置请参考 [《穿山甲电商联盟（抖音电商 CPS）接口接入与资质指南》](file:///docs/douyin-csj-api-integration.md)。

---

## 一、 抖音电商开放平台生态与架构概述

抖音电商的 CPS 返利与推广生态主要由 **巨量百应（Buyin）** 与 **精选联盟** 承载。推广方角色为 **抖客（渠道推手 / 工具商）**。

```
                       ┌─────────────────────────┐
                       │  抖店开放平台 / 巨量百应  │
                       │  op.jinritemai.com      │
                       └────────────┬────────────┘
                                    │ OpenAPI (HMAC-SHA256)
                                    ▼
                       ┌─────────────────────────┐
                       │   AgenticCPS 后端服务    │
                       │   (DouyinOfficialClient)│
                       └────────────┬────────────┘
                                    │ 统一模型转换
                                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  精选联盟商品搜索  │  │   抖客转链与口令   │  │   推广订单与结算   │
│  (Goods Search)  │  │ (Promotion Link) │  │   (Order Sync)   │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## 二、 申请入驻与资质要求 (依据 guide-docs/213/14)

根据平台开发者入驻及应用类目审核规范，接入抖音官方电商与抖客 API 必须满足以下主体资质与合规要求：

### 1. 基础主体资质
* **企业法人主体**：
  * 仅支持中国大陆合法注册的**企业法人**主体（个体工商户通常无法申请高阶抖客或服务商工具应用）。
  * 营业执照须为“三证合一”正本/副本彩色扫描件，经营范围需包含“技术开发、软件开发、信息技术服务、电子商务”等相关技术或电商类目。
  * 企业经营状态正常，未被列入国家企业信用信息公示系统“经营异常名录”或“严重违法失信名单”。
* **法定代表人与经办人认证**：
  * 法人身份证正反面清晰彩色扫描件（在有效期内）。
  * 法人实名手机号核验，以及企业对公银行账户信息（用于对公小额打款验证真实性）。

### 2. 核心硬性资质（类目审核必备）
在抖店开放平台创建应用（如“自研工具”或“电商导购/服务商应用”）时，以下两项为硬性拦截条件：
* **软件著作权证书（软著）**：
  * 由国家版权局颁发的计算机软件著作权登记证书（证书原件彩色扫描件）。
  * **主体一致性原则**：软著著作权人必须与开放平台入驻企业主体名称**完全一致**（不支持非直属母子关系的第三方软著授权）。
  * **软件名称与功能匹配**：软件名称与说明需与电商、导购、订单分销或 CPS 返利管理相关（例如：《XX电商导购返利管理系统》《XX精选联盟选品分销平台》）。
* **ICP 备案证书 / 截图**：
  * 系统域名必须完成工信部 ICP 主体备案。
  * 域名备案主体必须与开放平台企业主体完全一致，系统公网部署必须支持 HTTPS 协议。
* **系统功能说明书**：
  * 平台提供标准化模板，需包含：系统拓扑结构、业务流程图、拟调用 API 清单、前后台功能界面真实截图（非原型草图），以及自研核心代码片段截图。

### 3. 抖客 / 百应业务权限开通
* 登录 [巨量百应平台 (buyin.jinritemai.com)](https://buyin.jinritemai.com/) 注册并认证为**抖客推手**或**服务商机构**。
* 在抖店开放平台控制台关联百应账号，申请开通**【精选货源/百应API】**或**【抖客权限包】**。
* 签署《抖音电商平台推广服务协议》，根据业务等级要求按需冻结或缴纳平台风险保证金。

---

## 三、 必须对接的官方 API 接口清单

对接抖音官方平台完成端到端 CPS 闭环，需集成以下五大核心类目接口：

| 业务场景 | 接口名称 (Method) | 接口路径 (Path) | 请求方式 | 接口功能与说明 |
| :--- | :--- | :--- | :--- | :--- |
| **基础鉴权** | `token.create` | `/token/create` | POST | 获取应用调用凭证 `access_token` 及 `refresh_token` |
| **基础鉴权** | `token.refresh` | `/token/refresh` | POST | 刷新过期的 `access_token` |
| **渠道管理** | `buyin.doukePidCreate` | `/buyin/doukePidCreate` | POST | 为推广渠道或下级用户创建独立追踪推广位（PID） |
| **渠道管理** | `buyin.doukePidList` | `/buyin/doukePidList` | POST | 查询已创建的抖客推广位（PID）列表及状态 |
| **选品搜索** | `buyin.kolMaterialsProductsSearch` | `/buyin/kolMaterialsProductsSearch` | POST | 检索精选联盟商品池（按标题、类目、佣金率、券后价、销量检索） |
| **商品详情** | `buyin.kolProductsDetail` | `/buyin/kolProductsDetail` | POST | 获取达人/抖客视角下的商品详细信息、双佣金结构及素材 |
| **推广转链** | `buyin.doukeCommandParseAndShare` | `/buyin/doukeCommandParseAndShare` | POST | 解析输入口令/商品链接，转换为专属带 PID 的推广长短链、抖口令与唤端 DeepLink |
| **订单同步** | `buyin.doukeOrderList` | `/buyin/doukeOrderList` | POST | 按支付时间/更新时间拉取抖客分销订单明细（付款、收货、结算、退款状态） |
| **分销订单** | `buyin.doukeOrderAds` | `/buyin/doukeOrderAds` | POST | 按推广位或广告位维度查询独立分账效果订单 |

---

## 四、 接口通信规范与签名算法 (HMAC-SHA256)

### 1. 网关与公共参数
* **官方生产网关**：`https://openapi-fxg.jinritemai.com`
* **协议版本**：`v=2`
* **字符编码**：`UTF-8`
* **公共请求参数**：

| 参数名 | 类型 | 是否必选 | 说明 |
| :--- | :--- | :--- | :--- |
| `app_key` | String | 是 | 开发者应用 AppKey |
| `method` | String | 是 | 调用的 API 名称，如 `buyin.kolMaterialsProductsSearch` |
| `param_json` | String | 是 | 业务参数的紧凑型 JSON 字符串（无多余空格与换行） |
| `timestamp` | String | 是 | 请求时间戳，格式 `yyyy-MM-dd HH:mm:ss`（上海时区 GMT+8） |
| `v` | String | 是 | API 协议版本，固定为 `"2"` |
| `sign_method` | String | 是 | 签名算法，推荐固定为 `"hmac-sha256"` |
| `sign` | String | 是 | 签名计算结果（64 位小写十六进制字符串） |
| `access_token` | String | 视接口而定 | 抖客/达人授权访问令牌（通过 `config.authToken` 传入） |

### 2. 签名计算规则
1. **参数提取与过滤**：提取全部待发送的公共参数和业务参数，排除 `sign` 与 `sign_method` 自身，排除值为空或 null 的参数。
2. **ASCII 字典序升序排列**：按照参数名（Key）的字符字典序排序。
3. **无分隔符拼接**：拼接为 `key1value1key2value2...` 格式。
4. **前后加盐 (AppSecret)**：
   $$\text{RawString} = \text{app\_secret} + \text{sorted\_string} + \text{app\_secret}$$
5. **HMAC-SHA256 加密**：以 `app_secret` 为 Key 计算 `RawString` 的 HMAC-SHA256 哈希值，转为 64 位小写 Hex。

---

## 五、 系统工程实现

AgenticCPS 现已完成抖音官方平台的标准适配：

1. **签名实现**：
   `com.qiji.cps.module.cps.client.official.douyin.DouyinOpenApiSigner`
   * 实现字典序排序、紧凑型 JSON 格式化、前後拼装 Secret 及 HMAC-SHA256 签名。
   * 支持快速构建带签名的完整 URL 与参数映射。
2. **官方客户端实现**：
   `com.qiji.cps.module.cps.client.official.douyin.DouyinOfficialVendorClient`
   * 实现 `searchGoods`（支持分转元、佣金百分比规范化、类目/价格过滤）。
   * 实现 `generatePromotionLink`（支持口令解析、绑定 PID、生成抖口令及 App 唤起链接）。
   * 实现 `queryOrderPage` / `queryOrders`（支持游标分页、四态状态机映射、退款标记）。
   * 实现 `testConnection`（通过轻量级商品搜索探测凭证有效性）。
   * 声明能力集：`GOODS_SEARCH`, `PROMOTION_LINK`, `ORDER_QUERY`, `CONNECTION_TEST`。
3. **顶层平台适配器**：
   `com.qiji.cps.module.cps.client.douyin.DouyinPlatformClientAdapter`
   * 采用委托模式，从 `CpsPlatformClientFactory` 动态路由至官方激活客户端。

---

## 六、 后台配置与启用指引

### 1. 数据库配置
在 `cps_platform` 表与 `cps_api_vendor` 表中已内置平台元数据（版本 `v2.1.5`）：
* `cps_platform`：`platform_code = 'douyin'`, `active_vendor_code = 'official'`
* `cps_api_vendor`：`vendor_code = 'official'`, `platform_code = 'douyin'`

### 2. 运营后台启用步骤
1. 进入管理后台：**【CPS运营管理】->【平台配置】->【API供应商管理】**。
2. 找到平台为“抖音联盟”、供应商为“官方API”的记录，点击“配置”：
   * `appKey`：填写在抖店开放平台申请的应用 AppKey。
   * `appSecret`：填写对应的 AppSecret。
   * `authToken`：填写百应授权获取的 AccessToken（若需调用带达人授权的选品/转链）。
   * `defaultAdzoneId`：填写默认抖客 PID（如 `dy_pid_default`）。
   * `apiBaseUrl`：留空则默认使用 `https://openapi-fxg.jinritemai.com`。
3. 点击“测试连接”，验证网络及密钥有效性。
4. 确认无误后，将供应商状态切换为“启用”，并将抖音联盟的激活供应商设置为“官方API”。
