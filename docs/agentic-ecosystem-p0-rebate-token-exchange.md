# Agentic 生态 P0：CPS 返利兑换 AI Token

## 已落地范围

- AgenticCPS 负责返利余额、冻结、解冻、确认扣减和本地兑换订单。
- aitoken-platform `billing-service` 负责兑换预估、兑换提交、Token 入账和兑换订单查询。
- AgenticAIoT 本轮不改业务代码，只预留 `sourceSystem`、`tenantId`、`sceneCode`、`businessId` 等生态字段。

## 统一签名

服务间接口统一请求头：

```http
X-App-Id: AgenticCPS
X-Tenant-Id: 1
X-Timestamp: 1710000000
X-Nonce: random-string
X-Signature: hmac-sha256-base64
X-Idempotency-Key: unique-business-key
```

签名串：

```text
method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + idempotencyKey + "\n" + sha256(body)
```

## 核心链路

1. App 调用 `POST /cps/rebate/token-exchange/preview` 预估可得 Token。
2. App 调用 `POST /cps/rebate/token-exchange/submit` 提交兑换。
3. CPS 创建 `cps_rebate_token_exchange_order`。
4. CPS 将 `cps_rebate_account.available_balance` 转入 `frozen_balance`。
5. CPS 调用 aitoken `POST /api/v1/openapi/token/exchange/submit`。
6. aitoken 成功增加 new-api `user.quota` 并记录 `credit_transfer_record`。
7. CPS 确认扣减冻结金额，兑换单进入 `SUCCESS`。
8. aitoken 失败时 CPS 解冻返利；调用超时时 CPS 订单进入 `PROCESSING` 等待补偿。

## Smoke Case

先启动 aitoken `billing-service`，并确保存在：

- `credit_source.source_code = AgenticCPS`
- 对应 `credit_conversion_rule`
- new-api `user.id` 与 CPS `memberId` 可对应

本地开发默认密钥：

```text
agentic-cps-dev-secret
```

兑换预估：

```http
POST /api/v1/openapi/token/exchange/preview
```

兑换提交：

```http
POST /api/v1/openapi/token/exchange/submit
```

CPS 用户端入口：

```http
POST /cps/rebate/token-exchange/preview
POST /cps/rebate/token-exchange/submit
GET  /cps/rebate/token-exchange/{exchangeOrderNo}
```

CPS 服务间返利资产接口：

```http
GET  /openapi/cps/rebate/balance
POST /openapi/cps/rebate/freeze
POST /openapi/cps/rebate/unfreeze
POST /openapi/cps/rebate/confirm-deduct
```
