package com.qiji.cps.module.cps.controller.app.marketing;

import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivitiesReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityCardRespVO;
import com.qiji.cps.module.cps.service.marketing.AppCpsMarketingService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppCpsMarketingControllerTest {

    @InjectMocks
    private AppCpsMarketingController controller;

    @Mock
    private AppCpsMarketingService marketingService;

    @Test
    void activities_exposesPublicReadOnlyRouteAndDelegatesNormalizedIds() throws Exception {
        AppCpsMarketingActivitiesReqVO request = new AppCpsMarketingActivitiesReqVO();
        request.setIds(List.of(3L, 1L, 3L, 9L));

        controller.getActivities(request);

        verify(marketingService).getActivitiesByIds(List.of(3L, 1L, 9L));
        Method method = AppCpsMarketingController.class.getDeclaredMethod(
                "getActivities", AppCpsMarketingActivitiesReqVO.class);
        assertEquals("/activities", method.getAnnotation(GetMapping.class).value()[0]);
        assertNotNull(method.getAnnotation(PermitAll.class));
    }

    @Test
    void activitiesRequest_requiresIdsAndAppliesLimitAfterDeduplication() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            AppCpsMarketingActivitiesReqVO request = new AppCpsMarketingActivitiesReqVO();
            assertFalse(validator.validate(request).isEmpty());

            request.setIds(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 1L));
            assertEquals(Set.of(), validator.validate(request));
            assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), request.getIds());

            request.setIds(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L));
            assertFalse(validator.validate(request).isEmpty());
        }
    }

    @Test
    void activityCardResponse_exposesOnlyDiyDisplayAndNavigationFields() {
        Set<String> fields = Arrays.stream(AppCpsMarketingActivityCardRespVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("id", "activityName", "platformCode", "platformName", "mainPic", "shortDesc",
                "rebateDesc", "tagText", "jumpType", "jumpUrl", "searchKeyword"), fields);
    }
}
