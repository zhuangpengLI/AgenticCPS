package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.module.cps.dal.dataobject.onboarding.CpsPlatformOnboardingDraftDO;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import org.junit.jupiter.api.Test;

import static com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingTestFixtures.validPayload;
import static com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingTestFixtures.vendor;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsPlatformOnboardingModelTest {

    @Test
    void vendorToString_shouldExposeIdentifiersWithoutSecrets() {
        CpsOnboardingVendor vendor = vendor("dataoke");
        vendor.setAppKey("vendor-app-key-sensitive");
        vendor.setAppSecret("vendor-app-secret-sensitive");
        vendor.setAuthToken("vendor-auth-token-sensitive");
        vendor.setExtraConfig("{\"vendorSensitive\":\"vendor-extra-sensitive\"}");

        String result = vendor.toString();

        assertTrue(result.contains("vendorCode=dataoke"));
        assertTrue(result.contains("platformCode=taobao"));
        assertFalse(result.contains("vendor-app-key-sensitive"));
        assertFalse(result.contains("vendor-app-secret-sensitive"));
        assertFalse(result.contains("vendor-auth-token-sensitive"));
        assertFalse(result.contains("vendor-extra-sensitive"));
    }

    @Test
    void payloadToString_shouldExposeIdentifiersWithoutNestedConfiguration() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getPlatform().setExtraConfig("{\"platformSensitive\":\"platform-extra-sensitive\"}");
        CpsOnboardingVendor vendor = payload.getVendors().get(0);
        vendor.setAppKey("payload-app-key-sensitive");
        vendor.setAppSecret("payload-app-secret-sensitive");
        vendor.setAuthToken("payload-auth-token-sensitive");
        vendor.setExtraConfig("{\"vendorSensitive\":\"payload-vendor-extra-sensitive\"}");

        String result = payload.toString();

        assertTrue(result.contains("platformCode=taobao"));
        assertTrue(result.contains("primaryVendorCode=dataoke"));
        assertFalse(result.contains("payload-app-key-sensitive"));
        assertFalse(result.contains("payload-app-secret-sensitive"));
        assertFalse(result.contains("payload-auth-token-sensitive"));
        assertFalse(result.contains("payload-vendor-extra-sensitive"));
        assertFalse(result.contains("platform-extra-sensitive"));
    }

    @Test
    void draftToString_shouldNotExposeEncryptedPayloadPlaintext() {
        CpsPlatformOnboardingDraftDO draft = CpsPlatformOnboardingDraftDO.builder()
                .id(1L)
                .platformCode("taobao")
                .mode("RECONFIGURE")
                .payloadCiphertext("{\"appSecret\":\"draft-secret-sensitive\"}")
                .draftVersion(3)
                .status("DRAFT")
                .build();

        String result = draft.toString();

        assertTrue(result.contains("platformCode=taobao"));
        assertTrue(result.contains("draftVersion=3"));
        assertFalse(result.contains("draft-secret-sensitive"));
    }

}
