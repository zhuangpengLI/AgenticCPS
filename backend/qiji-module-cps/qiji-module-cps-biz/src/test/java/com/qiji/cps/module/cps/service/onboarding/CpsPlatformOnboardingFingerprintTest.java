package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingTestFixtures.validPayload;
import static com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingTestFixtures.vendor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsPlatformOnboardingFingerprintTest {

    private final CpsPlatformOnboardingFingerprint fingerprint =
            new CpsPlatformOnboardingFingerprint(new ObjectMapper());

    @Test
    void calculate_shouldIgnoreVendorAdzoneAndRuleInputOrder() {
        CpsPlatformOnboardingPayload first = validPayload();
        CpsPlatformOnboardingPayload second = validPayload();
        Collections.reverse(second.getVendors());
        Collections.reverse(second.getAdzones());
        Collections.reverse(second.getRebateRules());

        assertEquals(fingerprint.calculate(first), fingerprint.calculate(second));
    }

    @Test
    void calculate_whenBusinessCredentialChanges_shouldChangeFingerprint() {
        CpsPlatformOnboardingPayload original = validPayload();
        CpsPlatformOnboardingPayload changed = validPayload();
        changed.getVendors().get(0).setAppSecret("rotated-secret");

        assertNotEquals(fingerprint.calculate(original), fingerprint.calculate(changed));
    }

    @Test
    void calculate_shouldNotMutateInputCollections() {
        CpsPlatformOnboardingPayload payload = validPayload();
        List<CpsOnboardingVendor> vendorsBefore = List.copyOf(payload.getVendors());
        List<CpsOnboardingAdzone> adzonesBefore = List.copyOf(payload.getAdzones());
        List<CpsOnboardingRebateRule> rulesBefore = List.copyOf(payload.getRebateRules());

        fingerprint.calculate(payload);

        assertEquals(vendorsBefore, payload.getVendors());
        assertEquals(adzonesBefore, payload.getAdzones());
        assertEquals(rulesBefore, payload.getRebateRules());
    }

    @Test
    void calculate_whenCollectionsAreNull_shouldMatchEmptyCollections() {
        CpsPlatformOnboardingPayload nullCollections = validPayload();
        nullCollections.setVendors(null);
        nullCollections.setAdzones(null);
        nullCollections.setRebateRules(null);
        CpsPlatformOnboardingPayload emptyCollections = validPayload();
        emptyCollections.setVendors(new ArrayList<>());
        emptyCollections.setAdzones(new ArrayList<>());
        emptyCollections.setRebateRules(new ArrayList<>());

        assertEquals(fingerprint.calculate(nullCollections), fingerprint.calculate(emptyCollections));
    }

    @Test
    void mergeSecrets_whenIncomingSecretsBlank_shouldKeepStoredSecrets() {
        CpsOnboardingVendor incoming = vendor("dataoke");
        incoming.setVendorName("incoming vendor");
        incoming.setAppSecret("  ");
        incoming.setAuthToken(null);
        CpsOnboardingVendor stored = vendor("dataoke");
        stored.setAppSecret("stored-secret");
        stored.setAuthToken("stored-token");

        CpsOnboardingVendor merged = fingerprint.mergeSecrets(incoming, stored);

        assertEquals("stored-secret", merged.getAppSecret());
        assertEquals("stored-token", merged.getAuthToken());
        assertEquals("incoming vendor", merged.getVendorName());
    }

    @Test
    void mergeSecrets_whenIncomingSecretsPresent_shouldReplaceStoredSecrets() {
        CpsOnboardingVendor incoming = vendor("dataoke");
        incoming.setAppSecret("replacement-secret");
        incoming.setAuthToken("replacement-token");
        CpsOnboardingVendor stored = vendor("dataoke");
        stored.setAppSecret("stored-secret");
        stored.setAuthToken("stored-token");

        CpsOnboardingVendor merged = fingerprint.mergeSecrets(incoming, stored);

        assertEquals("replacement-secret", merged.getAppSecret());
        assertEquals("replacement-token", merged.getAuthToken());
    }

    @Test
    void mergeSecrets_shouldNotMutateEitherInput() {
        CpsOnboardingVendor incoming = vendor("dataoke");
        incoming.setAppSecret("");
        incoming.setAuthToken("replacement-token");
        CpsOnboardingVendor stored = vendor("dataoke");
        stored.setAppSecret("stored-secret");
        stored.setAuthToken("stored-token");

        CpsOnboardingVendor merged = fingerprint.mergeSecrets(incoming, stored);

        assertNotSame(incoming, merged);
        assertNotSame(stored, merged);
        assertEquals("", incoming.getAppSecret());
        assertEquals("replacement-token", incoming.getAuthToken());
        assertEquals("stored-secret", stored.getAppSecret());
        assertEquals("stored-token", stored.getAuthToken());
    }

    @Test
    void calculate_whenKeysAndPrioritiesAreNull_shouldRemainDeterministic() {
        CpsPlatformOnboardingPayload first = validPayload();
        first.setVendors(new ArrayList<>(List.of(nullKeyVendor("first"), nullKeyVendor("second"))));
        first.setAdzones(new ArrayList<>(List.of(nullKeyAdzone("first"), nullKeyAdzone("second"))));
        first.setRebateRules(new ArrayList<>(List.of(nullScopeRule("30.00"), nullScopeRule("60.00"))));
        CpsPlatformOnboardingPayload second = validPayload();
        second.setVendors(new ArrayList<>(first.getVendors()));
        second.setAdzones(new ArrayList<>(first.getAdzones()));
        second.setRebateRules(new ArrayList<>(first.getRebateRules()));
        Collections.reverse(second.getVendors());
        Collections.reverse(second.getAdzones());
        Collections.reverse(second.getRebateRules());

        assertEquals(fingerprint.calculate(first), fingerprint.calculate(second));
    }

    @Test
    void calculate_whenPriorityIsNull_shouldMatchDefaultPriority() {
        CpsPlatformOnboardingPayload nullPriority = validPayload();
        nullPriority.getVendors().forEach(vendor -> vendor.setPriority(null));
        nullPriority.getRebateRules().forEach(rule -> rule.setPriority(null));
        CpsPlatformOnboardingPayload defaultPriority = validPayload();
        defaultPriority.getVendors().forEach(vendor -> vendor.setPriority(0));
        defaultPriority.getRebateRules().forEach(rule -> rule.setPriority(0));

        assertEquals(fingerprint.calculate(nullPriority), fingerprint.calculate(defaultPriority));
    }

    @Test
    void calculate_shouldReturnLowercaseSha256Hex() {
        String result = fingerprint.calculate(validPayload());

        assertEquals(64, result.length());
        assertTrue(result.matches("[0-9a-f]{64}"));
        assertFalse(result.matches(".*[A-F].*"));
    }

    @Test
    void mergeSecrets_whenStoredMissing_shouldReturnIncomingCopy() {
        CpsOnboardingVendor incoming = vendor("dataoke");

        CpsOnboardingVendor merged = fingerprint.mergeSecrets(incoming, null);

        assertEquals(incoming, merged);
        assertNotSame(incoming, merged);
    }

    private CpsOnboardingVendor nullKeyVendor(String name) {
        CpsOnboardingVendor result = vendor("temporary");
        result.setVendorCode(null);
        result.setVendorName(name);
        return result;
    }

    private CpsOnboardingAdzone nullKeyAdzone(String name) {
        return CpsOnboardingAdzone.builder()
                .platformCode("taobao")
                .adzoneName(name)
                .status(1)
                .build();
    }

    private CpsOnboardingRebateRule nullScopeRule(String rate) {
        return CpsOnboardingRebateRule.builder()
                .rebateRate(new BigDecimal(rate))
                .priority(null)
                .status(1)
                .build();
    }

}
