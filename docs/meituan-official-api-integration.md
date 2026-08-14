# 美团联盟官方 API 接入

当前实现使用美团 API Gateway 官方 `sign-java` 示例中的签名规则，并接入联盟 CPS 的三个接口：

- `POST /cps_open/common/api/v1/query_coupon`：商品搜索/选品
- `POST /cps_open/common/api/v1/get_referral_link`：推广链接
- `POST /cps_open/common/api/v1/query_order`：订单同步

配置 `cps_api_vendor` 时使用 `vendorCode=official`、`platformCode=meituan`，填写美团分配的 `appKey` 和 `appSecret`。`apiBaseUrl` 可填写完整基础地址；不填写时默认使用 `https://media.meituan.com/cps_open/common/api/v1`。可选扩展配置放在 `extraConfig`：`platform`、`bizLine`、`cityId`、`linkType`、`businessLine`（逗号分隔）、`tradeType`、`timeoutMs`。

每次 JSON POST 会按官方规则计算 UTF-8 body 的 Base64-MD5，并发送 `S-Ca-App`、毫秒级 `S-Ca-Timestamp`、`S-Ca-Signature-Headers` 和 HMAC-SHA256 Base64 的 `S-Ca-Signature`。密钥只从运行时供应商配置读取，不写入代码或配置示例。

官方接口说明：

- [API 接入指南](https://media.meituan.com/pc/index.html#/help?path=API%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97)
- [商品查询](https://page.meituan.net/html/1701831807616_1db0df/index.html)
- [获取推广链接](https://page.meituan.net/html/1701831845934_13e608/index.html)
- [订单查询](https://page.meituan.net/html/1706169509872_eb0353/index.html)
