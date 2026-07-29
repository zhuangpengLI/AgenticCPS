package com.qiji.cps.module.cps.controller.admin.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsOnboardingVendorRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDraftDeleteReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDraftSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingLifecycleReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPublishReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingTestReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingVendorTestReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingValidateReqVO;
import com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingLifecycleService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CpsPlatformOnboardingControllerTest {

    private static final String QUERY =
            "@ss.hasPermission('cps:platform-onboarding:query')";
    private static final String CREATE_OR_UPDATE =
            "@ss.hasPermission(#request.draftVersion == null ? "
                    + "'cps:platform-onboarding:create' : 'cps:platform-onboarding:update')";
    private static final String UPDATE =
            "@ss.hasPermission('cps:platform-onboarding:update')";
    private static final String DELETE =
            "@ss.hasPermission('cps:platform-onboarding:delete')";
    private static final String TEST =
            "@ss.hasPermission('cps:platform-onboarding:test')";
    private static final String PUBLISH =
            "@ss.hasPermission('cps:platform-onboarding:publish')";

    @Test
    void everyRoute_declaresItsExactPermissionAndMapping() {
        Map<String, String> permissions = Map.ofEntries(
                Map.entry("getPage", QUERY),
                Map.entry("get", QUERY),
                Map.entry("saveDraft", CREATE_OR_UPDATE),
                Map.entry("deleteDraft", DELETE),
                Map.entry("validate", UPDATE),
                Map.entry("test", TEST),
                Map.entry("testVendor", TEST),
                Map.entry("publish", PUBLISH),
                Map.entry("enable", PUBLISH),
                Map.entry("disable", UPDATE),
                Map.entry("delete", DELETE),
                Map.entry("platformCapabilities", QUERY),
                Map.entry("vendorDescriptors", QUERY));
        permissions.forEach((methodName, permission) -> assertEquals(
                permission, method(methodName).getAnnotation(PreAuthorize.class).value(),
                methodName));

        assertEquals("/page", mapping(method("getPage"), GetMapping.class));
        assertEquals("/get", mapping(method("get"), GetMapping.class));
        assertEquals("/draft", mapping(method("saveDraft"), PostMapping.class));
        assertEquals("/draft", mapping(method("deleteDraft"), DeleteMapping.class));
        assertEquals("/validate", mapping(method("validate"), PostMapping.class));
        assertEquals("/test", mapping(method("test"), PostMapping.class));
        assertEquals("/test-vendor", mapping(method("testVendor"), PostMapping.class));
        assertEquals("/publish", mapping(method("publish"), PostMapping.class));
        assertEquals("/enable", mapping(method("enable"), PutMapping.class));
        assertEquals("/disable", mapping(method("disable"), PutMapping.class));
        assertEquals("/delete", mapping(method("delete"), DeleteMapping.class));
        assertEquals("/platform-capabilities",
                mapping(method("platformCapabilities"), GetMapping.class));
        assertEquals("/vendor-descriptors",
                mapping(method("vendorDescriptors"), GetMapping.class));
    }

    @Test
    void requestModels_rejectMissingRequiredFieldsAndAcceptPlatformOnlyDraftDelete() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertFalse(validator.validate(new CpsPlatformOnboardingDraftSaveReqVO()).isEmpty());
            assertFalse(validator.validate(new CpsPlatformOnboardingValidateReqVO()).isEmpty());
            assertFalse(validator.validate(new CpsPlatformOnboardingTestReqVO()).isEmpty());
            assertFalse(validator.validate(new CpsPlatformOnboardingVendorTestReqVO()).isEmpty());
            assertFalse(validator.validate(new CpsPlatformOnboardingPublishReqVO()).isEmpty());
            assertFalse(validator.validate(new CpsPlatformOnboardingLifecycleReqVO()).isEmpty());

            CpsPlatformOnboardingDraftDeleteReqVO delete =
                    new CpsPlatformOnboardingDraftDeleteReqVO();
            assertFalse(validator.validate(delete).isEmpty());
            delete.setPlatformCode("taobao");
            assertEquals(Set.of(), validator.validate(delete));
        }
    }

    @Test
    void deleteDraft_acceptsPlatformCodeOnlyQueryContract() {
        CpsPlatformOnboardingLifecycleService lifecycleService =
                mock(CpsPlatformOnboardingLifecycleService.class);
        CpsPlatformOnboardingController controller =
                new CpsPlatformOnboardingController();
        ReflectionTestUtils.setField(controller, "lifecycleService", lifecycleService);

        assertEquals(Boolean.TRUE,
                controller.deleteDraft("taobao", null, null).getData());

        verify(lifecycleService).deleteDraft(argThat(request ->
                "taobao".equals(request.getPlatformCode())
                        && request.getDraftVersion() == null));
    }

    @Test
    void vendorResponse_exposesOnlyConfigurationEvidenceNotCredentialValues()
            throws Exception {
        Set<String> fields = Arrays.stream(CpsOnboardingVendorRespVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertFalse(fields.contains("appKey"));
        assertFalse(fields.contains("appSecret"));
        assertFalse(fields.contains("authToken"));
        assertFalse(fields.contains("extraConfig"));

        String json = new ObjectMapper().writeValueAsString(
                CpsOnboardingVendorRespVO.builder()
                        .vendorCode("dataoke")
                        .appKeyConfigured(true)
                        .appSecretConfigured(true)
                        .authTokenConfigured(true)
                        .extraConfigConfigured(true)
                        .configuredFields(List.of("customToken"))
                        .build());
        assertFalse(json.contains("raw-app-key"));
        assertFalse(json.contains("raw-app-secret"));
        assertFalse(json.contains("raw-auth-token"));
        assertFalse(json.contains("raw-extra-config"));
    }

    private static Method method(String name) {
        return Arrays.stream(CpsPlatformOnboardingController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static <A extends java.lang.annotation.Annotation> String mapping(
            Method method, Class<A> annotationType) {
        A annotation = method.getAnnotation(annotationType);
        String[] values;
        if (annotation instanceof GetMapping value) {
            values = value.value();
        } else if (annotation instanceof PostMapping value) {
            values = value.value();
        } else if (annotation instanceof PutMapping value) {
            values = value.value();
        } else if (annotation instanceof DeleteMapping value) {
            values = value.value();
        } else {
            throw new AssertionError("Missing mapping on " + method.getName());
        }
        assertEquals(1, values.length, method.getName());
        return values[0];
    }
}
