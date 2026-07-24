package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_PLATFORM_ENABLED;

@ExtendWith(MockitoExtension.class)
class CpsPlatformOnboardingLifecycleServiceTest {

    @Mock
    private CpsPlatformService platformService;
    @Mock
    private CpsPlatformClientFactory clientFactory;

    @InjectMocks
    private CpsPlatformOnboardingLifecycleService service;

    @Test
    void page_shouldBeEmptyBeforeRuntimeRowsExist() {
        when(clientFactory.getRegisteredPlatformCodes()).thenReturn(Set.of("taobao"));
        when(clientFactory.getRegisteredVendorDescriptors()).thenReturn(java.util.List.of());
        when(platformService.getPlatformPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.empty());
        PageResult<?> result = service.getPage(new CpsPlatformOnboardingPageReqVO());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void deletePlatform_whenEnabled_shouldReject() {
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").status(1).build());
        assertThrows(RuntimeException.class, () -> service.deletePlatformBundle("taobao"));
    }
}
