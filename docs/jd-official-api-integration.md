# 京东联盟官方 API 集成

当前 CPS 的 `official/jd` 客户端使用项目内的京东官方 Java SDK，支持：

- `jd.union.open.goods.query`：关键词商品查询（需要京东联盟高级接口权限）；
- `jd.union.open.promotion.common.get`：网站/APP 通用推广链接；
- `jd.union.open.order.row.query`：订单行查询；
- 连接测试。

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

`extraConfig` 预留给京东扩展参数。不要把 appKey、secretKey 或 access token 写入源码、SQL 或版本库。

## 官方资料与权限

- [京东联盟新手引导/API 权限管理](https://news.jd.com/153_1.html)：账号开通、appkey/secretkey 获取、接口权限申请与正式路由示例。
- [京东联盟/JOS 接口总览](https://jos.jd.com/jdunion)：商品、转链、订单接口说明及 Java SDK 建议。
- [京东 API 调用指南](https://open.healthjd.com/docs/%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97/%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0-%E4%BA%AC%E4%B8%9CAPI%E8%B0%83%E7%94%A8%E6%8C%87%E5%8D%97/)：`routerjson`、签名和 `360buy_param_json` 调用约定。

`goods.query`、订单行查询及带渠道参数的转链可能需要单独申请；京东会按账号、接口和入/出参白名单控制权限。若接口返回权限错误，应先在京东联盟后台申请开通，而不是修改客户端签名逻辑。
