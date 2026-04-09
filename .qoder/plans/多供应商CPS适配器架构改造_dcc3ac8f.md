# 多供应商 CPS 适配器架构改造方案

## 现状分析

### 当前架构（1:1 绑定）

```
CpsPlatformClient (接口)
├── TaobaoPlatformClientAdapter  ─── 硬编码大淘客 API
├── JdPlatformClientAdapter      ─── 硬编码大淘客 API
├── PddPlatformClientAdapter     ─── 硬编码大淘客 API
└── DouyinPlatformClientAdapter  ─── 桩实现
```

**核心问题**：「电商平台」和「API 供应商」概念耦合。`PddPlatformClientAdapter` 既代表"拼多多平台"又代表"大淘客供应商"，无法切换到好单库等其他供应商。

### 目标架构（M:N 解耦）

```
电商平台 (taobao/jd/pdd)  ×  API供应商 (dataoke/haodanku/miaoyouquan/shihuizhu)
```

同一电商平台可以通过不同 API 供应商对接，管理员在后台配置选择使用哪个供应商。

---

## 改造后的类结构

```
client/
├── CpsPlatformClient.java                    # [不变] 策略接口
├── CpsPlatformClientFactory.java             # [增强] 供应商感知的路由工厂
├── dto/                                      # [不变] 现有 DTO
│
├── common/                                   # [新增] 公共抽象层
│   ├── AbstractPlatformClientAdapter.java    # 顶层抽象基类 (通用工具方法)
│   └── CpsApiProviderEnum.java              # API 供应商枚举
│
├── dataoke/                                  # [重构] 大淘客供应商
│   ├── DtkOpenApiClient.java                # [增强] 统一 HTTP 客户端（签名+请求）
│   ├── AbstractDtkPlatformClient.java       # [新增] 大淘客抽象基类
│   ├── DtkTaobaoPlatformClient.java         # [重构自 TaobaoPlatformClientAdapter]
│   ├── DtkJdPlatformClient.java             # [重构自 JdPlatformClientAdapter]
│   └── DtkPddPlatformClient.java            # [重构自 PddPlatformClientAdapter]
│
├── haodanku/                                 # [新增] 好单库供应商
│   ├── HdkOpenApiClient.java               # 好单库 HTTP 客户端
│   ├── AbstractHdkPlatformClient.java       # 好单库抽象基类
│   └── HdkTaobaoPlatformClient.java         # 好单库淘宝适配器（示例）
│
├── miaoyouquan/                              # [新增] 喵有卷供应商（预留）
│   └── ...
│
├── shihuizhu/                                # [新增] 实惠猪供应商（预留）
│   └── ...
│
└── douyin/
    └── DouyinPlatformClientAdapter.java     # [不变] 桩实现
```

---

## Task 1: 创建 API 供应商枚举

**文件**: `yudao-module-cps-api/.../enums/CpsApiProviderEnum.java`

定义所有支持的 CPS API 供应商：

```java
public enum CpsApiProviderEnum {
    DATAOKE("dataoke", "大淘客"),
    HAODANKU("haodanku", "好单库"),
    MIAOYOUQUAN("miaoyouquan", "喵有卷"),
    SHIHUIZHU("shihuizhu", "实惠猪"),
    ;
    private final String code;
    private final String name;
}
```

---

## Task 2: 扩展 CpsPlatformDO 数据模型

**文件**: `yudao-module-cps-biz/.../dal/dataobject/platform/CpsPlatformDO.java`

新增 `providerCode` 字段，标识当前平台使用哪个 API 供应商：

```java
/**
 * API 供应商编码（如 dataoke、haodanku）
 * 枚举 {@link CpsApiProviderEnum}
 * 默认值: dataoke（兼容历史数据）
 */
private String providerCode;
```

**数据库变更**（`cps_platform` 表）：

```sql
ALTER TABLE cps_platform ADD COLUMN provider_code VARCHAR(32) DEFAULT 'dataoke' 
  COMMENT 'API供应商编码' AFTER platform_code;
-- 历史数据兼容
UPDATE cps_platform SET provider_code = 'dataoke' WHERE provider_code IS NULL;
```

---

## Task 3: 提取顶层抽象基类

**文件**: `client/common/AbstractPlatformClientAdapter.java`

提取所有适配器共享的通用工具方法，消除 ~80% 的重复代码：

```java
public abstract class AbstractPlatformClientAdapter implements CpsPlatformClient {

    protected final ObjectMapper objectMapper;
    
    @Resource
    protected CpsPlatformService platformService;

    protected AbstractPlatformClientAdapter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** 获取供应商编码（子类实现） */
    public abstract String getProviderCode();

    /** 日志前缀（如 "[淘宝-大淘客适配器]"） */
    protected abstract String getLogPrefix();

    // --- 以下方法从现有适配器中提取 ---
    
    protected CpsPlatformDO getPlatformConfig() { ... }      // 通用配置获取+日志
    protected CpsGoodsSearchResult buildEmptyResult(...) { ... }  // 空结果构建
    protected BigDecimal parseDecimal(JsonNode, String) { ... }   // 安全数值解析
    protected BigDecimal parseSafeDecimal(String) { ... }         // 字符串数值解析
    protected boolean isSuccessCode(JsonNode, String expectedCode) { ... } // 响应码校验
}
```

---

## Task 4: 创建大淘客抽象基类

**文件**: `client/dataoke/AbstractDtkPlatformClient.java`

将三个适配器共享的大淘客签名、HTTP 请求逻辑提取到此处：

```java
public abstract class AbstractDtkPlatformClient extends AbstractPlatformClientAdapter {

    @Override
    public String getProviderCode() {
        return CpsApiProviderEnum.DATAOKE.getCode();
    }

    /**
     * 大淘客统一 GET 请求（MD5签名）
     * 从 CpsPlatformDO.apiBaseUrl 读取基础 URL（不再硬编码）
     */
    protected JsonNode doGet(CpsPlatformDO platform, String path, Map<String, Object> params) {
        String baseUrl = platform.getApiBaseUrl(); // 关键改动：从配置读取
        String appKey = platform.getAppKey();
        String appSecret = platform.getAppSecret();
        
        // 签名逻辑（原 doGet 内容，提取一次）
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.format("%06d", new Random().nextInt(1000000));
        String sign = DigestUtil.md5Hex(
            String.format("appKey=%s&timer=%s&nonce=%s", appKey, timer, nonce) + appSecret
        ).toLowerCase();
        
        // 合并参数、构建URL、发起请求...
    }

    /** 大淘客响应成功判断: code == "0" */
    protected boolean isSuccess(JsonNode response) {
        return isSuccessCode(response, "0");
    }

    // 通用的 testConnection 模板（子类可覆盖测试路径和参数）
    @Override
    public boolean testConnection() { ... }
}
```

**代码复用效果**：三个适配器的 `doGet()`、`isSuccess()`、构造函数等 ~60 行重复代码被消除。

---

## Task 5: 重构现有大淘客适配器

将 `TaobaoPlatformClientAdapter`、`JdPlatformClientAdapter`、`PddPlatformClientAdapter` 迁移为继承 `AbstractDtkPlatformClient`。

### 5.1 PddPlatformClientAdapter 重构示例

**文件**: `client/dataoke/DtkPddPlatformClient.java`

```java
@Slf4j
@Component
public class DtkPddPlatformClient extends AbstractDtkPlatformClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.PDD.getCode();
    }

    @Override
    protected String getLogPrefix() {
        return "[拼多多-大淘客适配器]";
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        // 仅保留拼多多特有的参数映射和响应解析逻辑
        // doGet()、isSuccess()、buildEmptyResult() 等均调用基类方法
    }

    // parsePddGoodsItem()、parsePddOrder()、convertSortType() 等特有逻辑
}
```

**每个适配器减少代码量预估**：从 ~300 行 → ~150 行（去除重复的签名、HTTP、工具方法）

### 5.2 旧类保留与兼容

原 `taobao/`、`jd/`、`pdd/` 包下的旧类可选择：
- **方案 A（推荐）**：直接重命名移入 `dataoke/` 包，原位置保留 `@Deprecated` 的空壳类委托到新类
- **方案 B**：原地重构，包结构不变，仅修改继承关系

---

## Task 6: 增强 CpsPlatformClientFactory 支持多供应商路由

**文件**: `client/CpsPlatformClientFactory.java`

改造注册机制，从 `platformCode → adapter` 变为 `(platformCode, providerCode) → adapter`：

```java
@Component
public class CpsPlatformClientFactory {

    /** 组合键: "platformCode:providerCode" → 适配器 */
    private final Map<String, CpsPlatformClient> clientMap = new ConcurrentHashMap<>();
    
    /** 向后兼容: platformCode → 默认适配器（按 DB 配置的 providerCode 选择） */
    public CpsPlatformClient getClient(String platformCode) {
        CpsPlatformDO config = platformService.getPlatformByCode(platformCode);
        String providerCode = config != null ? config.getProviderCode() : "dataoke";
        String key = platformCode + ":" + providerCode;
        return clientMap.get(key);
    }

    /** 明确指定供应商获取 */
    public CpsPlatformClient getClient(String platformCode, String providerCode) {
        return clientMap.get(platformCode + ":" + providerCode);
    }

    @PostConstruct
    public void init() {
        for (CpsPlatformClient client : platformClients) {
            // 适配器需实现 getProviderCode()
            String key = client.getPlatformCode() + ":" + getProviderCode(client);
            clientMap.put(key, client);
            log.info("注册适配器: {} (供应商: {})", client.getPlatformCode(), getProviderCode(client));
        }
    }
}
```

**关键设计**：`getClient(platformCode)` 方法签名不变，内部根据 DB 配置的 `providerCode` 自动选择对应供应商的适配器，**对上层业务代码完全透明**。

---

## Task 7: 新增好单库供应商集成

### 7.1 好单库 HTTP 客户端

**文件**: `client/haodanku/HdkOpenApiClient.java`

```java
public class HdkOpenApiClient {
    /** 好单库鉴权：仅通过 apikey 参数，无需签名计算 */
    public JsonNode execute(String baseUrl, String path, String apiKey, Map<String, Object> params) {
        params.put("apikey", apiKey);
        // 构建 URL + GET 请求
    }
}
```

### 7.2 好单库抽象基类

**文件**: `client/haodanku/AbstractHdkPlatformClient.java`

```java
public abstract class AbstractHdkPlatformClient extends AbstractPlatformClientAdapter {
    
    @Override
    public String getProviderCode() {
        return CpsApiProviderEnum.HAODANKU.getCode();
    }
    
    protected JsonNode doGet(CpsPlatformDO platform, String path, Map<String, Object> params) {
        // 好单库的 HTTP 请求逻辑（apikey 鉴权，无 MD5 签名）
        String baseUrl = platform.getApiBaseUrl(); // http://v2.api.haodanku.com
        params.put("apikey", platform.getAppKey());
        // ...
    }
    
    /** 好单库响应成功判断: code == 1 */
    protected boolean isSuccess(JsonNode response) {
        return isSuccessCode(response, "1");
    }
}
```

### 7.3 好单库淘宝适配器（示例实现）

**文件**: `client/haodanku/HdkTaobaoPlatformClient.java`

```java
@Slf4j
@Component
public class HdkTaobaoPlatformClient extends AbstractHdkPlatformClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.TAOBAO.getCode();
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        CpsPlatformDO platform = getPlatformConfig();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("back", request.getPageSize());
        params.put("min_id", request.getPageNo());
        // 好单库特有的参数映射...
        
        JsonNode response = doGet(platform, "/item_search", params);
        // 好单库特有的响应解析...
    }
}
```

---

## Task 8: CpsPlatformCodeEnum 扩展

**文件**: `yudao-module-cps-api/.../enums/CpsPlatformCodeEnum.java`

如需新增纯好单库的电商平台（非淘宝/京东/拼多多），在枚举中新增：

```java
// 现有不变
TAOBAO("taobao", "淘宝联盟"),
JD("jd", "京东联盟"),
PDD("pdd", "拼多多联盟"),
DOUYIN("douyin", "抖音联盟"),
// 若好单库有独有的平台可按需新增
```

多数情况下枚举不需要变动，因为好单库等供应商对接的仍是淘宝/京东/拼多多。

---

## Task 9: 配置管理 VO/API 适配

需要同步更新以下文件，在创建/更新平台配置时支持 `providerCode` 字段：

- `CpsPlatformCreateReqVO` / `CpsPlatformUpdateReqVO` - 新增 providerCode 字段
- `CpsPlatformRespVO` - 新增 providerCode 字段
- `CpsPlatformController` - 无逻辑变更，VO 自动映射
- 前端 `admin-vue3` 平台配置表单 - 新增供应商下拉选择

---

## Task 10: 数据库迁移脚本

**文件**: `sql/cps/cps_platform_provider.sql`

```sql
-- 1. 新增 provider_code 字段
ALTER TABLE cps_platform ADD COLUMN provider_code VARCHAR(32) DEFAULT 'dataoke' 
  COMMENT 'API供应商编码(dataoke/haodanku/miaoyouquan/shihuizhu)' AFTER platform_code;

-- 2. 更新历史数据
UPDATE cps_platform SET provider_code = 'dataoke' WHERE provider_code IS NULL;

-- 3. 修改唯一索引为组合唯一（platformCode + providerCode）
-- 如原有 platform_code 唯一索引需调整为允许同平台多供应商
ALTER TABLE cps_platform DROP INDEX idx_platform_code;
ALTER TABLE cps_platform ADD UNIQUE INDEX uk_platform_provider (platform_code, provider_code, tenant_id);
```

---

## 实施顺序与依赖关系

```
Task 1 (供应商枚举) ──┐
Task 2 (数据模型扩展) ─┤
Task 10 (数据库迁移) ──┤
                      ├──▶ Task 3 (顶层抽象基类)
                      │        │
                      │        ▼
                      │    Task 4 (大淘客抽象基类)
                      │        │
                      │        ▼
                      │    Task 5 (重构现有适配器)
                      │        │
                      │        ▼
                      ├──▶ Task 6 (增强工厂路由)
                      │        │
                      │        ▼
                      ├──▶ Task 7 (好单库集成)
                      │
                      └──▶ Task 8 + 9 (枚举扩展 + VO/API 适配)
```

**推荐分批实施**：
- **第一批（代码质量）**: Task 1, 3, 4, 5 -- 提取基类、消除重复，不改变外部行为
- **第二批（架构升级）**: Task 2, 6, 8, 9, 10 -- 引入供应商路由，扩展数据模型
- **第三批（新平台接入）**: Task 7 -- 好单库实际接入

---

## 扩展性设计总结

### 新增供应商的步骤（如接入"实惠猪"）

1. 在 `CpsApiProviderEnum` 新增 `SHIHUIZHU("shihuizhu", "实惠猪")`
2. 创建 `client/shihuizhu/AbstractShzPlatformClient.java`（实现 HTTP 鉴权逻辑）
3. 为需要的平台创建适配器（如 `ShzPddPlatformClient.java`）
4. 标记 `@Component`，自动注册到工厂，**无需修改任何核心代码**
5. 在管理后台为 "pdd" 平台配置 `providerCode = "shihuizhu"`

### 设计模式总结

| 模式 | 应用位置 | 作用 |
|------|---------|------|
| 策略模式 | CpsPlatformClient 接口 | 平台适配器可替换 |
| 模板方法 | AbstractDtkPlatformClient | 固定签名+请求流程，子类定制参数映射 |
| 工厂模式 | CpsPlatformClientFactory | 动态路由到正确的适配器 |
| 开放-关闭原则 | @Component 自动注册 | 新增供应商零修改核心代码 |
