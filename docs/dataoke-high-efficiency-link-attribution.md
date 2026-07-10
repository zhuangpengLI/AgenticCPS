# 大淘客高效转链与订单归因开发指南

> 来源文档：
> - 大淘客《高效转链使用攻略》：https://www.dataoke.com/kfpt/open-gz.html?id=89
> - 大淘客《高效转链中 externalId 参数如何用来绑定用户》：https://www.dataoke.com/kfpt/open-gz.html?id=88
> - 大淘客《渠道ID以及会员运营ID使用详解（解决哪个用户下单问题）》：https://www.dataoke.com/kfpt/open-gz.html?id=87
>
> 整理日期：2026-07-07。后续实现前若大淘客或淘宝联盟规则变更，以最新官方文档为准。

## 1. 这套机制解决什么问题

淘宝 CPS 的核心难点不是“能不能转链”，而是“订单回来以后能不能知道是谁下单”。大淘客高效转链接口提供 `pid`、`channelId`、`specialId`、`externalId` 等参数，但它们的用途不同：

| 参数 | 淘宝联盟语义 | 主要用途 | AgenticCPS 使用边界 |
|---|---|---|---|
| `pid` | 推广位，格式通常为 `mm_xxx_xxx_xxx` | 决定佣金归属到哪个淘宝客推广位 | 必须保存到转链记录和订单归因证据中；渠道/会员场景必须使用专属 PID |
| `goodsId` | 淘宝商品 ID | 高效转链必填商品标识 | 从商品搜索、详情、解析结果传入 |
| `couponId` | 优惠券 ID | 指定某个优惠券进行转链 | 非必填；传入前必须确认券 ID 正确 |
| `channelId` / `relationId` | 渠道关系 ID | 识别渠道方、推广合伙人或分销渠道 | 仅渠道推广场景使用；必须配合渠道专属 PID，订单查询用 `orderScene=2` |
| `specialId` | 会员运营 ID | 识别已备案会员、粉丝、消费者 | 必须配合会员专属 PID，订单查询用 `orderScene=3` |
| `externalId` | 淘宝客外部用户标记 | 用本系统用户 ID、openid 等触发/查询会员绑定关系 | 主要用于新用户绑定到 `specialId`，不能单独当作订单归因闭环 |
| `xid` | 团长与下游渠道合作标识 | 统计渠道推广效果 | 只做效果统计，不作为会员资产归属的可信主键 |

结论：后续开发不能把 `externalId = memberId` 简化成“订单一定会回传 memberId”。可靠归因要么靠 `relationId`，要么靠 `specialId`，并且必须使用匹配的专属 PID 与订单查询场景。

## 2. 高效转链请求与返回要点

### 2.1 请求参数规则

- `goodsId` 是转链必填项。
- `pid` 非必填的前提是大淘客推广中心已配置默认 PID；如果要切换到同一淘宝客账号下的其它推广位，必须显式传入。
- `couponId` 用于指定优惠券；错误券 ID 可能导致无法正常跳转。
- `channelId` 是渠道关系 ID，普通转链不需要。使用它时必须满足两个条件：使用渠道专属 PID，并传递 `relationId`/`channelId`。
- `specialId` 是会员运营 ID。使用它时必须使用会员专属 PID，否则订单里不保证透出会员运营 ID。
- `externalId` 是开发者自定义用户标记，如系统用户 ID、微信 openid。它适合新用户备案/绑定流程，用来建立 `externalId -> specialId` 映射。
- `xid` 只用于团长与下游渠道合作的效果统计，不替代 `relationId` 或 `specialId`。

### 2.2 返回参数规则

大淘客高效转链会返回多种可投放链接或口令，官方说明这些链接都带有推广 PID：

- `couponClickUrl`：商品优惠券推广链接。
- `tpwd`：淘口令。
- `longTpwd`：长口令，适配 iOS 14 等场景。
- `shortUrl`：短链接。
- `kuaiZhanUrl`：快站地址，可在微信中直接打开商品详情，仅支持大淘客商品。

开发时要把“用户实际拿到的投放物料”和“生成它时使用的归因参数”一起持久化，至少包括：`platformCode`、`vendorCode`、`goodsId`、`pid`、`channelId/relationId`、`specialId`、`externalId`、`xid`、`memberId`、`sourceContext`、返回链接类型、转链时间、请求幂等键或业务流水号。

### 2.3 预估佣金只做买前展示

大淘客文档给出的买前预估佣金思路是：

```text
券后价 * 佣金比例
```

实现时注意：

- 券后价优先使用 `actualPrice`。
- 优惠券面额可从 `couponInfo` 的“满 X 元减 Y 元”中提取 `Y`。
- `maxCommissionRate` 是百分比，例如返回 `20` 表示 20%。
- 买前预估不是结算金额，真实佣金还会受税费、红包、活动、退款、低佣等影响；返利入账必须以订单接口和结算状态为准。

## 3. 三种归因模式

### 3.1 普通 PID 转链

适用场景：只需要自己淘宝客账号拿佣金，不需要精确识别哪个用户下单。

```text
转链：goodsId + 普通 PID
下单：淘宝记录 PID
查单：普通订单查询
归因：只能归到推广位，不能稳定归到 AgenticCPS memberId
```

AgenticCPS 中普通 PID 不应用于需要返利到账的用户购买链路，除非还有其它可信点击/登录态/订单绑定机制兜底。

### 3.2 渠道 ID（relationId）归因

适用场景：推广渠道、代理、合伙人、具备推广能力的渠道方。

前置条件：

- 淘宝联盟账号已开通渠道管理权限。
- 渠道方已完成备案，生成 `relationId`。
- 使用渠道专属 PID。

使用流程：

```text
1. 渠道方授权或备案，生成 relationId
2. AgenticCPS 保存 localMemberId/channelOwnerId -> relationId -> channel PID
3. 高效转链传入 goodsId + channel PID + channelId/relationId
4. 用户下单
5. 订单同步指定 orderScene=2
6. 订单明细返回 relationId
7. AgenticCPS 用 relationId 映射到本地用户/渠道
```

检查项：

- `channelId` 和渠道专属 PID 缺一不可。
- 订单查询必须使用渠道订单场景 `orderScene=2`，否则可能查不到渠道关系字段。
- 如果订单没有 `relationId`，优先排查 PID 类型、链接上是否带了 `relationId`、`relationId` 是否属于当前账号、订单查询场景是否正确。
- 使用百川 SDK 时，避免先打开渠道推广 H5 再由百川二次转链；二次转链可能导致渠道 ID 丢失。
- 渠道 ID 可能因违规或长期自推自买等原因失效；失效后佣金结算可能仍正常，但订单字段不再透出渠道 ID。

### 3.3 会员运营 ID（specialId）归因

适用场景：识别消费者/粉丝/会员，用于用户返利、私域运营、二次营销。

前置条件：

- 淘宝联盟账号已开通会员运营管理权限。
- 消费者与推广者已绑定关系，生成 `specialId`。
- 使用会员专属 PID。
- 商品属于会透出会员 ID 的营销商品库范围；是否透出以推广当日查询和联盟订单报表为准。

使用流程：

```text
1. 用户授权或备案，生成 specialId
2. AgenticCPS 保存 localMemberId -> externalId -> specialId -> member PID
3. 高效转链传入 goodsId + member PID，可带 specialId
4. 用户下单
5. 订单同步指定 orderScene=3
6. 订单明细返回 specialId
7. AgenticCPS 用 specialId 映射到本地 memberId
```

检查项：

- 传 `specialId` 时必须使用会员专属 PID。
- 订单查询必须使用会员订单场景 `orderScene=3`。
- 订单没有 `specialId` 时，排查会员运营权限、用户绑定关系、会员专属 PID、订单查询场景、商品是否属于营销商品库。
- 某些单品或活动即使满足条件，也可能不返回会员 ID；但总订单仍可正常结算佣金。这种订单不能直接入账到本地会员，除非有其它可信证据。

## 4. externalId 的正确使用方式

`externalId` 不是订单归因主键，它是“把本系统用户标记带到淘宝联盟备案链路中”的桥。

推荐流程：

```text
1. AgenticCPS 为本地用户生成稳定 externalId
   例：tenantId + ":" + memberId，或经过脱敏/哈希后的等价值

2. 新用户首次绑定时调用高效转链
   入参包含 goodsId、会员专属 PID、externalId

3. 把淘口令或链接发给用户
   用户打开后完成会员申请/绑定

4. 通过淘宝联盟私域用户备案信息查询能力获取 externalId 对应的 specialId
   大淘客文档说明该能力指向淘宝联盟接口，不等于高效转链接口自动回传订单归属

5. AgenticCPS 持久化映射
   tenantId + memberId + externalId + specialId + pid + status + bindTime

6. 后续转链和订单同步以 specialId 为主进行归因
```

实现约束：

- `externalId` 可以取本系统账户 ID、微信 openid 等，但不要直接暴露敏感 ID；推荐使用可追溯但不可枚举的稳定标记。
- 同一租户内 `externalId` 必须唯一；跨租户必须带租户隔离。
- `externalId -> specialId` 映射必须可审计，记录绑定来源、绑定时间、状态、失败原因。
- 未拿到 `specialId` 前，不要承诺“订单一定能识别到会员”。
- 如果供应商订单响应确实返回了外部标记，也只能作为辅助证据；资金入账仍要优先使用可信登录上下文、`specialId`、`relationId` 或已验证映射。

## 5. AgenticCPS 后续实现准则

### 5.1 转链接口准则

- App 用户端转链必须以登录态 `memberId` 为准。
- MCP 转链必须优先使用可信 `ToolContext` 或已验证 API Key 绑定主体，不能让请求体 `memberId` 覆盖可信上下文。
- 管理后台运营转链可以指定测试 PID、渠道 ID、会员 ID，但必须标记为运营测试或渠道投放，不得直接写入用户返利归因。
- `CpsPromotionLinkRequest` / 转链记录应区分以下字段：
  - 本地用户：`memberId`
  - 外部用户标记：`externalId`
  - 渠道关系：`relationId/channelId`
  - 会员运营：`specialId`
  - 推广位：`pid`
  - 订单场景：`orderScene`
  - 推广位类型：普通、渠道专属、会员专属
- 传 `channelId` 时校验 PID 类型是渠道专属；传 `specialId` 或走会员绑定时校验 PID 类型是会员专属。
- 高效转链返回失败时，保留供应商错误码、请求参数摘要和归因参数，方便排查 PID/权限/券 ID 问题。

### 5.2 订单同步准则

订单同步不能只拉普通订单。淘宝订单建议按业务需要覆盖：

| 查询场景 | 目的 | 归因字段 |
|---|---|---|
| 普通订单 | 兜底结算、运营对账 | PID、订单号 |
| `orderScene=2` | 渠道订单 | `relationId/channelId` |
| `orderScene=3` | 会员运营订单 | `specialId` |

订单归因优先级建议：

```text
1. specialId -> local member mapping
2. relationId/channelId -> local channel/member mapping
3. supplier returned externalId -> verified externalId mapping（仅当供应商明确返回且已验证）
4. transfer record / click record / manual reconciliation
5. 无可信归因：只做平台订单入库，不做会员返利入账
```

必须保存的订单证据字段：

- `tenantId`
- `platformCode`
- `vendorCode`
- `tradeId` / 平台订单号
- `goodsId`
- `pid`
- `relationId/channelId`
- `specialId`
- `externalId`
- `orderScene`
- `memberId`（本地归因结果）
- `attributionSource`（specialId、relationId、externalId、manual、unknown）
- `rawResponse` 或脱敏后的供应商字段摘要

### 5.3 测试准则

改动高效转链、订单同步、MCP 转链或推广位管理时，至少补充以下测试：

- ToolContext 用户 A + 请求体 memberId 用户 B：最终转链归因必须使用 A。
- `channelId` + 普通 PID：应拒绝或明确降级，不得伪装为渠道订单。
- `specialId` + 非会员专属 PID：应拒绝或明确提示配置错误。
- `orderScene=2` 返回 `relationId`：能映射到本地用户/渠道。
- `orderScene=3` 返回 `specialId`：能映射到本地 memberId。
- 无 `specialId` / `relationId` 的订单：不得自动入账给请求体中的 memberId。
- `externalId` 绑定流程：未取得 `specialId` 前状态为待绑定，取得后映射可用于订单归因。

## 6. 常见问题排查

| 现象 | 优先排查 |
|---|---|
| 转链成功但订单没有渠道 ID | 是否使用渠道专属 PID；是否传了 `channelId/relationId`；查单是否传 `orderScene=2`；是否被百川二次转链 |
| 转链成功但订单没有会员 ID | 是否开通会员运营权限；用户是否已绑定；是否使用会员专属 PID；查单是否传 `orderScene=3`；商品是否属于营销商品库 |
| 使用 `externalId` 后仍不知道谁下单 | 是否已经通过备案查询拿到 `specialId` 并保存映射；是否误把 `externalId` 当订单回传字段 |
| 预估佣金和结算佣金不一致 | 税费、红包、低佣、退款、活动、商品状态都会影响真实佣金；以订单和结算接口为准 |
| 渠道 ID 后续不再透出 | 渠道方违规、长期自推自买或其它联盟规则可能导致失效；失效后不能继续用该字段做用户归因 |

## 7. AgenticCPS 当前落地方案

### 7.1 数据模型

`cps_adzone` 保存本地会员/渠道与淘宝联盟外部标识的映射：

- `adzone_id`：实际转链 PID。
- `relation_type=member`：本地会员专属推广位，`relation_id` 存本地 `memberId`，`external_special_id` 存淘宝会员运营 ID。
- `relation_type=channel`：渠道推广位，`relation_id` 存本地渠道/会员主体 ID，`external_relation_id` 存淘宝渠道关系 ID。

`cps_order` 保存订单归因证据：

- `external_info`：平台真实返回的 `external_id`。
- `special_id`：淘宝会员运营 ID。
- `relation_id`：淘宝渠道关系 ID。
- `order_scene`：淘宝查单场景。
- `attribution_source`：最终绑定会员的依据，取值为 `specialId`、`relationId`、`externalId`、`adzone`、`transferRecord`。

升级已有库时执行：

```sql
source backend/sql/mysql/cps-2026-07-07-add-dataoke-attribution-fields.sql;
```

### 7.2 转链执行流程

用户侧或 MCP 生成淘宝推广链接时：

```text
1. 使用可信登录态/ToolContext 获取本地 memberId
2. 查询 cps_adzone 中该 memberId 的可用推广位
3. 如果 relation_type=member：
   - pid = adzone_id
   - specialId = external_special_id
   - externalId = memberId
   - 本次链路期望查单 orderScene=3
4. 如果 relation_type=channel：
   - pid = adzone_id
   - channelId/relationId = external_relation_id
   - externalId = memberId
   - 本次链路期望查单 orderScene=2
5. 如果没有专属推广位：
   - 使用平台默认 PID
   - 仅保留 externalId 兜底，不承诺能稳定识别会员订单
```

### 7.3 下单与订单同步流程

淘宝订单同步必须覆盖三类场景：

```text
1. orderScene=1 拉普通订单
2. orderScene=2 拉渠道订单，解析 relation_id
3. orderScene=3 拉会员运营订单，解析 special_id
4. 每个场景按 3 小时窗口分页拉取，避免大淘客/淘宝联盟窗口限制
5. 订单入库后按 specialId -> relationId -> externalId -> 专属PID -> 唯一转链记录 归因
6. 找不到可信归因时只保存订单，不给本地会员入账
```

后台手动同步默认会对淘宝执行上述三场景拉取；定时任务 `cpsOrderSyncJob` 也使用同一逻辑。

### 7.4 运营配置流程

新会员运营链路上线前按以下顺序配置：

```text
1. 在淘宝联盟开通会员运营权限，并完成用户绑定/备案，取得 specialId
2. 在淘宝联盟创建或确认会员专属 PID
3. 在 AgenticCPS 管理后台【CPS推广位】新增或编辑用户专属推广位：
   平台=淘宝
   推广位ID=会员专属 PID
   类型=用户专属
   关联用户=本地 memberId
   会员运营ID=淘宝 specialId
   状态=启用
4. 让该会员从 AgenticCPS 生成链接并下单
5. 执行淘宝订单同步 queryType=4
6. 在订单详情确认：
   special_id 有值
   order_scene=3
   member_id 已绑定到本地会员
   attribution_source=specialId
```

渠道链路类似，只是第 1 步取得 `relationId`，第 3 步选择“渠道专属”并填写“淘宝渠道ID”，第 6 步确认 `order_scene=2` 和 `attribution_source=relationId`。

### 7.5 验收标准

一条链路只有同时满足以下条件，才视为“转链和下单跑通”：

- 转链请求里 `pid` 是对应的会员/渠道专属 PID。
- 会员场景请求带 `specialId`，渠道场景请求带 `channelId/relationId`。
- 订单同步能在对应 `orderScene` 拉到订单。
- 订单记录保留 `special_id` 或 `relation_id`。
- `member_id` 由可信字段映射出来，而不是直接相信请求体。
- 订单详情能看到 `attribution_source`，便于运营排查。

## 8. 后续开发入口

- 大淘客/好单库配置与测试：`docs/大淘客与好单库配置及接口测试指南.md`
- MCP 接入：`docs/agentic-cps-mcp-server-guide.md`
- CPS 技术债：`docs/cps-tech-debt-inventory.md`
- 项目地图：`docs/project-map.md`
