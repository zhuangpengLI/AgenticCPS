# CPS Phase 7 优化与完善 实现计划

> **For agentic workers:** Use `subagent-driven-development` or `executing-plans` to implement this plan.

**Goal:** 实现 CPS 系统 Phase 7 的全部优化与完善功能，包括转链记录管理、冻结解冻、风控规则（基础版）、缓存性能优化和核心 Service 单元测试。

**Architecture:**
- Task 7.1 转链记录：Mapper 查询方法 + Service + Controller + 前端页面
- Task 7.2 冻结解冻：Mapper 查询 + FreezeService（CRUD 配置 + 自动解冻 + 手动解冻）+ Job + Controller + 前端
- Task 7.3 风控规则：新建 `yudao_cps_risk_rule` 表 + RiskService（Redis 频率限制 + DB 黑名单）+ 前端管理页
- Task 7.4 性能优化：关键 Mapper 增加 Redis @Cacheable + 补充 cps-all-in-one.sql 索引
- Task 7.5 单元测试：FreezeService + RiskService 核心逻辑 Mock 单元测试

**Tech Stack:** Java 17 / Spring Boot 3.x / MyBatis Plus / Redis / Quartz / Vue3 / Element Plus

---

## Task 1：转链记录管理

**Files:**
- Modify: `backend/.../dal/mysql/transfer/CpsTransferRecordMapper.java`
- Create: `backend/.../service/transfer/CpsTransferService.java`
- Create: `backend/.../service/transfer/CpsTransferServiceImpl.java`
- Create: `backend/.../controller/admin/transfer/vo/CpsTransferRecordPageReqVO.java`
- Create: `backend/.../controller/admin/transfer/vo/CpsTransferRecordRespVO.java`
- Create: `backend/.../controller/admin/transfer/CpsTransferRecordController.java`
- Create: `frontend/.../api/cps/transfer.ts`
- Create: `frontend/.../views/cps/transfer/index.vue`

### Task 1.1：扩展 CpsTransferRecordMapper

新增分页查询和统计方法：

```java
@Mapper
public interface CpsTransferRecordMapper extends BaseMapperX<CpsTransferRecordDO> {

    default PageResult<CpsTransferRecordDO> selectPage(CpsTransferRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eqIfPresent(CpsTransferRecordDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsTransferRecordDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsTransferRecordDO::getStatus, reqVO.getStatus())
                .likeIfPresent(CpsTransferRecordDO::getItemTitle, reqVO.getItemTitle())
                .betweenIfPresent(CpsTransferRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsTransferRecordDO::getId));
    }

    /** 统计会员当日转链次数（风控用） */
    default long countTodayByMember(Long memberId, LocalDate date) {
        return selectCount(new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eq(CpsTransferRecordDO::getMemberId, memberId)
                .between(CpsTransferRecordDO::getCreateTime,
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
    }
}
```

### Task 1.2：CpsTransferService 接口

```java
public interface CpsTransferService {
    /** 管理端分页查询转链记录 */
    PageResult<CpsTransferRecordDO> getTransferPage(CpsTransferRecordPageReqVO reqVO);

    /** 统计指定会员当日转链次数 */
    long countTodayByMember(Long memberId);
}
```

### Task 1.3：CpsTransferServiceImpl

```java
@Service @Validated
public class CpsTransferServiceImpl implements CpsTransferService {
    @Resource private CpsTransferRecordMapper transferMapper;

    @Override
    public PageResult<CpsTransferRecordDO> getTransferPage(CpsTransferRecordPageReqVO reqVO) {
        return transferMapper.selectPage(reqVO);
    }

    @Override
    public long countTodayByMember(Long memberId) {
        return transferMapper.countTodayByMember(memberId, LocalDate.now());
    }
}
```

### Task 1.4：VO 类

`CpsTransferRecordPageReqVO.java`：
```java
@Schema(description = "转链记录分页请求 VO")
@Data
public class CpsTransferRecordPageReqVO extends PageParam {
    @Schema(description = "会员ID") private Long memberId;
    @Schema(description = "平台编码") private String platformCode;
    @Schema(description = "商品标题（模糊）") private String itemTitle;
    @Schema(description = "状态（0无效 1有效）") private Integer status;
    @Schema(description = "创建时间范围") private LocalDateTime[] createTime;
}
```

`CpsTransferRecordRespVO.java`：
```java
@Schema(description = "转链记录 Response VO")
@Data
public class CpsTransferRecordRespVO {
    private Long id;
    private Long memberId;
    private String platformCode;
    private String itemId;
    private String itemTitle;
    private String originalContent;
    private String promotionUrl;
    private String taoCommand;
    private String platformOrderId;
    private String adzoneId;
    private LocalDateTime expireTime;
    private Integer status;
    private LocalDateTime createTime;
}
```

### Task 1.5：CpsTransferRecordController

```java
@Tag(name = "管理后台 - CPS转链记录")
@RestController
@RequestMapping("/admin-api/cps/transfer-record")
@Validated
public class CpsTransferRecordController {

    @Resource private CpsTransferService transferService;

    @GetMapping("/page")
    @Operation(summary = "转链记录分页查询")
    @PreAuthorize("@ss.hasPermission('cps:transfer-record:query')")
    public CommonResult<PageResult<CpsTransferRecordRespVO>> getTransferPage(
            @Valid CpsTransferRecordPageReqVO reqVO) {
        PageResult<CpsTransferRecordDO> page = transferService.getTransferPage(reqVO);
        return success(BeanUtils.toBean(page, CpsTransferRecordRespVO.class));
    }
}
```

### Task 1.6：前端 API + 页面

`api/cps/transfer.ts`：基础分页查询接口（内联 pageNo/pageSize，参考 order.ts 风格）。

`views/cps/transfer/index.vue`：列表页，含平台/会员/状态/时间范围筛选，显示商品标题、推广链接、关联订单号等列。

---

## Task 2：冻结解冻配置与定时任务

**Files:**
- Modify: `backend/.../dal/mysql/freeze/CpsFreezeConfigMapper.java`
- Modify: `backend/.../dal/mysql/freeze/CpsFreezeRecordMapper.java`
- Create: `backend/.../service/freeze/CpsFreezeService.java`
- Create: `backend/.../service/freeze/CpsFreezeServiceImpl.java`
- Create: `backend/.../controller/admin/freeze/vo/CpsFreezeConfigPageReqVO.java`
- Create: `backend/.../controller/admin/freeze/vo/CpsFreezeConfigRespVO.java`
- Create: `backend/.../controller/admin/freeze/vo/CpsFreezeConfigSaveReqVO.java`
- Create: `backend/.../controller/admin/freeze/vo/CpsFreezeRecordPageReqVO.java`
- Create: `backend/.../controller/admin/freeze/vo/CpsFreezeRecordRespVO.java`
- Create: `backend/.../controller/admin/freeze/CpsFreezeController.java`
- Create: `backend/.../job/CpsFreezeUnfreezeJob.java`
- Create: `frontend/.../api/cps/freeze.ts`
- Create: `frontend/.../views/cps/freeze/config/index.vue`
- Create: `frontend/.../views/cps/freeze/record/index.vue`

### Task 2.1：扩展 CpsFreezeConfigMapper + CpsFreezeRecordMapper

`CpsFreezeConfigMapper.java`：
```java
/** 查询启用的配置（优先取平台专属，其次取全平台） */
default CpsFreezeConfigDO selectActiveByPlatform(String platformCode) {
    // 先查平台专属
    CpsFreezeConfigDO config = selectOne(new LambdaQueryWrapperX<CpsFreezeConfigDO>()
            .eq(CpsFreezeConfigDO::getPlatformCode, platformCode)
            .eq(CpsFreezeConfigDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    if (config != null) return config;
    // 再查全平台（platform_code IS NULL）
    return selectOne(new LambdaQueryWrapperX<CpsFreezeConfigDO>()
            .isNull(CpsFreezeConfigDO::getPlatformCode)
            .eq(CpsFreezeConfigDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
}

default PageResult<CpsFreezeConfigDO> selectPage(CpsFreezeConfigPageReqVO reqVO) {
    return selectPage(reqVO, new LambdaQueryWrapperX<CpsFreezeConfigDO>()
            .eqIfPresent(CpsFreezeConfigDO::getPlatformCode, reqVO.getPlatformCode())
            .eqIfPresent(CpsFreezeConfigDO::getStatus, reqVO.getStatus())
            .orderByDesc(CpsFreezeConfigDO::getId));
}
```

`CpsFreezeRecordMapper.java`：
```java
/** 查询已到达解冻时间且状态为 frozen 的记录（批量自动解冻） */
default List<CpsFreezeRecordDO> selectPendingUnfreeze(int limit) {
    return selectList(new LambdaQueryWrapperX<CpsFreezeRecordDO>()
            .eq(CpsFreezeRecordDO::getStatus, CpsFreezeStatusEnum.FROZEN.getStatus())
            .le(CpsFreezeRecordDO::getUnfreezeTime, LocalDateTime.now())
            .last("LIMIT " + limit));
}

default PageResult<CpsFreezeRecordDO> selectPage(CpsFreezeRecordPageReqVO reqVO) {
    return selectPage(reqVO, new LambdaQueryWrapperX<CpsFreezeRecordDO>()
            .eqIfPresent(CpsFreezeRecordDO::getMemberId, reqVO.getMemberId())
            .eqIfPresent(CpsFreezeRecordDO::getStatus, reqVO.getStatus())
            .betweenIfPresent(CpsFreezeRecordDO::getCreateTime, reqVO.getCreateTime())
            .orderByDesc(CpsFreezeRecordDO::getId));
}
```

### Task 2.2：CpsFreezeService 接口

```java
public interface CpsFreezeService {
    // 配置管理
    Long createFreezeConfig(CpsFreezeConfigSaveReqVO reqVO);
    void updateFreezeConfig(CpsFreezeConfigSaveReqVO reqVO);
    void deleteFreezeConfig(Long id);
    PageResult<CpsFreezeConfigDO> getFreezeConfigPage(CpsFreezeConfigPageReqVO reqVO);
    CpsFreezeConfigDO getActiveConfig(String platformCode);

    // 解冻操作
    /** 自动批量解冻（定时任务调用） */
    int batchUnfreeze(int batchSize);
    /** 手动解冻指定记录（管理员操作） */
    void manualUnfreeze(Long recordId);
    /** 分页查询冻结记录 */
    PageResult<CpsFreezeRecordDO> getFreezeRecordPage(CpsFreezeRecordPageReqVO reqVO);
}
```

### Task 2.3：CpsFreezeServiceImpl 关键逻辑

```java
@Override
@Transactional(rollbackFor = Exception.class)
public int batchUnfreeze(int batchSize) {
    List<CpsFreezeRecordDO> list = freezeRecordMapper.selectPendingUnfreeze(batchSize);
    int count = 0;
    for (CpsFreezeRecordDO record : list) {
        try {
            // 更新状态为已解冻
            CpsFreezeRecordDO update = new CpsFreezeRecordDO();
            update.setId(record.getId());
            update.setStatus(CpsFreezeStatusEnum.UNFREEZED.getStatus());
            update.setActualUnfreezeTime(LocalDateTime.now());
            freezeRecordMapper.updateById(update);
            // TODO: 调用返利账户解冻逻辑（账户可用余额 += freezeAmount）
            count++;
        } catch (Exception e) {
            log.error("[batchUnfreeze] 解冻失败, recordId={}", record.getId(), e);
        }
    }
    return count;
}

@Override
public void manualUnfreeze(Long recordId) {
    CpsFreezeRecordDO record = freezeRecordMapper.selectById(recordId);
    if (record == null) throw exception(FREEZE_RECORD_NOT_EXISTS);
    if (!CpsFreezeStatusEnum.FROZEN.getStatus().equals(record.getStatus())) {
        throw exception(FREEZE_RECORD_STATUS_INVALID);
    }
    CpsFreezeRecordDO update = new CpsFreezeRecordDO();
    update.setId(recordId);
    update.setStatus(CpsFreezeStatusEnum.UNFREEZED.getStatus());
    update.setActualUnfreezeTime(LocalDateTime.now());
    freezeRecordMapper.updateById(update);
}
```

### Task 2.4：CpsFreezeUnfreezeJob（定时任务）

```java
/**
 * CPS 冻结返利自动解冻定时任务
 *
 * 处理器名字：cpsFreezeUnfreezeJob
 * CRON 表达式：0 0 2 * * ?（每日凌晨 2 点执行）
 * 参数示例：{"batchSize":500}
 */
@Slf4j
@Component("cpsFreezeUnfreezeJob")
public class CpsFreezeUnfreezeJob implements JobHandler {
    @Resource private CpsFreezeService freezeService;

    @Override
    public String execute(String param) throws Exception {
        int batchSize = parseBatchSize(param, 500);
        int count = freezeService.batchUnfreeze(batchSize);
        return "自动解冻完成，本次解冻=" + count + "条";
    }
    // parseBatchSize 方法同 CpsRebateSettleJob 风格
}
```

### Task 2.5：CpsFreezeController

提供配置 CRUD 和冻结记录查询、手动解冻三类接口：
- `GET /admin-api/cps/freeze/config/page` — 配置分页
- `POST /admin-api/cps/freeze/config/create` — 新建配置
- `PUT /admin-api/cps/freeze/config/update` — 更新配置
- `DELETE /admin-api/cps/freeze/config/delete` — 删除配置
- `GET /admin-api/cps/freeze/record/page` — 冻结记录分页
- `PUT /admin-api/cps/freeze/record/manual-unfreeze` — 手动解冻

### Task 2.6：前端页面

`views/cps/freeze/config/index.vue`：配置管理（列表+新增/编辑弹窗+启用/禁用开关）。  
`views/cps/freeze/record/index.vue`：冻结记录列表（含状态筛选，已冻结记录显示"手动解冻"按钮）。

---

## Task 3：风控规则（基础版：频率限制 + 黑名单）

**Files:**
- Modify: `backend/sql/module/cps-all-in-one.sql`（追加 `yudao_cps_risk_rule` 建表）
- Create: `backend/.../dal/dataobject/risk/CpsRiskRuleDO.java`
- Create: `backend/.../dal/mysql/risk/CpsRiskRuleMapper.java`
- Create: `backend/.../service/risk/CpsRiskService.java`
- Create: `backend/.../service/risk/CpsRiskServiceImpl.java`
- Create: `backend/.../controller/admin/risk/vo/CpsRiskRulePageReqVO.java`
- Create: `backend/.../controller/admin/risk/vo/CpsRiskRuleRespVO.java`
- Create: `backend/.../controller/admin/risk/vo/CpsRiskRuleSaveReqVO.java`
- Create: `backend/.../controller/admin/risk/CpsRiskRuleController.java`
- Modify: `backend/yudao-module-cps-api/src/main/java/cn/iocoder/yudao/module/cps/enums/CpsRiskRuleTypeEnum.java`（新建）
- Create: `frontend/.../api/cps/risk.ts`
- Create: `frontend/.../views/cps/risk/index.vue`

### Task 3.1：数据库表（追加到 cps-all-in-one.sql）

```sql
-- ----------------------------
-- 15. CPS风控规则表（Phase7新增）
-- ----------------------------
DROP TABLE IF EXISTS `yudao_cps_risk_rule`;
CREATE TABLE `yudao_cps_risk_rule` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_type`    varchar(32)  NOT NULL COMMENT '规则类型（rate_limit:频率限制 blacklist:黑名单）',
  `target_type`  varchar(32)  NOT NULL DEFAULT 'member' COMMENT '目标类型（member:会员 ip:IP）',
  `target_value` varchar(128)          DEFAULT NULL COMMENT '目标值（blacklist类型：会员ID或IP地址）',
  `limit_count`  int                   DEFAULT NULL COMMENT '限制次数（rate_limit类型：每日最大转链次数）',
  `status`       tinyint      NOT NULL DEFAULT '1' COMMENT '状态（0禁用 1启用）',
  `remark`       varchar(255)          DEFAULT NULL COMMENT '备注',
  `creator`      varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`      varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`    bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_rule_type` (`rule_type`) USING BTREE,
  KEY `idx_target_value` (`target_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS风控规则表';

-- 默认规则：每日转链频率限制 100 次
INSERT INTO `yudao_cps_risk_rule` (`rule_type`, `target_type`, `limit_count`, `status`, `remark`)
VALUES ('rate_limit', 'member', 100, 1, '每日转链次数默认上限');
```

### Task 3.2：CpsRiskRuleDO

```java
@TableName("yudao_cps_risk_rule")
@Data @EqualsAndHashCode(callSuper = true) @Builder @NoArgsConstructor @AllArgsConstructor
public class CpsRiskRuleDO extends TenantBaseDO {
    @TableId private Long id;
    /** 规则类型 rate_limit / blacklist */
    private String ruleType;
    /** 目标类型 member / ip */
    private String targetType;
    /** 黑名单目标值（会员ID或IP） */
    private String targetValue;
    /** 频率限制次数（rate_limit 类型使用） */
    private Integer limitCount;
    private Integer status;
    private String remark;
}
```

### Task 3.3：CpsRiskRuleMapper

```java
@Mapper
public interface CpsRiskRuleMapper extends BaseMapperX<CpsRiskRuleDO> {

    default PageResult<CpsRiskRuleDO> selectPage(CpsRiskRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsRiskRuleDO>()
                .eqIfPresent(CpsRiskRuleDO::getRuleType, reqVO.getRuleType())
                .eqIfPresent(CpsRiskRuleDO::getStatus, reqVO.getStatus())
                .orderByDesc(CpsRiskRuleDO::getId));
    }

    /** 查询启用的全局频率限制配置 */
    default CpsRiskRuleDO selectActiveRateLimit() {
        return selectOne(new LambdaQueryWrapperX<CpsRiskRuleDO>()
                .eq(CpsRiskRuleDO::getRuleType, "rate_limit")
                .eq(CpsRiskRuleDO::getStatus, 1)
                .isNull(CpsRiskRuleDO::getTargetValue)
                .last("LIMIT 1"));
    }

    /** 查询黑名单：会员ID是否在黑名单中 */
    default boolean existsBlacklist(String targetType, String targetValue) {
        return selectCount(new LambdaQueryWrapperX<CpsRiskRuleDO>()
                .eq(CpsRiskRuleDO::getRuleType, "blacklist")
                .eq(CpsRiskRuleDO::getTargetType, targetType)
                .eq(CpsRiskRuleDO::getTargetValue, targetValue)
                .eq(CpsRiskRuleDO::getStatus, 1)) > 0;
    }
}
```

### Task 3.4：CpsRiskService 接口 + 实现

```java
public interface CpsRiskService {
    /** 风控检查：会员是否允许转链（返回 true=通过 false=拦截） */
    boolean checkTransferAllowed(Long memberId, String clientIp);

    // 规则 CRUD
    Long createRule(CpsRiskRuleSaveReqVO reqVO);
    void updateRule(CpsRiskRuleSaveReqVO reqVO);
    void deleteRule(Long id);
    PageResult<CpsRiskRuleDO> getRulePage(CpsRiskRulePageReqVO reqVO);
}
```

`CpsRiskServiceImpl` 核心逻辑（频率限制用 Redis INCR + EXPIRE 计数器，黑名单查 DB）：

```java
@Override
public boolean checkTransferAllowed(Long memberId, String clientIp) {
    // 1. 黑名单检查（会员ID）
    if (riskRuleMapper.existsBlacklist("member", String.valueOf(memberId))) {
        log.warn("[RiskCheck] 会员黑名单拦截 memberId={}", memberId);
        return false;
    }
    // 2. IP 黑名单检查
    if (clientIp != null && riskRuleMapper.existsBlacklist("ip", clientIp)) {
        log.warn("[RiskCheck] IP黑名单拦截 ip={}", clientIp);
        return false;
    }
    // 3. 频率限制（Redis 计数器）
    CpsRiskRuleDO rateRule = riskRuleMapper.selectActiveRateLimit();
    if (rateRule != null && rateRule.getLimitCount() != null) {
        String redisKey = "cps:risk:rate:" + memberId + ":" + LocalDate.now();
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count == 1) {
            stringRedisTemplate.expire(redisKey, Duration.ofDays(1));
        }
        if (count > rateRule.getLimitCount()) {
            log.warn("[RiskCheck] 频率超限 memberId={} count={} limit={}", memberId, count, rateRule.getLimitCount());
            return false;
        }
    }
    return true;
}
```

### Task 3.5：CpsRiskRuleController

- `GET /admin-api/cps/risk/rule/page`
- `POST /admin-api/cps/risk/rule/create`
- `PUT /admin-api/cps/risk/rule/update`
- `DELETE /admin-api/cps/risk/rule/delete`

### Task 3.6：前端风控规则管理页

`views/cps/risk/index.vue`：列表（规则类型/目标类型/目标值/限制次数/状态）+ 新增/编辑弹窗 + 启用/禁用开关。

---

## Task 4：性能优化（缓存 + 索引）

**Files:**
- Modify: `backend/sql/module/cps-all-in-one.sql`（补充复合索引）
- Modify: `backend/.../service/platform/CpsPlatformServiceImpl.java`（添加 @Cacheable）
- Modify: `backend/.../service/rebate/CpsRebateSettleServiceImpl.java`（添加配置缓存）
- Create: `backend/.../config/CpsCacheConfig.java`（CacheManager 配置）

### Task 4.1：补充数据库索引

在 `cps-all-in-one.sql` 的 `yudao_cps_order` 表追加：
```sql
-- 补充复合索引（不修改已有 DDL，追加 ALTER TABLE 脚本）
-- 加入 SET FOREIGN_KEY_CHECKS = 1 前：
ALTER TABLE `yudao_cps_order`
  ADD INDEX `idx_member_status` (`member_id`, `order_status`) USING BTREE,
  ADD INDEX `idx_platform_create` (`platform_code`, `create_time`) USING BTREE;

ALTER TABLE `yudao_cps_rebate_record`
  ADD INDEX `idx_member_status` (`member_id`, `rebate_status`) USING BTREE;

ALTER TABLE `yudao_cps_transfer_record`
  ADD INDEX `idx_member_create` (`member_id`, `create_time`) USING BTREE;
```

### Task 4.2：CpsCacheConfig（Spring Cache 配置）

```java
@Configuration
public class CpsCacheConfig {

    public static final String CACHE_PLATFORM = "cps:platform";
    public static final String CACHE_REBATE_CONFIG = "cps:rebateConfig";
    public static final String CACHE_RISK_RATE_RULE = "cps:riskRateRule";

    @Bean
    public CacheManager cpsCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(factory)
                .withCacheConfiguration(CACHE_PLATFORM, config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CACHE_REBATE_CONFIG, config.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CACHE_RISK_RATE_RULE, config.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
```

### Task 4.3：在 CpsPlatformServiceImpl 添加缓存

```java
@Cacheable(cacheNames = CpsCacheConfig.CACHE_PLATFORM, key = "#platformCode")
public CpsPlatformDO getPlatformByCode(String platformCode) { ... }

@CacheEvict(cacheNames = CpsCacheConfig.CACHE_PLATFORM, key = "#reqVO.platformCode")
public void updatePlatform(CpsPlatformSaveReqVO reqVO) { ... }
```

### Task 4.4：返利配置缓存

在 `CpsRebateSettleServiceImpl.getMatchedConfig()` 方法上添加：
```java
@Cacheable(cacheNames = CpsCacheConfig.CACHE_REBATE_CONFIG,
           key = "#platformCode + ':' + #memberId")
```

---

## Task 5：核心业务 Service 单元测试

**Files:**
- Create: `backend/.../service/freeze/CpsFreezeServiceImplTest.java`
- Create: `backend/.../service/risk/CpsRiskServiceImplTest.java`

### Task 5.1：CpsFreezeServiceImplTest

```java
@ExtendWith(MockitoExtension.class)
class CpsFreezeServiceImplTest {

    @InjectMocks private CpsFreezeServiceImpl freezeService;
    @Mock private CpsFreezeRecordMapper freezeRecordMapper;
    @Mock private CpsFreezeConfigMapper freezeConfigMapper;

    @Test
    @DisplayName("batchUnfreeze - 正常批量解冻")
    void batchUnfreeze_normal() {
        // Arrange
        CpsFreezeRecordDO record = CpsFreezeRecordDO.builder()
                .id(1L).memberId(100L).freezeAmount(new BigDecimal("50.00"))
                .status("frozen").unfreezeTime(LocalDateTime.now().minusHours(1))
                .build();
        when(freezeRecordMapper.selectPendingUnfreeze(10)).thenReturn(List.of(record));

        // Act
        int count = freezeService.batchUnfreeze(10);

        // Assert
        assertEquals(1, count);
        verify(freezeRecordMapper).updateById(argThat(r ->
                "unfreezed".equals(r.getStatus()) && r.getActualUnfreezeTime() != null));
    }

    @Test
    @DisplayName("manualUnfreeze - 记录不存在抛异常")
    void manualUnfreeze_notFound() {
        when(freezeRecordMapper.selectById(999L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> freezeService.manualUnfreeze(999L));
    }

    @Test
    @DisplayName("manualUnfreeze - 已解冻状态再解冻抛异常")
    void manualUnfreeze_alreadyUnfreezed() {
        CpsFreezeRecordDO record = CpsFreezeRecordDO.builder()
                .id(1L).status("unfreezed").build();
        when(freezeRecordMapper.selectById(1L)).thenReturn(record);
        assertThrows(ServiceException.class, () -> freezeService.manualUnfreeze(1L));
    }

    @Test
    @DisplayName("getActiveConfig - 平台专属配置优先于全平台")
    void getActiveConfig_platformPriority() {
        CpsFreezeConfigDO platformConfig = CpsFreezeConfigDO.builder()
                .id(2L).platformCode("taobao").unfreezeDays(7).build();
        when(freezeConfigMapper.selectActiveByPlatform("taobao")).thenReturn(platformConfig);

        CpsFreezeConfigDO result = freezeService.getActiveConfig("taobao");
        assertEquals(7, result.getUnfreezeDays());
    }
}
```

### Task 5.2：CpsRiskServiceImplTest

```java
@ExtendWith(MockitoExtension.class)
class CpsRiskServiceImplTest {

    @InjectMocks private CpsRiskServiceImpl riskService;
    @Mock private CpsRiskRuleMapper riskRuleMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("checkTransferAllowed - 会员在黑名单中返回 false")
    void checkTransferAllowed_memberBlacklisted() {
        when(riskRuleMapper.existsBlacklist("member", "100")).thenReturn(true);
        assertFalse(riskService.checkTransferAllowed(100L, "1.2.3.4"));
    }

    @Test
    @DisplayName("checkTransferAllowed - 频率超限返回 false")
    void checkTransferAllowed_rateLimitExceeded() {
        when(riskRuleMapper.existsBlacklist(any(), any())).thenReturn(false);
        CpsRiskRuleDO rule = CpsRiskRuleDO.builder().limitCount(5).build();
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(rule);
        when(valueOps.increment(anyString())).thenReturn(6L); // 超过 5 次

        assertFalse(riskService.checkTransferAllowed(100L, null));
    }

    @Test
    @DisplayName("checkTransferAllowed - 正常通过")
    void checkTransferAllowed_allowed() {
        when(riskRuleMapper.existsBlacklist(any(), any())).thenReturn(false);
        CpsRiskRuleDO rule = CpsRiskRuleDO.builder().limitCount(100).build();
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(rule);
        when(valueOps.increment(anyString())).thenReturn(1L); // 第 1 次

        assertTrue(riskService.checkTransferAllowed(100L, "1.2.3.4"));
    }
}
```

---

## Task 6：编译验证

```bash
cd f:\ai\AgenticCPS\backend
mvn compile -pl yudao-module-cps/yudao-module-cps-biz -am -q
```

期望：**BUILD SUCCESS，无错误**

---

## 文件目录总览

```
backend/yudao-module-cps/yudao-module-cps-biz/src/main/java/.../cps/
├── config/
│   └── CpsCacheConfig.java                          [Task 4.2 新建]
├── controller/admin/
│   ├── freeze/
│   │   ├── CpsFreezeController.java                 [Task 2.5 新建]
│   │   └── vo/
│   │       ├── CpsFreezeConfigPageReqVO.java
│   │       ├── CpsFreezeConfigRespVO.java
│   │       ├── CpsFreezeConfigSaveReqVO.java
│   │       ├── CpsFreezeRecordPageReqVO.java
│   │       └── CpsFreezeRecordRespVO.java
│   ├── risk/
│   │   ├── CpsRiskRuleController.java               [Task 3.5 新建]
│   │   └── vo/
│   │       ├── CpsRiskRulePageReqVO.java
│   │       ├── CpsRiskRuleRespVO.java
│   │       └── CpsRiskRuleSaveReqVO.java
│   └── transfer/
│       ├── CpsTransferRecordController.java         [Task 1.5 新建]
│       └── vo/
│           ├── CpsTransferRecordPageReqVO.java
│           └── CpsTransferRecordRespVO.java
├── dal/
│   ├── dataobject/risk/
│   │   └── CpsRiskRuleDO.java                       [Task 3.2 新建]
│   └── mysql/
│       ├── freeze/
│       │   ├── CpsFreezeConfigMapper.java           [Task 2.1 修改]
│       │   └── CpsFreezeRecordMapper.java           [Task 2.1 修改]
│       ├── risk/
│       │   └── CpsRiskRuleMapper.java               [Task 3.3 新建]
│       └── transfer/
│           └── CpsTransferRecordMapper.java         [Task 1.1 修改]
├── service/
│   ├── freeze/
│   │   ├── CpsFreezeService.java                    [Task 2.2 新建]
│   │   └── CpsFreezeServiceImpl.java                [Task 2.3 新建]
│   ├── risk/
│   │   ├── CpsRiskService.java                      [Task 3.4 新建]
│   │   └── CpsRiskServiceImpl.java                  [Task 3.4 新建]
│   └── transfer/
│       ├── CpsTransferService.java                  [Task 1.2 新建]
│       └── CpsTransferServiceImpl.java              [Task 1.3 新建]
└── job/
    └── CpsFreezeUnfreezeJob.java                    [Task 2.4 新建]

backend/yudao-module-cps-api/src/main/java/.../cps/enums/
└── CpsRiskRuleTypeEnum.java                         [Task 3 新建]

backend/sql/module/
└── cps-all-in-one.sql                              [Task 3.1/4.1 修改]

frontend/admin-vue3/src/
├── api/cps/
│   ├── transfer.ts                                  [Task 1.6 新建]
│   ├── freeze.ts                                    [Task 2.6 新建]
│   └── risk.ts                                      [Task 3.6 新建]
└── views/cps/
    ├── transfer/index.vue                           [Task 1.6 新建]
    ├── freeze/
    │   ├── config/index.vue                         [Task 2.6 新建]
    │   └── record/index.vue                         [Task 2.6 新建]
    └── risk/index.vue                               [Task 3.6 新建]

backend/.../service/freeze/CpsFreezeServiceImplTest.java [Task 5.1 新建]
backend/.../service/risk/CpsRiskServiceImplTest.java     [Task 5.2 新建]
```
