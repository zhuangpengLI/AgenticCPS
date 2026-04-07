---
name: project-architecture-standards
description: AgenticCPS 项目架构设计与编码规范。当用户要求生成业务代码、新增模块、创建 Service/Controller/Mapper/DO/VO、生成前端页面、设计数据表、或问到项目规范时触发。触发词包括："生成代码"、"新增功能"、"写接口"、"创建模块"、"generate code"、"add feature"、"create service"、"new module"。
license: MIT
metadata:
  author: AgenticCPS
  version: "1.0"
---

# AgenticCPS 项目架构设计与编码规范

当为 AgenticCPS 项目生成任何代码时，**必须**遵循以下规范。

---

## PART 1：项目整体架构

### 1.1 Maven 多模块结构（必须了解）

```
backend/
├── yudao-dependencies/          # 依赖版本统一管理（BOM）
├── yudao-framework/             # 框架扩展（各 starter）
│   ├── yudao-spring-boot-starter-web/       # Web 基础（CommonResult、分页等）
│   ├── yudao-spring-boot-starter-security/  # 安全认证
│   ├── yudao-spring-boot-starter-mybatis/   # MyBatis Plus 扩展
│   ├── yudao-spring-boot-starter-redis/     # Redis 扩展
│   └── yudao-spring-boot-starter-test/      # 测试基类
├── yudao-module-system/         # 系统管理（用户、角色、菜单、权限）
├── yudao-module-infra/          # 基础设施（文件、代码生成、定时任务）
├── yudao-module-member/         # 会员中心
├── yudao-module-pay/            # 支付系统
├── yudao-module-mall/           # 商城系统
├── yudao-module-ai/             # AI 大模型（Spring AI + MCP）
├── yudao-module-cps/            # CPS 返利核心模块（主战场）
│   ├── yudao-module-cps-api/    # API 定义（枚举、远程接口）
│   └── yudao-module-cps-biz/    # 业务实现
└── yudao-server/                # 主启动类
```

### 1.2 CPS 模块内部结构（最重要）

```
yudao-module-cps-biz/src/main/java/cn/zhijian/cps/
├── controller/
│   ├── admin/        # 管理端 REST API（路径前缀 /admin-api/cps/）
│   └── app/          # 会员端 REST API（路径前缀 /app-api/cps/）
├── service/          # 业务逻辑层（接口 + Impl）
├── dal/
│   ├── dataobject/   # DO（数据库实体，命名：Cps{Name}DO）
│   └── mysql/        # Mapper 接口（命名：Cps{Name}Mapper）
├── enums/            # 枚举类 + ErrorCodeConstants
├── convert/          # MapStruct 转换器（DO ↔ VO/DTO）
├── job/              # 定时任务（Quartz）
├── client/           # CPS 平台适配器（策略模式）
│   ├── CpsPlatformClient.java           # 统一接口
│   ├── AbstractCpsPlatformClient.java   # 抽象基类
│   ├── TaobaoCpsPlatformClient.java     # 淘宝适配器
│   ├── JingdongCpsPlatformClient.java   # 京东适配器
│   ├── PinduoduoCpsPlatformClient.java  # 拼多多适配器
│   └── DouyinCpsPlatformClient.java     # 抖音适配器
├── mcp/              # MCP AI 接口层
│   ├── tool/         # AI 可调用工具（@CpsMcpTool）
│   ├── resource/     # 只读数据资源（@CpsMcpResource）
│   └── prompt/       # 预定义提示词模板（@CpsMcpPrompt）
└── config/           # Spring 配置类
```

### 1.3 分层架构原则（必须遵守）

```
客户端 → Controller → Service → Mapper/DO → MySQL
                    ↓
              CpsPlatformClient（外部 API）
                    ↓
              Job（定时任务）
```

- **[RULE-ARCH-01]** [强制] Controller 只做参数校验、权限检查、调用 Service、封装 CommonResult，**禁止**在 Controller 写业务逻辑
- **[RULE-ARCH-02]** [强制] Service 负责业务编排、事务控制、异常转换，通过接口定义（不直接依赖 Impl）
- **[RULE-ARCH-03]** [强制] Mapper 只做数据库操作，**禁止**在 Mapper 写业务逻辑
- **[RULE-ARCH-04]** [强制] 跨模块调用必须通过 `*Api` 接口（在 `-api` 子模块中定义），**禁止**直接注入其他模块的 ServiceImpl

---

## PART 2：后端 Java 编码规范

### 2.1 命名规范（必须）

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 数据库实体 | `Cps{Name}DO` | `CpsPlatformDO` |
| Mapper 接口 | `Cps{Name}Mapper` | `CpsPlatformMapper` |
| Service 接口 | `Cps{Name}Service` | `CpsPlatformService` |
| Service 实现 | `Cps{Name}ServiceImpl` | `CpsPlatformServiceImpl` |
| Controller | `Cps{Name}Controller` | `CpsPlatformController` |
| 创建/更新请求 VO | `Cps{Name}SaveReqVO` | `CpsPlatformSaveReqVO` |
| 查询请求 VO | `Cps{Name}PageReqVO` | `CpsPlatformPageReqVO` |
| 响应 VO | `Cps{Name}RespVO` | `CpsPlatformRespVO` |
| MapStruct 转换器 | `Cps{Name}Convert` | `CpsPlatformConvert` |
| 定时任务 | `Cps{Name}Job` | `CpsOrderSyncJob` |
| 错误码 | 全大写下划线 | `PLATFORM_NOT_EXISTS` |

**[RULE-JAVA-01]** [强制] CPS 模块所有类必须以 `Cps` 前缀开头

### 2.2 Lombok 使用规范（必须）

```java
// DO 类（数据库实体）
@TableName("cps_platform")
@KeySequence("cps_platform_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformDO extends BaseDO { ... }

// VO/DTO 类（请求/响应）
@Schema(description = "CPS 平台配置 - 创建/修改 Request VO")
@Data
public class CpsPlatformSaveReqVO {
    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotEmpty(message = "平台编码不能为空")
    private String platformCode;
}
```

**[RULE-JAVA-02]** [强制] DO 类继承 `BaseDO`（含 id、createTime、updateTime、creator、updater、deleted、tenantId）
**[RULE-JAVA-03]** [强制] VO 类用 `@Schema` 注解描述字段，请求 VO 必填字段用 `@NotEmpty/@NotNull` 校验

### 2.3 Controller 编写规范（必须）

```java
@Tag(name = "管理后台 - CPS 平台配置")
@RestController
@RequestMapping("/admin-api/cps/platform")
@Validated
public class CpsPlatformController {

    @Resource
    private CpsPlatformService platformService;

    @PostMapping("/create")
    @Operation(summary = "创建平台配置")
    @PreAuthorize("@ss.hasPermission('cps:platform:create')")
    public CommonResult<Long> createPlatform(@RequestBody @Valid CpsPlatformSaveReqVO createReqVO) {
        return success(platformService.createPlatform(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新平台配置")
    @PreAuthorize("@ss.hasPermission('cps:platform:update')")
    public CommonResult<Boolean> updatePlatform(@RequestBody @Valid CpsPlatformSaveReqVO updateReqVO) {
        platformService.updatePlatform(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除平台配置")
    @PreAuthorize("@ss.hasPermission('cps:platform:delete')")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deletePlatform(@RequestParam("id") Long id) {
        platformService.deletePlatform(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取平台配置详情")
    @PreAuthorize("@ss.hasPermission('cps:platform:query')")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<CpsPlatformRespVO> getPlatform(@RequestParam("id") Long id) {
        CpsPlatformDO platform = platformService.getPlatform(id);
        return success(CpsPlatformConvert.INSTANCE.convert(platform));
    }

    @GetMapping("/page")
    @Operation(summary = "获取平台配置分页")
    @PreAuthorize("@ss.hasPermission('cps:platform:query')")
    public CommonResult<PageResult<CpsPlatformRespVO>> getPlatformPage(@Valid CpsPlatformPageReqVO pageReqVO) {
        PageResult<CpsPlatformDO> pageResult = platformService.getPlatformPage(pageReqVO);
        return success(CpsPlatformConvert.INSTANCE.convertPage(pageResult));
    }
}
```

**[RULE-CTRL-01]** [强制] 管理端路径前缀必须是 `/admin-api/cps/`，会员端是 `/app-api/cps/`
**[RULE-CTRL-02]** [强制] 每个接口必须有 `@PreAuthorize` 权限注解，权限标识格式 `cps:{resource}:{action}`
**[RULE-CTRL-03]** [强制] 返回值必须用 `CommonResult<T>` 封装，使用 `success(data)` 静态方法
**[RULE-CTRL-04]** [强制] DO 对象不能直接暴露给前端，必须通过 Convert 转换为 RespVO

### 2.4 Service 编写规范（必须）

```java
public interface CpsPlatformService {
    Long createPlatform(CpsPlatformSaveReqVO createReqVO);
    void updatePlatform(CpsPlatformSaveReqVO updateReqVO);
    void deletePlatform(Long id);
    CpsPlatformDO getPlatform(Long id);
    PageResult<CpsPlatformDO> getPlatformPage(CpsPlatformPageReqVO pageReqVO);
}

@Service
@Validated
public class CpsPlatformServiceImpl implements CpsPlatformService {

    @Resource
    private CpsPlatformMapper platformMapper;

    @Override
    public Long createPlatform(CpsPlatformSaveReqVO createReqVO) {
        // 校验平台编码唯一性
        validatePlatformCodeUnique(null, createReqVO.getPlatformCode());
        // 插入
        CpsPlatformDO platform = CpsPlatformConvert.INSTANCE.convert(createReqVO);
        platformMapper.insert(platform);
        return platform.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlatform(CpsPlatformSaveReqVO updateReqVO) {
        // 校验存在
        validatePlatformExists(updateReqVO.getId());
        // 校验编码唯一性
        validatePlatformCodeUnique(updateReqVO.getId(), updateReqVO.getPlatformCode());
        // 更新
        CpsPlatformDO updateObj = CpsPlatformConvert.INSTANCE.convert(updateReqVO);
        platformMapper.updateById(updateObj);
    }

    private void validatePlatformExists(Long id) {
        if (platformMapper.selectById(id) == null) {
            throw exception(PLATFORM_NOT_EXISTS);
        }
    }
}
```

**[RULE-SVC-01]** [强制] Service 实现类必须加 `@Service` 和 `@Validated`
**[RULE-SVC-02]** [强制] 涉及多表写操作必须加 `@Transactional(rollbackFor = Exception.class)`
**[RULE-SVC-03]** [强制] 业务异常必须使用 `throw exception(ERROR_CODE)` 方式抛出（来自 ServiceExceptionUtil）
**[RULE-SVC-04]** [强制] 每个 Service 必须有 `validateXxxExists(id)` 方法校验记录是否存在

### 2.5 Mapper 编写规范（必须）

```java
@Mapper
public interface CpsPlatformMapper extends BaseMapperX<CpsPlatformDO> {

    default PageResult<CpsPlatformDO> selectPage(CpsPlatformPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsPlatformDO>()
                .likeIfPresent(CpsPlatformDO::getPlatformName, reqVO.getPlatformName())
                .eqIfPresent(CpsPlatformDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CpsPlatformDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsPlatformDO::getId));
    }

    default CpsPlatformDO selectByPlatformCode(String platformCode) {
        return selectOne(CpsPlatformDO::getPlatformCode, platformCode);
    }
}
```

**[RULE-MAP-01]** [强制] Mapper 继承 `BaseMapperX<DO>`，享受内置分页、条件查询能力
**[RULE-MAP-02]** [强制] 分页查询用 `LambdaQueryWrapperX`，条件字段用 `IfPresent` 系列方法（值为 null 时自动忽略该条件）
**[RULE-MAP-03]** [建议] 复杂 SQL 用 XML 文件，简单条件查询用 Lambda 方式写在 default 方法里

### 2.6 错误码规范（必须）

```java
// CPS 模块错误码定义在 ErrorCodeConstants.java
public interface ErrorCodeConstants {
    // ========== 平台配置 1-006-001-xxx ==========
    ErrorCode PLATFORM_NOT_EXISTS         = new ErrorCode(1_006_001_000, "平台配置不存在");
    ErrorCode PLATFORM_CODE_EXISTS        = new ErrorCode(1_006_001_001, "平台编码已存在");
    ErrorCode PLATFORM_DISABLED           = new ErrorCode(1_006_001_002, "平台已被禁用");
    ErrorCode PLATFORM_CONNECT_FAIL       = new ErrorCode(1_006_001_003, "平台连接测试失败");

    // ========== 订单管理 1-006-003-xxx ==========
    ErrorCode ORDER_NOT_EXISTS            = new ErrorCode(1_006_003_000, "订单不存在");
    ErrorCode ORDER_STATUS_ILLEGAL        = new ErrorCode(1_006_003_002, "订单状态不合法，当前状态：{}");
}
```

**[RULE-ERR-01]** [强制] 错误码格式：`1_006_{模块编号三位}_{序号三位}`，CPS 模块段为 `1_006`
**[RULE-ERR-02]** [强制] 错误码消息支持 `{}` 占位符，通过 `exception(ERROR_CODE, param1, param2)` 传参
**[RULE-ERR-03]** [强制] 禁止使用 HTTP 状态码作为业务错误码（如不能用 404、500 表示业务错误）

### 2.7 金额与数值规范（必须）

**[RULE-MONEY-01]** [强制] **所有金额字段必须用 `Integer` 类型，单位为分（人民币：1 元 = 100 分）**
**[RULE-MONEY-02]** [强制] **禁止使用 `Double`、`Float`、`BigDecimal` 存储金额**
**[RULE-MONEY-03]** [强制] 返利比例（百分比）用 Integer 存储，例如 5% 存储为 500（基点 bps，1 bps = 0.01%）

```java
// 正确
private Integer rebateAmount;   // 返利金额，单位：分
private Integer commissionRate; // 佣金比例，单位：bps（500 = 5%）

// 错误（禁止）
private Double rebateAmount;
private BigDecimal commissionRate;
```

### 2.8 多租户与软删除（必须）

**[RULE-TENANT-01]** [强制] CPS 模块所有表均有 `tenant_id` 字段（继承自 `BaseDO` 的多租户扩展），查询时框架自动注入
**[RULE-DELETE-01]** [强制] 所有业务数据使用软删除（`deleted` 字段），**禁止** `DELETE` 语句物理删除 CPS 数据

---

## PART 3：CPS 核心模块特殊架构规范

### 3.1 平台适配器（策略模式）

```java
// 新增平台时，只需实现此接口，无需修改核心逻辑
public interface CpsPlatformClient {
    String getPlatformCode();                                          // 平台编码
    CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest req);      // 商品搜索
    CpsGoodsDetail getGoodsDetail(CpsGoodsDetailRequest req);         // 商品详情
    CpsParsedContent parseContent(String content);                     // 链接解析
    CpsPromotionLink generatePromotionLink(CpsPromotionLinkRequest req); // 推广链接
    List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest req);          // 订单查询
    boolean testConnection();                                          // 连通性测试
}
```

**[RULE-CPS-01]** [强制] 新增 CPS 平台必须实现 `CpsPlatformClient` 接口并继承 `AbstractCpsPlatformClient`
**[RULE-CPS-02]** [强制] 新增平台只需注册为 Spring Bean，工厂类（`CpsPlatformClientFactoryImpl`）自动发现，**禁止**修改工厂类核心逻辑
**[RULE-CPS-03]** [强制] 平台 API 签名逻辑统一通过 `CpsApiSignUtil` 处理，不在适配器类内散写

### 3.2 返利计算优先级（必须了解）

系统按以下优先级顺序解析返利比例（高优先级覆盖低优先级）：

```
1. 会员个人配置（指定平台）
2. 会员个人配置（所有平台）
3. 会员等级 + 平台
4. 会员等级（所有平台）
5. 平台默认配置
6. 全局默认配置
```

**[RULE-CPS-04]** [强制] 编写返利相关逻辑时，必须按上述优先级顺序查找，不可跳级

### 3.3 订单状态流转（必须了解）

```
已下单 → 已付款 → 已收货 → 已结算 → 已到账
              ↓
          已退款 / 已失效
```

**[RULE-CPS-05]** [强制] 订单状态变更必须按照上述流程，不允许逆向流转（如已到账不能改回已结算）

### 3.4 MCP AI 接口层规范

```java
// Tool：AI 可调用的功能性接口（写操作/查询）
@Component
public class CpsGoodsSearchMcpTool {
    @CpsMcpTool(name = "searchGoods", description = "搜索商品并获取返利信息")
    public CpsGoodsSearchResult searchGoods(String keyword, String platform) { ... }
}

// Resource：AI 可读取的只读数据资源
@Component
public class CpsPlatformConfigResource {
    @CpsMcpResource(uri = "cps://platform/configs", description = "获取所有平台配置信息")
    public List<CpsPlatformRespVO> getPlatformConfigs() { ... }
}
```

**[RULE-MCP-01]** [强制] MCP Tool 只暴露有业务意义的接口，**禁止**直接暴露 CRUD 操作
**[RULE-MCP-02]** [强制] MCP 接口的入参和出参必须有清晰的 `description`，便于 AI 理解
**[RULE-MCP-03]** [强制] MCP 接口必须做权限校验，使用 API Key 机制（`cps_mcp_api_key` 表）

---

## PART 4：数据库表设计规范

### 4.1 表命名与字段规范

```sql
CREATE TABLE `cps_platform` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(50) NOT NULL                COMMENT '平台编码',
  `platform_name` varchar(100) NOT NULL               COMMENT '平台名称',
  `status`      tinyint      NOT NULL DEFAULT 0       COMMENT '状态（0=正常,1=禁用）',
  `config`      text                                  COMMENT '平台配置（JSON）',
  `creator`     varchar(64)  NOT NULL DEFAULT ''      COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  NOT NULL DEFAULT ''      COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)       NOT NULL DEFAULT b'0'    COMMENT '是否删除',
  `tenant_id`   bigint       NOT NULL DEFAULT 0       COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_code` (`platform_code`, `tenant_id`)
) ENGINE=InnoDB COMMENT='CPS 平台配置';
```

**[RULE-DB-01]** [强制] CPS 模块所有表名以 `cps_` 为前缀
**[RULE-DB-02]** [强制] 所有表必须有以下标准字段：`id`(bigint PK)、`creator`、`create_time`、`updater`、`update_time`、`deleted`(bit)、`tenant_id`(bigint)
**[RULE-DB-03]** [强制] 主键使用 `bigint AUTO_INCREMENT`，**禁止**用 UUID 或字符串主键
**[RULE-DB-04]** [强制] 所有字段必须有 `COMMENT`，枚举值需在注释中说明含义
**[RULE-DB-05]** [强制] 金额字段用 `int`（分），比例字段用 `int`（bps），**禁止**用 `decimal`

### 4.2 索引策略

**[RULE-DB-06]** [强制] `tenant_id` 必须包含在所有查询索引中（联合索引时放在第一位）
**[RULE-DB-07]** [建议] 状态字段（status）通常不单独建索引，与其他字段建联合索引
**[RULE-DB-08]** [强制] 时间范围查询字段（`create_time`、`update_time`）需建索引

```sql
-- 推荐索引模式
KEY `idx_tenant_status` (`tenant_id`, `status`),
KEY `idx_tenant_create_time` (`tenant_id`, `create_time`),
UNIQUE KEY `uk_tenant_code` (`tenant_id`, `platform_code`)
```

---

## PART 5：API 接口设计规范

### 5.1 RESTful 风格（必须）

| 操作 | HTTP 方法 | 路径示例 | 说明 |
|------|----------|---------|------|
| 创建 | POST | `/admin-api/cps/platform/create` | |
| 更新 | PUT | `/admin-api/cps/platform/update` | |
| 删除 | DELETE | `/admin-api/cps/platform/delete?id=1` | |
| 查询单个 | GET | `/admin-api/cps/platform/get?id=1` | |
| 分页查询 | GET | `/admin-api/cps/platform/page` | |
| 列表查询 | GET | `/admin-api/cps/platform/list` | 不分页 |
| 导出 | GET | `/admin-api/cps/platform/export-excel` | |

**[RULE-API-01]** [强制] 分页查询参数必须包含 `pageNo`（从 1 开始）和 `pageSize`（最大 100）
**[RULE-API-02]** [强制] 删除/查询单个使用 `@RequestParam("id")` 传参，**禁止**放在路径变量（避免 `/delete/1` 风格）

### 5.2 统一响应格式（必须）

```json
// 成功响应
{ "code": 0, "msg": "", "data": { ... } }

// 错误响应
{ "code": 1006001000, "msg": "平台配置不存在", "data": null }

// 分页响应
{
  "code": 0, "msg": "",
  "data": { "list": [...], "total": 100 }
}
```

**[RULE-API-03]** [强制] `code=0` 代表成功，非 0 代表业务错误
**[RULE-API-04]** [强制] 分页数据必须用 `PageResult<T>` 封装（含 `list` 和 `total` 字段）

---

## PART 6：前端编码规范（Vue3 + TypeScript）

### 6.1 目录结构（admin-vue3）

```
src/
├── views/cps/                  # CPS 模块页面
│   ├── platform/               # 平台配置
│   │   ├── index.vue           # 列表页
│   │   └── PlatformForm.vue    # 表单组件（Dialog 弹窗）
│   ├── order/                  # 订单管理
│   └── ...
├── api/cps/                    # CPS 接口封装
│   ├── platform.ts             # 平台配置 API
│   └── order.ts                # 订单 API
└── types/cps/                  # CPS 类型定义（可选，由 api 文件内联）
```

### 6.2 API 文件规范（必须）

```typescript
// api/cps/platform.ts
import request from '@/config/axios'

// 类型定义
export interface CpsPlatformVO {
  id: number
  platformCode: string
  platformName: string
  status: number
  createTime: Date
}

export interface CpsPlatformPageReqVO extends PageParam {
  platformName?: string
  status?: number
  createTime?: [Date, Date]
}

// API 函数
export const CpsPlatformApi = {
  createPlatform: (data: CpsPlatformVO) =>
    request.post({ url: '/admin-api/cps/platform/create', data }),

  updatePlatform: (data: CpsPlatformVO) =>
    request.put({ url: '/admin-api/cps/platform/update', data }),

  deletePlatform: (id: number) =>
    request.delete({ url: '/admin-api/cps/platform/delete', params: { id } }),

  getPlatform: (id: number) =>
    request.get({ url: '/admin-api/cps/platform/get', params: { id } }),

  getPlatformPage: (params: CpsPlatformPageReqVO) =>
    request.get({ url: '/admin-api/cps/platform/page', params }),
}
```

**[RULE-FE-01]** [强制] API 文件中必须定义接口的 TypeScript 类型，**禁止**使用 `any`
**[RULE-FE-02]** [强制] 所有 API 函数封装在以模块命名的对象中（如 `CpsPlatformApi`）

### 6.3 Vue 组件规范（必须）

```vue
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'

// 查询参数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  platformName: undefined,
  status: undefined,
})

// 列表数据
const list = ref<CpsPlatformVO[]>([])
const total = ref(0)
const loading = ref(false)

// 加载数据
const getList = async () => {
  loading.value = true
  try {
    const data = await CpsPlatformApi.getPlatformPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(getList)
</script>
```

**[RULE-FE-03]** [强制] 使用 `<script setup lang="ts">` 语法，**禁止**使用 Options API
**[RULE-FE-04]** [强制] 组件内状态用 `ref`/`reactive`，跨组件共享状态用 Pinia Store
**[RULE-FE-05]** [强制] API 调用必须有 `loading` 状态和 `try/finally` 保证 loading 释放

---

## PART 7：代码生成规范（codegen-rules）

### 7.1 后端代码生成模板类型

| templateType | 类型 | 适用场景 |
|-------------|------|---------|
| 1 | 通用表（Common） | 标准 CRUD + 分页（最常用） |
| 2 | 树表（Tree） | 具有父子关系的层级数据 |
| 11 | ERP 主子表 | 主表 + 明细表（如订单+订单项） |

**[RULE-CG-01]** [强制] 新增普通业务表使用 `templateType: 1`
**[RULE-CG-02]** [强制] 菜单/分类/区域等层级数据使用 `templateType: 2`，必须有 `parent_id`、`name`、`sort` 字段
**[RULE-CG-03]** [强制] 主子表使用 `templateType: 11`，主表和子表分别配置，通过外键关联

### 7.2 前端代码生成模板

| 模板 | 框架 | 说明 |
|------|------|------|
| `vue3` | Vue3 + Element Plus | 标准 CRUD 页面 |
| `vue3_vben` | Vue3 + Vben Admin | Modal 弹窗表单 |
| `vue3_vben5_antd` | Vue3 + Vben5 + Ant Design | VxeTable + Antd |
| `vue3_admin_uniapp` | UniApp | 移动端（Wot 组件库） |

**[RULE-CG-04]** [强制] admin-vue3 后台默认使用 `vue3` 或 `vue3_vben5_antd` 模板
**[RULE-CG-05]** [强制] 移动端（admin-uniapp、mall-uniapp）使用 `vue3_admin_uniapp` 模板

### 7.3 字段配置规范（生成时）

**[RULE-CG-06]** [强制] 标准字段（`id`、`creator`、`create_time`、`updater`、`update_time`、`deleted`、`tenant_id`）**禁止**放入生成字段列表

**[RULE-CG-07]** [建议] 状态字段（`status`）使用字典类型 `common_status`，列表页可以筛选，详情页展示字典标签

---

## PART 8：AI 编程规范（Vibe Coding）

### 8.1 .qoder 目录规范

```
.qoder/
├── skills/          # Qoder 技能（当前文件所在目录）
│   ├── unit-test-generation/SKILL.md     # 测试生成规则
│   └── project-architecture-standards/SKILL.md  # 本文件
├── agents/          # 自定义 Agent 配置
└── commands/        # 自定义命令
```

**[RULE-AI-01]** [强制] 项目根 `.qoder/skills/` 下每个 Skill 为独立目录，包含 `SKILL.md`
**[RULE-AI-02]** [强制] 后端模块的 `.qoder/` 目录用于 repowiki 等后端专属配置
**[RULE-AI-03]** [建议] 新功能开发前，在 `openspec/specs/` 目录创建需求规格文档

### 8.2 Vibe Coding 工作流

新功能开发推荐流程：
1. **Spec**：在 `openspec/specs/` 编写功能规格（需求分析、接口设计、数据库设计）
2. **Plan**：使用 Qoder Plan 模式制定实现计划
3. **Code**：按计划逐步生成代码（Controller → Service → Mapper → DO → VO → Convert）
4. **Test**：使用 `unit-test-generation` Skill 生成测试
5. **Review**：代码 Review 确认符合本规范

**[RULE-AI-04]** [建议] 复杂功能必须先写 Spec，不可直接写代码
**[RULE-AI-05]** [建议] 单次生成代码不超过一个完整 CRUD 模块（Controller + Service + Mapper + DO + VO）

### 8.3 MCP 集成规范

```yaml
# application-local.yaml 中的 MCP 配置
yudao:
  cps:
    mcp:
      enabled: true
      server-name: CPS-MCP-Server
      endpoint: /mcp/cps
```

**[RULE-AI-06]** [强制] MCP 服务端点固定为 `/mcp/cps`，使用 JSON-RPC 2.0 over Streamable HTTP
**[RULE-AI-07]** [强制] MCP Tool 方法名使用小驼峰（camelCase），如 `searchGoods`、`generatePromotionLink`
**[RULE-AI-08]** [强制] MCP Resource URI 格式为 `cps://{category}/{resource}`，如 `cps://platform/configs`

---

## PART 9：测试规范（概要）

> 详细规范参见 `unit-test-generation` Skill

### 9.1 测试基类选择

| 场景 | 基类 |
|------|------|
| 纯 Mockito（无 Spring） | `BaseMockitoUnitTest` |
| 依赖数据库（Service/Mapper） | `BaseDbUnitTest` |
| 依赖 Redis | `BaseRedisUnitTest` |
| 数据库 + Redis | `BaseDbAndRedisUnitTest` |

**[RULE-TEST-01]** [强制] CPS 模块 Service 测试优先使用 `BaseDbUnitTest`
**[RULE-TEST-02]** [强制] 测试方法命名：`test{Method}_{Scenario}` 或 `test{Method}_success`/`test{Method}_failed`
**[RULE-TEST-03]** [强制] 使用 AAA 三段式注释：`// 准备参数` / `// 调用` / `// 断言`

---

## PART 10：禁止事项（Anti-Patterns）

以下是明确禁止的编码模式：

```java
// ❌ 禁止：Controller 写业务逻辑
@PostMapping("/create")
public CommonResult<Long> create(@RequestBody CpsPlatformSaveReqVO req) {
    if (platformMapper.selectByCode(req.getCode()) != null) { // 禁止！
        throw new RuntimeException("编码已存在");              // 禁止！
    }
    ...
}

// ❌ 禁止：使用 BigDecimal/Double 存储金额
private BigDecimal rebateAmount; // 禁止！

// ❌ 禁止：跨模块直接注入 ServiceImpl
@Resource
private MemberUserServiceImpl memberUserService; // 禁止！应通过 MemberUserApi

// ❌ 禁止：物理删除 CPS 数据
platformMapper.deleteById(id);     // 如果没有软删除机制，禁止！

// ❌ 禁止：Controller 直接返回 DO
public CommonResult<CpsPlatformDO> getPlatform(Long id) { // 禁止，应返回 RespVO

// ❌ 禁止：前端 API 函数用 any
const createPlatform = (data: any) => ... // 禁止！

// ❌ 禁止：Service 中 catch 后不重新抛出
try {
    platformMapper.insert(platform);
} catch (Exception e) {
    log.error("插入失败", e); // 如果只记录日志不重新抛出，业务事务不会回滚！
}
```

---

## PART 11：快速生成 CRUD 模块检查清单

生成一个新的 CPS 业务模块时，必须包含以下文件：

**后端（必须）：**
- [ ] `Cps{Name}DO.java` - 数据库实体（继承 BaseDO）
- [ ] `Cps{Name}Mapper.java` - MyBatis Plus Mapper（继承 BaseMapperX）
- [ ] `Cps{Name}Service.java` - Service 接口
- [ ] `Cps{Name}ServiceImpl.java` - Service 实现（含 validate 方法）
- [ ] `Cps{Name}Controller.java` - 管理端 Controller（含权限注解）
- [ ] `Cps{Name}SaveReqVO.java` - 创建/修改请求 VO（含校验注解）
- [ ] `Cps{Name}PageReqVO.java` - 分页查询请求 VO
- [ ] `Cps{Name}RespVO.java` - 响应 VO（含 @Schema 注解）
- [ ] `Cps{Name}Convert.java` - MapStruct 转换器
- [ ] `ErrorCodeConstants.java` 中新增错误码常量

**数据库（必须）：**
- [ ] SQL 建表语句（含 cps_ 前缀、标准字段、索引、COMMENT）
- [ ] `clean.sql` 中新增 `DELETE FROM cps_{name};` 语句（供测试清理）
- [ ] `create_tables.sql` 中新增建表语句（供 H2 测试环境）

**测试（必须）：**
- [ ] `Cps{Name}ServiceImplTest.java` - Service 测试（继承 BaseDbUnitTest）
