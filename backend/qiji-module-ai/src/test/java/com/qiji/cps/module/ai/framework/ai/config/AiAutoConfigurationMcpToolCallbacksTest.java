package com.qiji.cps.module.ai.framework.ai.config;

import com.qiji.cps.module.ai.tool.method.PersonService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiAutoConfigurationMcpToolCallbacksTest {

    @Test
    void personDemoTools_requireExplicitOptIn() throws NoSuchMethodException {
        ConditionalOnProperty conditional = AiAutoConfiguration.class
                .getDeclaredMethod("toolCallbacks", PersonService.class)
                .getAnnotation(ConditionalOnProperty.class);

        assertNotNull(conditional);
        assertEquals("qiji.ai.mcp.demo-tools.enabled", conditional.value()[0]);
        assertEquals("true", conditional.havingValue());
    }
}
