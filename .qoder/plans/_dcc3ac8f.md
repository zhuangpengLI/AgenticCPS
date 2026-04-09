# CPS 多供应商 + 多电商平台 适配器架构全面改造方案

## 一、现状分析与核心问题

### 1.1 当前架构

```
CpsPlatformClient (接口)
├── TaobaoPlatformClientAdapter  -- 硬编码大淘客 API，~336 行
├── JdPlatformClientAdapter      -- 硬编码大淘客 API，~292 行
├── PddPlatformClientAdapter     -- 硬编码大淘客 API，~320 行
└── DouyinPlatformClientAdapter  -- 桩实现，~64 行
```

### 1.2 核心问题

| 问题 | 具体表现 |
|------|----------|
| 概念耦合 | 「电商平台」与「API 供应商」1:1 绑定，无法切换大淘客到好单库 |
| 大量重复代码 | doGet/签名/isSuccess/parseDecimal/buildEmptyResult 在 3 个适配器中几乎相同（~180 行重复） |
| BASE_URL 硬编码 | 三个适配器都硬编码 `https://openapi.dataoke.com/api`，忽略了 CpsPlatformDO.apiBaseUrl |
| DtkOpenApiClient 闲置 | 已有通用 HTTP 客户端但未被适配器使用 |
| 不支持官方 API | 无法对接淘宝联盟、京东联盟等官方 API（认证方式完全不同） |
| 平台覆盖不足 | 缺少唯品会、美团等电商平台支持 |
| 无容错机制 | 单一供应商，无降级/兜底策略 |

### 1.3 目标架构（M x N 矩阵 + 降级容错）

**电商平台 x API 供应商能力矩阵**：

| 电商平台 | 大淘客 | 好单库 | 喵有卷 | 实惠猪 | 官方 API |
|---------|--------|-------|--------|-------|----------|
| 淘宝 | Y | Y | Y | Y | Y（淘宝联盟/阿里妈妈） |
| 京东 | Y | Y | Y | Y | Y（京东联盟） |
| 拼多多 | Y | Y | Y | Y | Y（多多进宝） |
| 抖音 | Y | Y | - | - | Y（精选联盟，需企业资质） |
| 唯品会 | Y | - | - | - | Y（唯品会联盟，需企业资质） |
| 美团 | - | Y | - | - | Y（美团联盟，需企业资质） |

管理员在后台配置每个电商平台使用哪个 API 供应商，支持一键切换和降级备选。

---

## 二、目标类结构设计

```
client/
|-- CpsPlatformClient.java                     # [接口不变] 策略接口
|-- CpsPlatformClientFactory.java              # [重构] 供应商感知 + 降级路由工厂
|-- dto/                                       # [扩展] 现有 DTO + 新增万能解析 DTO
|
|-- common/                                    # [新增] 公共抽象层
|   |-- AbstractPlatformClientAdapter.java     # 顶层抽象基类（通用工具方法）
|   |-- AbstractAggregationClient.java         # 聚合平台抽象基类（HTTP + JSON 解析模板）
|   +-- AbstractOfficialClient.java            # 官方 API 抽象基类（OAuth/Token 认证模板）
|
|-- aggregation/                               # [聚合平台供应商]
|   |-- dataoke/                               # 大淘客（重构现有代码）
|   |   |-- DtkApiClient.java                  # [增强] 大淘客统一 HTTP 客户端（MD5 签名）
|   |   |-- AbstractDtkPlatformClient.java     # 大淘客抽象基类
|   |   |-- DtkTaobaoPlatformClient.java       # [重构自 TaobaoPlatformClientAdapter]
|   |   |-- DtkJdPlatformClient.java           # [重构自 JdPlatformClientAdapter]
|   |   |-- DtkPddPlatformClient.java          # [重构自 PddPlatformClientAdapter]
|   |   |-- DtkDouyinPlatformClient.java       # [新增] 大淘客抖音适配
|   |   +-- DtkVipPlatformClient.java          # [新增] 大淘客唯品会适配
|   |
|   |-- haodanku/                              # 好单库
|   |   |-- HdkApiClient.java                  # 好单库 HTTP 客户端（apikey 鉴权）
|   |   |-- AbstractHdkPlatformClient.java     # 好单库抽象基类
|   |   |-- HdkTaobaoPlatformClient.java
|   |   |-- HdkJdPlatformClient.java
|   |   |-- HdkPddPlatformClient.java
|   |   |-- HdkDouyinPlatformClient.java
|   |   +-- HdkMeituanPlatformClient.java     # [新增] 好单库美团适配
|   |
|   |-- miaoyouquan/                           # 喵有卷（预留 + 骨架实现）
|   +-- shihuizhu/                             # 实惠猪（预留 + 骨架实现）
|
|-- official/                                  # [官方联盟 API]
|   |-- taobao/TbAllianceClient.java           # 淘宝联盟（TOP 协议, HMAC-SHA256）
|   |-- jd/JdAllianceClient.java               # 京东联盟（API Key + MD5）
|   |-- pdd/PddAllianceClient.java             # 多多进宝（client_id + MD5）
|   |-- douyin/DyAllianceClient.java           # 抖音精选联盟（需企业资质，桩实现）
|   |-- vip/VipAllianceClient.java             # 唯品会联盟（需企业资质，桩实现）
|   +-- meituan/MtAllianceClient.java          # 美团联盟（需企业资质，桩实现）
+--
```

---

## 三、改造任务分解

### Task 1: 定义 API 供应商枚举与类型

**新增文件**: `yudao-module-cps-api/.../enums/CpsApiProviderEnum.java`

```java
@Getter @AllArgsConstructor
public enum CpsApiProviderEnum implements ArrayValuable<String> {
    // === 聚合平台 ===
    DATAOKE("dataoke", "大淘客", ProviderType.AGGREGATION),
    HAODANKU("haodanku", "好单库", ProviderType.AGGREGATION),
    MIAOYOUQUAN("miaoyouquan", "喵有卷", ProviderType.AGGREGATION),
    SHIHUIZHU("shihuizhu", "实惠猪", ProviderType.AGGREGATION),
    // === 官方联盟 API ===
    TAOBAO_OFFICIAL("taobao_official", "淘宝联盟官方", ProviderType.OFFICIAL),
    JD_OFFICIAL("jd_official", "京东联盟官方", ProviderType.OFFICIAL),
    PDD_OFFICIAL("pdd_official", "多多进宝官方", ProviderType.OFFICIAL),
    DOUYIN_OFFICIAL("douyin_official", "抖音精选联盟官方", ProviderType.OFFICIAL),
    VIP_OFFICIAL("vip_official", "唯品会联盟官方", ProviderType.OFFICIAL),
    MEITUAN_OFFICIAL("meituan_official", "美团联盟官方", ProviderType.OFFICIAL),
    ;
    private final String code;
    private final String name;
    private final ProviderType type;
    public enum ProviderType { AGGREGATION, OFFICIAL }
}
```

**扩展 CpsPlatformCodeEnum**：新增 `VIP("vip", "唯品会联盟")` 和 `MEITUAN("meituan", "美团联盟")`。

---

### Task 2: 扩展 CpsPlatformDO 数据模型

**修改文件**: `CpsPlatformDO.java` 新增字段：

```java
private String providerCode;           // API 供应商编码，默认 "dataoke"
private String providerType;           // 供应商类型 AGGREGATION / OFFICIAL
private String authMethod;             // 认证方式 MD5/APIKEY/OAUTH2/TOKEN/HMAC_SHA256
private String fallbackProviderCode;   // 降级备选供应商编码
```

**数据库变更**：

```sql
ALTER TABLE cps_platform
  ADD COLUMN provider_code VARCHAR(32) DEFAULT 'dataoke' COMMENT 'API供应商编码' AFTER platform_code,
  ADD COLUMN provider_type VARCHAR(16) DEFAULT 'AGGREGATION' COMMENT '供应商类型' AFTER provider_code,
  ADD COLUMN auth_method VARCHAR(32) DEFAULT 'MD5' COMMENT '认证方式' AFTER provider_type,
  ADD COLUMN fallback_provider_code VARCHAR(32) DEFAULT NULL COMMENT '降级备选供应商' AFTER auth_method;
UPDATE cps_platform SET provider_code='dataoke', provider_type='AGGREGATION', auth_method='MD5' WHERE provider_code IS NULL;
ALTER TABLE cps_platform DROP INDEX IF EXISTS idx_platform_code;
ALTER TABLE cps_platform ADD UNIQUE INDEX uk_platform_provider (platform_code, provider_code, tenant_id, deleted);
```

---

### Task 3: 提取顶层公共抽象基类

**新增**: `client/common/AbstractPlatformClientAdapter.java`

提取 3 个适配器共享的 ~180 行重复工具代码：`getPlatformConfig()`、`buildEmptyResult()`、`parseDecimal()`、`parseSafeDecimal()`、`isSuccessCode()`、ObjectMapper 初始化等。

子类须实现：`getProviderCode()`、`getProviderType()`、`getLogPrefix()`。

---

### Task 4: 创建聚合平台抽象基类

**新增**: `client/common/AbstractAggregationClient.java`

聚合平台通用能力：`buildGetUrl()`、`executeGet()`、抽象 `doGet()`/`isSuccess()`。providerType 固定为 AGGREGATION。

---

### Task 5: 创建大淘客供应商基类

**新增**: `client/aggregation/dataoke/AbstractDtkPlatformClient.java`

统一提取大淘客 MD5 签名 + HTTP 请求逻辑（从 CpsPlatformDO.apiBaseUrl 读取 URL，不再硬编码），以及通用的 `testConnection()` 模板方法。

子类仅需提供：`getPlatformCode()`、`getLogPrefix()`、`getTestPath()`/`getTestParams()`，以及平台特有的参数映射和响应解析。

**代码复用效果**：每个大淘客适配器从 ~300 行 -> ~120-150 行。

---

### Task 6: 重构现有 3 个大淘客适配器

将 `TaobaoPlatformClientAdapter` / `JdPlatformClientAdapter` / `PddPlatformClientAdapter` 迁移到 `aggregation/dataoke/` 包，继承 `AbstractDtkPlatformClient`，仅保留平台特有的参数映射和响应解析逻辑。

旧类标记 `@Deprecated` 并移除 `@Component`，避免重复注册。

---

### Task 7: 创建好单库供应商基类 + 适配器

- `HdkApiClient.java`：好单库 HTTP 客户端（apikey 鉴权，无签名计算）
- `AbstractHdkPlatformClient.java`：好单库抽象基类（doGet 中 `params.put("apikey", appKey)`）
- 具体适配器：`HdkTaobaoPlatformClient`、`HdkJdPlatformClient`、`HdkPddPlatformClient` 等

好单库成功码：code=1 或 code=200。

---

### Task 8: 创建官方 API 抽象基类

**新增**: `client/common/AbstractOfficialClient.java`

官方 API 通用能力：`refreshAccessToken()`、`isTokenExpired()`、`doPost()` 模板。providerType 固定为 OFFICIAL。

各官方平台认证方式：

| 官方平台 | 认证方式 | 签名算法 | 备注 |
|---------|---------|---------|------|
| 淘宝联盟 | TOP 协议 | HMAC-SHA256 | 个人可申请 |
| 京东联盟 | API Key + Secret Key | MD5 | 个人可申请 |
| 多多进宝 | client_id + client_secret | MD5 | 个人可申请，最友好 |
| 抖音精选联盟 | OAuth2 + AppId | SHA256 | 需企业资质 |
| 唯品会联盟 | Token | HMAC-SHA256 | 需企业资质 |
| 美团联盟 | API Key | SHA256 | 需企业资质 |

---

### Task 9: 官方 API 适配器实现

- 淘宝联盟：`TbAllianceClient.java`（TOP 协议 HMAC-SHA256 签名）
- 京东联盟：`JdAllianceClient.java`
- 多多进宝：`PddAllianceClient.java`
- 抖音/唯品会/美团：桩实现（需企业资质，方法体 return null/empty）

---

### Task 10: 增强 CpsPlatformClientFactory

重构注册机制为组合键 `"platformCode:providerCode"` 路由：

- `getClient(platformCode)`：向后兼容，自动从 DB 读取 providerCode
- `getClient(platformCode, providerCode)`：明确指定供应商
- `getClientWithFallback(platformCode)`：主供应商不可用时自动降级
- `getAvailableProviders(platformCode)`：查询某平台所有已注册供应商

---

### Task 11: 配置管理 VO/API/前端适配

- VO 新增：providerCode, providerType, authMethod, fallbackProviderCode
- Service 新增：`getPlatformByCodeAndProvider(platformCode, providerCode)`
- 前端：平台管理页新增供应商选择、类型标签、降级配置

---

### Task 12: CpsPlatformClient 接口扩展（可选）

新增默认方法 `universalParse(String content)` 支持万能解析（输入任意商品链接/口令，返回商品信息 + 推广链接）。default 返回 null，具体适配器按需实现。

---

## 四、实施路线图

### Phase 1: 基础架构（不改变外部行为）

| Task | 内容 | 预估 |
|------|------|------|
| Task 1 | 创建 CpsApiProviderEnum 枚举 | 0.5h |
| Task 3 | 提取 AbstractPlatformClientAdapter 顶层基类 | 1h |
| Task 4 | 创建 AbstractAggregationClient 聚合基类 | 0.5h |
| Task 5 | 创建 AbstractDtkPlatformClient 大淘客基类 | 1h |
| Task 6 | 重构现有 3 个大淘客适配器继承新基类 | 2h |
| 验证 | 运行现有单元测试，确保行为不变 | 0.5h |

成果：代码量从 ~950 行减少到 ~500 行，消除 ~450 行重复代码。

### Phase 2: 多供应商架构（核心升级）

| Task | 内容 | 预估 |
|------|------|------|
| Task 2 | 扩展 CpsPlatformDO + 数据库迁移 | 1h |
| Task 10 | 重构 CpsPlatformClientFactory | 1.5h |
| Task 11 | 更新 VO/API/前端配置 | 2h |
| Task 1 补充 | 扩展 CpsPlatformCodeEnum (VIP/MEITUAN) | 0.5h |
| 验证 | 集成测试 + 回归测试 | 1h |

### Phase 3: 新聚合平台接入

| Task | 内容 | 预估 |
|------|------|------|
| Task 7 | 好单库基类 + 淘宝/京东/拼多多适配器 | 3h |
| 喵有卷 | 喵有卷基类 + 适配器（骨架） | 1.5h |
| 实惠猪 | 实惠猪基类 + 适配器（骨架） | 1.5h |

### Phase 4: 官方 API 接入

| Task | 内容 | 预估 |
|------|------|------|
| Task 8 | 创建 AbstractOfficialClient 基类 | 1h |
| Task 9 | 淘宝/京东/多多进宝官方适配器 | 7h |
| 桩实现 | 抖音/唯品会/美团（需企业资质） | 1h |
| Task 12 | 万能解析接口（可选） | 1h |

### Phase 5: 降级容错 + 测试完善

| Task | 内容 | 预估 |
|------|------|------|
| 降级路由 | Factory 降级逻辑完善 | 1h |
| 单元测试 | 所有新适配器测试 | 3h |
| 集成测试 | 端到端联调 | 2h |

---

## 五、设计模式总结

| 模式 | 应用位置 | 作用 |
|------|---------|------|
| 策略模式 | CpsPlatformClient 接口 | 统一 API 契约，适配器可替换 |
| 模板方法 | AbstractDtkPlatformClient / AbstractHdkPlatformClient | 固定签名+请求流程，子类定制参数映射和响应解析 |
| 工厂模式 | CpsPlatformClientFactory | 根据 (platformCode, providerCode) 动态路由 |
| 组合模式 | 三级继承 (Abstract -> Aggregation/Official -> Concrete) | 最大化代码复用 |
| 开闭原则 | @Component 自动注册 | 新增供应商/平台零修改核心代码 |
| 降级模式 | Factory.getClientWithFallback() | 主供应商不可用自动切换备选 |

---

## 六、扩展操作速查

**新增聚合供应商**（如"折淘客"）：枚举新增 -> 创建基类(鉴权) -> 创建适配器(@Component) -> 后台配置 -> 零改核心代码

**新增电商平台**（如"快手"）：枚举新增 -> 对应供应商目录创建适配器 -> DB 插入配置 -> 零改核心代码

**切换供应商**（如拼多多从大淘客切到好单库）：后台修改 providerCode -> 工厂自动路由 -> 业务代码零改动

---

## 七、测试策略

### 单元测试（每个适配器必测）

平台配置不存在返回空/null、参数映射正确性、响应解析正确性、HTTP 异常不抛出、testConnection 逻辑验证。

### 工厂路由测试

getClient("pdd") 按配置返回对应供应商适配器、getClientWithFallback 自动降级、getAvailableProviders 返回正确列表。

### 集成测试

管理后台创建/切换供应商配置、商品搜索跨供应商切换验证、降级容错模拟。
