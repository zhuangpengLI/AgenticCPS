# 滴滴联盟 DUnion SDK 集成指南

部署配置和调用示例参见：[滴滴联盟 DUnion SDK 使用与配置](didi-union-sdk-usage-configuration.md)。

## 1. 目标与范围

AgenticCPS 将 `dunion-java-sdk` 1.3 作为仓内 Maven 子模块接入，通过 `didi + official` 供应商配置复用现有 CPS 平台适配器、转链记录、订单同步和会员归因链路。

本次集成范围：

- 支持 H5 推广链接、小程序路径、二维码、推广海报和券码素材。
- 支持滴滴联盟订单列表同步和订单归因诊断。
- H5 转链接入现有 `CpsPlatformClient.generatePromotionLink`。
- 订单接入现有定时/手工同步、幂等保存、状态更新和返利结算链路。
- 不新增管理后台页面，不提供滴滴商品搜索，不开放生产模拟订单回调。

## 2. 功能矩阵

| 能力 | DUnion SDK 接口 | AgenticCPS 接入点 | 说明 |
|---|---|---|---|
| H5 推广链接 | `generateH5Link` | `CpsPlatformClient.generatePromotionLink` | 标准转链默认产物 |
| 小程序路径 | `generateMiniLink` | 素材生成后台 API | 用于运营素材，不替代标准 H5 转链 |
| H5 二维码 | `generateH5Code` | 素材生成后台 API | 先取 H5 链接取得 `dsi`，再生成二维码 |
| 小程序码 | `generateMiniCode` | 素材生成后台 API | 先取小程序路径取得 `dsi`，再生成小程序码 |
| 推广海报 | `generatePoster` | 素材生成后台 API | 先取链取得 `dsi`，再生成海报 |
| 券码 | `generateCouponPwd` | 素材生成后台 API | 返回滴滴联盟券码 |
| 订单列表 | `queryOrderList` | 订单定时/手工同步 | 按时间窗口和分页同步 |
| 订单归因诊断 | `selfQueryOrder` | 后台诊断 API | 返回预估归因成功/失败信息 |
| 模拟订单回调 | `mockOrderCallback` | 仅自动化测试 | 不注册生产 Controller 路由 |

滴滴平台不支持商品搜索。全平台搜索应自动跳过 `didi`，直接指定滴滴搜索应返回 `PLATFORM_CAPABILITY_UNSUPPORTED`。

## 3. 模块与调用关系

```text
qiji-module-cps-biz
  -> qiji-module-cps-sdk-dunion
  -> cn.didi.union.client.UnionClient
  -> https://union.didi.cn/openapi/v1.0
```

仓内 SDK 模块位于：

```text
backend/qiji-module-cps/qiji-module-cps-sdk-dunion/
```

SDK 保留 `cn.didi.union` 包和原签名算法，并补充以下加固：

- `baseUrl` 可配置，默认指向滴滴联盟生产地址。
- 连接超时和读取超时均受控。
- GET 参数使用 UTF-8 可靠编码。
- 非 2xx HTTP 状态、空响应和网络异常统一转换为明确异常。
- 业务响应 `errno != 0` 视为失败，保留 `errmsg` 与 `traceid` 供排错。
- 日志禁止输出 `accessKey`、签名原文和完整原始响应。
- 依赖版本由项目 BOM 管理，不使用本地 JAR 或 `systemPath`。

## 4. 配置步骤

### 4.1 前置条件

准备滴滴联盟提供的以下参数：

- App-Key。
- accessKey。
- 可用活动 ID `activityId`。
- 默认推广位 ID `promotionId`。

### 4.2 创建平台与供应商配置

1. 在目标租户的平台管理中创建或启用平台编码 `didi`，平台名称建议为“滴滴联盟”。
2. 在目标租户的 API 供应商管理中创建 `vendorCode=official`、`vendorType=official`、`platformCode=didi` 的配置。
3. 填写 App-Key、accessKey、默认推广位和可选 API 基础地址。
4. 启用配置后调用连接测试接口。

项目 SQL 不写固定 `tenant_id` 的滴滴种子数据。每个租户必须通过平台管理和 API 供应商管理维护自己的配置。

### 4.3 配置字段映射

| AgenticCPS 配置 | DUnion 含义 | 规则 |
|---|---|---|
| `appKey` | `App-Key` | 必填 |
| `appSecret` | `accessKey` | 必填，按密钥管理要求加密存储 |
| `defaultAdzoneId` | `promotionId` | 未显式传入推广位时使用 |
| `apiBaseUrl` | SDK `baseUrl` | 可选，空值使用官方生产地址 |
| `extraConfig.timeoutMs` | 连接/读取超时 | 允许 `1000-30000` 毫秒，默认 `5000` 毫秒 |

`extraConfig` 示例：

```json
{
  "timeoutMs": 5000
}
```

## 5. 标准转链

标准转链字段映射：

| AgenticCPS | DUnion | 说明 |
|---|---|---|
| `goodsId` | `activityId` | 必须是正整数活动 ID |
| `adzoneId` | `promotionId` | 请求值为空时使用供应商默认推广位 |
| 可信 `memberId` | `sourceId` | 仅允许登录上下文或 MCP `ToolContext` 提供 |
| DUnion `link` | `longUrl` | 标准转链保存 H5 链接 |
| `dsi/appId/appSource/traceId` | `extraFields` | 用于素材续调和诊断 |

会员归因不得信任请求体中的 `memberId`。App 和 MCP 转链必须继续从已认证登录上下文或可信 `ToolContext` 生成 `sourceId`，并复用现有转链记录。

## 6. 后台 API

### 6.1 生成运营素材

```http
POST /admin-api/cps/didi-union/material/generate
Authorization: Bearer <admin-token>
Content-Type: application/json
```

权限：`cps:toolbox:link`

请求体只允许活动、推广位和素材类型：

```json
{
  "activityId": 10001,
  "promotionId": 20001,
  "materialType": "H5_QR_CODE"
}
```

`promotionId` 可省略，省略时使用当前启用供应商配置的 `defaultAdzoneId`。`materialType` 支持：

- `H5_LINK`
- `MINI_LINK`
- `H5_QR_CODE`
- `MINI_QR_CODE`
- `POSTER`
- `COUPON_CODE`

示例响应：

```json
{
  "code": 0,
  "data": {
    "materialType": "H5_QR_CODE",
    "sourceId": "ops-4a4efea1-506d-48ea-993e-56d2ce23d72c",
    "link": "https://example.didi.cn/promotion",
    "dsi": "example-dsi",
    "qrCodeUrl": "https://example.didi.cn/qrcode",
    "traceId": "didi-trace-id"
  }
}
```

后台素材统一生成 `ops-UUID` 形式的 `sourceId`，其用途是运营审计，不得据此给会员入账。接口不接收 `memberId`。

### 6.2 连接测试

```http
GET /admin-api/cps/didi-union/connection-test
Authorization: Bearer <admin-token>
```

权限：`cps:api-vendor:query`

连接测试使用当前租户启用的 `didi + official` 供应商配置，验证配置解析、签名、网络连通、HTTP 状态和业务 `errno`。测试失败时仅返回脱敏后的错误摘要和 `traceId`。

### 6.3 订单归因诊断

```http
GET /admin-api/cps/didi-union/order-attribution?orderId=123456789
Authorization: Bearer <admin-token>
```

权限：`cps:order:query`

响应包含滴滴联盟返回的预估归因成功列表和失败列表。订单 ID 必填且不得为空。该接口只用于诊断，不修改订单、返利账户或归因结果。

## 7. 订单字段映射

| DUnion 字段 | AgenticCPS 字段 | 转换规则 |
|---|---|---|
| `orderId` | `orderId` / 平台订单号 | 原样保存并用于幂等 |
| `productId` | `itemId` | 业务线/商品标识 |
| `title` | `itemTitle` | 原样保存 |
| `payPrice` | `finalPrice` | 分除以 100，转换为元 |
| `promotionId` | `adzoneId` | 转为字符串 |
| `sourceId` | `externalId` | 进入现有归因链路 |
| `cpaProfit + cpsProfit` | `commissionAmount` | 两项先按分求和，再除以 100 转为元 |
| `payTime` | `payTime` | 秒级时间戳转上海时区时间 |
| `refundTime` | `refundTime` | 秒级时间戳转上海时区时间 |
| `refundPrice` | 退款信息 | 大于 0 时标记退款 |
| `cpaProfit/cpsProfit/cpaType` | `extraFields` | 保留原始佣金构成 |
| `isRisk/failReason/status` | `extraFields/rawPayload` | 用于风控、失败与状态排错 |

分页同步必须把滴滴页码转换为现有 `nextPositionIndex` 语义；达到末页时返回空游标，避免定时任务重复拉取。

## 8. 状态映射

滴滴订单状态采用保守映射，防止未结算佣金提前进入可返利链路：

| DUnion 条件 | AgenticCPS 状态 | 处理 |
|---|---|---|
| `orderStatus = 8` 或 `refundPrice > 0` | 已退款/失效 | 标记退款，不入账 |
| `isRisk != 0` | 已失效 | 记录风险字段与失败原因 |
| SDK `status = 6` | 已失效 | 结算取消 |
| SDK `status = 7` | 已结算 | 允许进入后续返利结算 |
| SDK `status = 8` | 已失效 | 结算失败 |
| SDK `status = 1-5` | 最高映射为已付款 | 不提前标记收货、结算或到账 |

订单更新必须遵守现有幂等和状态单向推进规则，禁止乱序回调造成状态回退或重复返利。

## 9. 排错

| 现象 | 检查项 | 处理建议 |
|---|---|---|
| 找不到启用客户端 | 当前租户是否存在启用的 `didi + official` 配置 | 核对平台编码、供应商编码、状态和租户 |
| 参数校验失败 | `activityId`、`promotionId` 是否为正整数 | 修正活动 ID；推广位为空时确认默认推广位已配置 |
| HTTP 401/403 | App-Key、accessKey、签名时间与参数编码 | 重新核对凭证，禁止从日志复制被脱敏的密钥 |
| HTTP 404 | `apiBaseUrl` 是否包含错误路径 | 通常留空使用官方默认地址 |
| HTTP 429/5xx | 上游限流或故障 | 按任务退避重试，不在同步循环内无限重试 |
| `errno != 0` | `errmsg`、`traceId` | 使用脱敏错误和 traceId 联系滴滴联盟排查 |
| 转链成功但订单未归因 | `sourceId`、`promotionId`、订单诊断结果 | 确认真正使用可信会员转链，不要使用 `ops-UUID` 素材入账 |
| 订单未结算 | SDK `status`、`orderStatus`、`isRisk` | 状态 7 才映射已结算；退款、风控和失败均不入账 |
| 全平台搜索无滴滴结果 | 预期行为 | 滴滴适配器不提供商品搜索 |

## 10. 安全要求

- 不记录 `appSecret/accessKey`、签名明文、Authorization 头或完整原始响应。
- 错误日志只保留接口、HTTP 状态、业务错误码、脱敏摘要和 `traceId`。
- 后台素材接口不得增加 `memberId`、`sourceId` 等可伪造归因字段。
- 会员 `sourceId` 只能来自 App 登录上下文或 MCP 可信上下文。
- `ops-UUID` 仅用于运营素材追踪，不得触发会员返利资产变更。
- 平台和供应商配置受租户隔离；禁止在 SQL 中写固定租户凭证或种子数据。
- 所有真实凭证通过配置管理维护，不提交到 Git、测试代码或文档。
- 模拟订单回调仅能在 SDK 自动化测试中使用，不得暴露生产 Controller。

## 11. 限制

- 当前不提供滴滴商品搜索、商品详情和内容解析。
- 当前不新增管理后台页面，仅提供后台 API。
- 当前不开放模拟订单回调 API。
- 当前不新增数据表，复用平台、供应商、转链、订单和返利表。
- 素材接口生成的 `ops-UUID` 不具备会员返利归因资格。
- SDK 源码未附带 LICENSE；纳入仓库前必须确认授权。授权不能确认时，应按公开协议重新实现客户端。
- 真实接口冒烟测试需要有效滴滴联盟凭证、活动 ID 和推广位 ID。

## 12. 验证命令

在 `backend` 目录执行：

```bash
mvn test -pl qiji-module-cps/qiji-module-cps-sdk-dunion -am
mvn test -pl qiji-module-cps/qiji-module-cps-biz -am "-Dtest=DidiOfficialVendorClientTest,DidiPlatformClientAdapterTest,DidiUnionMaterialServiceTest,DidiUnionControllerTest,CpsOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"
mvn test -pl qiji-module-cps -am
```

数据库验证使用 MySQL 8 对 `backend/sql/module/cps-update.sql` 中 2026-07-10 增量块连续执行两次，确认三列保持可空且第二次执行不报错。生产冒烟测试只使用专用测试租户和测试推广位。
