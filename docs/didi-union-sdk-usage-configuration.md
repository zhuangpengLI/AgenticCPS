# 滴滴联盟 DUnion SDK 使用与配置

## 1. 适用范围

本文面向 AgenticCPS 的部署、运营和后端开发人员，说明如何配置并使用仓内模块 `qiji-module-cps-sdk-dunion`。

当前支持：

- H5 推广链接、小程序路径、H5 二维码、小程序码、推广海报和券码。
- 滴滴联盟订单同步。
- 连接测试和订单归因诊断。

当前不支持滴滴商品搜索。SDK 的模拟订单回调仅供自动化测试使用，不提供生产后台 API。

## 2. 前置条件

准备以下滴滴联盟参数：

| 参数 | 说明 | 是否必填 |
|---|---|---|
| `App-Key` | 滴滴联盟应用标识 | 是 |
| `accessKey` | 滴滴联盟签名密钥 | 是 |
| `activityId` | 滴滴联盟活动 ID，正整数 | 调用素材或转链时必填 |
| `promotionId` | 滴滴联盟推广位 ID，正整数 | 是，建议配置默认值 |
| `apiBaseUrl` | API 基础地址 | 是，生产环境填写 `https://union.didi.cn/openapi/v1.0` |

AgenticCPS 按租户读取配置。每个需要使用滴滴联盟的租户都必须分别创建 `didi` 平台和 `official:didi` 供应商配置。

## 3. Maven 依赖

模块已加入 `backend/qiji-module-cps` Maven Reactor。仓内其他子模块使用时添加：

```xml
<dependency>
    <groupId>com.qiji.cps</groupId>
    <artifactId>qiji-module-cps-sdk-dunion</artifactId>
</dependency>
```

版本由父 POM 和项目 BOM 统一管理，不要使用本地 JAR 或 `systemPath`。

## 4. AgenticCPS 配置

### 4.1 创建滴滴平台

平台编码必须为 `didi`，激活供应商必须为 `official`：

```http
POST /admin-api/cps/platform/create
Authorization: Bearer <admin-token>
tenant-id: <tenant-id>
Content-Type: application/json

{
  "platformCode": "didi",
  "platformName": "滴滴联盟",
  "defaultAdzoneId": "20001",
  "platformServiceRate": 0,
  "sort": 50,
  "status": 1,
  "activeVendorCode": "official",
  "remark": "滴滴联盟官方 SDK"
}
```

`status=1` 表示启用。`defaultAdzoneId` 可以同时在平台和供应商中维护，实际 SDK 调用优先使用请求中的推广位，其次使用启用供应商的默认推广位。

### 4.2 创建官方供应商

配置映射固定如下：

| AgenticCPS 字段 | DUnion 参数 | 说明 |
|---|---|---|
| `vendorCode` | - | 固定为 `official` |
| `vendorType` | - | 固定为 `official` |
| `platformCode` | - | 固定为 `didi` |
| `appKey` | `App-Key` | 必填 |
| `appSecret` | `accessKey` | 必填，不要填写 App-Key |
| `defaultAdzoneId` | `promotionId` | 正整数 |
| `apiBaseUrl` | SDK `baseUrl` | 生产地址见下方示例 |
| `extraConfig` | SDK 超时 | 数据库和请求 VO 中为 JSON 字符串 |

```http
POST /admin-api/cps/api-vendor/create
Authorization: Bearer <admin-token>
tenant-id: <tenant-id>
Content-Type: application/json

{
  "vendorCode": "official",
  "vendorName": "滴滴联盟官方",
  "vendorType": "official",
  "platformCode": "didi",
  "appKey": "<DIDI_APP_KEY>",
  "appSecret": "<DIDI_ACCESS_KEY>",
  "apiBaseUrl": "https://union.didi.cn/openapi/v1.0",
  "defaultAdzoneId": "20001",
  "extraConfig": "{\"timeoutMs\":\"5000\"}",
  "priority": 10,
  "status": 1,
  "remark": "滴滴联盟生产配置"
}
```

超时规则：

- 默认 `5000` 毫秒。
- 最小 `1000` 毫秒，最大 `30000` 毫秒。
- 超出范围会自动收敛到边界值，非数字值回退为 `5000`。
- 同一个值同时用于连接超时和读取超时。

更新供应商时，`appSecret` 留空表示保留原密钥。不要在日志、文档或工单中记录真实 `accessKey`。

### 4.3 测试连接

```http
GET /admin-api/cps/didi-union/connection-test
Authorization: Bearer <admin-token>
tenant-id: <tenant-id>
```

所需权限：`cps:api-vendor:query`。

成功响应：

```json
{
  "code": 0,
  "data": true,
  "msg": ""
}
```

连接测试会查询最近一小时的一条订单，用于验证租户配置、签名、网络、HTTP 状态和滴滴业务状态。`data=false` 时依次检查平台和供应商是否启用、编码是否准确、密钥、基础地址以及服务器出口网络。

## 5. 生成推广素材

统一接口：

```http
POST /admin-api/cps/didi-union/material/generate
Authorization: Bearer <admin-token>
tenant-id: <tenant-id>
Content-Type: application/json

{
  "activityId": 10001,
  "promotionId": 20001,
  "materialType": "H5_LINK"
}
```

所需权限：`cps:toolbox:link`。`activityId` 和 `promotionId` 必须为正整数；省略 `promotionId` 时使用供应商的 `defaultAdzoneId`。

| `materialType` | 主要返回字段 | 调用过程 |
|---|---|---|
| `H5_LINK` | `link`、`dsi` | 生成 H5 推广链接 |
| `MINI_LINK` | `link`、`dsi`、`appId` | 生成小程序路径 |
| `H5_QR_CODE` | `link`、`dsi`、`qrCodeUrl` | 先生成 H5 链接，再生成二维码 |
| `MINI_QR_CODE` | `link`、`dsi`、`qrCodeUrl` | 先生成小程序路径，再生成小程序码 |
| `POSTER` | `link`、`dsi`、`posterUrl` | 先生成小程序路径，再生成海报 |
| `COUPON_CODE` | `couponCode` | 生成券码 |

示例响应：

```json
{
  "code": 0,
  "data": {
    "materialType": "H5_LINK",
    "activityId": 10001,
    "promotionId": 20001,
    "sourceId": "ops-4a4efea1-506d-48ea-993e-56d2ce23d72c",
    "link": "https://union.didi.cn/example",
    "dsi": "example-dsi",
    "appId": "example-app-id",
    "appSource": "example-app-source",
    "qrCodeUrl": null,
    "posterUrl": null,
    "couponCode": null,
    "traceId": "example-trace-id"
  },
  "msg": ""
}
```

后台素材的 `sourceId` 固定为 `ops-UUID`，只用于运营审计，不能用于会员返利归因。会员推广链接必须走现有 App 或 MCP 转链入口，由可信登录上下文生成会员归因标识。

## 6. 标准转链与订单同步

滴滴适配器已经接入 `CpsPlatformClient`，业务层无需直接调用 SDK：

| 标准字段 | DUnion 字段 |
|---|---|
| `goodsId` | `activityId` |
| `adzoneId` | `promotionId` |
| 可信 `externalId/memberId` | `sourceId` |
| 返回 `link` | `longUrl` |
| `dsi/appId/appSource/traceId` | `extraFields` |

滴滴不支持商品搜索。全平台搜索会跳过滴滴；明确指定 `platformCode=didi` 搜索时返回 `PLATFORM_CAPABILITY_UNSUPPORTED`。

订单同步复用现有定时和手工同步流程。订单查询页大小限制为 `1-100`，默认 `50`，分页最多推进至第 `100` 页。订单金额和佣金由分转换为元，佣金为 CPA 与 CPS 收益之和。

状态处理原则：

- SDK 状态 `7` 映射为已结算。
- 退款、风控或失败状态映射为退款/失效。
- 其他处理中状态最多映射为已付款，避免提前入账。

## 7. 订单归因诊断

```http
GET /admin-api/cps/didi-union/order-attribution?orderId=<didi-order-id>
Authorization: Bearer <admin-token>
tenant-id: <tenant-id>
```

所需权限：`cps:order:query`。该接口只查询滴滴联盟预估归因结果，不修改本地订单或返利账户。

```json
{
  "code": 0,
  "data": {
    "orderId": "123456789",
    "traceId": "example-trace-id",
    "successList": [],
    "failList": [
      {
        "failReason": "未匹配推广来源",
        "sceneName": "网约车"
      }
    ]
  },
  "msg": ""
}
```

## 8. Java SDK 直接调用

业务适配器之外的内部开发可以直接构造 SDK 客户端：

```java
import cn.didi.union.client.DunionClientFactory;
import cn.didi.union.client.UnionClient;
import cn.didi.union.models.DunionClientConfig;
import cn.didi.union.models.LinkResponse;
import cn.didi.union.models.Result;

DunionClientConfig config = DunionClientConfig.builder()
        .appKey(System.getenv("DIDI_APP_KEY"))
        .accessKey(System.getenv("DIDI_ACCESS_KEY"))
        .baseUrl("https://union.didi.cn/openapi/v1.0")
        .connectTimeout(5000)
        .readTimeout(5000)
        .build();

UnionClient client = DunionClientFactory.build(config).getUnionClient();
Result<LinkResponse> result = client.generateH5Link(
        10001L,
        20001L,
        "ops-example",
        5000);

if (!result.isSuccess() || result.getModel() == null) {
    throw new IllegalStateException(result.getError() == null
            ? "DUnion request failed"
            : result.getError().getMessage());
}

String promotionUrl = result.getModel().getData().getLink();
```

常用方法：

```java
client.generateMiniLink(activityId, promotionId, sourceId, timeoutMs);
client.generateH5Code(dsi, sourceId, timeoutMs);
client.generateMiniCode(dsi, sourceId, timeoutMs);
client.generatePoster(dsi, sourceId, timeoutMs);
client.generateCouponPwd(activityId, promotionId, sourceId, timeoutMs);
client.queryOrderList(startEpochSecond, endEpochSecond, orderType, page, size, timeoutMs);
client.selfQueryOrder(orderId, timeoutMs);
```

二维码和海报推荐先调用链接接口取得 `dsi`，再调用对应素材接口。所有调用都必须检查 `Result.isSuccess()` 和 `getModel()`，不得只依赖 HTTP 200。

## 9. 常见问题

| 现象 | 排查重点 |
|---|---|
| 找不到供应商配置 | 当前租户是否存在并启用了 `vendorCode=official`、`platformCode=didi` 的记录 |
| `appKey/appSecret 不能为空` | `appKey` 填 App-Key，`appSecret` 填 accessKey |
| `promotionId 必须为正整数` | 请求推广位或供应商 `defaultAdzoneId` 必须是纯数字正整数 |
| HTTP 401/403 | 凭证、签名时间、服务器时间和租户配置 |
| HTTP 404 | `apiBaseUrl` 应为基础地址，不要附加具体接口路径 |
| `errno != 0` | 根据脱敏错误摘要和 `traceId` 联系滴滴联盟排查业务参数 |
| 请求超时 | 检查服务器出口网络，再适当调高 `extraConfig.timeoutMs` |
| 搜索提示能力不支持 | 滴滴联盟适配器不提供商品搜索，这是预期行为 |
| 订单未归因 | 检查转链时的可信 `sourceId`，再调用订单归因诊断接口 |

## 10. 安全要求

- 不得提交真实 App-Key、accessKey、推广位或生产 Token。
- 不得记录 accessKey、签名明文、完整请求头或完整第三方响应。
- 用户侧转链不得接受请求体伪造的 `memberId`。
- 后台 `ops-UUID` 素材不得触发会员返利入账。
- 不得在生产 Controller 中开放 `mockOrderCallback`。
- 自定义 `apiBaseUrl` 仅用于受控测试服务器，生产环境使用滴滴官方 HTTPS 地址。

## 11. 验证命令

```bash
cd backend

mvn test -pl qiji-module-cps/qiji-module-cps-sdk-dunion -am

mvn test -pl qiji-module-cps/qiji-module-cps-biz -am \
  -Dtest=DidiOfficialVendorClientTest,DidiPlatformClientAdapterTest,DidiUnionMaterialServiceTest,DidiUnionControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

真实环境验收至少覆盖：连接测试、H5 链接、一种二维码或海报、订单同步和订单归因诊断。
