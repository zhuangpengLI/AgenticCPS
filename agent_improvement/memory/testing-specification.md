# 单元测试与集成测试规范

> 基于 AgenticCPS (ruoyi-vue-pro) 后端项目现有测试代码分析总结的测试规范，用于指导 AI 助手生成高质量测试代码。

## 1. 测试基类体系

项目提供 4 个测试基类，位于 `yudao-framework/yudao-spring-boot-starter-test`，根据测试场景选择继承：

| 基类 | 适用场景 | 关键能力 |
|------|---------|---------|
| `BaseMockitoUnitTest` | 纯逻辑单元测试（不需要 Spring 容器） | Mockito 扩展 |
| `BaseDbUnitTest` | 需要数据库的 Service/Mapper 测试 | H2 内存数据库 + MyBatis Plus |
| `BaseRedisUnitTest` | 需要 Redis 的测试 | 内嵌 Redis (jedis-mock, 端口 16379) |
| `BaseDbAndRedisUnitTest` | 同时需要数据库 + Redis 的测试 | H2 + Redis 全能力 |

### 1.1 选择基类的决策规则

```
如果 Service 不依赖数据库和 Redis → BaseMockitoUnitTest
如果 Service 依赖本模块 Mapper (数据库操作) → BaseDbUnitTest
如果 Service 依赖 Redis 缓存但不依赖数据库 → BaseRedisUnitTest
如果 Service 同时依赖数据库和 Redis → BaseDbAndRedisUnitTest
Controller 纯逻辑测试 → BaseMockitoUnitTest
```

### 1.2 基类注解说明

```java
// BaseDbUnitTest 的关键注解：
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BaseDbUnitTest.Application.class)
@ActiveProfiles("unit-test")  // 使用 application-unit-test.yaml
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)  // 每个测试方法后清理数据
```

## 2. 测试类结构模板

### 2.1 Service 层测试（继承 BaseDbUnitTest）

这是最常见的测试类型。对于自己模块的 Mapper 走 H2 内存数据库，对于其他模块的 Service 走 Mock。

```java
package cn.iocoder.yudao.module.{moduleName}.service.{businessName};

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.test.core.util.RandomUtils;
// ... 其他必要 import

import jakarta.annotation.Resource;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.{moduleName}.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import({ClassName}ServiceImpl.class)  // 导入被测 Service 实现类
public class {ClassName}ServiceImplTest extends BaseDbUnitTest {

    @Resource
    private {ClassName}ServiceImpl {classNameVar}Service;  // 被测 Service

    @Resource
    private {ClassName}Mapper {classNameVar}Mapper;  // 本模块 Mapper（走 H2）

    @MockitoBean
    private OtherModuleService otherService;  // 其他模块依赖（走 Mock）

    // ========== 测试方法 ==========

    // ... 测试方法

    // ========== 随机对象 ==========

    @SafeVarargs
    private static {ClassName}DO random{ClassName}DO(Consumer<{ClassName}DO>... consumers) {
        Consumer<{ClassName}DO> consumer = (o) -> {
            // 对枚举类型字段设置合法值
            o.setType(randomEle({ClassName}TypeEnum.values()).getType());
        };
        return RandomUtils.randomPojo({ClassName}DO.class, ArrayUtils.append(consumer, consumers));
    }
}
```

### 2.2 Service 层测试（继承 BaseMockitoUnitTest）

适用于不需要数据库的纯逻辑 Service 或 Controller 测试。

```java
@ExtendWith(MockitoExtension.class)  // 由基类提供
public class {ClassName}ServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private {ClassName}ServiceImpl {classNameVar}Service;  // 被测对象

    @Mock
    private {ClassName}Mapper {classNameVar}Mapper;  // Mock 依赖

    @Mock
    private OtherService otherService;  // Mock 依赖

    // ... 测试方法
}
```

### 2.3 Controller 层测试

```java
public class {ClassName}ControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private {ClassName}Controller {classNameVar}Controller;

    @Mock
    private {ClassName}Service {classNameVar}Service;

    // ... 测试方法
}
```

## 3. 测试方法命名规范

### 3.1 命名模式

采用 `test{MethodName}_{scenario}` 格式：

| 场景 | 命名示例 | 说明 |
|------|---------|------|
| 成功路径 | `testCreateConfig_success()` | 正常创建成功 |
| 不存在 | `testUpdateConfig_notExists()` | 数据不存在时的异常 |
| 业务校验失败 | `testDeleteConfig_canNotDeleteSystemType()` | 业务规则校验失败 |
| 状态校验 | `testUpdateJobStatus_changeStatusInvalid()` | 状态变更不合法 |
| 参数校验 | `testCreateJob_cronExpressionValid()` | 参数格式不正确 |
| 唯一性校验 | `testValidateConfigKeyUnique_keyDuplicateForCreate()` | 唯一键重复 |
| 分页查询 | `testGetConfigPage()` | 分页查询测试 |
| 单条查询 | `testGetConfig()` | 根据 ID 查询 |
| 列表查询 | `testGetConfigList()` | 列表查询 |

### 3.2 方法粒度原则

- **一个测试方法只测试一个场景**
- CRUD 操作的每个分支路径都应有独立的测试方法
- 异常路径和正常路径分开测试

## 4. 测试方法内部结构（AAA 模式）

**所有测试方法必须遵循三段式结构**，并使用标准化中文注释分隔：

```java
@Test
public void testCreateConfig_success() {
    // 准备参数
    ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class)
            .setId(null); // 防止 id 被赋值

    // 调用
    Long configId = configService.createConfig(reqVO);
    // 断言
    assertNotNull(configId);
    // 校验记录的属性是否正确
    ConfigDO config = configMapper.selectById(configId);
    assertPojoEquals(reqVO, config, "id");
    assertEquals(ConfigTypeEnum.CUSTOM.getType(), config.getType());
}
```

### 4.1 标准注释标记

| 注释 | 含义 | 使用场景 |
|------|------|---------|
| `// 准备参数` | 构造方法入参 | Arrange 阶段 |
| `// mock 数据` | 向数据库插入测试数据 | 当需要已有数据时 |
| `// mock 方法` / `// mock 方法（xxx）` | 设置 Mock 行为 | 使用 when/thenReturn |
| `// 调用` | 执行被测方法 | Act 阶段 |
| `// 断言` | 验证返回值 | Assert 阶段 |
| `// 校验记录的属性是否正确` | 从 DB 查数据验证 | 写操作后验证 |
| `// 校验数据不存在了` | 验证删除成功 | 删除操作后 |
| `// 校验调用` / `// 校验调用参数` | verify Mock 调用 | 验证 Mock 交互 |
| `// 调用，并断言异常` | 执行并验证异常 | 异常路径测试 |
| `// 调用成功` | 执行验证无异常 | 校验方法成功路径 |
| `// 测试 xxx 不匹配` | 分页查询中插入不匹配数据 | 分页查询测试 |

## 5. 测试数据准备

### 5.1 randomPojo() — 核心数据生成方法

使用 PODAM 库自动生成随机对象，支持 Consumer 回调进行定制：

```java
// 基本用法：生成完全随机的对象
ConfigDO config = randomPojo(ConfigDO.class);

// 带回调：定制指定字段
ConfigDO config = randomPojo(ConfigDO.class, o -> {
    o.setName("测试名称");
    o.setStatus(CommonStatusEnum.ENABLE.getStatus());
});

// 多个回调链
ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
    o.setId(null);  // 创建时不设 ID
});
```

**PODAM 内置的智能字段生成规则**（在 RandomUtils 的 static 块中配置）：
- `String` 字段：生成 10 位随机字符串
- `status` 字段：随机 CommonStatusEnum 值（0 或 1）
- 以 `type`/`status`/`category`/`scope`/`result` 结尾的 Integer 字段：生成 0~127 范围的 tinyint 值
- `LocalDateTime` 字段：生成随机日期，纳秒设为 0（兼容数据库存储）
- `deleted` 字段：固定为 false（未删除状态）

### 5.2 私有辅助方法 random{ClassName}DO()

**每个测试类底部定义**一个专用的随机数据工厂方法，用于设置该 DO 特有的枚举字段合法值：

```java
// ========== 随机对象 ==========

@SafeVarargs
private static ConfigDO randomConfigDO(Consumer<ConfigDO>... consumers) {
    Consumer<ConfigDO> consumer = (o) -> {
        o.setType(randomEle(ConfigTypeEnum.values()).getType()); // 保证 type 字段合法
    };
    return RandomUtils.randomPojo(ConfigDO.class, ArrayUtils.append(consumer, consumers));
}
```

### 5.3 cloneIgnoreId() — 分页查询测试利器

用于基于已有对象创建新变体，同时自动忽略 id 字段，避免唯一性冲突：

```java
// 典型的分页查询测试模式
ConfigDO dbConfig = randomConfigDO(o -> {
    o.setName("芋艿");
    o.setConfigKey("yunai");
    o.setType(ConfigTypeEnum.SYSTEM.getType());
    o.setCreateTime(buildTime(2021, 2, 1));
});
configMapper.insert(dbConfig);

// 插入不匹配的数据：每次只改变一个查询条件，其余保持一致
configMapper.insert(cloneIgnoreId(dbConfig, o -> o.setName("土豆")));       // name 不匹配
configMapper.insert(cloneIgnoreId(dbConfig, o -> o.setConfigKey("tudou"))); // key 不匹配
configMapper.insert(cloneIgnoreId(dbConfig, o -> o.setType(ConfigTypeEnum.CUSTOM.getType()))); // type 不匹配
configMapper.insert(cloneIgnoreId(dbConfig, o -> o.setCreateTime(buildTime(2021, 1, 1))));     // 时间不匹配
```

### 5.4 常用随机数据工具

```java
randomString()          // 10 位随机字符串
randomLongId()          // 随机 Long ID
randomInteger()         // 随机 Integer
randomLocalDateTime()   // 随机 LocalDateTime（纳秒为 0）
randomCommonStatus()    // 随机 0 或 1
randomEmail()           // 随机邮箱 (xxx@qq.com)
randomMobile()          // 随机手机号 (13800138xxx)
randomURL()             // 随机 URL
randomPojoList(clazz)   // 随机对象列表（1~5 个元素）
```

## 6. 断言和验证规范

### 6.1 assertPojoEquals() — 对象属性比对

**核心断言工具**，逐字段比较两个对象的属性值：

```java
// 基本用法：比对所有匹配字段
assertPojoEquals(reqVO, config);

// 忽略指定字段：创建时忽略 id（由数据库自动生成）
assertPojoEquals(reqVO, config, "id");

// 特点：
// 1. 宽松模式：expected 有的字段 actual 没有 → 自动忽略
// 2. 逐字段比较 + 友好的错误消息："Field(name) 不匹配"
// 3. 忽略 Jacoco 自动生成的 $jacocoData 合成字段
```

### 6.2 assertServiceException() — 业务异常断言

专用于验证 ServiceException 的错误码和错误消息：

```java
// 基本用法
assertServiceException(() -> configService.deleteConfig(id), CONFIG_NOT_EXISTS);

// 带消息参数
assertServiceException(() -> authService.validateCaptcha(reqVO),
        AUTH_LOGIN_CAPTCHA_CODE_ERROR, "就是不对");

// 内部机制：
// 1. 断言抛出 ServiceException
// 2. 验证 errorCode.getCode() 匹配
// 3. 通过 ServiceExceptionUtil.doFormat() 格式化消息后验证 message 匹配
```

### 6.3 标准 JUnit 断言

```java
assertNotNull(configId);                             // 非空校验
assertNull(configMapper.selectById(id));             // 删除后为空
assertEquals(1, pageResult.getTotal());              // 精确值比较
assertEquals(ConfigTypeEnum.CUSTOM.getType(), config.getType());
assertTrue(path.matches("\\d{8}/test_\\d+\\.jpg"));  // 正则匹配
assertSame(result, content);                          // 引用相同
```

### 6.4 Mockito verify() — Mock 调用验证

```java
// 验证方法被调用
verify(schedulerManager).addJob(eq(job.getId()), eq(job.getHandlerName()),
        eq(job.getHandlerParam()), eq(job.getCronExpression()),
        eq(reqVO.getRetryCount()), eq(reqVO.getRetryInterval()));

// 使用 argThat 进行复杂参数验证
verify(loginLogService).createLoginLog(
        argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                && o.getUserId().equals(user.getId()))
);

// 验证从未调用
verify(loginLogService, never()).createLoginLog(any());

// 验证 void 方法调用
verify(client).delete(eq("tudou.jpg"));
```

## 7. Mock 策略

### 7.1 @MockitoBean vs @Mock

| 注解 | 适用基类 | 注入方式 | 使用场景 |
|------|---------|---------|---------|
| `@MockitoBean` | BaseDbUnitTest / BaseRedisUnitTest | 注入到 Spring 容器 | 替换 Spring 容器中的真实 Bean |
| `@Mock` | BaseMockitoUnitTest | Mockito 自动注入 | 配合 `@InjectMocks` 使用 |
| `@InjectMocks` | BaseMockitoUnitTest | Mockito 自动注入 | 自动将 @Mock 注入到被测对象 |

### 7.2 Mock 行为定义

```java
// 基本 when/thenReturn
when(userService.getUserByUsername(eq(username))).thenReturn(user);

// 返回 null（默认行为，可省略）
when(userService.getUserByUsername(eq("nonexistent"))).thenReturn(null);

// argThat 复杂条件匹配
when(client.upload(same(content), argThat(path -> {
    assertTrue(path.matches(directory + "/\\d{8}/" + name + "_\\d+.jpg"));
    return true;
}), eq(type))).thenReturn(url);

// void 方法的 Mock
doNothing().when(smsCodeApi).useSmsCode(argThat(req -> {
    assertEquals(mobile, req.getMobile());
    return true;
}));

// 静态方法 Mock（需要 try-with-resources）
try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
    springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(handlerName)))
            .thenReturn(jobLogCleanJob);
    // ... 测试逻辑
}
```

### 7.3 私有字段注入

当需要设置被测对象的私有字段（如 @Value 配置值）时：

```java
@BeforeEach
public void setUp() {
    // 使用 Hutool 的 ReflectUtil 设置私有字段
    ReflectUtil.setFieldValue(authService, "validator",
            Validation.buildDefaultValidatorFactory().getValidator());
    // 或直接通过 setter（如果有）
    authService.setCaptchaEnable(true);
}
```

## 8. 不同测试场景的标准模板

### 8.1 创建操作测试

```java
@Test
public void testCreate{ClassName}_success() {
    // 准备参数
    {ClassName}SaveReqVO reqVO = randomPojo({ClassName}SaveReqVO.class)
            .setId(null); // 防止 id 被赋值

    // 调用
    Long {classNameVar}Id = {classNameVar}Service.create{ClassName}(reqVO);
    // 断言
    assertNotNull({classNameVar}Id);
    // 校验记录的属性是否正确
    {ClassName}DO {classNameVar} = {classNameVar}Mapper.selectById({classNameVar}Id);
    assertPojoEquals(reqVO, {classNameVar}, "id");
}
```

### 8.2 更新操作测试

```java
@Test
public void testUpdate{ClassName}_success() {
    // mock 数据
    {ClassName}DO db{ClassName} = random{ClassName}DO();
    {classNameVar}Mapper.insert(db{ClassName});
    // 准备参数
    {ClassName}SaveReqVO reqVO = randomPojo({ClassName}SaveReqVO.class, o -> {
        o.setId(db{ClassName}.getId()); // 设置更新的 ID
    });

    // 调用
    {classNameVar}Service.update{ClassName}(reqVO);
    // 校验是否更新正确
    {ClassName}DO {classNameVar} = {classNameVar}Mapper.selectById(reqVO.getId());
    assertPojoEquals(reqVO, {classNameVar});
}
```

### 8.3 删除操作测试

```java
@Test
public void testDelete{ClassName}_success() {
    // mock 数据
    {ClassName}DO db{ClassName} = random{ClassName}DO();
    {classNameVar}Mapper.insert(db{ClassName});
    // 准备参数
    Long id = db{ClassName}.getId();

    // 调用
    {classNameVar}Service.delete{ClassName}(id);
    // 校验数据不存在了
    assertNull({classNameVar}Mapper.selectById(id));
}
```

### 8.4 分页查询测试

```java
@Test
public void testGet{ClassName}Page() {
    // mock 数据
    {ClassName}DO db{ClassName} = random{ClassName}DO(o -> { // 等会查询到
        o.setName("测试名称");
        o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        o.setCreateTime(buildTime(2021, 2, 1));
    });
    {classNameVar}Mapper.insert(db{ClassName});
    // 测试 name 不匹配
    {classNameVar}Mapper.insert(cloneIgnoreId(db{ClassName}, o -> o.setName("其他")));
    // 测试 status 不匹配
    {classNameVar}Mapper.insert(cloneIgnoreId(db{ClassName}, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
    // 测试 createTime 不匹配
    {classNameVar}Mapper.insert(cloneIgnoreId(db{ClassName}, o -> o.setCreateTime(buildTime(2021, 1, 1))));
    // 准备参数
    {ClassName}PageReqVO reqVO = new {ClassName}PageReqVO();
    reqVO.setName("测试");
    reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
    reqVO.setCreateTime(buildBetweenTime(2021, 1, 15, 2021, 2, 15));

    // 调用
    PageResult<{ClassName}DO> pageResult = {classNameVar}Service.get{ClassName}Page(reqVO);
    // 断言
    assertEquals(1, pageResult.getTotal());
    assertEquals(1, pageResult.getList().size());
    assertPojoEquals(db{ClassName}, pageResult.getList().get(0));
}
```

### 8.5 不存在校验测试

```java
@Test
public void testDelete{ClassName}_notExists() {
    // 准备参数
    Long id = randomLongId();

    // 调用, 并断言异常
    assertServiceException(() -> {classNameVar}Service.delete{ClassName}(id), {CLASSNAME}_NOT_EXISTS);
}
```

### 8.6 单条查询测试

```java
@Test
public void testGet{ClassName}() {
    // mock 数据
    {ClassName}DO db{ClassName} = random{ClassName}DO();
    {classNameVar}Mapper.insert(db{ClassName});
    // 准备参数
    Long id = db{ClassName}.getId();

    // 调用
    {ClassName}DO {classNameVar} = {classNameVar}Service.get{ClassName}(id);
    // 断言
    assertNotNull({classNameVar});
    assertPojoEquals(db{ClassName}, {classNameVar});
}
```

## 9. 测试配置文件规范

### 9.1 application-unit-test.yaml

每个模块的 `src/test/resources/` 下需要包含：

```yaml
spring:
  main:
    lazy-initialization: true  # 开启懒加载，加快速度
    banner-mode: off           # 禁用 Banner

spring:
  datasource:
    name: ruoyi-vue-pro
    url: jdbc:h2:mem:testdb;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;
    driver-class-name: org.h2.Driver
    username: sa
    password:
    druid:
      async-init: true
      initial-size: 1
  sql:
    init:
      schema-locations: classpath:/sql/create_tables.sql
      encoding: UTF-8
  data:
    redis:
      host: 127.0.0.1
      port: 16379
      database: 0

mybatis-plus:
  lazy-initialization: true
  type-aliases-package: ${yudao.info.base-package}.module.*.dal.dataobject

yudao:
  info:
    base-package: cn.iocoder.yudao
```

### 9.2 create_tables.sql

在 `src/test/resources/sql/create_tables.sql` 中定义 H2 兼容的建表语句：

```sql
CREATE TABLE IF NOT EXISTS "table_name" (
    "id" bigint(20) NOT NULL GENERATED BY DEFAULT AS IDENTITY COMMENT '编号',
    "name" varchar(100) NOT NULL DEFAULT '' COMMENT '名字',
    "status" tinyint NOT NULL COMMENT '状态',
    "creator" varchar(64) DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" bit NOT NULL DEFAULT FALSE,
    "tenant_id" bigint NOT NULL DEFAULT '0',
    PRIMARY KEY ("id")
) COMMENT '表注释';
```

**H2 兼容性要点**：
- 主键自增使用 `GENERATED BY DEFAULT AS IDENTITY`，不使用 MySQL 的 `AUTO_INCREMENT`
- 字段名和表名用双引号包裹
- `bit` 类型代替 `tinyint(1)`
- `timestamp` 和 `datetime` 都可使用
- 大文本使用 `clob` 代替 `text`/`longtext`

### 9.3 clean.sql

在 `src/test/resources/sql/clean.sql` 中列出所有测试表的清理语句：

```sql
DELETE FROM "cps_xxx";
DELETE FROM "cps_yyy";
```

**注意**：每新增一个 `create_tables.sql` 中的表，必须同步在 `clean.sql` 中添加对应的 DELETE 语句。

## 10. 不同层次的测试侧重点

### 10.1 Service 层测试（最重要）

**侧重点**：业务逻辑正确性
- 每个 public 方法至少覆盖：成功路径 + 所有异常路径
- CRUD 操作的数据库交互验证（插入后查询比对）
- 业务校验规则（存在性校验、唯一性校验、状态校验）
- Mock 其他模块的 Service，只关注本模块逻辑
- 分页查询测试需覆盖所有查询条件

**关键做法**：
```
本模块 Mapper → 走 H2 内存数据库（真实数据库操作）
其他模块 Service → 走 @MockitoBean（Mock）
其他模块 API → 走 @MockitoBean（Mock）
```

### 10.2 DAO/Mapper 层测试

通常**不单独测试 Mapper**，而是在 Service 层测试中通过 `mapper.insert()` + `mapper.selectById()` 间接验证。

若 Mapper 有自定义 SQL（非 MyBatis Plus 自动生成），可编写独立测试：

```java
@Import({ClassName}Mapper.class)  // 注意：只导入 Mapper
public class {ClassName}MapperTest extends BaseDbUnitTest {

    @Resource
    private {ClassName}Mapper {classNameVar}Mapper;

    @Test
    public void testSelectByCustomCondition() {
        // mock 数据
        {ClassName}DO db{ClassName} = randomPojo({ClassName}DO.class, o -> { ... });
        {classNameVar}Mapper.insert(db{ClassName});

        // 调用
        List<{ClassName}DO> result = {classNameVar}Mapper.selectByCustomCondition(...);
        // 断言
        assertEquals(1, result.size());
        assertPojoEquals(db{ClassName}, result.get(0));
    }
}
```

### 10.3 Controller 层测试

**侧重点**：请求参数解析、响应格式正确性

```java
public class {ClassName}ControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private {ClassName}Controller controller;

    @Mock
    private {ClassName}Service service;

    @Test
    public void testSomeEndpoint() {
        // 准备参数
        SomeReqVO reqVO = randomPojo(SomeReqVO.class);
        // mock 方法
        when(service.someMethod(eq(reqVO))).thenReturn(expectedResult);

        // 调用
        CommonResult<SomeRespVO> result = controller.someEndpoint(reqVO);
        // 断言
        assertEquals(0, result.getCode());
        assertPojoEquals(expectedResult, result.getData());
    }
}
```

## 11. 特殊场景处理

### 11.1 静态方法 Mock

必须使用 `try-with-resources` 确保 MockedStatic 正确关闭：

```java
@Test
public void testWithStaticMock() {
    try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
        springUtilMockedStatic.when(() -> SpringUtil.getBean(eq("handlerName")))
                .thenReturn(someBean);
        // ... 测试逻辑
    }
}
```

### 11.2 @BeforeEach 初始化

用于在每个测试方法前设置通用状态：

```java
@BeforeEach
public void setUp() {
    // 设置配置项
    authService.setCaptchaEnable(true);
    // 或注入私有字段
    ReflectUtil.setFieldValue(service, "fieldName", value);
    // 或重置静态变量
    FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
}
```

### 11.3 @Disabled 标记

对于尚未完成或需要重构的测试，使用 `@Disabled` 标记而非删除：

```java
@Test
@Disabled // TODO 需要后续重构
public void testSomeMethod() {
    // ...
}
```

### 11.4 日期时间工具

```java
// 构建精确时间点
LocalDateTime time = buildTime(2021, 2, 1);  // 2021-02-01 00:00:00

// 构建时间范围
LocalDateTime[] timeRange = buildBetweenTime(2021, 1, 15, 2021, 2, 15);
```

## 12. CPS 模块测试特别指引

### 12.1 CPS 模块特有的测试场景

| 测试场景 | 基类选择 | 重点验证 |
|---------|---------|---------|
| 平台配置 CRUD | BaseDbUnitTest | 字段合法性、枚举值范围 |
| 返利计算逻辑 | BaseMockitoUnitTest | 优先级规则、金额计算（分为单位） |
| 订单同步服务 | BaseDbUnitTest | 状态流转、幂等性 |
| 平台适配器 | BaseMockitoUnitTest | HTTP 调用 Mock、响应解析 |
| 会员等级+返利 | BaseDbAndRedisUnitTest | 缓存命中逻辑 |

### 12.2 金额字段测试

所有金额使用 Integer（分），测试中也必须使用分为单位：

```java
@Test
public void testCalculateRebate_success() {
    // 准备参数 — 金额单位为分
    Integer goodsPrice = 9900;     // 99.00 元
    Integer commissionRate = 1000; // 10.00%
    Integer expectedRebate = 990;  // 9.90 元

    // 调用
    Integer rebate = rebateService.calculateRebate(goodsPrice, commissionRate);
    // 断言
    assertEquals(expectedRebate, rebate);
}
```

### 12.3 多租户测试

CPS 查询必须包含租户隔离，测试中需关注 tenant_id：

```java
// create_tables.sql 中必须包含 tenant_id 字段
"tenant_id" bigint NOT NULL DEFAULT '0',
```

## 13. 测试 Checklist

编写/审查测试时，确认以下事项：

- [ ] 选择了正确的测试基类
- [ ] `@Import` 导入了被测 Service 实现类
- [ ] 本模块 Mapper 使用 `@Resource` 注入，其他模块依赖使用 `@MockitoBean`
- [ ] 测试方法命名遵循 `test{Method}_{scenario}` 格式
- [ ] 方法内部遵循 AAA（准备/调用/断言）三段式 + 标准中文注释
- [ ] 创建操作测试：`setId(null)` + 查询验证 + `assertPojoEquals(reqVO, do, "id")`
- [ ] 更新操作测试：先插入数据 → 设置更新 ID → 查询验证
- [ ] 删除操作测试：先插入数据 → 删除 → `assertNull(mapper.selectById(id))`
- [ ] 分页查询测试：匹配数据 + 每个条件一条不匹配数据 + `cloneIgnoreId()`
- [ ] 异常路径测试：`assertServiceException()` + 正确的 ErrorCode 常量
- [ ] `create_tables.sql` 包含所有需要的表（H2 兼容语法）
- [ ] `clean.sql` 包含所有表的 DELETE 语句
- [ ] 枚举字段在 `random{ClassName}DO()` 中设置了合法值
- [ ] 金额字段使用 Integer（分），不使用 BigDecimal
