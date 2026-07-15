package com.qiji.cps.module.cps.service.freeze;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeConfigPageReqVO;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeConfigSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeConfigMapper;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * {@link CpsFreezeServiceImpl} 单元测试
 *
 * @author CPS System
 */
@ExtendWith(MockitoExtension.class)
class CpsFreezeServiceImplTest {

    @InjectMocks
    private CpsFreezeServiceImpl freezeService;

    @Mock
    private CpsFreezeRecordMapper freezeRecordMapper;

    @Mock
    private CpsFreezeConfigMapper freezeConfigMapper;

    @Mock
    private CpsRebateAssetService rebateAssetService;

    // ==================== batchUnfreeze 测试 ====================

    @Test
    @DisplayName("batchUnfreeze - 正常批量解冻，返回成功数量")
    void batchUnfreeze_normal() {
        // Arrange：准备一条已到期的冻结记录
        CpsFreezeRecordDO record = CpsFreezeRecordDO.builder()
                .id(1L)
                .memberId(100L)
                .freezeAmount(new BigDecimal("50.00"))
                .status("frozen")
                .unfreezeTime(LocalDateTime.now().minusHours(1))
                .build();
        when(freezeRecordMapper.selectPendingUnfreeze(10)).thenReturn(List.of(record));

        // Act
        int count = freezeService.batchUnfreeze(10);

        // Assert
        assertEquals(1, count);
        verify(rebateAssetService).releaseOrderRebate(eq(1L), any());
    }

    @Test
    @DisplayName("batchUnfreeze - 没有待解冻记录，返回 0")
    void batchUnfreeze_empty() {
        when(freezeRecordMapper.selectPendingUnfreeze(10)).thenReturn(List.of());

        int count = freezeService.batchUnfreeze(10);

        assertEquals(0, count);
        verifyNoInteractions(rebateAssetService);
    }

    // ==================== manualUnfreeze 测试 ====================

    @Test
    @DisplayName("manualUnfreeze - 记录不存在，抛出 ServiceException")
    void manualUnfreeze_notFound() {
        assertThrows(IllegalArgumentException.class, () -> freezeService.manualUnfreeze(999L));
    }

    @Test
    @DisplayName("manualUnfreeze - 记录已解冻，抛出 ServiceException（状态不合法）")
    void manualUnfreeze_alreadyUnfreezed() {
        assertThrows(IllegalArgumentException.class, () -> freezeService.manualUnfreeze(1L));
    }

    @Test
    @DisplayName("manualUnfreeze - 正常解冻，更新状态为 unfreezed")
    void manualUnfreeze_success() {
        com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsManualUnfreezeReqVO req =
                new com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsManualUnfreezeReqVO();
        req.setRecordId(1L);
        req.setReason("风险复核通过");
        req.setIdempotencyKey("manual-1");

        freezeService.manualUnfreeze(req, 99L);

        verify(rebateAssetService).manualReleaseOrderRebate(eq(1L), argThat(context ->
                "99".equals(context.operatorId()) && "manual-1".equals(context.idempotencyKey())));
    }

    // ==================== getActiveConfig 测试 ====================

    @Test
    @DisplayName("getActiveConfig - 平台专属配置优先于全平台默认配置")
    void getActiveConfig_platformPriority() {
        CpsFreezeConfigDO platformConfig = CpsFreezeConfigDO.builder()
                .id(2L)
                .platformCode("taobao")
                .unfreezeDays(7)
                .build();
        platformConfig.setMinAmountCent(0L);
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(List.of(platformConfig));

        CpsFreezeConfigDO result = freezeService.getActiveConfig("taobao");

        assertNotNull(result);
        assertEquals(7, result.getUnfreezeDays());
        assertEquals("taobao", result.getPlatformCode());
    }

    @Test
    @DisplayName("getActiveConfig - 无平台专属配置时返回全平台默认（null表示未找到）")
    void getActiveConfig_noConfig() {
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(List.of());

        CpsFreezeConfigDO result = freezeService.getActiveConfig("pdd");

        assertNull(result);
    }

    // ==================== createFreezeConfig 测试 ====================

    @Test
    @DisplayName("createFreezeConfig - 正常创建，调用 insert 并返回ID")
    void createFreezeConfig_success() {
        CpsFreezeConfigSaveReqVO reqVO = new CpsFreezeConfigSaveReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setMinAmountCent(0L);
        reqVO.setUnfreezeDays(7);
        reqVO.setStatus(1);

        // insert 不返回值，模拟自动填充 ID
        doAnswer(invocation -> {
            CpsFreezeConfigDO config = invocation.getArgument(0);
            config.setId(100L);
            return null;
        }).when(freezeConfigMapper).insert(any(CpsFreezeConfigDO.class));

        Long id = freezeService.createFreezeConfig(reqVO);

        assertEquals(100L, id);
        verify(freezeConfigMapper).insert(any(CpsFreezeConfigDO.class));
    }

    @Test
    void amountRangeUsesLeftClosedRightOpenAndPlatformFallback() {
        CpsFreezeConfigDO exact = CpsFreezeConfigDO.builder().id(2L).platformCode("taobao")
                .minAmountCent(1000L).maxAmountCent(5000L).unfreezeDays(7).status(1).build();
        CpsFreezeConfigDO global = CpsFreezeConfigDO.builder().id(1L).platformCode(null)
                .minAmountCent(0L).maxAmountCent(null).unfreezeDays(15).status(1).build();
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(List.of(exact, global));

        assertEquals(7, freezeService.getActiveConfig("taobao", 1000L).getUnfreezeDays());
        assertEquals(15, freezeService.getActiveConfig("taobao", 5000L).getUnfreezeDays());
    }

    @Test
    void overlappingEnabledRangeIsRejected() {
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(List.of(
                CpsFreezeConfigDO.builder().id(1L).platformCode("taobao")
                        .minAmountCent(0L).maxAmountCent(2000L).status(1).build()));
        CpsFreezeConfigSaveReqVO req = new CpsFreezeConfigSaveReqVO();
        req.setPlatformCode("taobao");
        req.setMinAmountCent(1000L);
        req.setMaxAmountCent(3000L);
        req.setUnfreezeDays(15);
        req.setStatus(1);

        assertThrows(IllegalArgumentException.class, () -> freezeService.createFreezeConfig(req));
        verify(freezeConfigMapper, never()).insert(any(CpsFreezeConfigDO.class));
    }

    @Test
    void deleteFreezeConfigRejectsRemovingLastGlobalFallback() {
        CpsFreezeConfigDO fallback = CpsFreezeConfigDO.builder().id(9L).platformCode(null)
                .minAmountCent(0L).maxAmountCent(null).status(1).build();
        when(freezeConfigMapper.selectById(9L)).thenReturn(fallback);
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(List.of(fallback));

        assertThrows(IllegalStateException.class, () -> freezeService.deleteFreezeConfig(9L));

        verify(freezeConfigMapper, never()).deleteById(9L);
    }

    @Test
    void updateFreezeConfigRejectsDisablingLastGlobalFallback() {
        CpsFreezeConfigDO fallback = CpsFreezeConfigDO.builder().id(9L).platformCode(null)
                .minAmountCent(0L).maxAmountCent(null).status(1).build();
        when(freezeConfigMapper.selectById(9L)).thenReturn(fallback);
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(List.of(fallback));
        CpsFreezeConfigSaveReqVO req = new CpsFreezeConfigSaveReqVO();
        req.setId(9L); req.setPlatformCode(null); req.setMinAmountCent(0L);
        req.setMaxAmountCent(null); req.setUnfreezeDays(15); req.setStatus(0);

        assertThrows(IllegalStateException.class, () -> freezeService.updateFreezeConfig(req));

        verify(freezeConfigMapper, never()).updateById(any(CpsFreezeConfigDO.class));
    }

    // ==================== getFreezeConfigPage 测试 ====================

    @Test
    @DisplayName("getFreezeConfigPage - 调用 Mapper 分页查询")
    void getFreezeConfigPage_callsMapper() {
        CpsFreezeConfigPageReqVO reqVO = new CpsFreezeConfigPageReqVO();
        freezeService.getFreezeConfigPage(reqVO);
        verify(freezeConfigMapper).selectPage(reqVO);
    }

}
