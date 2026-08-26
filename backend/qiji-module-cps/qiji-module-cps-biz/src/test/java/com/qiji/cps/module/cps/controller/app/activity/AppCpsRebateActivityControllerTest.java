package com.qiji.cps.module.cps.controller.app.activity;

import com.qiji.cps.framework.security.core.LoginUser;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionRespVO;
import com.qiji.cps.module.cps.controller.app.activity.vo.AppCpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCpsRebateActivityControllerTest {

    @InjectMocks
    private AppCpsRebateActivityController controller;

    @Mock
    private CpsRebateActivityService activityService;

    @Test
    void generatePromotionUsesLoginMemberAndDoesNotAcceptAdzoneOrMemberId() {
        List<String> requestFields = List.of(AppCpsRebateActivityPromotionReqVO.class.getDeclaredFields()).stream()
                .map(java.lang.reflect.Field::getName).toList();
        assertFalse(requestFields.contains("memberId"));
        assertFalse(requestFields.contains("adzoneId"));

        AppCpsRebateActivityPromotionReqVO request = new AppCpsRebateActivityPromotionReqVO();
        request.setActivityId(8L);
        request.setChannelTag("mall");
        when(activityService.generatePromotionContent(
                org.mockito.ArgumentMatchers.any(CpsRebateActivityPromotionReqVO.class), eq(1001L)))
                .thenReturn(CpsRebateActivityPromotionRespVO.builder()
                        .activityId(8L).attributionStatus("MEMBER_TRACKED").build());

        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUser)
                    .thenReturn(new LoginUser().setId(1001L).setUserType(1));
            assertEquals("MEMBER_TRACKED", controller.generatePromotion(request).getData().getAttributionStatus());
        }

        ArgumentCaptor<CpsRebateActivityPromotionReqVO> captor =
                ArgumentCaptor.forClass(CpsRebateActivityPromotionReqVO.class);
        verify(activityService).generatePromotionContent(captor.capture(), eq(1001L));
        assertEquals(8L, captor.getValue().getActivityId());
        assertEquals("mall", captor.getValue().getChannelTag());
        assertEquals(null, captor.getValue().getAdzoneId());
        assertEquals(null, captor.getValue().getMemberId());
    }
}
