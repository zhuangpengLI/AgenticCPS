---
name: unit-test-generation
description: 生成符合项目规范的后端单元测试和集成测试代码。当用户要求编写测试、生成测试、补充测试用例、或创建 *Test.java 文件时触发。触发词包括："写测试"、"生成测试"、"单元测试"、"集成测试"、"测试用例"、"write test"、"generate test"、"unit test"。
license: MIT
metadata:
  author: AgenticCPS
  version: "1.0"
---

# 后端单元测试生成规则

当为 AgenticCPS 后端（ruoyi-vue-pro）生成测试代码时，**必须**遵循以下规范。

## 1. 测试基类选择（必须）

根据被测类的依赖决定继承哪个基类：

| 场景 | 基类 | 包路径 |
|------|------|--------|
| 纯逻辑（无 Spring 容器） | `BaseMockitoUnitTest` | `cn.iocoder.yudao.framework.test.core.ut` |
| 依赖数据库（Service/Mapper） | `BaseDbUnitTest` | 同上 |
| 仅依赖 Redis | `BaseRedisUnitTest` | 同上 |
| 同时依赖数据库 + Redis | `BaseDbAndRedisUnitTest` | 同上 |

**决策规则**：
- Service 依赖本模块 Mapper → `BaseDbUnitTest`（最常见）
- Service 依赖本模块 Mapper + Redis 缓存 → `BaseDbAndRedisUnitTest`
- Controller 测试 / 纯工具类测试 → `BaseMockitoUnitTest`

## 2. 测试类结构（必须）

### 2.1 继承 BaseDbUnitTest 的 Service 测试

```java
@Import({ClassName}ServiceImpl.class)
public class {ClassName}ServiceImplTest extends BaseDbUnitTest {

    @Resource
    private {ClassName}ServiceImpl {classNameVar}Service;  // 被测 Service

    @Resource
    private {ClassName}Mapper {classNameVar}Mapper;  // 本模块 Mapper（走 H2）

    @MockitoBean
    private OtherModuleService otherService;  // 其他模块依赖（走 Mock）

    // ========== 测试方法 ==========

    // ... CRUD 测试方法

    // ========== 随机对象 ==========

    @SafeVarargs
    private static {ClassName}DO random{ClassName}DO(Consumer<{ClassName}DO>... consumers) {
        Consumer<{ClassName}DO> consumer = (o) -> {
            // 设置枚举字段的合法值
        };
        return RandomUtils.randomPojo({ClassName}DO.class, ArrayUtils.append(consumer, consumers));
    }
}
```

**关键注入规则**：
- 本模块 Mapper → `@Resource`（走 H2 内存数据库）
- 被测 Service 实现类 → `@Resource` + 类顶部 `@Import`
- 其他模块 Service/API → `@MockitoBean`（Mock 注入 Spring 容器）

### 2.2 继承 BaseMockitoUnitTest 的测试

```java
public class {ClassName}ControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private {ClassName}Controller controller;  // 被测对象

    @Mock
    private {ClassName}Service service;  // Mock 依赖
}
```

**关键注入规则**：
- 被测对象 → `@InjectMocks`
- 所有依赖 → `@Mock`

## 3. 测试方法命名（必须）

格式：`test{MethodName}_{scenario}`

| 场景 | 命名示例 |
|------|---------|
| 成功路径 | `testCreateXxx_success` |
| 数据不存在 | `testUpdateXxx_notExists` |
| 业务规则校验失败 | `testDeleteXxx_canNotDeleteSystemType` |
| 状态非法 | `testUpdateXxxStatus_changeStatusInvalid` |
| 唯一性冲突 | `testValidateXxxKeyUnique_keyDuplicateForCreate` |
| 分页查询 | `testGetXxxPage` |
| 单条查询 | `testGetXxx` |

## 4. 测试方法内部结构（必须遵循 AAA + 标准注释）

每个测试方法**必须**使用以下中文注释分段：

```java
@Test
public void testCreateXxx_success() {
    // 准备参数
    XxxSaveReqVO reqVO = randomPojo(XxxSaveReqVO.class).setId(null);

    // 调用
    Long id = xxxService.createXxx(reqVO);
    // 断言
    assertNotNull(id);
    // 校验记录的属性是否正确
    XxxDO xxx = xxxMapper.selectById(id);
    assertPojoEquals(reqVO, xxx, "id");
}
```

**标准注释列表**（按场景使用）：
- `// 准备参数` — 构造入参
- `// mock 数据` — 向 DB 插入数据（mapper.insert）
- `// mock 方法` 或 `// mock 方法（xxx）` — when/thenReturn
- `// 调用` — 执行被测方法
- `// 断言` — 验证返回值
- `// 校验记录的属性是否正确` — 从 DB 查询验证写操作
- `// 校验数据不存在了` — 验证删除成功
- `// 校验调用` — verify Mock 调用
- `// 调用, 并断言异常` — 异常路径
- `// 测试 xxx 不匹配` — 分页查询不匹配数据

## 5. 测试数据准备（必须）

### 5.1 使用 randomPojo() 生成数据

```java
// 基本随机对象
XxxDO xxx = randomPojo(XxxDO.class);

// Consumer 回调定制字段
XxxDO xxx = randomPojo(XxxDO.class, o -> {
    o.setName("测试");
    o.setStatus(CommonStatusEnum.ENABLE.getStatus());
});
```

### 5.2 每个测试类底部定义 random{ClassName}DO()

```java
// ========== 随机对象 ==========

@SafeVarargs
private static XxxDO randomXxxDO(Consumer<XxxDO>... consumers) {
    Consumer<XxxDO> consumer = (o) -> {
        o.setType(randomEle(XxxTypeEnum.values()).getType());
    };
    return RandomUtils.randomPojo(XxxDO.class, ArrayUtils.append(consumer, consumers));
}
```

### 5.3 分页查询测试使用 cloneIgnoreId()

```java
XxxDO dbXxx = randomXxxDO(o -> { /* 设置可查到的值 */ });
xxxMapper.insert(dbXxx);
xxxMapper.insert(cloneIgnoreId(dbXxx, o -> o.setName("不匹配")));    // name 不匹配
xxxMapper.insert(cloneIgnoreId(dbXxx, o -> o.setStatus(999)));        // status 不匹配
```

## 6. 断言规范（必须）

| 断言工具 | 用途 | 示例 |
|---------|------|------|
| `assertPojoEquals(expected, actual)` | 对象属性逐字段比对 | 写操作后验证 |
| `assertPojoEquals(expected, actual, "id")` | 忽略 id 字段比对 | 创建操作验证 |
| `assertServiceException(executable, ERROR_CODE)` | 验证业务异常 | 异常路径测试 |
| `assertEquals(expected, actual)` | 精确值比较 | 单个字段验证 |
| `assertNotNull(value)` | 非空检查 | 创建返回 ID |
| `assertNull(value)` | 为空检查 | 删除后查询 |

**导入语句**（必须包含）：
```java
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static org.junit.jupiter.api.Assertions.*;
```

## 7. Mock 策略（必须）

```java
// when/thenReturn 模式
when(userService.getUserByUsername(eq(username))).thenReturn(user);

// verify 验证调用
verify(schedulerManager).addJob(eq(id), eq(name), eq(param), eq(cron));

// argThat 复杂参数验证
verify(loginLogService).createLoginLog(
    argThat(o -> o.getLogType().equals(type) && o.getResult().equals(result))
);

// 静态方法 Mock（必须 try-with-resources）
try (MockedStatic<SpringUtil> mock = mockStatic(SpringUtil.class)) {
    mock.when(() -> SpringUtil.getBean(eq(name))).thenReturn(bean);
    // 测试逻辑
}

// 私有字段注入
ReflectUtil.setFieldValue(service, "fieldName", value);
```

## 8. 标准 CRUD 测试模板

### 创建成功
```java
@Test
public void testCreateXxx_success() {
    // 准备参数
    XxxSaveReqVO reqVO = randomPojo(XxxSaveReqVO.class).setId(null);
    // 调用
    Long id = xxxService.createXxx(reqVO);
    // 断言
    assertNotNull(id);
    // 校验记录的属性是否正确
    XxxDO xxx = xxxMapper.selectById(id);
    assertPojoEquals(reqVO, xxx, "id");
}
```

### 更新成功
```java
@Test
public void testUpdateXxx_success() {
    // mock 数据
    XxxDO dbXxx = randomXxxDO();
    xxxMapper.insert(dbXxx);
    // 准备参数
    XxxSaveReqVO reqVO = randomPojo(XxxSaveReqVO.class, o -> o.setId(dbXxx.getId()));
    // 调用
    xxxService.updateXxx(reqVO);
    // 校验是否更新正确
    XxxDO xxx = xxxMapper.selectById(reqVO.getId());
    assertPojoEquals(reqVO, xxx);
}
```

### 删除成功
```java
@Test
public void testDeleteXxx_success() {
    // mock 数据
    XxxDO dbXxx = randomXxxDO();
    xxxMapper.insert(dbXxx);
    // 准备参数
    Long id = dbXxx.getId();
    // 调用
    xxxService.deleteXxx(id);
    // 校验数据不存在了
    assertNull(xxxMapper.selectById(id));
}
```

### 不存在异常
```java
@Test
public void testDeleteXxx_notExists() {
    // 准备参数
    Long id = randomLongId();
    // 调用, 并断言异常
    assertServiceException(() -> xxxService.deleteXxx(id), XXX_NOT_EXISTS);
}
```

### 分页查询
```java
@Test
public void testGetXxxPage() {
    // mock 数据
    XxxDO dbXxx = randomXxxDO(o -> {
        o.setName("测试名称");
        o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        o.setCreateTime(buildTime(2021, 2, 1));
    });
    xxxMapper.insert(dbXxx);
    // 测试 name 不匹配
    xxxMapper.insert(cloneIgnoreId(dbXxx, o -> o.setName("其他")));
    // 测试 status 不匹配
    xxxMapper.insert(cloneIgnoreId(dbXxx, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
    // 测试 createTime 不匹配
    xxxMapper.insert(cloneIgnoreId(dbXxx, o -> o.setCreateTime(buildTime(2021, 1, 1))));
    // 准备参数
    XxxPageReqVO reqVO = new XxxPageReqVO();
    reqVO.setName("测试");
    reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
    reqVO.setCreateTime(buildBetweenTime(2021, 1, 15, 2021, 2, 15));
    // 调用
    PageResult<XxxDO> pageResult = xxxService.getXxxPage(reqVO);
    // 断言
    assertEquals(1, pageResult.getTotal());
    assertEquals(1, pageResult.getList().size());
    assertPojoEquals(dbXxx, pageResult.getList().get(0));
}
```

### 单条查询
```java
@Test
public void testGetXxx() {
    // mock 数据
    XxxDO dbXxx = randomXxxDO();
    xxxMapper.insert(dbXxx);
    // 准备参数
    Long id = dbXxx.getId();
    // 调用
    XxxDO xxx = xxxService.getXxx(id);
    // 断言
    assertNotNull(xxx);
    assertPojoEquals(dbXxx, xxx);
}
```

## 9. 测试资源文件（必须同步维护）

当为新模块/新表生成测试时，**必须**同步维护：

### create_tables.sql（H2 兼容语法）

```sql
CREATE TABLE IF NOT EXISTS "cps_xxx" (
    "id" bigint(20) NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    "name" varchar(100) NOT NULL DEFAULT '',
    "status" tinyint NOT NULL,
    "creator" varchar(64) DEFAULT '',
    "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater" varchar(64) DEFAULT '',
    "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" bit NOT NULL DEFAULT FALSE,
    "tenant_id" bigint NOT NULL DEFAULT '0',
    PRIMARY KEY ("id")
);
```

**H2 兼容要点**：
- 主键自增：`GENERATED BY DEFAULT AS IDENTITY`（不用 AUTO_INCREMENT）
- 字段名/表名：双引号包裹
- 大文本：`clob`（不用 text/longtext）
- 布尔：`bit`

### clean.sql
```sql
DELETE FROM "cps_xxx";
```

**每新增一个表，必须同步添加 DELETE 语句。**

## 10. CPS 模块特别规则

- 金额字段使用 `Integer`（单位：分），**绝不使用 BigDecimal**
- 所有表包含 `tenant_id` 字段（多租户隔离）
- 所有表包含 `deleted` 字段（软删除）
- CPS 表名统一 `cps_` 前缀
- 订单状态流转测试需覆盖：已下单 → 已付款 → 已收货 → 已结算 → 已到账，以及退款/失效分支

## 11. 覆盖率要求

- 每个 Service public 方法：至少覆盖 **成功路径 + 所有异常路径**
- 分页查询：覆盖 **所有查询条件**（每个条件一条不匹配数据）
- 状态变更：覆盖 **所有合法/非法状态转换**
- 校验逻辑：覆盖 **存在性、唯一性、状态** 等校验规则

## 12. 禁止事项

- **禁止** 使用 `@Autowired` 注入，统一使用 `@Resource`
- **禁止** 在测试中使用 `Thread.sleep()` 等待
- **禁止** 在 BaseDbUnitTest 子类中使用 `@Mock`（应使用 `@MockitoBean`）
- **禁止** 在 BaseMockitoUnitTest 子类中使用 `@MockitoBean`（应使用 `@Mock`）
- **禁止** 硬编码 ID 值作为已有数据（应通过 mapper.insert 插入后获取）
- **禁止** 金额使用 Double/BigDecimal（必须 Integer 分） 
- **禁止** 忽略 clean.sql / create_tables.sql 的同步更新
