package com.qiji.cps.module.cps.service.selection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CpsSelectionAiRecommendServiceClasspathTest {

    @Test
    @DisplayName("服务方法签名 - 不暴露 AI 模型实体类，避免可选 AI 类缺失时启动失败")
    void serviceMethodSignatures_doNotExposeAiModelDo() {
        String aiModelDoClassName = "com.qiji.cps.module.ai.dal.dataobject.model.AiModelDO";

        boolean exposesAiModelDo = Arrays.stream(CpsSelectionAiRecommendService.class.getDeclaredMethods())
                .anyMatch(method -> signatureContains(method, aiModelDoClassName));

        assertFalse(exposesAiModelDo);
    }

    private boolean signatureContains(Method method, String className) {
        if (method.getReturnType().getName().equals(className)) {
            return true;
        }
        return Arrays.stream(method.getParameterTypes())
                .anyMatch(parameterType -> parameterType.getName().equals(className));
    }
}
