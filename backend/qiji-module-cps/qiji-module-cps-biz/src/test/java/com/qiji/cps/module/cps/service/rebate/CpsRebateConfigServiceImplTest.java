package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.REBATE_CONFIG_AMOUNT_RANGE_INVALID;

@ExtendWith(MockitoExtension.class)
class CpsRebateConfigServiceImplTest {

    @Mock
    private CpsRebateConfigMapper mapper;

    private CpsRebateConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CpsRebateConfigServiceImpl();
        ReflectionTestUtils.setField(service, "rebateConfigMapper", mapper);
    }

    @Test
    void memberAndPlatformRuleWinsBeforeAllOtherLayers() {
        CpsRebateConfigDO global = config(1L, null, null, null, 100);
        CpsRebateConfigDO levelPlatform = config(2L, null, 3L, "taobao", 90);
        CpsRebateConfigDO memberAll = config(3L, 9L, null, null, 10);
        CpsRebateConfigDO memberPlatform = config(4L, 9L, null, "taobao", 1);
        when(mapper.selectListByStatus(1)).thenReturn(List.of(global, levelPlatform, memberAll, memberPlatform));

        CpsRebateConfigDO matched = service.matchRebateConfig(9L, 3L, "taobao");

        assertEquals(4L, matched.getId());
    }

    @Test
    void createRebateConfig_whenMinGreaterThanMax_shouldReject() {
        CpsRebateConfigSaveReqVO request = invalidRangeRequest();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createRebateConfig(request));

        assertEquals(REBATE_CONFIG_AMOUNT_RANGE_INVALID.getCode(), exception.getCode());
        verify(mapper, never()).insert(any(CpsRebateConfigDO.class));
    }

    @Test
    void updateRebateConfig_whenMinGreaterThanMax_shouldReject() {
        CpsRebateConfigSaveReqVO request = invalidRangeRequest();
        request.setId(9L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateRebateConfig(request));

        assertEquals(REBATE_CONFIG_AMOUNT_RANGE_INVALID.getCode(), exception.getCode());
        verify(mapper, never()).updateById(any(CpsRebateConfigDO.class));
    }

    private static CpsRebateConfigSaveReqVO invalidRangeRequest() {
        CpsRebateConfigSaveReqVO request = new CpsRebateConfigSaveReqVO();
        request.setPlatformCode("taobao");
        request.setRebateRate(new BigDecimal("20"));
        request.setMinRebateAmount(new BigDecimal("100"));
        request.setMaxRebateAmount(new BigDecimal("50"));
        request.setStatus(1);
        return request;
    }

    private static CpsRebateConfigDO config(Long id, Long memberId, Long levelId,
                                             String platform, int priority) {
        return CpsRebateConfigDO.builder().id(id).memberId(memberId).memberLevelId(levelId)
                .platformCode(platform).rebateRate(new BigDecimal("80"))
                .status(0).priority(priority).build();
    }
}
