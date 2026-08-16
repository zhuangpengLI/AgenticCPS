# 京东联盟官方 API 集成

当前 CPS 的 `official/jd` 客户端使用项目内的京东官方 Java SDK，支持以下系统业务能力：

- 商品查询：`jd.union.open.goods.query`；
- 京粉精选商品：`jd.union.open.goods.jingfen.query`（商品广场/选品主题使用）；
- 猜你喜欢：`jd.union.open.goods.recommend.query`（推荐 SKU 会自动二次调用商品查询补齐商品卡字段）；
- 优惠券领取情况：`jd.union.open.coupon.query`；
- 网站/App 转链：`jd.union.open.promotion.common.get`；
- 社交媒体转链：`jd.union.open.promotion.bysubunionid.get`；
- 工具商转链：`jd.union.open.promotion.byunionid.get`；
- 普通订单行查询：`jd.union.open.order.row.query`；
- 礼金订单查询：`jd.union.open.order.bonus.query`（通过 `extraConfig.includeBonusOrders=true` 开启，独立标记为 `BONUS`）；
- 连接测试。

另外，后台提供京东远端管理入口，会调用并同步以下接口返回的推广位到本地 `cps_adzone`：

- `jd.union.open.position.create`、`jd.union.open.position.query`：创建/查询推广位；
- `jd.union.open.user.pid.get`：获取 PID；
- `jd.union.open.channel.relation.get`：生成渠道关系 ID。

官方 SDK 客户端还已保留以下营销/效果接口的统一调用方法，供后续业务模块直接复用：

- 礼金创建/停止：`jd.union.open.coupon.gift.get`、`jd.union.open.coupon.gift.stop`；
- 礼金效果：`jd.union.open.statistics.giftcoupon.query`；
- 京享红包效果：`jd.union.open.statistics.redpacket.query`；
- 推广效果：`jd.union.open.statistics.promotion.query`。

后台路由为 `/admin-api/cps/adzone/jd/remote-create`、`/jd/remote-sync`、`/jd/pid`、`/jd/channel-relation`，权限沿用 `cps:adzone:create/query`。

SDK 文件位于 `backend/qiji-module-cps/qiji-module-cps-biz/lib/jd-api-sdk-java-20260814.jar`，Maven 坐标为
`com.jd.open.api:open-api-sdk:2.0`。服务打包时通过 `includeSystemScope` 将该 JAR 放入 Spring Boot fat jar。

当前文件 SHA-256：`BC49D82415094FDB41DE8DC3B19361BA51DBEC1CAEB6D9CB31404A544C1EDAC5`。

## 后台供应商配置

在 CPS API 供应商中新增/启用 `vendorCode=official`、`platformCode=jd`，字段映射如下：

| 配置字段 | 京东含义 |
| --- | --- |
| `appKey` | 京东联盟网站/APP/社交媒体审核通过后获得的 appkey |
| `appSecret` | 对应 secretkey |
| `authToken` | OAuth/access token；未使用授权 token 时可留空 |
| `apiBaseUrl` | SDK 推荐 `https://api.jd.com/routerjson`；留空时客户端使用该默认值 |
| `defaultAdzoneId` | 默认推广位/PID；转链时可由请求中的 `adzoneId` 覆盖 |

`extraConfig` 可配置：

| 配置项 | 作用 |
| --- | --- |
| `promotionMode` | `common`（默认）、`social`、`tool`，选择三种转链接口 |
| `unionId` | 远端推广位/PID 管理的默认联盟 ID |
| `jdEliteId` | 京粉精选接口的 eliteId，默认 `1` |
| `jdRecommendSceneId` | 猜你喜欢场景 ID，默认 `1` |
| `includeBonusOrders` | `true` 时合并礼金订单查询 |
| `jdBonusOptType` | 礼金订单查询类型，默认 `1` |

不要把 appKey、secretKey 或 access token 写入源码、SQL 或版本库。

商品广场、场景推荐和优惠券查询已统一走现有 CPS 服务入口，不需要新增前端调用方式。京东 PID 支持常见的 `unionId_siteId_positionId` 复合格式，转链时会拆分为 SDK 所需的 `siteId`、`positionId` 和 `pid`，避免把复合 PID 错误当成数值 0。

上述营销/效果接口当前只完成 SDK 客户端层接入，没有强行写入本地礼金、红包或效果报表表结构；后续建立对应业务实体、状态机和对账入口后，可直接复用 `JdOfficialExtendedClient`，避免再次改动签名和请求封装。

## 官方资料与权限

- [京东联盟新手引导/API 权限管理](https://news.jd.com/153_1.html)：账号开通、appkey/secretkey 获取、接口权限申请与正式路由示例。
- [京东联盟/JOS 接口总览](https://jos.jd.com/jdunion)：商品、转链、订单接口说明及 Java SDK 建议。
- [京东 API 调用指南](https://open.healthjd.com/docs/%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97/%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0-%E4%BA%AC%E4%B8%9CAPI%E8%B0%83%E7%94%A8%E6%8C%87%E5%8D%97/)：`routerjson`、签名和 `360buy_param_json` 调用约定。

`goods.query`、订单行查询及带渠道参数的转链可能需要单独申请；京东会按账号、接口和入/出参白名单控制权限。若接口返回权限错误，应先在京东联盟后台申请开通，而不是修改客户端签名逻辑。
