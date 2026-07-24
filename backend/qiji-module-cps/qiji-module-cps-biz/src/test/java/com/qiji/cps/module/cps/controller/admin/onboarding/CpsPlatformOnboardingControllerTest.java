package com.qiji.cps.module.cps.controller.admin.onboarding;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsPlatformOnboardingControllerTest {

    @Test
    void controller_declares_exact_onboarding_permissions() throws Exception {
        Path source = Path.of("src/main/java/com/qiji/cps/module/cps/controller/admin/onboarding/"
                + "CpsPlatformOnboardingController.java");
        if (!Files.exists(source)) {
            source = Path.of("backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/"
                    + "controller/admin/onboarding/CpsPlatformOnboardingController.java");
        }
        if (!Files.exists(source)) {
            source = Path.of("qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/"
                    + "controller/admin/onboarding/CpsPlatformOnboardingController.java");
        }
        String text = Files.readString(source, StandardCharsets.UTF_8);
        assertTrue(text.contains("cps:platform-onboarding:query"));
        assertTrue(text.contains("cps:platform-onboarding:publish"));
    }
}
