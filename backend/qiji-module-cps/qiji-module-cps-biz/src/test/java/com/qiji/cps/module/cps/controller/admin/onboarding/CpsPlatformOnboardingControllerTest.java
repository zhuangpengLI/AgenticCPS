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
        for (String route : new String[]{"/page", "/get", "/draft", "/validate", "/test",
                "/publish", "/enable", "/disable", "/delete", "/platform-capabilities",
                "/vendor-descriptors"}) {
            assertTrue(text.contains(route), route);
        }
        assertTrue(text.contains("cps:platform-onboarding:create"));
        assertTrue(text.contains("cps:platform-onboarding:update"));
        assertTrue(text.contains("cps:platform-onboarding:delete"));
        assertTrue(text.contains("cps:platform-onboarding:test"));
        String vendorResponse = Files.readString(Path.of(
                source.getParent().toString(), "vo", "CpsOnboardingVendorRespVO.java"),
                StandardCharsets.UTF_8);
        assertTrue(!vendorResponse.contains("String appSecret"));
        assertTrue(!vendorResponse.contains("String authToken"));
    }
}
