# 好单库 OpenAPI 接口文档

> 整理自 [好单库开放平台](https://www.haodanku.com/openapi/api_detail)
>
> 更新时间：2025-04-15

## 概述

好单库（haodanku.com）是一个淘客选品导购平台，提供 OpenAPI 接口服务。API 共分为 **6 大类别**，合计 **68+ 个接口**。

### 基础信息

| 项目 | 说明 |
|------|------|
| v2 基础URL | `http://v2.api.haodanku.com` |
| v3 基础URL | `http://v3.api.haodanku.com` (转链/订单/增值类) |
| v3 HTTPS | `https://v3.api.haodanku.com` (REST接口) |
| 认证方式 | `apikey` 参数（应用中心获取） |
| 返回格式 | JSON |
| 状态码 | v2接口: `code=1` 成功；v3 REST接口: `code=200` 成功 |

### 认证说明

- **v2 接口**：仅需 `apikey` 参数即可调用
- **v3 REST 接口**（订单拉取/粘贴板识别/短链接等）：需要 `app_id` + `sign` 签名 + `date` 时间戳

---

## 目录

- [一、数据搜索类](#一数据搜索类)
  - [1.1 超级搜索API (supersearch)](#11-超级搜索api-supersearch)
  - [1.2 商品筛选API (column)](#12-商品筛选api-column)
  - [1.3 超级分类API](#13-超级分类api)
  - [1.4 猜你喜欢API](#14-猜你喜欢api)
  - [1.5 热搜关键词记录API](#15-热搜关键词记录api)
  - [1.6 天猫国际商品](#16-天猫国际商品)
  - [1.7 闲鱼商品详情](#17-闲鱼商品详情)
- [二、工具类](#二工具类)
  - [2.1 淘联商品转链 (ratesurl)](#21-淘联商品转链-ratesurl)
  - [2.2 店铺转链API](#22-店铺转链api)
  - [2.3 淘联会场转链](#23-淘联会场转链)
  - [2.4 闲鱼推广转链](#24-闲鱼推广转链)
  - [2.5 其他工具类API](#25-其他工具类api)
- [三、增值类](#三增值类)
  - [3.1 口令解析](#31-口令解析)
  - [3.2 粘贴板识别](#32-粘贴板识别)
  - [3.3 订单拉取](#33-订单拉取)
  - [3.4 好单库订单查询(订单号查询)](#34-好单库订单查询订单号查询)
  - [3.5 短链接生成](#35-短链接生成)
  - [3.6 其他增值类API](#36-其他增值类api)
- [四、入库类](#四入库类)
  - [4.1 商品列表页API (itemlist)](#41-商品列表页api-itemlist)
  - [4.2 商品更新API](#42-商品更新api)
  - [4.3 定时拉取API](#43-定时拉取api)
  - [4.4 失效商品列表API](#44-失效商品列表api)
  - [4.5 其他入库类API](#45-其他入库类api)
- [五、CPS活动类](#五cps活动类)
- [六、特色板块类](#六特色板块类)
- [附录：CPS项目接口映射](#附录cps项目接口映射)

---

## 一、数据搜索类

### 1.1 超级搜索API (supersearch)

> **CPS项目核心接口** - 关键词商品搜索，同时包含好单库精选高佣商品和联盟全网商品。

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/supersearch` |
| 请求方式 | GET |
| 说明 | 同时包含好单库精选高佣商品和联盟全网商品（好单库精选高佣靠前） |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| apikey | string | 你的apikey | 是 | 应用中心获取的 Apikey 值 |
| keyword | string | `%25e5%25a5%25b3%25e8%25a3%2585` | 是 | 搜索关键词，支持商品标题和宝贝ID搜索。**特殊符号需进行两次 urlencode 编码** |
| back | integer | 10 | 是 | 每页返回条数，可选：`1, 2, 10, 20, 50, 100` |
| min_id | integer | 1 | 是 | 分页参数，默认开始值为 1。来源于上次响应的 `min_id` 值 |
| tb_p | integer | 1 | 是 | 淘宝分页参数，默认开始值为 1。来源于上次响应的 `tb_p` 值 |
| sort | integer | 0 | 否 | 排序：0-综合，1-最新，2-销量(高→低)，3-销量(低→高)，4-价格(低→高)，5-价格(高→低)，6-佣金比例(高→低) |
| is_tmall | integer | 0 | 否 | 是否只取天猫商品：0-否，1-是（默认0） |
| is_coupon | integer | 0 | 否 | 是否只取有券商品：0-否，1-是（默认0） |
| limitrate | integer | 0 | 否 | 佣金比例过滤，0~100 |
| startprice | integer | 0 | 否 | 最低原价（默认0），如传10则只取≥10元商品 |

#### 返回参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码（1成功，0失败） |
| min_id | Integer | 下一页分页参数值 |
| tb_p | Integer | 下一页联盟超级搜分页参数值 |
| msg | string | 返回信息说明 |
| **data** | **Array** | **商品数据数组** |
| ↳ itemid | Integer | 宝贝ID |
| ↳ itemtitle | string | 宝贝标题 |
| ↳ itemshorttitle | string | 宝贝短标题 |
| ↳ itemdesc | string | 宝贝推荐语 |
| ↳ itemprice | float | 在售价 |
| ↳ itemsale | integer | 宝贝月销量 |
| ↳ itempic | string | 宝贝主图URL（建议加后缀 `_310x310.jpg` 优化加载） |
| ↳ itemendprice | float | 宝贝券后价 |
| ↳ shoptype | string | 店铺类型：B-天猫店，C-淘宝店 |
| ↳ couponurl | string | 优惠券链接 |
| ↳ activityid | string | 优惠券活动ID |
| ↳ couponmoney | float | 优惠券金额 |
| ↳ tkrates | float | 佣金比例(%) |
| ↳ couponstarttime | integer | 优惠券开始时间(时间戳) |
| ↳ couponendtime | integer | 优惠券结束时间(时间戳) |
| ↳ videoid | integer | 视频ID（>0 有视频） |
| ↳ item_from | string | 数据来源：`haodanku`(好单库精选高佣) / `tb`(联盟超级搜API) |

#### 请求示例

```
http://v2.api.haodanku.com/supersearch?apikey=你的apikey&keyword=%25e5%25a5%25b3%25e8%25a3%2585&back=10&min_id=1&tb_p=1&sort=0&is_tmall=0&is_coupon=0&limitrate=0
```

> **分页说明**：使用 `min_id` + `tb_p` 滚动分页机制，比传统页码更可靠，数据更新时保证不重复。

---

### 1.2 商品筛选API (column)

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/column` |
| 请求方式 | GET |
| 说明 | 根据灵活的筛选条件筛选好单库商品 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| apikey | string | 你的apikey | 是 | Apikey值 |
| type | integer | 1 | 是 | 筛选类型：1-今日上新，2-9.9包邮，3-30元封顶，4-聚划算，5-淘抢购，6-0点过夜单，7-预告单，8-品牌单，9-天猫商品，10-视频单，11-天猫超市单，12-偏远地区包邮单，13-淘宝商品，14-天猫国际，15-阿里健康大药房 |
| back | integer | 500 | 是 | 每页条数，可选：`1,2,10,20,50,100,120,200,500,1000` |
| min_id | integer | 1 | 是 | 分页参数，默认1 |
| sort | integer | 0 | 否 | 排序：0-综合(最新)，1-券后价(低→高)，2-券后价(高→低)，3-券面额(高→低)，4-月销量(高→低)，5-佣金比例(高→低)，6-券面额(低→高)，7-月销量(低→高)，8-佣金比例(低→高)，9-全天销量(高→低)，10-全天销量(低→高)，11-近2小时销量(高→低)，12-近2小时销量(低→高)，13-优惠券领取量(高→低)，14-好单库指数(高→低) |
| cid | integer | 0 | 否 | 类目：0-全部，1-女装，2-男装，3-内衣，4-美妆，5-配饰，6-鞋品，7-箱包，8-儿童，9-母婴，10-居家，11-美食，12-数码，13-家电，14-其他，15-车品，16-文体，17-宠物 |
| price_min | integer | 100 | 否 | 券后价最低价 |
| price_max | integer | 200 | 否 | 券后价最高价 |
| sale_min | integer | 1000 | 否 | 月销量最低值 |
| sale_max | integer | 5000 | 否 | 月销量最高值 |
| coupon_min | integer | 10 | 否 | 优惠券最低金额 |
| coupon_max | integer | 20 | 否 | 优惠券最高金额 |
| item_type | string | 0 | 否 | 是否只获取营销返利商品：0-否，1-是 |

#### 返回参数（商品通用字段）

| 参数 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码（1成功，0失败） |
| min_id | Integer | 下一页参数值 |
| msg | string | 返回信息说明 |
| **data** | **Array** | **商品数据数组** |
| ↳ product_id | integer | 自增ID |
| ↳ itemid | integer | 宝贝ID |
| ↳ seller_id | integer | 放单人ID |
| ↳ itemtitle | string | 宝贝标题 |
| ↳ itemshorttitle | string | 宝贝短标题 |
| ↳ itemdesc | string | 宝贝推荐语 |
| ↳ itemprice | float | 在售价 |
| ↳ itemsale | integer | 月销量 |
| ↳ itemsale2 | integer | 近2小时销量 |
| ↳ todaysale | integer | 当天销量 |
| ↳ itempic | string | 主图URL |
| ↳ itempic_copy | string | 推广长图 |
| ↳ fqcat | integer | 商品类目 |
| ↳ itemendprice | float | 券后价 |
| ↳ shoptype | string | B-天猫，C-淘宝 |
| ↳ tktype | string | 佣金计划类型 |
| ↳ tkrates | float | 佣金比例(%) |
| ↳ cuntao | integer | 是否村淘(1是) |
| ↳ tkmoney | float | 预计可得佣金 |
| ↳ couponurl | string | 优惠券链接 |
| ↳ couponmoney | float | 优惠券金额 |
| ↳ couponsurplus | integer | 优惠券剩余量 |
| ↳ couponreceive | integer | 优惠券领取量 |
| ↳ couponreceive2 | integer | 2小时内券领取量 |
| ↳ todaycouponreceive | integer | 今日券领取量 |
| ↳ couponnum | integer | 优惠券总量 |
| ↳ couponexplain | string | 优惠券使用条件 |
| ↳ couponstarttime | integer | 优惠券开始时间(时间戳) |
| ↳ couponendtime | integer | 优惠券结束时间(时间戳) |
| ↳ start_time | integer | 活动开始时间(时间戳) |
| ↳ end_time | integer | 活动结束时间(时间戳) |
| ↳ starttime | integer | 发布时间(时间戳) |
| ↳ is_brand | integer | 是否品牌(1是) |
| ↳ is_live | integer | 是否直播(1是) |
| ↳ guide_article | string | 推广导购文案 |
| ↳ videoid | integer | 视频ID |
| ↳ activity_type | string | 活动类型 |
| ↳ general_index | integer | 好单库指数 |
| ↳ planlink | string | 营销计划链接 |
| ↳ seller_name | string | 放单人名号 |
| ↳ userid | integer | 店主userid |
| ↳ sellernick | string | 店铺掌柜名 |
| ↳ shopname | string | 店铺名 |
| ↳ discount | float | 折扣力度 |
| ↳ couponinfo | string | 折扣信息 |
| ↳ min_buy | int | 最低拍下件数 |

---

### 1.3 超级分类API

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/super_classify` |
| 请求方式 | GET |
| 说明 | 返回好单库所有一级、二级分类及分类图标 |

#### 请求参数

| 参数 | 类型 | 必须 | 说明 |
|------|------|:----:|------|
| apikey | string | 是 | Apikey值 |

#### 返回参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码 |
| msg | string | 返回信息 |
| general_classify | Array | 分类数据，含 cid、main_name、son_name、imgurl 等 |

---

### 1.4 猜你喜欢API

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/get_similar_info` |
| 请求方式 | GET |
| 说明 | 根据宝贝ID查出二级类目及相同类目产品 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| apikey | string | 你的apikey | 是 | Apikey值 |
| itemid | string | 564856111764 | 是 | 宝贝ID |
| back | int | 10 | 否 | 返回条数（默认10，最大50） |

#### 返回参数

与[商品筛选API](#12-商品筛选api-column)商品通用字段相同。

---

### 1.5 热搜关键词记录API

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/hot_key` |
| 请求方式 | GET |
| 说明 | 返回好单库统计的搜索热词，用于搜索推荐 |

#### 请求参数

| 参数 | 类型 | 必须 | 说明 |
|------|------|:----:|------|
| apikey | string | 是 | Apikey值 |

---

### 1.6 天猫国际商品

获取天猫国际商品列表数据。

### 1.7 闲鱼商品详情 (NEW)

获取闲鱼商品详情信息。

---

## 二、工具类

### 2.1 淘联商品转链 (ratesurl)

> **CPS项目核心接口** - 单品推广转链，生成淘口令和推广链接。

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v3.api.haodanku.com/ratesurl` |
| 请求方式 | **POST** |
| 版本 | v2.0.3 |
| 使用人数 | 109,689 |
| 说明 | 单个商品转链，生成推广链接和淘口令 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| apikey | string | 你的apikey | 是 | Apikey值 |
| itemid | string | xxxx-xxxx | 是 | 商品ID |
| pid | string | mm_0000_0000_000 | 是 | 推广位ID（需是授权淘宝号下的推广位） |
| tb_name | string | 点滴优惠 | 是 | 已授权的淘宝账号昵称 |
| material_url | string | https://traveldetail... | 否 | 物料链接（目前仅支持飞猪旅游） |
| relation_id | string | 566455 | 否 | 渠道ID |
| special_id | string | 123456 | 否 | 会员运营ID |
| external_id | string | 123456 | 否 | 淘宝客外部用户标记 |
| activityid | string | 7d6e6619ff75... | 否 | 优惠券ID |
| get_taoword | integer | 0 | 否 | 是否返回淘口令：0-否，1-是（默认0） |
| title | string | 商品标题 | 否 | 商品标题（get_taoword=1时必填） |
| logo | string | https://img.alicdn.com/... | 否 | 商品主图链接 |
| is_special | integer | 1 | 否 | 特殊玩法：1-签到红包，2-百亿补贴等 |
| is_elm_convert | integer | 0 | 否 | 是否生成饿了么红包中间页：0-否，1-是 |
| sid | string | 123_abc | 否 | 饿了么红包中间页渠道标识 |
| is_supered | integer | 0 | 否 | 是否启用单品带超红：0-否，1-是 |

#### 返回参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | integer | 状态码（1成功，0失败） |
| msg | string | 提示语 |
| **data** | **Object** | **转链结果** |
| ↳ coupon_click_url | string | 高佣优惠券链接（推广链接） |
| ↳ item_url | string | 单品链接 |
| ↳ taoword | string | 淘口令（如 `￥a*0S*Lh*y64￥`） |
| ↳ new_model | string | 淘口令(新格式) |
| ↳ max_commission_rate | string | 佣金比例 |
| ↳ min_commission_rate | string | 预估最低佣金率(%) |
| ↳ couponmoney | string | 优惠券金额 |
| ↳ couponstarttime | string | 优惠券开始时间 |
| ↳ couponendtime | string | 优惠券结束时间 |
| ↳ couponexplain | string | 优惠券使用条件 |
| ↳ couponnum | string | 优惠券总量 |
| ↳ couponsurplus | string | 优惠券剩余量 |
| ↳ couponreceive | string | 优惠券领取量 |
| ↳ title | string | 商品标题 |
| ↳ itemid | string | 商品ID |
| ↳ ysyl_click_url | string | 预售有礼-推广链接 |
| ↳ ysyl_tlj_face | string | 预售有礼-预估淘礼金(元) |
| ↳ ysyl_tlj_send_time | string | 预售有礼-淘礼金发放时间 |
| ↳ ysyl_tlj_use_start_time | string | 预售有礼-淘礼金使用开始时间 |
| ↳ ysyl_tlj_use_end_time | string | 预售有礼-淘礼金使用结束时间 |
| ↳ special | Array | 特殊玩法转链结果 |
| ↳ elm_watchword | string | 饿了么二合一口令(简版) |
| ↳ elm_watchword_suggest | string | 饿了么二合一口令 |

---

### 2.2 店铺转链API

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/shopConvert_code` |
| 请求方式 | **POST** |
| 说明 | 单个店铺转链，推广某一个淘宝店铺里的所有商品时使用 |

---

### 2.3 淘联会场转链

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/createConference_code` |
| 请求方式 | **POST** |
| 说明 | 联盟会场活动转链（包含饿了么红包活动） |

---

### 2.4 闲鱼推广转链 (NEW)

闲鱼商品/活动转链。

### 2.5 其他工具类API

| API名称 | 说明 |
|---------|------|
| 好单库放单 | 好单库放单 |
| 定向计划商品列表 | 获取好单库APP申请成功的定向计划商品数据 |
| 闲鱼商品列表 | 获取闲鱼商品列表 |
| 个性化福利清单-线报数据 | 获取单个专区的线报数据 |
| 个性化福利清单-商品列表 | 获取单个专区的商品数据 |
| 个性化福利清单 | 获取单个专区的类目数据 |
| 天猫超市-通用券 | 返回天猫超市通用折扣券口令、链接和二维码 |
| 88VIP拉新订单 | 获取88VIP拉新订单 |

---

## 三、增值类

### 3.1 口令解析

| 项目 | 说明 |
|------|------|
| 请求地址 | `https://v3.api.haodanku.com/rest` |
| 请求方式 | **POST** |
| method | `analyze.taoword` |
| 说明 | 将口令解析并获取其中的商品数据信息 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| method | string | analyze.taoword | 是 | 请求方法 |
| apikey | string | 123456 | 是 | API密钥 |
| taoword | string | xxxxxxx | 是 | 淘宝口令 |
| timestamp | string | 2021-10-18 12:00:00 | 是 | 时间戳 |
| sign | string | $xxxxxxx$ | 是 | 签名 |
| uid | int | 0 | 否 | 用户ID |
| extend | int | 0 | 否 | 扩展字段 |

#### 返回参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | integer | 状态码（200成功） |
| msg | string | 提示语 |
| itemid | string | 商品ID |
| click_url | string | 推广链接 |
| activityid | string | 活动ID |
| couponurl | string | 优惠券链接 |
| item_url | string | 商品详情链接 |
| tkrates | string | 佣金比例 |
| itemprice | string | 原价 |
| itemendprice | string | 优惠后价格 |
| itempic | string | 商品图片 |
| couponmoney | string | 优惠券金额 |
| itemtitle | string | 商品标题 |
| itemshorttitle | string | 商品简称 |
| itemdesc | string | 商品描述 |
| biz_scene_id | string | 业务场景ID |

---

### 3.2 粘贴板识别

> 支持多平台内容解析：淘宝、京东、拼多多、美团、快手、淘宝闪购等。

| 项目 | 说明 |
|------|------|
| 请求地址 | `https://v3.api.haodanku.com/rest` |
| 请求方式 | **POST** (JSON) |
| method | `analyze.clipboard` |
| 版本 | v3.0.1 |
| 说明 | 多平台内容解析并转换为推广链接/口令 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| method | string | analyze.clipboard | 是 | API接口名称 |
| app_id | string | 2021***24511 | 是 | APPID |
| sign | string | - | 是 | API签名 |
| date | string | 2021-12-12 00:00:00 | 是 | 时间戳 |
| v | string | 3.0.1 | 否 | 版本号 |
| content | string | &ad5a1f1a$ | 是 | 待解析的文案 |
| platform | integer | 0 | 否 | 指定平台解析（默认0自动识别） |
| is_change | integer | 0 | 是 | 是否转换：0-否，1-是 |
| tb_name | string | 淘宝 | 否 | 好单库授权的淘宝昵称 |
| tb_pid | string | mm_xx_xx_xx | 否 | 淘宝PID |
| tb_rid | string | 6541***154 | 否 | 淘宝关系/渠道ID |
| jd_union_id | string | afa***421ds | 否 | 京东联盟ID |
| jd_pid | string | xxx_xxx | 否 | 京东PID |
| jd_sub_union_id | string | 123abc | 否 | 京东subUnionId |
| pdd_union | string | asd11...asfaccv | 否 | 多多进宝ID |
| pdd_pid | string | xxx_xxx_xxx | 否 | 多多进宝PID |
| channel | string | 123_abc | 否 | CPS平台子渠道 |

#### 返回参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | integer | 状态码（200成功） |
| msg | string | 返回信息 |
| item_id | string | 商品ID |
| click_url | string | 券链接 |
| item_url | string | 商品链接 |
| item_title | string | 商品标题 |
| item_price | string | 商品原价 |
| rates | string | 佣金比例 |
| item_pic | string | 商品图片 |
| trans_url | object | 转链后的链接类内容 `{coupon_url, item_url}` |
| trans_tpwd | object | 转链后的口令类内容 `{model, password_simple}` |
| platform | int | 平台：0-未知，1-淘宝，2-京东，3-拼多多，4-美团，5-快手 |

---

### 3.3 订单拉取

> 拉取淘系推广的订单数据，实时获取订单的佣金、渠道推广等情况。

| 项目 | 说明 |
|------|------|
| 请求地址 | `https://v3.api.haodanku.com/rest` |
| 请求方式 | **POST** (JSON) |
| method | `tbk.order` |
| 版本 | v3.7.12 |
| 使用人数 | 13,074 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| method | string | tbk.order | 是 | API接口名称 |
| v | string | 3.7.12 | 否 | 版本号 |
| app_id | string | 2021***24511 | 是 | APPID |
| sign | string | - | 是 | API签名 |
| date | string | 2021-12-12 00:00:00 | 是 | 时间戳 |
| tb_name | string | 淘宝 | 是 | 好单库后台已授权的淘宝昵称 |
| start_time | Int | 1638374400 | 否 | 订单查询开始时间 |
| end_time | Int | 1638374400 | 否 | 订单查询结束时间 |
| page_no | Int | 1 | 否 | 第几页（默认1，范围1~100） |
| page_size | Int | 20 | 否 | 每页返回数（默认20，范围1~100） |
| jump_type | Int | 1 | 否 | 跳转类型：-1向前翻页，1向后翻页 |
| tk_status | Int | - | 否 | 淘客订单状态 |
| order_scene | Int | 1 | 否 | 场景订单场景类型 |
| query_type | Int | 1 | 否 | 查询时间类型 |
| member_type | Int | - | 否 | 推广者角色类型 |
| position_index | String | - | 否 | 位点（除第一页外，都需传递） |

#### 返回参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | integer | 状态码（200成功） |
| msg | string | 返回信息 |
| has_next | boolean | 是否有下一页 |
| has_pre | boolean | 是否有上一页 |
| page_no | integer | 页码 |
| page_size | integer | 页大小 |
| position_index | string | 位点 |
| **data** | **Array** | **订单数据** |
| ↳ adzone_id | string | 推广位ID |
| ↳ adzone_name | string | 推广位名称 |
| ↳ alimama_rate | string | 平台技术服务费比率 |
| ↳ alimama_share_fee | string | 平台技术服务费 |
| ↳ alipay_total_price | string | 付款金额 |
| ↳ click_time | string | 点击时间 |
| ↳ item_id | string | 商品ID |
| ↳ item_img | string | 商品图片 |
| ↳ item_link | string | 商品链接 |
| ↳ item_num | integer | 商品数量 |
| ↳ item_price | string | 商品单价 |
| ↳ item_title | string | 商品标题 |
| ↳ order_type | string | 平台类型（天猫等） |
| ↳ trade_id | string | 子订单号 |
| ↳ trade_parent_id | string | 订单编号 |
| ↳ total_commission_fee | string | 佣金金额 |
| ↳ total_commission_rate | string | 佣金比率 |
| ↳ tk_status | integer | 淘客订单状态 |

---

### 3.4 好单库订单查询(订单号查询)

通过订单号查询好单库小程序的淘宝订单数据。

---

### 3.5 短链接生成

| 项目 | 说明 |
|------|------|
| 请求地址 | `https://v3.api.haodanku.com/rest` |
| 请求方式 | **POST** (JSON) |
| method | `short.link` |
| 说明 | 长链接转换为短链接（快站短链） |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| method | string | short.link | 是 | API接口名称 |
| app_id | string | 2021***24511 | 是 | APPID |
| sign | string | - | 是 | API签名 |
| date | string | 2021-12-12 00:00:00 | 是 | 时间戳 |
| link | string | https://xxxx.cn/aaaa | 是 | 待转换的原链接（必须urlencode后传入） |

#### 返回参数

| 参数 | 类型 | 说明 |
|------|------|------|
| code | integer | 状态码（200成功） |
| msg | string | 返回信息 |
| data.short_url | string | 生成的短链接 |

---

### 3.6 其他增值类API

| API名称 | 说明 |
|---------|------|
| 单页推广链接生成 | 生成好单库单页专区各个板块单页的推广链接 |
| 单品推广集合页 (NEW) | 生成淘宝单品快站集合页 |
| 二合一链接解析 | 将二合一链接解析获取相关数据 |
| cms推广链接生成 (NEW) | 生成好单库CMS推广链接 |

---

## 四、入库类

### 4.1 商品列表页API (itemlist)

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/itemlist` |
| 请求方式 | GET |
| 版本 | v3.7.12 |
| 使用人数 | 14,330 |
| 说明 | 获取好单库全部商品数据 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| apikey | string | 你的apikey | 是 | Apikey值 |
| v | string | 3.7.12 | 否 | 版本号 |
| nav | integer | 3 | 是 | 默认全部：3-全部商品，4-纯视频单，5-聚淘专区 |
| cid | integer | 0 | 否 | 类目（同column接口） |
| back | integer | 500 | 是 | 每页条数，可选：`1,2,10,20,50,100,120,200,500` |
| min_id | integer | 1 | 是 | 分页参数 |
| sort | integer | 0 | 否 | 排序（同column接口） |
| price_min | integer | 100 | 否 | 券后价最低值 |
| price_max | integer | 200 | 否 | 券后价最高值 |
| sale_min | integer | 1000 | 否 | 月销量最低值 |
| sale_max | integer | 5000 | 否 | 月销量最高值 |
| coupon_min | integer | 10 | 否 | 优惠券最低金额 |
| coupon_max | integer | 20 | 否 | 优惠券最高金额 |
| tkrates_min | integer | 30 | 否 | 佣金比例最低值 |
| tkrates_max | integer | 50 | 否 | 佣金比例最高值 |
| tkmoney_min | integer | 30 | 否 | 佣金最低值 |
| item_type | string | 0 | 否 | 是否只获取营销返利商品：0-否，1-是 |

#### 返回参数

与[商品筛选API](#12-商品筛选api-column)商品通用字段相同。

---

### 4.2 商品更新API

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/update_item` |
| 请求方式 | GET |
| 说明 | 更新好单库商品数据（销量、券领取量等） |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| apikey | string | 你的apikey | 是 | Apikey值 |
| sort | integer | 1 | 否 | 更新排序：1-好单指数，2-月销量，3-近2小时销量，4-当天销量，5-在线人数，6-活动开始时间 |
| back | integer | 500 | 是 | 每页条数，可选：`1,2,10,20,50,100,120,200,500,1000` |
| min_id | integer | 1 | 是 | 分页参数 |

#### 返回参数（更新字段）

| 参数 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码 |
| min_id | Integer | 下一页参数 |
| product_id | Integer | 自增ID |
| itemid | Integer | 宝贝ID |
| itemsale | integer | 月销量 |
| itemsale2 | integer | 近2小时销量 |
| todaysale | integer | 当天销量 |
| general_index | integer | 好单库指数 |
| couponurl | string | 优惠券链接 |
| couponreceive | integer | 优惠券领取量 |
| couponsurplus | integer | 优惠券剩余量 |
| activityid | string | 优惠券ID |
| couponmoney | integer | 优惠券金额 |
| couponinfo | string | 折扣信息 |
| min_buy | int | 最低拍下件数 |

---

### 4.3 定时拉取API

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/timing_items` |
| 请求方式 | GET |
| 说明 | 定时拉取今天上架的商品（增量入库） |
| 应用场景 | 第一次拉取全量商品后，后续只需调用本接口实现当天0点开始的增量入库 |

#### 请求参数

| 参数 | 类型 | 示例值 | 必须 | 说明 |
|------|------|--------|:----:|------|
| apikey | string | 你的apikey | 是 | Apikey值 |
| start | integer | 0 | 是 | 小时点数，0代表从今天0点开始 |

---

### 4.4 失效商品列表API

| 项目 | 说明 |
|------|------|
| 请求地址 | `http://v2.api.haodanku.com/get_down_items` |
| 请求方式 | GET |
| 说明 | 获取一定时间段内失效的商品 |

---

### 4.5 其他入库类API

| API名称 | 说明 |
|---------|------|
| 淘宝优质推荐商品 | 获取淘宝优质推荐商品 |
| 好单库放单商品 | 好单库放单商品 |
| 闲鱼CPA数据明细 | 闲鱼CPA数据明细 |
| 完整黑名单库API | 返回好单库完整黑名单库数据 |
| 我的放单商品 | 返回在好单库平台发布的商品数据 |
| 单品上榜排名 | 获取商品榜单排名及该类榜单列表 |
| 闲鱼订单拉取 | 获取闲鱼CPS订单数据 |

---

## 五、CPS活动类

| API名称 | 说明 |
|---------|------|
| 淘宝直播订单拉取 | 淘宝直播订单拉取 |
| 淘宝直播商品转链 | 淘宝直播商品转链 |
| 淘宝直播商品查询 | 获取淘宝直播商品 |

---

## 六、特色板块类

共 27+ 个API，覆盖好单库各特色板块：

| API名称 | 状态 | 说明 |
|---------|:----:|------|
| 防折叠朋友圈API | NEW | 获取好单库发圈素材朋友圈防折叠数据 |
| 联盟会场列表 | - | 获取淘宝联盟官方会场列表数据 |
| 素材商品列表 | - | 获取素材广场列表数据 |
| 好货专场API | - | 9宫格形式展示到微信朋友圈 |
| "小样种草机"商品列表 | NEW | 获取"小样种草机"单页商品数据 |
| 精选活动 | - | 好单库首页人工选品专题页数据 |
| 品牌实时榜 | - | 好单库官网品牌实时榜数据 |
| 签到红包商品 | - | 签到红包单页商品数据 |
| 实时佣金榜API | - | 新版榜单-实时佣金榜商品数据 |
| 定向计划商品API | - | 定向计划 |
| 高佣专场商品API | - | 优质高佣金商品 |
| 精选低价包邮专区API | - | 9.9元、6.9元、3.9元包邮商品 |
| 偏远地区包邮商品API | - | 偏远地区包邮商品数据 |
| 抖货商品API | - | 抖货商品、视频地址、详细信息 |
| 品牌列表API | - | 超值大牌页面的细分品牌及商品 |
| 好单库榜单 | - | 好单库淘系榜单数据 |
| 今日值得买API | - | 推荐优质商品（12条） |
| 精选专题API | - | 精选专题数据 |
| 精编文案API | - | 选品专员精选的商品文案 |
| 快抢商品 | 即将下线 | 快抢商品数据 |
| 我的收藏 | - | 收藏的商品数据 |
| 品牌信息API | - | 所有品牌集合 |
| 单个品牌详情API | - | 指定品牌下所有商品 |
| 今日推荐品牌API | - | "今日推荐品牌"数据 |
| 朋友圈API | - | 选品专员精选的商品文案 |
| 精选专题商品API | - | 精选专题商品数据 |
| 猫超好货商品 | - | "猫超好货"单页商品列表 |
| 淘宝主题品牌栏目商品 | - | 淘宝主题品牌栏目商品 |
| 淘宝品牌分类榜单 | - | 淘宝品牌分类榜单 |
| 达人说API | 已停更 | 达人说文章信息 |
| 文章详情API | 已停更 | 达人文章详情 |
| 作者文章API | 已停更 | 达人的所有文章 |

---

## 附录：CPS项目接口映射

> 以下为 AgenticCPS 项目中好单库适配器使用的核心接口映射关系。

### 淘宝平台 (HdkTaobaoVendorClient)

| 功能 | 项目代码路径 | 好单库实际接口 | HTTP方法 | 域名 |
|------|------------|---------------|---------|------|
| 商品搜索 | `/supersearch` | `/supersearch` | GET | v2 |
| 推广转链 | `/ratesurl` | `/ratesurl` | **POST** | **v3** |
| 连接测试 | `/supersearch` | `/supersearch` | GET | v2 |

### 字段映射（转链响应）

| CPS项目字段 | 好单库API字段 | 说明 |
|------------|-------------|------|
| shortUrl | `coupon_click_url` | 高佣优惠券链接（推广链接） |
| longUrl | `item_url` | 单品链接 |
| tpwd | `taoword` | 淘口令 |

### 字段映射（搜索响应）

| CPS项目字段 | 好单库API字段 | 说明 |
|------------|-------------|------|
| goodsId | `itemid` | 宝贝ID |
| title | `itemtitle` | 宝贝标题 |
| mainPic | `itempic` | 主图URL |
| originalPrice | `itemprice` | 在售价 |
| actualPrice | `itemendprice` | 券后价 |
| couponPrice | `couponmoney` | 优惠券金额 |
| commissionRate | `tkrates` | 佣金比例 |
| monthSales | `itemsale` | 月销量 |
| shopName | `shopname` | 店铺名 |
| shopType | `shoptype` | 店铺类型(B/C) |

### 重要注意事项

1. **域名差异**：搜索接口使用 `v2.api.haodanku.com`，转链接口使用 `v3.api.haodanku.com`
2. **HTTP方法差异**：搜索用 GET，转链用 POST（表单提交）
3. **关键词编码**：supersearch 的 keyword 参数需进行**两次 urlencode 编码**
4. **分页机制**：使用 `min_id` + `tb_p` 滚动分页，非传统页码
5. **back参数限制**：只允许特定值（1,2,10,20,50,100 等），不支持任意数字
6. **get_taoword参数**：转链时需传 `get_taoword=1` 才能获取淘口令
7. **tb_name参数**：转链时必须传授权淘宝昵称，否则无法生成推广链接
