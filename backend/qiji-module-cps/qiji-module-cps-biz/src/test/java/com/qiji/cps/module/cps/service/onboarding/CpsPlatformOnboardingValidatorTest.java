package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.CpsVendorDescriptor;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingCheckRespVO;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CpsPlatformOnboardingValidatorTest {

    @Mock
    private CpsPlatformClientFactory clientFactory;
    @Mock
    private CpsApiVendorClient dataokeClient;
    @Mock
    private CpsApiVendorClient officialClient;

    private CpsPlatformOnboardingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CpsPlatformOnboardingValidator(clientFactory, new ObjectMapper());
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of("taobao"));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(List.of(
                descriptor("dataoke", Set.of(CpsVendorCapability.GOODS_SEARCH,
                        CpsVendorCapability.CONNECTION_TEST)),
                descriptor("official", Set.of(CpsVendorCapability.PROMOTION_LINK,
                        CpsVendorCapability.CONNECTION_TEST))));
        when(clientFactory.getVendorDescriptor("dataoke", "taobao"))
                .thenReturn(descriptor("dataoke", Set.of(CpsVendorCapability.GOODS_SEARCH,
                        CpsVendorCapability.CONNECTION_TEST)));
        when(clientFactory.getVendorDescriptor("official", "taobao"))
                .thenReturn(descriptor("official", Set.of(CpsVendorCapability.PROMOTION_LINK,
                        CpsVendorCapability.CONNECTION_TEST)));
        when(clientFactory.getVendorClient("dataoke", "taobao")).thenReturn(dataokeClient);
        when(clientFactory.getVendorClient("official", "taobao")).thenReturn(officialClient);
    }

    @Test
    void validate_validBundle_shouldNormalizeWithoutMutatingCaller() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().get(0).setDefaultAdzoneId("old-default");
        payload.getVendors().get(0).setPlatformCode(" TAOBAO ");
        payload.getAdzones().get(0).setPlatformCode(" TAOBAO ");
        payload.getPlatform().setActiveVendorCode("stale-vendor");
        payload.getPlatform().setDefaultAdzoneId("stale-adzone");

        CpsPlatformOnboardingValidator.ValidationResult result =
                validator.validateNormalized(payload);

        assertTrue(result.response().isSuccess());
        assertNotSame(payload, result.normalizedPayload());
        assertEquals("old-default", payload.getVendors().get(0).getDefaultAdzoneId());
        assertEquals("stale-vendor", payload.getPlatform().getActiveVendorCode());
        assertEquals("stale-adzone", payload.getPlatform().getDefaultAdzoneId());
        assertEquals("dataoke", result.normalizedPayload().getPlatform().getActiveVendorCode());
        assertEquals("adzone-primary", result.normalizedPayload().getPlatform().getDefaultAdzoneId());
        assertEquals("adzone-primary",
                result.normalizedPayload().getVendors().get(0).getDefaultAdzoneId());
        assertEquals("taobao", result.normalizedPayload().getVendors().get(0).getPlatformCode());
        assertEquals("taobao", result.normalizedPayload().getAdzones().get(0).getPlatformCode());
    }

    @Test
    void validate_shouldRejectForeignVendorAndAdzonePlatforms() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().get(1).setPlatformCode(" jd ");
        payload.getAdzones().get(1).setPlatformCode("pdd");

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "VENDOR_PLATFORM_INVALID", "vendors[1].platformCode");
        assertContains(result, "ADZONE_PLATFORM_INVALID", "adzones[1].platformCode");
    }

    @Test
    void validate_nullVendor_shouldReturnDeterministicItemError() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().add(0, null);

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertFalse(result.isSuccess());
        assertContains(result, "VENDOR_CONFIG_INVALID", "vendors[0]");
    }

    @Test
    void validate_nullAdzone_shouldReturnDeterministicItemError() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getAdzones().add(0, null);

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertFalse(result.isSuccess());
        assertContains(result, "ADZONE_CONFIG_INVALID", "adzones[0]");
    }

    @Test
    void validate_shouldPreserveOpaqueAdzoneIdCase() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getAdzones().get(0).setAdzoneId(" AdZone-AbC ");
        payload.setRuntimeDefaultAdzoneId(" AdZone-AbC ");

        CpsPlatformOnboardingValidator.ValidationResult result =
                validator.validateNormalized(payload);

        assertTrue(result.response().isSuccess());
        assertEquals("AdZone-AbC", result.normalizedPayload().getRuntimeDefaultAdzoneId());
        assertEquals("AdZone-AbC", result.normalizedPayload().getAdzones().get(0).getAdzoneId());
        assertEquals("AdZone-AbC",
                result.normalizedPayload().getVendors().get(0).getDefaultAdzoneId());
    }

    @Test
    void validate_shouldReturnAllErrorsInStableOrder() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getPlatform().setPlatformCode("unknown");
        payload.setPrimaryVendorCode(" ");
        payload.getVendors().add(CpsPlatformOnboardingTestFixtures.vendor(" DATAOKE "));
        payload.setRuntimeDefaultAdzoneId("foreign");
        payload.getRebateRules().forEach(rule -> rule.setPlatformCode("unknown"));
        payload.getRebateRules().get(0).setMinRebateAmount(new BigDecimal("5"));
        payload.getRebateRules().get(0).setMaxRebateAmount(new BigDecimal("1"));
        payload.getRebateRules().add(CpsOnboardingRebateRule.builder()
                .platformCode("unknown").rebateRate(new BigDecimal("70"))
                .status(1).priority(0).build());

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertFalse(result.isSuccess());
        assertContains(result, "PLATFORM_NOT_REGISTERED", "platform.platformCode");
        assertContains(result, "PRIMARY_VENDOR_REQUIRED", "primaryVendorCode");
        assertContains(result, "VENDOR_DUPLICATE", "vendors");
        assertContains(result, "DEFAULT_ADZONE_INVALID", "runtimeDefaultAdzoneId");
        assertContains(result, "REBATE_AMOUNT_INVALID", "rebateRules[0]");
        assertContains(result, "REBATE_SCOPE_DUPLICATE", "rebateRules");
        assertEquals(result.getItems().stream().map(CpsPlatformOnboardingCheckRespVO.Item::getCode).toList(),
                validator.validate(payload).getItems().stream()
                        .map(CpsPlatformOnboardingCheckRespVO.Item::getCode).toList());
    }

    @Test
    void validate_platformRegisteredOnlyByVendorDescriptor_shouldAccept() {
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of());

        assertTrue(validator.validate(validPayload()).isSuccess());
    }

    @Test
    void validate_primaryMustExistOnceAndBeEnabled() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().get(0).setStatus(0);

        assertContains(validator.validate(payload), "PRIMARY_VENDOR_REQUIRED", "primaryVendorCode");
    }

    @Test
    void validate_enabledVendorMustHaveDescriptorClientSchemaAndBusinessCapability() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().get(0).setAppSecret(null);
        when(clientFactory.getVendorDescriptor("official", "taobao")).thenReturn(null);
        when(clientFactory.getVendorClient("official", "taobao")).thenReturn(null);
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(
                descriptor("dataoke", Set.of(CpsVendorCapability.CONNECTION_TEST)));

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "VENDOR_CONFIG_INVALID", "vendors[0].appSecret");
        assertContains(result, "VENDOR_NOT_REGISTERED", "vendors[0].capabilities");
        assertContains(result, "VENDOR_NOT_REGISTERED", "vendors[1].vendorCode");
    }

    @Test
    void validate_enabledVendorMustDeclareConfigSchema() {
        CpsVendorDescriptor descriptor = CpsVendorDescriptor.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .vendorType("aggregator")
                .capabilities(Set.of(CpsVendorCapability.GOODS_SEARCH))
                .configSchema(null)
                .build();
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(descriptor);

        assertContains(validator.validate(validPayload()),
                "VENDOR_CONFIG_SCHEMA_REQUIRED", "vendors[0].configSchema");
    }

    @Test
    void validate_shouldApplyDescriptorSchemaToParsedExtraConfig() {
        CpsVendorDescriptor descriptor = descriptor("dataoke",
                Set.of(CpsVendorCapability.GOODS_SEARCH));
        descriptor = CpsVendorDescriptor.builder()
                .vendorCode(descriptor.getVendorCode())
                .platformCode(descriptor.getPlatformCode())
                .vendorType(descriptor.getVendorType())
                .capabilities(descriptor.getCapabilities())
                .configSchema(new CpsVendorConfigSchema(List.of(
                        CpsVendorConfigField.required("vendor", false))))
                .build();
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(descriptor);

        CpsPlatformOnboardingPayload payload = validPayload();
        assertTrue(validator.validate(payload).isSuccess());

        payload.getVendors().get(0).setExtraConfig("{}");
        assertContains(validator.validate(payload),
                "VENDOR_CONFIG_INVALID", "vendors[0].vendor");

        payload.getVendors().get(0).setExtraConfig("{invalid");
        assertContains(validator.validate(payload),
                "VENDOR_CONFIG_INVALID", "vendors[0].extraConfig");
    }

    @Test
    void validate_vendorApiBaseUrlBelongingToAnotherVendor_shouldReject() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().get(0).setApiBaseUrl("https://v2.api.haodanku.com");

        CpsOnboardingVendor haodanku = payload.getVendors().get(1);
        haodanku.setVendorCode("haodanku");
        haodanku.setApiBaseUrl("https://openapi.dataoke.com/api");
        when(clientFactory.getVendorDescriptor("haodanku", "taobao"))
                .thenReturn(descriptor("haodanku", Set.of(CpsVendorCapability.GOODS_SEARCH)));
        when(clientFactory.getVendorClient("haodanku", "taobao")).thenReturn(officialClient);

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "VENDOR_CONFIG_INVALID", "vendors[0].apiBaseUrl");
        assertContains(result, "VENDOR_CONFIG_INVALID", "vendors[1].apiBaseUrl");
    }

    @Test
    void validate_malformedExtraConfig_shouldStillAggregateCapabilityFailure() {
        CpsVendorDescriptor descriptor = CpsVendorDescriptor.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .vendorType("aggregator")
                .capabilities(Set.of(CpsVendorCapability.CONNECTION_TEST))
                .configSchema(new CpsVendorConfigSchema(List.of(
                        CpsVendorConfigField.required("vendor", false))))
                .build();
        when(clientFactory.getVendorDescriptor("dataoke", "taobao")).thenReturn(descriptor);
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().get(0).setExtraConfig("{invalid");

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "VENDOR_CONFIG_INVALID", "vendors[0].extraConfig");
        assertContains(result, "VENDOR_CONFIG_INVALID", "vendors[0].vendor");
        assertContains(result, "VENDOR_NOT_REGISTERED", "vendors[0].capabilities");
    }

    @Test
    void validate_shouldApplyTaobaoChannelAndMemberAttributionRules() {
        CpsPlatformOnboardingPayload payload = validPayload();
        CpsOnboardingAdzone channel = payload.getAdzones().get(0);
        channel.setAdzoneType("channel");
        channel.setRelationType("channel");
        channel.setRelationId(null);
        channel.setExternalRelationId(null);

        CpsOnboardingAdzone member = payload.getAdzones().get(1);
        member.setAdzoneType("member");
        member.setRelationType("member");
        member.setRelationId(null);
        member.setAdzoneId("invalid-member-pid");
        member.setExternalSpecialId("not-numeric");

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "ADZONE_RELATION_REQUIRED", "adzones[0].relationId");
        assertContains(result, "ADZONE_RELATION_REQUIRED", "adzones[0].externalRelationId");
        assertContains(result, "ADZONE_RELATION_REQUIRED", "adzones[1].relationId");
        assertContains(result, "ADZONE_CONFIG_INVALID", "adzones[1].adzoneId");
        assertContains(result, "ADZONE_CONFIG_INVALID", "adzones[1].externalSpecialId");
    }

    @Test
    void validate_conflictingAdzoneAndRelationTypes_shouldReject() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getAdzones().get(1).setAdzoneType("channel");
        payload.getAdzones().get(1).setRelationType("member");

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "ADZONE_CONFIG_INVALID", "adzones[1].relationType");
    }

    @Test
    void validate_adzoneRules_shouldRequireEnabledGeneralDefaultAndUniqueIds() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getAdzones().get(0).setStatus(0);
        payload.getAdzones().get(1).setAdzoneType("channel");
        payload.getAdzones().add(CpsOnboardingAdzone.builder()
                .platformCode("taobao").adzoneId(" adzone-primary ")
                .adzoneType("general").status(0).build());

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "GENERAL_ADZONE_REQUIRED", "adzones");
        assertContains(result, "DEFAULT_ADZONE_INVALID", "runtimeDefaultAdzoneId");
        assertContains(result, "ADZONE_DUPLICATE", "adzones");
    }

    @Test
    void validate_adzoneDefaultMustBeUniqueEnabledGeneralAndMatchRuntime() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getAdzones().get(1).setIsDefault(1);

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "DEFAULT_ADZONE_INVALID", "runtimeDefaultAdzoneId");
    }

    @Test
    void validate_adzoneDefaultFlagMustMatchRuntimeDefault() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getAdzones().get(0).setIsDefault(0);
        payload.getAdzones().get(1).setIsDefault(1);

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "DEFAULT_ADZONE_INVALID", "runtimeDefaultAdzoneId");
    }

    @Test
    void validate_disabledVendor_shouldRejectMalformedExtraConfig() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getVendors().get(1).setStatus(0);
        payload.getVendors().get(1).setExtraConfig("{invalid");

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "VENDOR_CONFIG_INVALID", "vendors[1].extraConfig");
    }

    @Test
    void validate_rebateRules_shouldValidateEveryFieldAndRequireDefault() {
        CpsPlatformOnboardingPayload payload = validPayload();
        CpsOnboardingRebateRule rule = payload.getRebateRules().get(0);
        rule.setPlatformCode(null);
        rule.setMemberId(9L);
        rule.setRebateRate(new BigDecimal("101"));
        rule.setMinRebateAmount(new BigDecimal("-0.01"));
        rule.setStatus(2);
        rule.setPriority(-1);
        payload.getRebateRules().removeIf(item -> item.getMemberLevelId() == null);

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "REBATE_PLATFORM_INVALID", "rebateRules[0].platformCode");
        assertContains(result, "REBATE_MEMBER_RULE_FORBIDDEN", "rebateRules[0].memberId");
        assertContains(result, "REBATE_RATE_INVALID", "rebateRules[0].rebateRate");
        assertContains(result, "REBATE_AMOUNT_INVALID", "rebateRules[0]");
        assertContains(result, "REBATE_STATUS_INVALID", "rebateRules[0].status");
        assertContains(result, "REBATE_PRIORITY_INVALID", "rebateRules[0].priority");
        assertContains(result, "DEFAULT_REBATE_REQUIRED", "rebateRules");
    }

    @Test
    void validate_rebatePlatformMustMatchRoot() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getRebateRules().get(0).setPlatformCode("jd");

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "REBATE_PLATFORM_INVALID", "rebateRules[0].platformCode");
    }

    @Test
    void validate_sameNormalizedScopeWithDifferentPriority_shouldBeAllowed() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getRebateRules().add(CpsOnboardingRebateRule.builder()
                .platformCode(" TAOBAO ").memberLevelId(20L)
                .rebateRate(new BigDecimal("50")).status(1).priority(1).build());

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertTrue(result.isSuccess(), () -> "different priorities must be independent scopes: " + result);
    }

    @Test
    void validate_sameNormalizedScopeAndPriority_shouldBeDuplicate() {
        CpsPlatformOnboardingPayload payload = validPayload();
        payload.getRebateRules().add(CpsOnboardingRebateRule.builder()
                .platformCode(" TAOBAO ").memberLevelId(20L)
                .rebateRate(new BigDecimal("50")).status(1).priority(10).build());

        CpsPlatformOnboardingCheckRespVO result = validator.validate(payload);

        assertContains(result, "REBATE_SCOPE_DUPLICATE", "rebateRules");
    }

    private CpsPlatformOnboardingPayload validPayload() {
        CpsPlatformOnboardingPayload payload = CpsPlatformOnboardingTestFixtures.validPayload();
        payload.getRebateRules().add(CpsOnboardingRebateRule.builder()
                .platformCode("taobao")
                .rebateRate(new BigDecimal("60.00"))
                .minRebateAmount(BigDecimal.ZERO)
                .maxRebateAmount(new BigDecimal("100"))
                .status(1)
                .priority(0)
                .build());
        return payload;
    }

    private static CpsVendorDescriptor descriptor(String vendorCode,
                                                  Set<CpsVendorCapability> capabilities) {
        return CpsVendorDescriptor.builder()
                .vendorCode(vendorCode)
                .platformCode("taobao")
                .vendorType("aggregator")
                .capabilities(capabilities)
                .configSchema(CpsVendorConfigSchema.standard())
                .build();
    }

    private static void assertContains(CpsPlatformOnboardingCheckRespVO result,
                                       String code, String fieldPath) {
        assertTrue(result.getItems().stream().anyMatch(item ->
                        code.equals(item.getCode()) && fieldPath.equals(item.getFieldPath())),
                () -> "missing " + code + " at " + fieldPath + ": " + result);
    }

}
