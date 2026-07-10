# CPS MCP 身份校验器构造器修复设计

## 背景

后端启动时，Spring 创建 `CpsMcpIdentityVerifier` Bean 失败，并报告找不到无参构造器。该组件同时声明了一个公开的生产构造器和一个包级可见的测试构造器；在存在多个构造器且没有明确注入标记时，Spring 未选择生产构造器，转而尝试无参实例化。

## 目标

- 恢复 Spring 应用上下文对 `CpsMcpIdentityVerifier` 的正常创建。
- 保留可注入固定 `Clock` 的测试构造器，确保签名时效测试可重复。
- 不改变 MCP 身份签名、租户校验、有效期校验、nonce 防重放或工具授权语义。

## 设计

在公开的双参数构造器上添加 Spring `@Autowired` 标记，使其成为容器创建 Bean 时唯一明确的候选构造器：

```java
@Autowired
public CpsMcpIdentityVerifier(QijiAiProperties properties, CpsMcpNonceStore nonceStore) {
    this(properties, nonceStore, Clock.systemUTC());
}
```

包级三参数构造器保持不变，仅供同包测试传入固定时钟。不会新增 `Clock` Bean、无参构造器或可空依赖，也不会调整现有验证流程。

## 回归测试

先在 `CpsMcpIdentityVerifierTest` 中增加一个最小 Spring 容器测试：注册 `QijiAiProperties`、`CpsMcpNonceStore` 和 `CpsMcpIdentityVerifier`，刷新上下文并断言能够取得校验器 Bean。

该测试在修复前应因 `No default constructor found` 失败；添加 `@Autowired` 后应通过。随后运行现有 MCP 身份验证、授权和工具配置测试，并编译 CPS biz 模块。

## 边界与风险

- 改动仅涉及一个构造器注解和一个启动回归测试。
- 不修改用户当前工作区中的前端、配置或其他未提交文件。
- 主要风险是测试没有真实覆盖 Spring 构造器选择；因此测试必须使用真实 Spring `ApplicationContext`，不能只通过反射检查注解。
