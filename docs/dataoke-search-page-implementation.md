# 大淘客搜索页面开发指南

> 来源：大淘客《大淘客助您快速搭建搜索页面》：https://www.dataoke.com/kfpt/open-gz.html?id=70
> 整理日期：2026-07-07。官方文档更新时间为 2020-11-20，后续实现前若大淘客搜索接口、签名、字段或版本号变更，以最新官方文档为准。

## 1. 官方搜索页方案总结

大淘客把购物类 App 的搜索页拆成“输入辅助 + 搜索召回 + 热搜导购”三层能力：

```text
用户进入搜索页
  -> 展示热搜记录，帮助用户快速选择搜索方向
  -> 用户输入关键词
  -> 调用联想词接口，减少输入成本并扩展相关属性词
  -> 用户提交搜索
  -> 根据业务目标选择超级搜索 / 大淘客搜索 / 联盟搜索
  -> 返回商品列表
  -> 用户点击商品
  -> 再走转链、领券、下单和订单归因链路
```

官方文档列出的搜索页接口分工：

| 能力 | 官方接口类型 | 数据来源 | 适用场景 | AgenticCPS 实现原则 |
|---|---|---|---|---|
| 超级搜索 | 超级搜索 | 大淘客 + 淘宝联盟 | 追求覆盖更广的搜索结果 | 可作为搜索召回扩展，但注意联盟侧可能更慢，需设置超时、降级和结果来源标识。 |
| 大淘客搜索 | 大淘客搜索 | 大淘客商品库 | 追求字段完整、返回精确、性能稳定 | 默认优先用于淘宝商品广场搜索和运营选品搜索。 |
| 联盟搜索 | 联盟搜索 | 淘宝联盟 | 大淘客库召回不足时补量 | 适合作为补充召回，不应覆盖大淘客搜索的高质量字段。 |
| 联想词 | 搜索联想词 | 关键词相关属性词 | 搜索框输入提示 | 前端应做防抖，后端返回空列表时不要报错。 |
| 热搜记录 | 热搜记录 | 大淘客 CMS 采集统计热词 | 搜索页默认内容、热搜榜 | 可作为搜索页首屏和空关键词兜底。 |

官方一图流还强调：

- 搜索页顶部提供“输入商品名或粘贴宝贝标题”的搜索框，并支持取消/清空。
- 搜索入口可以按平台或 Tab 切换，例如淘宝、京东、拼多多；每个平台背后可以路由到不同供应商或平台搜索。
- 联想词用于用户输入时的即时提示，目标是减少用户输入、增加候选选择。
- 热搜记录用于搜索页首屏，目标是帮助用户快速决策并提升选品体验。
- 三类搜索接口返回不同数据：大淘客搜索字段更全、结果更精确；联盟搜索返回联盟商品；超级搜索覆盖大淘客和淘宝联盟，但联盟侧可能触发多次请求，速度可能更慢。

## 2. AgenticCPS 当前落地映射

当前管理后台商品广场已经具备大淘客搜索页的主要拼装点：

| 页面能力 | 管理端 API | 服务/客户端 | 大淘客接口或实现 |
|---|---|---|---|
| 搜索页元数据 | `GET /admin-api/cps/goods-square/meta` | `CpsGoodsSquareServiceImpl.getMeta()` | 淘宝类目来自 `DtkTaobaoVendorClient.getSelectionMeta()`，兜底合并默认活动、热词、类目、排序、筛选项。 |
| 热搜记录 | `GET /admin-api/cps/goods-square/hot-keywords` | `CpsGoodsSquareServiceImpl.getHotKeywords()` -> `CpsSearchAssistVendorClient` | `DtkTaobaoVendorClient` 调 `/category/get-top100`，版本 `v1.0.1`。 |
| 搜索联想词 | `GET /admin-api/cps/goods-square/suggestions` | `CpsGoodsSquareServiceImpl.suggestKeywords()` -> `CpsSearchAssistVendorClient` | `DtkTaobaoVendorClient` 调 `/goods/search-suggestion`，版本 `v1.0.2`。 |
| 关键词搜索 | `GET /admin-api/cps/goods-square/search` | `CpsGoodsSquareServiceImpl.searchGoods()` -> `CpsGoodsService` -> `CpsPlatformClientFactory` | 淘宝 dataoke 默认走 `/goods/get-dtk-search-goods`，版本 `v2.1.2`。 |
| 图片搜索 | `POST /admin-api/cps/goods-square/search-by-image` | `CpsGoodsSquareServiceImpl.searchByImage()` | `DtkTaobaoVendorClient` 调 `https://openapiv2.dataoke.com/open-api/goods/search-by-image`，版本 `v1.0.0`。 |
| 选品主题商品 | `GET /admin-api/cps/goods-square/selection-theme-goods` | `CpsGoodsSquareServiceImpl.getSelectionThemeGoods()` | 读取已发布、允许商品广场展示且在有效期内的主题商品快照，不把主题编码当作第三方搜索频道。 |
| 商品转链 | `POST /admin-api/cps/goods-square/link` | `CpsGoodsSquareServiceImpl.generateLink()` -> `CpsGoodsService.generatePromotionLink()` | 淘宝 dataoke 走 `/tb-service/get-privilege-link`，归因规则见 `docs/dataoke-high-efficiency-link-attribution.md`。 |

相关代码入口：

- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/goods/CpsGoodsSquareController.java`
- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/goods/CpsGoodsSquareServiceImpl.java`
- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/dataoke/DtkTaobaoVendorClient.java`
- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/selection/CpsSearchAssistVendorClient.java`
- `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/client/selection/CpsTaobaoSelectionVendorClient.java`

## 3. 推荐实现方式

### 3.1 前端搜索页交互

搜索页应按以下状态组织：

1. 未输入关键词：展示热搜榜、运营活动入口、默认类目和推荐榜单。
2. 正在输入：对 `/goods-square/suggestions` 做 250ms-500ms 防抖请求，展示联想词列表；空关键词不请求。
3. 提交搜索：调用 `/goods-square/search`，传入平台、供应商、关键词、分页、排序和筛选条件。
4. 结果为空：保留关键词和筛选条件，展示热搜兜底和可切换平台提示，不要直接跳转。
5. 点击转链：只有用户明确选择商品后才调用 `/goods-square/link`；管理端可传运营推广位，用户端/MCP 必须使用可信登录上下文或 ToolContext。

### 3.2 后端搜索编排

默认策略：

```text
淘宝 + dataoke
  -> 优先大淘客搜索 `/goods/get-dtk-search-goods`
  -> 返回字段统一映射为 CpsGoodsItem
  -> 保留 vendorCode/source/goodsSign/goodsId
  -> 前端只消费统一 VO，不直接绑定大淘客原始字段
```

扩展策略：

- 如需接入官方“超级搜索”，应以搜索模式或召回策略参数扩展 `CpsGoodsSearchRequest`，不要把超级搜索逻辑硬编码进默认大淘客搜索。
- 如需增加“联盟搜索”补量，应在服务层合并结果并标记来源，避免覆盖大淘客搜索的更完整字段。
- 搜索、联想词、热搜接口都应允许失败降级：搜索失败返回空列表和错误提示；联想词失败返回空列表；热搜失败返回默认热词。
- 搜索结果只用于导购展示、选品和转链前置展示，不得直接驱动订单归因、返利入账、冻结/扣减或 Token 兑换。

### 3.3 字段归一化

大淘客搜索结果进入系统时必须映射到统一模型：

- 商品身份：`goodsId`、`goodsSign`、`platformCode`、`vendorCode`。
- 展示字段：标题、主图、店铺、品牌、类目、活动标签。
- 价格字段：原价、券后价、券金额、券门槛、券有效期。
- 佣金字段：佣金率、预估佣金金额。
- 销量/排序字段：月销量、热度、榜单来源、排序类型。

不要让前端、MCP Tool 或选品库直接依赖大淘客原始 JSON 字段。字段新增时应先扩展 DTO/VO，再在供应商客户端中做兼容解析。

## 4. 后续开发注意事项

- 开发大淘客搜索页、商品广场、联想词、热搜或搜索召回优化前，先阅读本文件和 `docs/大淘客与好单库配置及接口测试指南.md`。
- 开发搜索结果转链、订单同步、App/MCP 链接生成和推广位归因时，还必须阅读 `docs/dataoke-high-efficiency-link-attribution.md`。
- 大淘客搜索页能力属于 CPS 导购和运营选品链路；不得在 CPS 内新增 Token 钱包、模型计费或 IoT 设备逻辑。
- 热搜、联想词和搜索结果可以做缓存，但缓存键必须包含 `platformCode`、`vendorCode`、关键词、搜索模式、排序和主要筛选项，避免跨平台污染。
- 联想词和热搜可以作为 MCP `cps_search_goods` 的辅助上下文，但 MCP 最终返回仍应是结构化商品结果，不应返回大淘客原始响应。
- 前端新增搜索页体验时，必须覆盖首屏热搜、输入联想、提交搜索、空状态、加载中、接口失败、平台切换和转链成功/失败状态。
- 后端新增或改动搜索召回策略时，优先补 `DtkTaobaoVendorClient`、`CpsGoodsSquareServiceImpl`、`CpsGoodsServiceImpl` 的单元测试；涉及管理端页面时补 Playwright 或 UI contract 测试。

## 5. 验收清单

- `GET /admin-api/cps/goods-square/hot-keywords` 在大淘客不可用时仍有默认热词兜底。
- `GET /admin-api/cps/goods-square/suggestions` 对空关键词返回空列表，对供应商失败不抛 500。
- `GET /admin-api/cps/goods-square/search` 能稳定返回统一商品字段，并保留 `vendorCode`、`goodsId`、`goodsSign`。
- 搜索页切换平台或供应商时不会复用上一次的错误热词/联想词/商品缓存。
- 转链动作只在用户明确选择商品后触发，并遵守大淘客归因文档中的 PID、`relationId/channelId`、`specialId`、`externalId` 边界。
