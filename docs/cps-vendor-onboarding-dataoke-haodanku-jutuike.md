# 大淘客、好单库、聚推客接入配置指南

本文面向第一次接入 AgenticCPS 的开发者，说明如何配置大淘客、好单库、聚推客，让商品查询、返利转链、活动拉取和订单查询走到真实供应商接口。

## 1. 先理解两个概念

AgenticCPS 把“电商平台”和“API 供应商”分开配置：

| 概念 | 数据表 | 示例 | 作用 |
| --- | --- | --- | --- |
| 电商平台 | `cps_platform` | `taobao`、`jd`、`pdd`、`union` | 用户要查哪个平台的商品或活动 |
| API 供应商 | `cps_api_vendor` | `dataoke`、`haodanku`、`jutuike` | 实际调用哪家开放平台接口 |

一次商品搜索的路由大致是：

```text
商品广场/返利查询/MCP
  -> platformCode=taobao/jd/pdd
  -> cps_platform.active_vendor_code
  -> cps_api_vendor(vendorCode + platformCode)
  -> 对应 Java VendorClient
```

如果请求里显式传了 `vendorCode`，会优先使用请求里的供应商；否则使用 `cps_platform.active_vendor_code`。

## 2. 当前代码支持范围

| 供应商 | vendorCode | platformCode | 商品搜索 | 商品转链 | 订单查询 | 活动拉取 | 配置重点 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 大淘客 | `dataoke` | `taobao` | 支持 | 支持 | 支持 | 支持淘宝活动 | `appKey` + `appSecret` |
| 大淘客 | `dataoke` | `jd` | 支持 | 支持 | 支持 | 不作为活动源 | `appKey` + `appSecret`，京东转链/订单还要补 `authToken` |
| 大淘客 | `dataoke` | `pdd` | 支持 | 支持 | 支持 | 不作为活动源 | `appKey` + `appSecret`，建议补默认推广位 |
| 好单库 | `haodanku` | `taobao` | 支持 | 支持 | 支持 | 支持活动中心 | `appKey` 填好单库 `apikey` |
| 好单库 | `haodanku` | `jd` | 支持 | 支持 | 支持 | 活动拉取不走这个 platformCode | `appKey` 填 `apikey`，京东通常还要补 `authToken` 或 `extraConfig.jd_user_id` |
| 好单库 | `haodanku` | `pdd` | 支持 | 支持 | 支持 | 活动拉取不走这个 platformCode | `appKey` 填 `apikey`，拼多多权限需在好单库侧开通 |
| 聚推客 | `jutuike` | `union` | 活动列表兼作搜索 | 活动转链 | 统一订单 | 支持 | `appKey` 填聚推客 `apikey` |

注意：聚推客当前适配器的 `platformCode` 是 `union`，主要面向聚推客联盟活动、活动转链和统一订单，不是淘宝/JD/PDD 商品库搜索的替代入口。

## 3. 需要提前准备的密钥和权限

### 大淘客

在大淘客开放平台创建应用后准备：

| 字段 | 填到 AgenticCPS 哪里 | 说明 |
| --- | --- | --- |
| AppKey | `cps_api_vendor.app_key` | 大淘客开放平台应用 Key |
| AppSecret | `cps_api_vendor.app_secret` | 用于 MD5 签名 |
| API Base URL | `cps_api_vendor.api_base_url` | 默认 `https://openapi.dataoke.com/api` |
| 京东 unionId / 授权 key | `cps_api_vendor.auth_token` | 仅 JD 转链、JD 订单查询需要 |
| 推广位 PID | `cps_api_vendor.default_adzone_id` 或 `cps_platform.default_adzone_id` | 淘宝/PDD 转链和订单归因建议配置 |

大淘客签名由代码自动完成：`MD5(appKey={appKey}&timer={timestamp}&nonce={random6}&key={appSecret})`，结果转大写后作为 `signRan` 发送。

### 好单库

在好单库开放平台创建应用后准备：

| 字段 | 填到 AgenticCPS 哪里 | 说明 |
| --- | --- | --- |
| apikey | `cps_api_vendor.app_key` | 普通接口鉴权参数 |
| API Base URL | `cps_api_vendor.api_base_url` | 推荐填 `https://v2.api.haodanku.com` |
| JD 用户/授权标识 | `cps_api_vendor.auth_token` 或 `extra_config.jd_user_id` | 京东搜索、转链、订单常需要 |
| PDD PID | `cps_api_vendor.default_adzone_id` | 拼多多搜索、转链、订单建议配置 |

好单库商品搜索使用 v2 域名；转链代码会把 `v2.api.haodanku.com` 自动替换成 `v3.api.haodanku.com`。

好单库活动中心当前先走站点活动目录 `https://www.haodanku.com/openapi/activity_list_cat`，再按分类分页调用 `https://www.haodanku.com/openapi/activity_list`。同步只保留项目已经完成官方活动转链的活动：淘宝会场对应 `POST https://v2.api.haodanku.com/createConference_code`，淘宝闪购对应 `POST https://v3.api.haodanku.com/elm_activity_ratesurl`。其他平台即使出现在活动目录中，只要项目尚未完成活动字段到官方转链参数的可靠映射，就计入跳过且不入库。活动目录本身不依赖 `cps_api_vendor` 配置；实际转链、商品搜索、返利查询和订单仍然需要对应的 `cps_api_vendor`。

### 聚推客

在聚推客开放平台准备：

| 字段 | 填到 AgenticCPS 哪里 | 说明 |
| --- | --- | --- |
| apikey | `cps_api_vendor.app_key` | 聚推客接口鉴权参数 |
| API Base URL | `cps_api_vendor.api_base_url` | 默认 `http://api.jutuike.com` |
| sid | 转链请求参数 | 活动转链必填，通常用于渠道或推广位标识 |
| act_id | 转链请求的 `goodsId` | 聚推客活动 ID |

当前已适配的聚推客接口：

| 能力 | 官方接口 | AgenticCPS 调用路径 |
| --- | --- | --- |
| 活动列表 | `/union/act_list` | 活动同步、聚推客活动搜索 |
| 活动转链 | `/union/act` | 聚推客推广链接生成 |
| 统一订单 | `/union/orders` | 聚推客订单查询 |

## 4. 后台页面配置方式

适合不想直接写 SQL 的新手。

### 4.1 配置 API 供应商

进入管理后台：

```text
CPS -> API 供应商配置
```

为每个平台创建供应商记录。

大淘客淘宝示例：

| 字段 | 值 |
| --- | --- |
| 供应商编码 | `dataoke` |
| 供应商名称 | `大淘客-淘宝` |
| 供应商类型 | `aggregator` |
| 所属平台 | `taobao` |
| API Key | 自己的大淘客 AppKey |
| API Secret | 自己的大淘客 AppSecret |
| API Base URL | `https://openapi.dataoke.com/api` |
| 授权 Token | 空，JD 才重点需要 |
| 默认推广位 | 淘宝 PID，可后补 |
| 状态 | 启用 |

好单库淘宝示例：

| 字段 | 值 |
| --- | --- |
| 供应商编码 | `haodanku` |
| 供应商名称 | `好单库-淘宝` |
| 供应商类型 | `aggregator` |
| 所属平台 | `taobao` |
| API Key | 好单库 apikey |
| API Secret | 空 |
| API Base URL | `https://v2.api.haodanku.com` |
| 状态 | 启用 |

聚推客示例：

| 字段 | 值 |
| --- | --- |
| 供应商编码 | `jutuike` |
| 供应商名称 | `聚推客联盟` |
| 供应商类型 | `aggregator` |
| 所属平台 | `union` |
| API Key | 聚推客 apikey |
| API Secret | 空 |
| API Base URL | `http://api.jutuike.com` |
| 状态 | 启用 |

### 4.2 配置平台默认供应商

进入：

```text
CPS -> 平台配置
```

建议先这样配置：

| 平台编码 | 平台名称 | 当前启用供应商 | 支持供应商 |
| --- | --- | --- | --- |
| `taobao` | 淘宝 | `dataoke` | `dataoke,haodanku,official` |
| `jd` | 京东 | `dataoke` | `dataoke,haodanku,official` |
| `pdd` | 拼多多 | `dataoke` | `dataoke,haodanku,official` |
| `union` | 联盟活动 | `jutuike` | `jutuike` |

如果希望默认使用好单库，把对应平台的“当前启用供应商”改成 `haodanku`。也可以不改默认值，在商品广场或接口请求里传 `vendorCode=haodanku` 临时切换。

### 4.3 测试连接

在“平台配置”列表点击“测试连接”。

建议按这个顺序测：

1. `taobao`
2. `pdd`
3. `jd`
4. `union`

如果 `jd` 失败，优先检查 `auth_token` 是否已填京东 unionId 或授权 key。大淘客 JD 适配器会在转链和订单查询时把 `auth_token` 作为 `unionId` / `key` 使用。

## 5. 推广位配置

推广位决定转链时使用哪个 PID，也决定淘宝订单能不能稳定归因到渠道或会员。供应商密钥只解决“能调用接口”，推广位配置解决“转出来的链接归谁、订单返利归谁”。

入口：

```text
CPS -> 推广位管理
```

### 5.1 推广位类型

| 类型 | `adzoneType` | 适用场景 | 关键字段 |
| --- | --- | --- | --- |
| 通用推广位 | `general` | 没有登录会员、运营后台手工转链、兜底转链 | `platformCode`、`adzoneId`、`isDefault` |
| 渠道专属推广位 | `channel` | 淘宝渠道备案、渠道订单归因 | `relationType=channel`、`relationId`、`externalRelationId` |
| 用户专属推广位 | `member` | 会员专属 PID、会员运营 ID 归因 | `relationType=member`、`relationId`、`externalSpecialId` |

字段说明：

| 字段 | 怎么填 |
| --- | --- |
| 平台 | `taobao`、`jd`、`pdd`、`douyin` 等平台编码 |
| 推广位 ID | 平台后台创建的 PID / 推广位，淘宝常见格式是 `mm_数字_数字_数字` |
| 推广位名称 | 方便运营识别，例如 `淘宝通用PID`、`用户1001专属PID` |
| 关联类型 | 通用推广位可为空；渠道填 `channel`；会员填 `member` |
| 关联 ID | 渠道 ID 或会员 ID；会员专属推广位必须填真实会员 ID |
| 淘宝渠道 ID | 淘宝渠道备案返回的 `relationId`，填到 `externalRelationId` |
| 淘宝会员运营 ID | 淘宝会员运营备案返回的 `specialId`，填到 `externalSpecialId` |
| 默认标记 | 只是推广位自身标记；真正兜底默认推广位还要在平台配置里设置 |
| 状态 | 必须启用，否则运行时不会自动匹配 |

### 5.2 淘宝推广位归因规则

淘宝比 JD、拼多多更严格，配置时重点区分 PID、渠道 ID、会员运营 ID：

| 场景 | 必填 |
| --- | --- |
| 普通兜底转链 | `adzoneId=mm_...`，并在平台配置里设置为默认推广位 |
| 会员专属返利 | `adzoneType=member`、`relationType=member`、`relationId=会员ID`、`externalSpecialId=specialId` |
| 渠道订单归因 | `adzoneType=channel`、`relationType=channel`、`relationId=本系统渠道ID`、`externalRelationId=淘宝relationId` |

运行时优先级：

1. 请求里显式传了 `adzoneId`，优先使用请求值。
2. 请求里有可信 `memberId`，优先查找该会员启用状态的专属推广位。
3. 没有会员专属推广位时，使用 `cps_platform.default_adzone_id`。

注意：淘宝如果只在请求里直接传 `adzoneId`，系统只能拿到 PID，不能自动补 `specialId` 或 `relationId`。会员返利归因建议在“推广位管理”里维护会员专属推广位，让系统按登录会员或 MCP ToolContext 自动匹配。

### 5.3 默认推广位配置

入口：

```text
CPS -> 平台配置
```

每个平台至少配置一个可用的默认推广位：

| 平台 | 默认推广位建议 |
| --- | --- |
| `taobao` | 淘宝通用 PID，格式通常是 `mm_数字_数字_数字` |
| `pdd` | 拼多多推广位 PID |
| `jd` | 京东按供应商接口要求填写，必要时配合供应商 `auth_token` / `unionId` |
| `douyin` | 抖音推广位或 PID |
| `union` | 聚推客活动通常不强依赖本系统推广位，按接口返回和供应商要求处理 |

同时建议在 `CPS -> API 供应商管理` 中，把同平台供应商的“默认推广位”也填上。运行时转链主要看 `cps_platform.default_adzone_id`，供应商默认推广位用于供应商级兜底和人工排查。

### 5.4 配置后验证

后台验证：

```http
GET /admin-api/cps/adzone/list-by-platform?platformCode=taobao
```

转链验证：

1. 不传 `adzoneId`，传入一个已有会员，确认返回结果里的推广位是该会员专属推广位。
2. 不传 `adzoneId`，也没有会员上下文，确认使用平台默认推广位。
3. 淘宝会员专属推广位转链后，检查请求日志里是否带上 `specialId` 和 `orderScene=3`。
4. 淘宝渠道推广位转链后，检查请求日志里是否带上 `relationId` / `channelId` 和 `orderScene=2`。

## 6. 返利配置

返利配置决定“平台佣金入库后，给会员返多少”。返利比例是按佣金计算，不是按商品售价计算。

入口：

```text
CPS -> 返利配置
```

### 6.1 字段说明

| 字段 | 怎么填 |
| --- | --- |
| 会员 ID | 为空表示不是个人专属规则；填值表示该会员专属规则 |
| 会员等级 ID | 为空表示不限制等级；填值表示该等级规则 |
| 平台编码 | 为空表示全平台；填 `taobao`、`jd`、`pdd` 等表示指定平台 |
| 返利比例 | 0 到 100，单位是百分比；例如 `60` 表示返给会员佣金的 60% |
| 单笔最高返利 | 单笔返利上限；`0` 表示不限制 |
| 单笔最低返利 | 单笔返利下限；`0` 表示不限制 |
| 优先级 | 同一命中层级内优先级越大越先使用 |
| 状态 | 必须启用，否则不会参与结算 |

### 6.2 命中优先级

系统按下面顺序找第一条启用的规则：

1. 会员个人配置 + 指定平台。
2. 会员个人配置 + 全平台。
3. 会员等级配置 + 指定平台。
4. 会员等级配置 + 全平台。
5. 平台默认配置。
6. 全局默认配置。

同一层级命中多条时，先按 `priority` 从高到低，再按 `id` 从大到小。

### 6.3 计算方式

示例：

```text
平台预估佣金：10.00 元
返利比例：60%
会员预估返利：10.00 * 60% = 6.00 元
```

如果设置了最高或最低返利，会在比例计算后再套用限制：

```text
返利金额 = 平台佣金 * 返利比例
返利金额 = min(返利金额, 单笔最高返利)
返利金额 = max(返利金额, 单笔最低返利)
```

### 6.4 推荐配置顺序

新手先按下面顺序配置，能最快跑通商品查询、转链、订单同步和返利结算：

1. 新增一条全局默认规则：平台为空、会员为空、等级为空、返利比例例如 `50`、状态启用。
2. 为重点平台新增平台默认规则：例如 `taobao=60`、`pdd=55`、`jd=50`。
3. 有会员等级体系后，再新增等级规则。
4. 只有特殊会员需要单独扶持时，再新增会员个人规则。

### 6.5 配置后验证

后台验证：

```http
GET /admin-api/cps/rebate-config/list-enabled
```

业务验证：

1. 用商品查询或转链接口生成带佣金的商品。
2. 确认接口返回的预估返利符合当前会员、等级、平台命中的规则。
3. 同步一笔测试订单后，确认订单结算时能找到启用的返利配置。

如果订单有佣金但一直没有返利，优先检查是否缺少启用的全局默认规则或平台默认规则。

## 7. SQL 配置方式

适合本地初始化、批量重建测试环境或自动化部署。

项目约定：

- 本地测试密钥只写入 `backend/sql/module/test_data.sql`。
- 不要把真实密钥写进 `backend/sql/module/cps-all-in-one.sql`。
- 不要把真实密钥写进 `backend/sql/module/cps-update.sql`。
- `test_data.sql` 是本地测试数据文件，应保持 Git 忽略。

### 7.1 最小 SQL 模板

下面模板只展示字段含义，真实密钥用自己的值替换。

```sql
SET @tenant_id = 1;

-- 1. 平台路由
INSERT INTO `cps_platform`
(`id`, `platform_code`, `platform_name`, `default_adzone_id`, `platform_service_rate`,
 `sort`, `status`, `extra_config`, `remark`, `active_vendor_code`, `supported_vendors`,
 `creator`, `updater`, `tenant_id`)
VALUES
(990721010001, 'taobao', '淘宝', NULL, 0.00, 10, 1, NULL,
 '淘宝默认走大淘客，可按请求切好单库', 'dataoke', 'dataoke,haodanku,official',
 'local', 'local', @tenant_id),
(990721010002, 'jd', '京东', NULL, 0.00, 20, 1, NULL,
 '京东默认走大淘客，转链/订单通常需要 auth_token', 'dataoke', 'dataoke,haodanku,official',
 'local', 'local', @tenant_id),
(990721010003, 'pdd', '拼多多', NULL, 0.00, 30, 1, NULL,
 '拼多多默认走大淘客，建议配置默认 PID', 'dataoke', 'dataoke,haodanku,official',
 'local', 'local', @tenant_id),
(990721010004, 'union', '联盟活动', NULL, 0.00, 90, 1, NULL,
 '聚推客统一活动与订单入口', 'jutuike', 'jutuike',
 'local', 'local', @tenant_id)
ON DUPLICATE KEY UPDATE
  `platform_name` = VALUES(`platform_name`),
  `active_vendor_code` = VALUES(`active_vendor_code`),
  `supported_vendors` = VALUES(`supported_vendors`),
  `status` = VALUES(`status`),
  `update_time` = NOW(),
  `deleted` = b'0';

-- 2. API 供应商
INSERT INTO `cps_api_vendor`
(`id`, `vendor_code`, `vendor_name`, `vendor_type`, `platform_code`,
 `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`,
 `extra_config`, `priority`, `status`, `remark`, `creator`, `updater`, `tenant_id`)
VALUES
(990721010010, 'dataoke', '大淘客-淘宝', 'aggregator', 'taobao',
 'YOUR_DATAOKE_APP_KEY', 'YOUR_DATAOKE_APP_SECRET',
 'https://openapi.dataoke.com/api', NULL, 'YOUR_TAOBAO_PID', NULL, 100, 1,
 '淘宝商品搜索、返利查询、转链、订单', 'local', 'local', @tenant_id),
(990721010011, 'dataoke', '大淘客-京东', 'aggregator', 'jd',
 'YOUR_DATAOKE_APP_KEY', 'YOUR_DATAOKE_APP_SECRET',
 'https://openapi.dataoke.com/api', 'YOUR_JD_UNION_ID_OR_KEY', NULL, NULL, 100, 1,
 '京东商品搜索、转链、订单；auth_token 必须按供应商要求补齐', 'local', 'local', @tenant_id),
(990721010012, 'dataoke', '大淘客-拼多多', 'aggregator', 'pdd',
 'YOUR_DATAOKE_APP_KEY', 'YOUR_DATAOKE_APP_SECRET',
 'https://openapi.dataoke.com/api', NULL, 'YOUR_PDD_PID', NULL, 100, 1,
 '拼多多商品搜索、转链、订单', 'local', 'local', @tenant_id),
(990721010020, 'haodanku', '好单库-淘宝', 'aggregator', 'taobao',
 'YOUR_HAODANKU_APIKEY', '', 'https://v2.api.haodanku.com',
 NULL, 'YOUR_TAOBAO_PID', NULL, 90, 1,
 '淘宝商品搜索、返利查询、转链、订单', 'local', 'local', @tenant_id),
(990721010021, 'haodanku', '好单库-京东', 'aggregator', 'jd',
 'YOUR_HAODANKU_APIKEY', '', 'https://v2.api.haodanku.com',
 'YOUR_JD_USER_ID', NULL, JSON_OBJECT('jd_user_id', 'YOUR_JD_USER_ID'), 90, 1,
 '京东商品搜索、转链、订单', 'local', 'local', @tenant_id),
(990721010022, 'haodanku', '好单库-拼多多', 'aggregator', 'pdd',
 'YOUR_HAODANKU_APIKEY', '', 'https://v2.api.haodanku.com',
 NULL, 'YOUR_PDD_PID', NULL, 90, 1,
 '拼多多商品搜索、转链、订单', 'local', 'local', @tenant_id),
(990721010030, 'jutuike', '聚推客联盟', 'aggregator', 'union',
 'YOUR_JUTUIKE_APIKEY', '', 'http://api.jutuike.com',
 NULL, NULL, NULL, 80, 1,
 '聚推客活动列表、活动转链、统一订单', 'local', 'local', @tenant_id)
ON DUPLICATE KEY UPDATE
  `vendor_name` = VALUES(`vendor_name`),
  `vendor_type` = VALUES(`vendor_type`),
  `app_key` = VALUES(`app_key`),
  `app_secret` = VALUES(`app_secret`),
  `api_base_url` = VALUES(`api_base_url`),
  `auth_token` = VALUES(`auth_token`),
  `default_adzone_id` = VALUES(`default_adzone_id`),
  `extra_config` = VALUES(`extra_config`),
  `priority` = VALUES(`priority`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`),
  `update_time` = NOW(),
  `deleted` = b'0';

-- 3. 推广位
INSERT INTO `cps_adzone`
(`id`, `platform_code`, `adzone_id`, `adzone_name`, `adzone_type`,
 `relation_type`, `relation_id`, `external_relation_id`, `external_special_id`,
 `is_default`, `status`, `creator`, `updater`, `tenant_id`)
VALUES
(990721020001, 'taobao', 'YOUR_TAOBAO_PID', '淘宝通用PID', 'general',
 NULL, NULL, NULL, NULL, 1, 1, 'local', 'local', @tenant_id),
(990721020002, 'taobao', 'YOUR_TAOBAO_MEMBER_PID', '淘宝会员1001专属PID', 'member',
 'member', 1001, NULL, 'YOUR_TAOBAO_SPECIAL_ID', 0, 1, 'local', 'local', @tenant_id),
(990721020003, 'taobao', 'YOUR_TAOBAO_CHANNEL_PID', '淘宝渠道专属PID', 'channel',
 'channel', 2001, 'YOUR_TAOBAO_RELATION_ID', NULL, 0, 1, 'local', 'local', @tenant_id),
(990721020004, 'pdd', 'YOUR_PDD_PID', '拼多多通用PID', 'general',
 NULL, NULL, NULL, NULL, 1, 1, 'local', 'local', @tenant_id)
ON DUPLICATE KEY UPDATE
  `adzone_name` = VALUES(`adzone_name`),
  `adzone_type` = VALUES(`adzone_type`),
  `relation_type` = VALUES(`relation_type`),
  `relation_id` = VALUES(`relation_id`),
  `external_relation_id` = VALUES(`external_relation_id`),
  `external_special_id` = VALUES(`external_special_id`),
  `is_default` = VALUES(`is_default`),
  `status` = VALUES(`status`),
  `update_time` = NOW(),
  `deleted` = b'0';

-- 4. 平台和供应商默认推广位
UPDATE `cps_platform`
SET `default_adzone_id` = 'YOUR_TAOBAO_PID', `update_time` = NOW()
WHERE `platform_code` = 'taobao' AND `tenant_id` = @tenant_id AND `deleted` = b'0';

UPDATE `cps_platform`
SET `default_adzone_id` = 'YOUR_PDD_PID', `update_time` = NOW()
WHERE `platform_code` = 'pdd' AND `tenant_id` = @tenant_id AND `deleted` = b'0';

UPDATE `cps_api_vendor`
SET `default_adzone_id` = 'YOUR_TAOBAO_PID', `update_time` = NOW()
WHERE `vendor_code` IN ('dataoke', 'haodanku')
  AND `platform_code` = 'taobao'
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0';

UPDATE `cps_api_vendor`
SET `default_adzone_id` = 'YOUR_PDD_PID', `update_time` = NOW()
WHERE `vendor_code` IN ('dataoke', 'haodanku')
  AND `platform_code` = 'pdd'
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0';

-- 5. 返利配置
INSERT INTO `cps_rebate_config`
(`id`, `member_id`, `member_level_id`, `platform_code`, `rebate_rate`,
 `max_rebate_amount`, `min_rebate_amount`, `status`, `priority`,
 `creator`, `updater`, `tenant_id`)
VALUES
(990721030001, NULL, NULL, NULL, 50.00, 0.00, 0.00, 1, 0, 'local', 'local', @tenant_id),
(990721030002, NULL, NULL, 'taobao', 60.00, 0.00, 0.00, 1, 10, 'local', 'local', @tenant_id),
(990721030003, NULL, NULL, 'pdd', 55.00, 0.00, 0.00, 1, 10, 'local', 'local', @tenant_id),
(990721030004, 1001, NULL, 'taobao', 80.00, 0.00, 0.00, 1, 100, 'local', 'local', @tenant_id)
ON DUPLICATE KEY UPDATE
  `member_id` = VALUES(`member_id`),
  `member_level_id` = VALUES(`member_level_id`),
  `platform_code` = VALUES(`platform_code`),
  `rebate_rate` = VALUES(`rebate_rate`),
  `max_rebate_amount` = VALUES(`max_rebate_amount`),
  `min_rebate_amount` = VALUES(`min_rebate_amount`),
  `status` = VALUES(`status`),
  `priority` = VALUES(`priority`),
  `update_time` = NOW(),
  `deleted` = b'0';
```

### 7.2 导入顺序

本地空库通常按这个顺序执行：

```text
1. backend/sql/module/cps-all-in-one.sql
2. backend/sql/module/test_data.sql
3. 启动后端
4. 后台测试连接和搜索
```

如果是已有库，只执行 `test_data.sql` 中本次供应商配置块即可。

## 8. 商品查询验证

### 8.1 后台验证

进入：

```text
CPS -> 商品广场
```

按平台验证：

| 验证目标 | 平台 | 供应商 | 关键词 |
| --- | --- | --- | --- |
| 大淘客淘宝 | `taobao` | `dataoke` | `纸巾` |
| 好单库淘宝 | `taobao` | `haodanku` | `纸巾` |
| 大淘客京东 | `jd` | `dataoke` | `手机` |
| 好单库京东 | `jd` | `haodanku` | `手机` |
| 大淘客拼多多 | `pdd` | `dataoke` | `水果` |
| 好单库拼多多 | `pdd` | `haodanku` | `水果` |

页面能返回商品卡片，说明商品搜索链路可用。再点击转链，如果能生成短链、长链或移动端链接，说明转链链路可用。

### 8.2 管理端 HTTP 验证

后台 API 前缀是 `/admin-api`。登录后携带系统鉴权 Cookie 或 Bearer Token。

商品搜索：

```http
GET /admin-api/cps/goods-square/search?platformCode=taobao&vendorCode=dataoke&keyword=纸巾&pageNo=1&pageSize=20
```

切好单库：

```http
GET /admin-api/cps/goods-square/search?platformCode=taobao&vendorCode=haodanku&keyword=纸巾&pageNo=1&pageSize=20
```

返利查询并生成推广内容：

```http
POST /admin-api/cps/goods/rebate-query
Content-Type: application/json

{
  "platformCode": "taobao",
  "vendorCode": "dataoke",
  "goodsId": "商品ID或商品链接",
  "memberId": 1
}
```

商品广场转链：

```http
POST /admin-api/cps/goods-square/link
Content-Type: application/json

{
  "platformCode": "taobao",
  "vendorCode": "dataoke",
  "goodsId": "商品ID",
  "title": "商品标题",
  "adzoneId": "可选推广位"
}
```

## 9. 活动拉取验证

### 9.1 后台验证

进入：

```text
CPS -> 活动广场
```

同步供应商建议：

| 供应商 | vendorCode | 说明 |
| --- | --- | --- |
| 全部 | `all` | 依次同步大淘客、好单库、聚推客 |
| 大淘客 | `dataoke` | 同步淘宝活动 |
| 好单库 | `haodanku` | 同步好单库活动中心，仅入库已接入官方活动转链的平台 |
| 聚推客 | `jutuike` | 同步聚推客联盟活动 |

后台页面当前默认同步表单是 `vendorCode=all`、`platformCode=taobao`。同步后列表能看到活动卡片，说明活动拉取可用。

### 9.2 管理端 HTTP 验证

活动中心查询：

```http
GET /admin-api/cps/rebate-activity/center?platformCode=taobao&pageNo=1&pageSize=20
```

活动同步：

```http
POST /admin-api/cps/rebate-activity/sync
Content-Type: application/json

{
  "vendorCode": "all",
  "platformCode": "taobao",
  "pageSize": 20,
  "maxPages": 2
}
```

只同步聚推客：

```http
POST /admin-api/cps/rebate-activity/sync
Content-Type: application/json

{
  "vendorCode": "jutuike",
  "pageSize": 20,
  "maxPages": 2
}
```

聚推客活动转链时，`goodsId` 应传聚推客活动 ID，`channelId` 应传聚推客要求的 `sid`。

## 10. 订单查询验证

订单查询通常比商品搜索权限更严格。先确认供应商后台已开通订单接口，再验证 AgenticCPS。

订单同步入口通常在：

```text
CPS -> 订单管理
```

按平台同步最近几小时订单。如果接口返回空列表，不一定是配置错误，常见原因是：

- 供应商后台没有订单权限。
- 查询时间范围内没有订单。
- 淘宝/JD/PDD 订单需要专属推广位、渠道 ID 或会员运营 ID 才能可靠归因。
- 大淘客 JD 没有配置 `auth_token`。
- 好单库 JD 没有配置 `auth_token` 或 `extra_config.jd_user_id`。
- 拼多多没有配置 PID，转链时无法带上推广位。

## 11. 常见错误排查

| 现象 | 优先检查 |
| --- | --- |
| 平台测试连接提示未找到适配器 | `platformCode` 是否是代码已注册的平台，例如 `taobao`、`jd`、`pdd`、`union` |
| 未找到供应商客户端 | `vendorCode + platformCode` 是否匹配，例如聚推客必须是 `jutuike + union` |
| 大淘客返回签名错误 | `app_key`、`app_secret` 是否正确，`api_base_url` 是否是 `https://openapi.dataoke.com/api` |
| 好单库返回鉴权失败 | `app_key` 是否填了好单库 `apikey`，不是 `appkey/appsecret` |
| 京东转链失败 | 大淘客看 `auth_token`，好单库看 `auth_token` 或 `extra_config.jd_user_id` |
| 拼多多转链失败 | `default_adzone_id` 是否填 PID，供应商侧是否开通多多进宝权限 |
| 聚推客活动转链失败 | 是否传了正确 `act_id` 和 `sid`；在 AgenticCPS 中 `goodsId=act_id`、`channelId=sid` |
| 活动同步为空 | 先把 `maxPages` 调小做测试，再检查供应商活动权限、关键词筛选和接口返回 |
| 商品搜索有结果但返利查询失败 | 搜索只证明商品接口可用，返利查询还依赖转链、推广位和供应商权限 |
| 转链结果没有推广位 | 检查 `cps_platform.default_adzone_id` 是否配置；如果依赖会员归因，检查 `cps_adzone` 是否有启用的会员专属推广位 |
| 淘宝会员专属推广位保存失败 | `adzoneId` 必须是 `mm_数字_数字_数字` 格式，`externalSpecialId` 必须是纯数字 specialId |
| 淘宝订单同步后无法归属会员 | 不要只在请求里直接传 PID；会员返利链路要配置 `adzoneType=member`、`relationId=会员ID`、`externalSpecialId=specialId` |
| 订单有佣金但没有返利 | 检查是否至少有一条启用的全局默认或平台默认 `cps_rebate_config` |
| 返利金额和预期不一致 | `rebateRate` 是按平台佣金计算，不是按商品售价计算；同层级多条规则时看 `priority` 和 `id` |

## 12. 上线前检查清单

- `cps_api_vendor` 中真实密钥只存在数据库或安全配置，不进入 Git。
- `cps_platform.active_vendor_code` 已设置为希望默认使用的供应商。
- `cps_platform.default_adzone_id` 已为需要转链的平台配置默认推广位。
- `cps_api_vendor.default_adzone_id` 已为对应供应商补齐，方便供应商级兜底和排查。
- 需要会员返利归因的平台已配置启用状态的会员专属推广位或渠道专属推广位。
- 至少有一条启用的全局默认返利配置，重点平台也有平台默认返利配置。
- 淘宝、京东、拼多多分别测试过商品搜索。
- 每个平台至少测试过一次转链。
- 订单接口在供应商后台确认已开通。
- 需要用户返利归因的链路已经配置 PID、relationId、specialId 或供应商要求的渠道参数。
- 生产环境不要直接使用本地 `test_data.sql` 的测试记录 ID。
- 生产环境密钥轮换后，同步更新 `cps_api_vendor` 并重新测试连接。
