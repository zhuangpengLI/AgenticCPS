# CPS MCP Identity Verifier Constructor Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore backend startup by making the production constructor of `CpsMcpIdentityVerifier` an explicit Spring injection point.

**Architecture:** Add a real Spring application-context regression test around the existing component, then mark its two-argument production constructor with `@Autowired`. Preserve the package-private constructor that accepts a fixed `Clock`, and leave all MCP identity and authorization behavior unchanged.

**Tech Stack:** Java 17+, Spring Framework application context, Spring dependency injection, JUnit 5, Maven

---

## File Structure

- Modify `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/mcp/security/CpsMcpIdentityVerifierTest.java` to cover real Spring constructor selection.
- Modify `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/security/CpsMcpIdentityVerifier.java` to identify the production constructor as the injection constructor.

### Task 1: Lock and repair Spring Bean construction

**Files:**
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/test/java/com/qiji/cps/module/cps/mcp/security/CpsMcpIdentityVerifierTest.java`
- Modify: `backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/mcp/security/CpsMcpIdentityVerifier.java`

- [ ] **Step 1: Write the failing Spring context regression test**

Add the imports:

```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
```

Add this test to `CpsMcpIdentityVerifierTest`:

```java
@Test
void springContext_createsVerifierWithProductionConstructor() {
    assertDoesNotThrow(() -> {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(QijiAiProperties.class, () -> properties(SECRET));
            context.registerBean(CpsMcpNonceStore.class, InMemoryNonceStore::new);
            context.register(CpsMcpIdentityVerifier.class);
            context.refresh();
            context.getBean(CpsMcpIdentityVerifier.class);
        }
    });
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run from `backend`:

```bash
mvn -B -ntp test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsMcpIdentityVerifierTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: the new test fails with a `BeanCreationException` rooted in `No default constructor found` for `CpsMcpIdentityVerifier`.

- [ ] **Step 3: Mark the production constructor for injection**

Add the import:

```java
import org.springframework.beans.factory.annotation.Autowired;
```

Change only the public production constructor:

```java
@Autowired
public CpsMcpIdentityVerifier(QijiAiProperties properties, CpsMcpNonceStore nonceStore) {
    this(properties, nonceStore, Clock.systemUTC());
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same Maven command. Expected: all `CpsMcpIdentityVerifierTest` tests pass with no constructor-instantiation error.

- [ ] **Step 5: Run the related MCP security regression suite**

Run from `backend`:

```bash
mvn -B -ntp test -pl qiji-module-cps/qiji-module-cps-biz -am -Dtest=CpsMcpIdentityVerifierTest,CpsMcpAuthorizationServiceTest,CpsMcpToolConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: all selected tests pass.

- [ ] **Step 6: Compile the CPS biz module and dependencies**

Run from `backend`:

```bash
mvn -B -ntp compile -pl qiji-module-cps/qiji-module-cps-biz -am -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Review the final diff and commit the repair**

Stage only the verifier, its test, and this plan. Commit using the repository Lore protocol with test evidence recorded in `Tested:`.
