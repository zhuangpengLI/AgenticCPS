package com.qiji.cps.module.cps.service.platform;

import com.qiji.cps.framework.common.enums.CommonStatusEnum;
import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import com.qiji.cps.module.cps.dal.mysql.platform.CpsPlatformMapper;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingCacheInvalidator;
import com.qiji.cps.module.cps.service.vendor.CpsApiVendorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsPlatformServiceImplTest {

    @InjectMocks
    private CpsPlatformServiceImpl service;

    @Mock
    private CpsPlatformMapper platformMapper;

    @Mock
    private CpsApiVendorService vendorService;

    @Mock
    private CpsAdzoneService adzoneService;

    @Mock
    private CpsPlatformOnboardingCacheInvalidator cacheInvalidator;

    @Test
    @DisplayName("createPlatform - activeVendorCode 必须属于当前平台启用供应商")
    void createPlatform_rejectsActiveVendorOutsideEnabledPlatformVendors() {
        CpsPlatformSaveReqVO reqVO = buildReqVO("taobao", "haodanku", null);
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(vendorService.getVendorByCodeAndPlatform("haodanku", "taobao"))
                .thenReturn(CpsApiVendorDO.builder().vendorCode("haodanku").platformCode("taobao").status(0).build());

        assertThrows(ServiceException.class, () -> service.createPlatform(reqVO));
        verify(platformMapper, never()).insert(org.mockito.ArgumentMatchers.<CpsPlatformDO>any());
    }

    @Test
    @DisplayName("createPlatform - defaultAdzoneId 必须属于当前平台启用推广位")
    void createPlatform_rejectsDefaultAdzoneOutsideEnabledPlatformAdzones() {
        CpsPlatformSaveReqVO reqVO = buildReqVO("taobao", null, "pid-1");
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(adzoneService.getAdzoneListByPlatformCode("taobao")).thenReturn(List.of(
                CpsAdzoneDO.builder().platformCode("taobao").adzoneId("pid-2").status(1).build()));

        assertThrows(ServiceException.class, () -> service.createPlatform(reqVO));
        verify(platformMapper, never()).insert(org.mockito.ArgumentMatchers.<CpsPlatformDO>any());
    }

    @Test
    @DisplayName("createPlatform - 默认供应商和推广位合法时允许保存")
    void createPlatform_allowsValidDefaultVendorAndAdzone() {
        CpsPlatformSaveReqVO reqVO = buildReqVO("taobao", "haodanku", "pid-1");
        when(platformMapper.selectByPlatformCode("taobao")).thenReturn(null);
        when(vendorService.getVendorByCodeAndPlatform("haodanku", "taobao"))
                .thenReturn(CpsApiVendorDO.builder()
                        .vendorCode("haodanku")
                        .platformCode("taobao")
                        .status(1)
                        .build());
        when(adzoneService.getAdzoneListByPlatformCode("taobao")).thenReturn(List.of(
                CpsAdzoneDO.builder().platformCode("taobao").adzoneId("pid-1").status(1).build()));
        when(platformMapper.insert(any(CpsPlatformDO.class))).thenReturn(1);

        service.createPlatform(reqVO);

        verify(platformMapper).insert(any(CpsPlatformDO.class));
    }

    private CpsPlatformSaveReqVO buildReqVO(String platformCode, String activeVendorCode, String defaultAdzoneId) {
        CpsPlatformSaveReqVO reqVO = new CpsPlatformSaveReqVO();
        reqVO.setPlatformCode(platformCode);
        reqVO.setPlatformName("淘宝");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setActiveVendorCode(activeVendorCode);
        reqVO.setDefaultAdzoneId(defaultAdzoneId);
        return reqVO;
    }

}
