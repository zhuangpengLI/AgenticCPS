# CPS联盟返利模块

<cite>
**本文引用的文件**
- [CpsPlatformCodeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java)
- [CpsVendorCodeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java)
- [CpsOrderStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java)
- [CpsRebateStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java)
- [CpsAdzoneTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java)
- [CpsFreezeStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsFreezeStatusEnum.java)
- [CpsRebateTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateTypeEnum.java)
- [CpsRiskRuleTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRiskRuleTypeEnum.java)
- [CpsWithdrawStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsWithdrawStatusEnum.java)
- [CpsErrorCodeConstants.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsErrorCodeConstants.java)
- [DtkJavaOpenPlatformSdkApplication.java](file://agent_improvement/sdk_demo/dataoke-sdk-java/src/main/java/com/dtk/api/DtkJavaOpenPlatformSdkApplication.java)
- [BaseController.java](file://agent_improvement/sdk_demo/dataoke-sdk-java/src/main/java/com/dtk/api/controller/base/BaseController.java)
- [SwaggerConfiguration.java](file://agent_improvement/sdk_demo/dataoke-sdk-java/src/main/java/com/dtk/api/controller/config/SwaggerConfiguration.java)
- [README.md](file://agent_improvement/sdk_demo/dataoke-sdk-java/README.md)
- [pom.xml](file://agent_improvement/sdk_demo/dataoke-sdk-java/pom.xml)
- [CPS系统PRD文档.md](file://docs/CPS系统PRD文档.md)
- [好单库OpenAPI接口文档.md](file://docs/好单库OpenAPI接口文档.md)
- [CpsAdzoneService.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneService.java)
- [CpsAdzoneDO.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/adzone/CpsAdzoneDO.java)
- [CpsAdzoneController.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/CpsAdzoneController.java)
- [CpsAdzoneSaveReqVO.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzoneSaveReqVO.java)
- [CpsAdzoneServiceImpl.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneServiceImpl.java)
- [CpsAdzoneMapper.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/adzone/CpsAdzoneMapper.java)
- [CpsAdzoneRespVO.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzoneRespVO.java)
- [CpsAdzonePageReqVO.java](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzonePageReqVO.java)
- [InEnum.java](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnum.java)
- [InEnumValidator.java](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnumValidator.java)
- [InEnumCollectionValidator.java](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnumCollectionValidator.java)
</cite>

## 更新摘要
**所做更改**
- 新增广告位类型枚举的详细说明和验证规则章节
- 完善推广位管理系统的架构描述
- 增强广告位类型与业务场景的关联分析
- 补充验证注解在推广位管理中的应用说明
- 新增推广位数据模型和响应对象的详细说明

## 目录
1. [引言](#引言)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [广告位类型与验证规则](#广告位类型与验证规则)
7. [推广位管理系统](#推广位管理系统)
8. [依赖关系分析](#依赖关系分析)
9. [性能考虑](#性能考虑)
10. [故障排查指南](#故障排查指南)
11. [结论](#结论)
12. [附录](#附录)

## 引言
本技术文档面向AgenticCPS CPS联盟返利模块，系统性阐述多平台适配器设计、商品搜索比价、推广链接生成、订单追踪、返利计算与提现管理、风控体系等核心能力。文档以策略模式组织平台适配器（淘宝、京东、拼多多、抖音等），并结合枚举化状态与规则体系，构建从商品搜索到返利到账的全链路闭环。

**更新** 新增广告位类型枚举的完善说明，包括GENERAL、CHANNEL、MEMBER三种类型的详细定义和验证规则，以及完整的推广位管理系统架构描述。

## 项目结构
CPS模块位于后端工程的独立模块中，采用分层+领域驱动的设计理念：
- 枚举层：统一定义平台、供应商、订单状态、返利状态、冻结状态、返利类型、风控规则类型、提现状态、错误码等
- SDK示例：提供第三方开放平台（如大淘客）的Java SDK示例，便于快速对接聚合API
- 文档：包含系统PRD与第三方OpenAPI接口文档，指导业务与集成
- 推广位管理：新增专门的推广位类型枚举和管理服务

```mermaid
graph TB
subgraph "CPS模块"
ENUMS["枚举定义<br/>平台/供应商/状态/规则/错误码<br/>广告位类型枚举"]
ADAPTERS["平台适配器<br/>淘宝/京东/拼多多/抖音"]
ORDERS["订单服务<br/>搜索/比价/推广/追踪"]
REBATES["返利服务<br/>计算/结算/到账/风控"]
WITHDRAW["提现服务<br/>申请/审核/打款"]
ADZONE["推广位管理<br/>类型枚举/验证规则/业务逻辑"]
end
subgraph "SDK示例"
DTOKE["大淘客SDK<br/>Java示例"]
end
ENUMS --> ADAPTERS
ADAPTERS --> ORDERS
ORDERS --> REBATES
REBATES --> WITHDRAW
ADZONE --> ORDERS
DTOKE --> ORDERS
```

**图表来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)
- [CpsVendorCodeEnum.java:1-52](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L1-L52)
- [CpsOrderStatusEnum.java:1-48](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java#L1-L48)
- [CpsRebateStatusEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L1-L40)
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)

**章节来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)
- [CpsVendorCodeEnum.java:1-52](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L1-L52)
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)

## 核心组件
- 平台与供应商枚举：统一平台编码（淘宝/京东/拼多多/抖音等）与供应商类型（聚合平台/官方API）
- 订单状态枚举：覆盖从下单到返利到账的完整生命周期
- 返利状态枚举：待结算、已到账、已扣回
- 冻结与提现状态：账户/返利资金的冻结与提现状态
- 风控规则类型：用于识别不同维度的风控策略
- 错误码常量：标准化错误返回，便于前端与运营定位问题
- **广告位类型枚举**：新增推广位类型定义，支持通用、渠道专属、用户专属三种类型

**更新** 新增广告位类型枚举，为推广位管理提供类型约束和业务区分。

**章节来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)
- [CpsVendorCodeEnum.java:1-52](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L1-L52)
- [CpsOrderStatusEnum.java:1-48](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java#L1-L48)
- [CpsRebateStatusEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L1-L40)
- [CpsFreezeStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsFreezeStatusEnum.java)
- [CpsWithdrawStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsWithdrawStatusEnum.java)
- [CpsRiskRuleTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRiskRuleTypeEnum.java)
- [CpsErrorCodeConstants.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsErrorCodeConstants.java)
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)

## 架构总览
CPS返利系统采用"策略+适配器"架构，围绕多平台API进行解耦：
- 平台适配器：针对不同联盟平台（淘宝、京东、拼多多、抖音等）抽象统一接口，屏蔽差异
- 商品搜索与比价：通过聚合平台SDK或官方API获取商品信息与优惠券，支持价格对比
- 推广链接生成：根据商品与广告位生成带推广参数的跳转链接
- 订单追踪：订阅/轮询平台订单状态，同步至本地订单表，推进生命周期流转
- 返利计算与结算：基于订单状态与佣金比例计算返利，完成结算与到账
- 提现管理：用户发起提现，风控校验通过后进入打款流程
- 风控系统：基于规则类型对异常行为进行拦截与处理
- **推广位管理**：新增推广位类型管理，支持不同类型推广位的创建、验证和使用

**更新** 新增推广位管理系统，提供完整的广告位类型管理和验证机制。

```mermaid
graph TB
CLIENT["客户端/小程序/APP"] --> SEARCH["商品搜索与比价"]
SEARCH --> LINKGEN["推广链接生成"]
LINKGEN --> ORDER["订单追踪"]
ORDER --> REBATE["返利计算与结算"]
REBATE --> WITHDRAW["提现管理"]
ORDER --> PLATFORM["平台适配器<br/>淘宝/京东/拼多多/抖音"]
SEARCH --> SDK["聚合平台SDK<br/>大淘客等"]
REBATE --> FROZEN["冻结/解冻"]
REBATE --> RISK["风控规则"]
ADZONE["推广位管理<br/>类型验证/业务逻辑"] --> LINKGEN
ADZONE --> VALIDATION["验证注解<br/>InEnum"]
VALIDATION --> ADZONE
```

**图表来源**
- [CpsPlatformCodeEnum.java:18-24](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L18-L24)
- [CpsVendorCodeEnum.java:20-24](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L20-L24)
- [CpsOrderStatusEnum.java:18-25](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java#L18-L25)
- [CpsRebateStatusEnum.java:18-21](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L18-L21)
- [CpsAdzoneTypeEnum.java:18-21](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L18-L21)

## 详细组件分析

### 平台适配器与策略模式
平台适配器通过策略模式实现，统一对外接口，内部按平台差异封装调用细节。核心平台包括：
- 淘宝联盟
- 京东联盟
- 拼多多联盟
- 抖音联盟
- 其他：唯品会、美团联盟

```mermaid
classDiagram
class 平台适配器 {
+搜索商品(关键词/类目)
+获取优惠券信息
+生成推广链接(商品ID/广告位)
+查询订单状态(订单号/时间范围)
+回调通知(订单状态变更)
}
class 淘宝适配器 {
+实现平台适配器
}
class 京东适配器 {
+实现平台适配器
}
class 拼多多适配器 {
+实现平台适配器
}
class 抖音适配器 {
+实现平台适配器
}
平台适配器 <|.. 淘宝适配器
平台适配器 <|.. 京东适配器
平台适配器 <|.. 拼多多适配器
平台适配器 <|.. 抖音适配器
```

**图表来源**
- [CpsPlatformCodeEnum.java:18-24](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L18-L24)

**章节来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)

### 商品搜索与比价
- 聚合平台接入：通过大淘客等聚合SDK批量获取商品信息、券后价、佣金比例等
- 本地缓存与去重：对热门商品建立索引，避免重复抓取
- 比价逻辑：按平台、类目、销量、佣金率等维度排序，输出最优商品

```mermaid
sequenceDiagram
participant 客户端 as "客户端"
participant 搜索服务 as "搜索服务"
participant SDK as "聚合SDK"
participant 缓存 as "本地缓存"
客户端->>搜索服务 : 发起搜索请求(关键词/类目)
搜索服务->>缓存 : 查询缓存
alt 命中缓存
缓存-->>搜索服务 : 返回商品列表
else 未命中缓存
搜索服务->>SDK : 调用聚合API
SDK-->>搜索服务 : 返回商品数据
搜索服务->>缓存 : 写入缓存
end
搜索服务-->>客户端 : 返回比价结果
```

**图表来源**
- [CpsVendorCodeEnum.java:20-24](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L20-L24)
- [DtkJavaOpenPlatformSdkApplication.java](file://agent_improvement/sdk_demo/dataoke-sdk-java/src/main/java/com/dtk/api/DtkJavaOpenPlatformSdkApplication.java)
- [BaseController.java](file://agent_improvement/sdk_demo/dataoke-sdk-java/src/main/java/com/dtk/api/controller/base/BaseController.java)

**章节来源**
- [CpsVendorCodeEnum.java:1-52](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L1-L52)
- [README.md](file://agent_improvement/sdk_demo/dataoke-sdk-java/README.md)

### 推广链接生成
- 输入：商品ID、广告位ID、渠道标识
- 输出：带推广参数的落地页链接（含推广位、优惠券、返佣比例等）
- 平台差异：各平台参数命名与字段略有不同，适配器内部统一封装
- **广告位类型支持**：根据广告位类型（通用/渠道专属/用户专属）生成相应的推广链接

**更新** 推广链接生成现在支持三种广告位类型，确保推广参数的正确性和业务场景的适配性。

```mermaid
flowchart TD
Start(["开始"]) --> Validate["校验输入参数"]
Validate --> GetAdzone["获取广告位信息"]
GetAdzone --> TypeCheck{"广告位类型?"}
TypeCheck --> |通用| General["通用推广参数"]
TypeCheck --> |渠道专属| Channel["渠道专属参数"]
TypeCheck --> |用户专属| Member["用户专属参数"]
General --> GenLink["生成推广链接"]
Channel --> GenLink
Member --> GenLink
GenLink --> PlatformCheck{"平台类型?"}
PlatformCheck --> |淘宝| Taobao["淘宝参数组装"]
PlatformCheck --> |京东| Jingdong["京东参数组装"]
PlatformCheck --> |拼多多| Pinduoduo["拼多多参数组装"]
PlatformCheck --> |抖音| Douyin["抖音参数组装"]
Taobao --> Return["返回链接"]
Jingdong --> Return
Pinduoduo --> Return
Douyin --> Return
```

**图表来源**
- [CpsPlatformCodeEnum.java:18-24](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L18-L24)
- [CpsAdzoneTypeEnum.java:18-21](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L18-L21)

**章节来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)

### 订单追踪与生命周期
订单生命周期从"已下单"到"已结算/已到账"，期间可能触发"已退款/已失效"。系统通过主动轮询或平台回调同步订单状态。

```mermaid
stateDiagram-v2
[*] --> 已下单
已下单 --> 已付款 : "支付成功"
已下单 --> 已失效 : "超时/取消"
已付款 --> 已收货 : "确认收货"
已付款 --> 已退款 : "发生退款"
已收货 --> 已结算 : "平台结算"
已结算 --> 已到账 : "返利到账"
已退款 --> 已失效 : "退款完成"
```

**图表来源**
- [CpsOrderStatusEnum.java:18-25](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java#L18-L25)
- [CpsRebateStatusEnum.java:18-21](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L18-L21)

**章节来源**
- [CpsOrderStatusEnum.java:1-48](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java#L1-L48)
- [CpsRebateStatusEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L1-L40)

### 返利计算与结算
- 计算依据：订单金额、佣金比例、平台补贴、活动返利
- 结算时机：订单"已结算"后，生成返利记录，状态置为"待结算"
- 到账：结算完成后，资金解冻并计入用户余额，状态置为"已到账"
- 扣回：若发生退款或违规，执行"已扣回"

```mermaid
flowchart TD
O["订单已结算"] --> Calc["计算返利金额"]
Calc --> Freeze["冻结返利资金"]
Freeze --> Confirm{"是否满足到账条件"}
Confirm --> |是| Settle["解冻并记账"]
Confirm --> |否| Hold["继续冻结/延后处理"]
Settle --> Done["状态=已到账"]
Hold --> Wait["等待条件满足"]
Wait --> Confirm
```

**图表来源**
- [CpsRebateStatusEnum.java:18-21](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L18-L21)
- [CpsFreezeStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsFreezeStatusEnum.java)

**章节来源**
- [CpsRebateStatusEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L1-L40)

### 提现管理流程
- 用户申请：填写提现金额、银行账户、手续费承担方式
- 风控校验：额度、频次、黑名单、异常交易等
- 审核与打款：人工复核或自动审批后发起打款
- 状态更新：申请中、已完成、已拒绝、已撤销

```mermaid
sequenceDiagram
participant 用户 as "用户"
participant 系统 as "提现服务"
participant 风控 as "风控引擎"
participant 支付 as "支付网关"
用户->>系统 : 提交提现申请
系统->>风控 : 触发风控校验
风控-->>系统 : 校验结果(通过/拒绝)
alt 通过
系统->>支付 : 发起打款
支付-->>系统 : 打款结果
系统-->>用户 : 更新状态(已完成/失败)
else 拒绝
系统-->>用户 : 更新状态(已拒绝)
end
```

**图表来源**
- [CpsWithdrawStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsWithdrawStatusEnum.java)
- [CpsRiskRuleTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRiskRuleTypeEnum.java)

**章节来源**
- [CpsWithdrawStatusEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsWithdrawStatusEnum.java)
- [CpsRiskRuleTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRiskRuleTypeEnum.java)

### 风控系统设计
- 规则类型：异常登录、频繁提现、高风险IP、刷单行为、设备指纹异常
- 处理策略：阻断、加审、限制额度、冻结账户
- 与订单/返利/提现联动：在关键节点触发校验，确保资金安全

```mermaid
flowchart TD
Trigger["触发点(订单/返利/提现)"] --> Rule["匹配风控规则"]
Rule --> Decision{"决策"}
Decision --> |阻断| Block["拒绝操作"]
Decision --> |加审| Review["提交人工审核"]
Decision --> |限制| Limit["降低限额/频率"]
Decision --> |冻结| Freeze["临时冻结账户"]
Review --> Result["返回处理结果"]
Limit --> Result
Freeze --> Result
Block --> End(["结束"])
Result --> End
```

**图表来源**
- [CpsRiskRuleTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRiskRuleTypeEnum.java)

**章节来源**
- [CpsRiskRuleTypeEnum.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRiskRuleTypeEnum.java)

## 广告位类型与验证规则

### 广告位类型枚举定义
CPS系统新增了完整的广告位类型枚举体系，支持三种核心类型：

- **GENERAL（通用）**：适用于所有渠道和用户的通用推广位，无特定限制
- **CHANNEL（渠道专属）**：专属于特定渠道的推广位，仅该渠道可使用
- **MEMBER（用户专属）**：专属于特定用户的推广位，仅该用户可使用

```mermaid
classDiagram
class CpsAdzoneTypeEnum {
+GENERAL("general", "通用")
+CHANNEL("channel", "渠道专属")
+MEMBER("member", "用户专属")
+array() String[]
}
class 推广位类型 {
+type : String
+name : String
+ARRAYS : String[]
}
CpsAdzoneTypeEnum --> 推广位类型
```

**图表来源**
- [CpsAdzoneTypeEnum.java:16-37](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L16-L37)

### 推广位管理服务架构
推广位管理采用完整的分层架构，确保类型安全和业务逻辑清晰：

- **控制器层**：提供RESTful API接口，处理推广位的创建、更新、查询和分页
- **服务层**：实现业务逻辑，包含推广位的验证、查询和管理
- **数据访问层**：持久化推广位信息，支持多种查询方式
- **数据传输对象**：封装请求和响应数据结构

```mermaid
graph TB
Controller["CpsAdzoneController<br/>RESTful接口"] --> Service["CpsAdzoneService<br/>业务逻辑"]
Service --> Impl["CpsAdzoneServiceImpl<br/>实现类"]
Impl --> Mapper["CpsAdzoneMapper<br/>数据访问"]
Mapper --> Database["数据库<br/>cps_adzone表"]
Controller --> VO["CpsAdzoneSaveReqVO<br/>请求参数"]
Controller --> Response["CpsAdzoneRespVO<br/>响应数据"]
```

**图表来源**
- [CpsAdzoneController.java:28-83](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/CpsAdzoneController.java#L28-L83)
- [CpsAdzoneService.java:16-48](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneService.java#L16-L48)
- [CpsAdzoneServiceImpl.java:25-71](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneServiceImpl.java#L25-L71)

### 验证规则与注解机制
推广位类型验证通过自定义注解实现，确保数据的完整性和一致性：

- **InEnum注解**：验证推广位类型是否在允许范围内
- **自动同步机制**：根据推广位类型自动设置关联类型
- **条件验证**：渠道专属和用户专属类型需要关联ID

```mermaid
flowchart TD
Input["推广位保存请求"] --> Validation["InEnum验证"]
Validation --> TypeCheck{"推广位类型检查"}
TypeCheck --> |GENERAL| GeneralCheck["无需关联ID"]
TypeCheck --> |CHANNEL| ChannelCheck["需要channel关联ID"]
TypeCheck --> |MEMBER| MemberCheck["需要member关联ID"]
GeneralCheck --> Success["验证通过"]
ChannelCheck --> Success
MemberCheck --> Success
Success --> Save["保存推广位"]
```

**图表来源**
- [CpsAdzoneSaveReqVO.java:28-36](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzoneSaveReqVO.java#L28-L36)
- [InEnum.java:22-35](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnum.java#L22-L35)

### 推广位数据模型
推广位采用完整的数据模型设计，支持灵活的业务场景：

- **基础信息**：平台编码、推广位ID、名称、类型
- **关联信息**：关联类型（channel/member）、关联ID
- **状态管理**：默认标记、启用状态
- **租户隔离**：支持多租户环境下的推广位管理

**章节来源**
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)
- [CpsAdzoneService.java:1-49](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneService.java#L1-L49)
- [CpsAdzoneDO.java:1-69](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/adzone/CpsAdzoneDO.java#L1-L69)
- [CpsAdzoneController.java:1-84](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/CpsAdzoneController.java#L1-L84)
- [CpsAdzoneSaveReqVO.java:1-46](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzoneSaveReqVO.java#L1-L46)
- [CpsAdzoneServiceImpl.java:1-72](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneServiceImpl.java#L1-L72)
- [CpsAdzoneMapper.java:1-43](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/adzone/CpsAdzoneMapper.java#L1-L43)
- [CpsAdzoneRespVO.java:1-43](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzoneRespVO.java#L1-L43)
- [CpsAdzonePageReqVO.java:1-28](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzonePageReqVO.java#L1-L28)
- [InEnum.java:1-35](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnum.java#L1-L35)
- [InEnumValidator.java:1-43](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnumValidator.java#L1-L43)
- [InEnumCollectionValidator.java:1-44](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnumCollectionValidator.java#L1-L44)

## 推广位管理系统

### 系统架构设计
推广位管理系统采用标准的三层架构设计，确保系统的可维护性和扩展性：

- **表现层（Controller层）**：提供RESTful API接口，处理HTTP请求和响应
- **业务层（Service层）**：封装业务逻辑，协调各个服务组件
- **数据访问层（DAO层）**：负责数据的持久化操作

```mermaid
graph TB
subgraph "表现层"
Create["创建推广位接口"]
Update["更新推广位接口"]
Delete["删除推广位接口"]
Get["获取推广位接口"]
Page["推广位分页接口"]
List["按平台获取推广位列表接口"]
end
subgraph "业务层"
Service["CpsAdzoneService接口"]
ServiceImpl["CpsAdzoneServiceImpl实现类"]
end
subgraph "数据访问层"
Mapper["CpsAdzoneMapper接口"]
DO["CpsAdzoneDO实体类"]
end
Create --> Service
Update --> Service
Delete --> Service
Get --> Service
Page --> Service
List --> Service
Service --> ServiceImpl
ServiceImpl --> Mapper
Mapper --> DO
```

**图表来源**
- [CpsAdzoneController.java:33-81](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/CpsAdzoneController.java#L33-L81)
- [CpsAdzoneService.java:16-48](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneService.java#L16-L48)
- [CpsAdzoneServiceImpl.java:31-63](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/adzone/CpsAdzoneServiceImpl.java#L31-L63)
- [CpsAdzoneMapper.java:18-42](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/adzone/CpsAdzoneMapper.java#L18-L42)

### 数据模型详解
推广位数据模型采用完整的ORM映射设计，支持丰富的业务属性：

- **主键标识**：Long类型的自增主键
- **平台关联**：String类型的平台编码，关联具体联盟平台
- **推广位标识**：String类型的推广位ID，对应平台的PID
- **推广位名称**：String类型的显示名称
- **推广位类型**：String类型的枚举值，支持三种类型
- **关联类型**：String类型的关联类型，自动同步自推广位类型
- **关联ID**：Long类型的关联标识，渠道专属和用户专属时必填
- **默认标记**：Integer类型的状态标记，0表示非默认，1表示默认
- **状态管理**：Integer类型的状态值，0表示禁用，1表示启用

**章节来源**
- [CpsAdzoneDO.java:24-68](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/dataobject/adzone/CpsAdzoneDO.java#L24-L68)

### 请求参数验证
推广位请求参数采用严格的验证机制，确保数据的完整性和有效性：

- **平台编码**：必填，非空验证
- **推广位ID**：必填，非空验证  
- **推广位类型**：必填，使用InEnum注解验证
- **关联类型**：自动生成，无需手动输入
- **关联ID**：条件必填，渠道专属和用户专属时必填
- **默认标记**：可选，默认值为0
- **状态**：必填，非空验证

**章节来源**
- [CpsAdzoneSaveReqVO.java:12-45](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzoneSaveReqVO.java#L12-L45)

### 响应数据结构
推广位响应数据采用简洁明了的数据结构设计：

- **基础信息**：主键ID、平台编码、推广位ID、名称
- **类型信息**：推广位类型、关联类型
- **关联信息**：关联ID
- **状态信息**：默认标记、状态
- **时间信息**：创建时间

**章节来源**
- [CpsAdzoneRespVO.java:10-42](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzoneRespVO.java#L10-L42)

### 分页查询功能
推广位分页查询支持多维度筛选和排序：

- **平台编码筛选**：支持按平台编码精确查询
- **名称模糊查询**：支持按推广位名称模糊匹配
- **类型筛选**：支持按推广位类型精确筛选
- **状态筛选**：支持按启用状态精确筛选
- **排序规则**：按创建时间倒序排列

**章节来源**
- [CpsAdzonePageReqVO.java:13-27](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/vo/CpsAdzonePageReqVO.java#L13-L27)
- [CpsAdzoneMapper.java:20-26](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/dal/mysql/adzone/CpsAdzoneMapper.java#L20-L26)

## 依赖关系分析
- 平台与供应商：平台编码与供应商类型相互独立，但通常存在"平台-供应商"映射关系
- 状态与流程：订单状态与返利状态共同决定业务流程走向
- SDK与平台：聚合SDK用于快速接入，官方API用于稳定与合规
- **推广位类型**：广告位类型枚举作为独立组件，被推广位管理服务广泛使用

**更新** 新增推广位类型与相关组件的依赖关系分析。

```mermaid
graph LR
平台枚举["平台编码枚举"] --> 适配器["平台适配器"]
供应商枚举["供应商枚举"] --> SDK["聚合SDK"]
订单状态["订单状态枚举"] --> 订单服务["订单服务"]
返利状态["返利状态枚举"] --> 返利服务["返利服务"]
风控规则["风控规则类型"] --> 风控服务["风控服务"]
广告位类型["广告位类型枚举"] --> 推广位服务["推广位服务"]
推广位服务 --> 订单服务
验证注解["InEnum验证注解"] --> 推广位服务
验证注解 --> 请求参数
请求参数 --> 推广位服务
```

**图表来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)
- [CpsVendorCodeEnum.java:1-52](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L1-L52)
- [CpsOrderStatusEnum.java:1-48](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java#L1-L48)
- [CpsRebateStatusEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L1-L40)
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)
- [InEnum.java:1-35](file://backend/qiji-framework/qiji-common/src/main/java/com/qiji/cps/framework/common/validation/InEnum.java#L1-L35)

**章节来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)
- [CpsVendorCodeEnum.java:1-52](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L1-L52)
- [CpsOrderStatusEnum.java:1-48](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsOrderStatusEnum.java#L1-L48)
- [CpsRebateStatusEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsRebateStatusEnum.java#L1-L40)
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)

## 性能考虑
- 缓存策略：热点商品与优惠信息使用本地缓存，降低聚合API调用频率
- 异步处理：订单同步、返利计算、提现打款采用消息队列异步执行
- 分页与限流：聚合API调用需遵循限流策略，避免触发平台风控
- 幂等设计：订单回调与轮询均需保证幂等，防止重复结算
- 监控与告警：对关键链路埋点，设置延迟与成功率阈值告警
- **推广位缓存**：推广位类型枚举和验证规则可进行缓存优化，减少重复验证开销
- **数据库索引**：推广位表应建立平台编码、状态、默认标记等常用查询字段的索引

**更新** 新增推广位缓存优化和数据库索引建议。

## 故障排查指南
- 错误码定位：参考错误码常量，快速定位问题类型（参数错误、权限不足、接口限流、上游异常）
- 日志追踪：为每个请求生成唯一traceId，串联搜索、推广、订单、返利、提现全流程日志
- 回放与重试：对失败的回调与打款任务提供重试与回放机制
- 平台差异：针对不同平台的特殊字段与错误信息，准备差异化处理分支
- **推广位验证**：当出现推广位类型错误时，检查InEnum注解配置和广告位类型枚举定义
- **数据一致性**：推广位类型与关联类型不一致时，检查自动同步逻辑

**更新** 新增推广位验证相关的故障排查指导。

**章节来源**
- [CpsErrorCodeConstants.java](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsErrorCodeConstants.java)

## 结论
CPS联盟返利模块通过策略化平台适配器与标准化状态机，实现了多平台、多场景的一致体验；配合聚合SDK与官方API，既能快速扩展又能满足合规要求。完善的风控与提现体系保障了资金安全与用户体验。

**更新** 新增的广告位类型管理增强了系统的灵活性和业务适配能力，通过严格的验证机制确保推广位使用的安全性和准确性。推广位管理系统采用完整的分层架构设计，支持灵活的业务场景和良好的扩展性。

## 附录

### API接口文档与集成指南
- 聚合平台OpenAPI：参考"好单库OpenAPI接口文档"，了解商品搜索、优惠券、推广链接等接口规范
- SDK示例：参考"大淘客Java SDK示例"，学习初始化、认证、调用与回调处理
- Swagger文档：可在SDK示例工程中启用Swagger，查看接口清单与参数说明
- **推广位管理API**：参考CpsAdzoneController提供的RESTful接口，了解推广位的创建、更新、查询和分页功能

**更新** 新增推广位管理API的集成指导。

**章节来源**
- [好单库OpenAPI接口文档.md](file://docs/好单库OpenAPI接口文档.md)
- [README.md](file://agent_improvement/sdk_demo/dataoke-sdk-java/README.md)
- [SwaggerConfiguration.java](file://agent_improvement/sdk_demo/dataoke-sdk-java/src/main/java/com/dtk/api/controller/config/SwaggerConfiguration.java)
- [CpsAdzoneController.java:1-84](file://backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/adzone/CpsAdzoneController.java#L1-L84)

### 配置示例与最佳实践
- 平台接入：在平台枚举中新增平台编码，并在适配器中实现对应方法
- 供应商选择：优先使用官方API以保证稳定性，聚合API用于快速扩展
- 状态管理：严格遵循订单与返利状态机，避免状态错乱
- 风控策略：结合业务场景动态调整规则阈值，定期评估效果
- **推广位类型**：根据业务需求选择合适的广告位类型，通用类型适用于大多数场景，渠道专属和用户专属类型用于精细化运营
- **验证规则**：确保InEnum注解正确配置，推广位类型与关联类型保持一致
- **数据模型**：合理设计推广位数据模型，支持业务扩展和性能优化

**更新** 新增推广位类型的最佳实践指导。

**章节来源**
- [CpsPlatformCodeEnum.java:1-47](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsPlatformCodeEnum.java#L1-L47)
- [CpsVendorCodeEnum.java:1-52](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsVendorCodeEnum.java#L1-L52)
- [CpsAdzoneTypeEnum.java:1-40](file://backend/qiji-module-cps/qiji-module-cps-api/src/main/java/com/qiji/cps/module/cps/enums/CpsAdzoneTypeEnum.java#L1-L40)