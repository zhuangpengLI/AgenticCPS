package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsAdzoneServiceImplTest {

    @InjectMocks
    private CpsAdzoneServiceImpl service;

    @Mock
    private CpsAdzoneMapper adzoneMapper;

    @Test
    @DisplayName("createAdzone - 淘宝会员推广位必须是合法 PID 格式")
    void createAdzone_rejectsInvalidTaobaoMemberPidFormat() {
        CpsAdzoneSaveReqVO reqVO = buildTaobaoMemberReqVO();
        reqVO.setAdzoneId("mm_taobao_member_1002");
        reqVO.setExternalSpecialId("116291900443");

        assertThrows(ServiceException.class, () -> service.createAdzone(reqVO));
        verify(adzoneMapper, never()).insert(any(CpsAdzoneDO.class));
    }

    @Test
    @DisplayName("createAdzone - 淘宝会员推广位必须配置会员运营ID")
    void createAdzone_rejectsTaobaoMemberPidWithoutSpecialId() {
        CpsAdzoneSaveReqVO reqVO = buildTaobaoMemberReqVO();
        reqVO.setAdzoneId("mm_44480323_46012675_116291900443");
        reqVO.setExternalSpecialId(null);

        assertThrows(ServiceException.class, () -> service.createAdzone(reqVO));
        verify(adzoneMapper, never()).insert(any(CpsAdzoneDO.class));
    }

    @Test
    @DisplayName("createAdzone - 淘宝会员推广位和会员运营ID合法时允许保存")
    void createAdzone_allowsValidTaobaoMemberPid() {
        CpsAdzoneSaveReqVO reqVO = buildTaobaoMemberReqVO();
        reqVO.setAdzoneId("mm_44480323_46012675_116291900443");
        reqVO.setExternalSpecialId("116291900443");
        when(adzoneMapper.insert(any(CpsAdzoneDO.class))).thenReturn(1);

        service.createAdzone(reqVO);

        verify(adzoneMapper).insert(any(CpsAdzoneDO.class));
    }

    @Test
    @DisplayName("updateAdzone - 淘宝会员推广位更新时同样校验 PID")
    void updateAdzone_rejectsInvalidTaobaoMemberPidFormat() {
        CpsAdzoneSaveReqVO reqVO = buildTaobaoMemberReqVO();
        reqVO.setId(9L);
        reqVO.setAdzoneId("mm_taobao_member_1002");
        reqVO.setExternalSpecialId("116291900443");
        when(adzoneMapper.selectById(9L)).thenReturn(CpsAdzoneDO.builder().id(9L).build());

        assertThrows(ServiceException.class, () -> service.updateAdzone(reqVO));
        verify(adzoneMapper, never()).updateById(any(CpsAdzoneDO.class));
    }

    private CpsAdzoneSaveReqVO buildTaobaoMemberReqVO() {
        CpsAdzoneSaveReqVO reqVO = new CpsAdzoneSaveReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setAdzoneName("会员285专属");
        reqVO.setAdzoneType("member");
        reqVO.setRelationType("member");
        reqVO.setRelationId(285L);
        reqVO.setIsDefault(0);
        reqVO.setStatus(1);
        return reqVO;
    }

}
