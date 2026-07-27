package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneBatchCreateRespVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ADZONE_RELATION_REQUIRED;
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
    @DisplayName("createAdzone - 淘宝渠道推广位必须配置渠道关系 ID")
    void createAdzone_rejectsTaobaoChannelWithoutExternalRelationId() {
        CpsAdzoneSaveReqVO reqVO = buildGeneralReqVO("mm_1_2_3", "渠道 PID");
        reqVO.setAdzoneType("channel");
        reqVO.setRelationType("channel");
        reqVO.setRelationId(9001L);
        reqVO.setExternalRelationId(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createAdzone(reqVO));

        assertEquals(ADZONE_RELATION_REQUIRED.getCode(), exception.getCode());
        verify(adzoneMapper, never()).insert(any(CpsAdzoneDO.class));
    }

    @Test
    @DisplayName("updateAdzone - 淘宝会员推广位缺 specialId 使用统一关联信息错误")
    void updateAdzone_rejectsTaobaoMemberWithoutSpecialIdUsingRelationRequiredCode() {
        CpsAdzoneSaveReqVO reqVO = buildTaobaoMemberReqVO();
        reqVO.setId(9L);
        reqVO.setAdzoneId("mm_1_2_3");
        reqVO.setExternalSpecialId(null);
        when(adzoneMapper.selectById(9L)).thenReturn(CpsAdzoneDO.builder().id(9L).build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateAdzone(reqVO));

        assertEquals(ADZONE_RELATION_REQUIRED.getCode(), exception.getCode());
        verify(adzoneMapper, never()).updateById(any(CpsAdzoneDO.class));
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

    @Test
    @DisplayName("batchCreateAdzones - 保留顺序并允许单条失败")
    void batchCreateAdzones_preservesInputOrderAndAllowsPartialFailure() {
        CpsAdzoneSaveReqVO general = buildGeneralReqVO("mm_1_2_3", "通用 PID");
        CpsAdzoneSaveReqVO invalidMember = buildTaobaoMemberReqVO();
        invalidMember.setAdzoneId("invalid-pid");
        invalidMember.setExternalSpecialId("116291900443");
        CpsAdzoneSaveReqVO channel = buildGeneralReqVO("mm_1_2_4", "渠道 PID");
        channel.setAdzoneType("channel");
        channel.setRelationType("channel");
        channel.setRelationId(9001L);
        channel.setExternalRelationId("tb-channel-9001");
        when(adzoneMapper.insert(any(CpsAdzoneDO.class))).thenReturn(1);

        CpsAdzoneBatchCreateRespVO response = service.batchCreateAdzones(List.of(general, invalidMember, channel));

        assertEquals(3, response.getTotalCount());
        assertEquals(2, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        assertEquals(0, response.getResults().get(0).getIndex());
        assertEquals("mm_1_2_3", response.getResults().get(0).getAdzoneId());
        assertEquals(Boolean.TRUE, response.getResults().get(0).getSuccess());
        assertEquals(1, response.getResults().get(1).getIndex());
        assertEquals(Boolean.FALSE, response.getResults().get(1).getSuccess());
        assertEquals("mm_1_2_4", response.getResults().get(2).getAdzoneId());
        verify(adzoneMapper, org.mockito.Mockito.times(2)).insert(any(CpsAdzoneDO.class));
    }

    private CpsAdzoneSaveReqVO buildGeneralReqVO(String adzoneId, String adzoneName) {
        CpsAdzoneSaveReqVO reqVO = new CpsAdzoneSaveReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setAdzoneId(adzoneId);
        reqVO.setAdzoneName(adzoneName);
        reqVO.setAdzoneType("general");
        reqVO.setRelationType("general");
        reqVO.setIsDefault(0);
        reqVO.setStatus(1);
        return reqVO;
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
