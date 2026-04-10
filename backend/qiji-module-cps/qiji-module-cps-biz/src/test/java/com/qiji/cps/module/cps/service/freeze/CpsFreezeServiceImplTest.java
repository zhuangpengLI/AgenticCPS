package com.qiji.cps.module.cps.service.freeze;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeConfigPageReqVO;
import com.qiji.cps.module.cps.controller.admin.freeze.vo.CpsFreezeConfigSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeConfigMapper;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
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
        verify(freezeRecordMapper).updateById(argThat(r ->
                "unfreezed".equals(r.getStatus()) && r.getActualUnfreezeTime() != null));
    }

    @Test
    @DisplayName("batchUnfreeze - 没有待解冻记录，返回 0")
    void batchUnfreeze_empty() {
        when(freezeRecordMapper.selectPendingUnfreeze(10)).thenReturn(List.of());

        int count = freezeService.batchUnfreeze(10);

        assertEquals(0, count);
        verify(freezeRecordMapper, never()).updateById(any());
    }

    // ==================== manualUnfreeze 测试 ====================

    @Test
    @DisplayName("manualUnfreeze - 记录不存在，抛出 ServiceException")
    void manualUnfreeze_notFound() {
        when(freezeRecordMapper.selectById(999L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> freezeService.manualUnfreeze(999L));
    }

    @Test
    @DisplayName("manualUnfreeze - 记录已解冻，抛出 ServiceException（状态不合法）")
    void manualUnfreeze_alreadyUnfreezed() {
        CpsFreezeRecordDO record = CpsFreezeRecordDO.builder()
                .id(1L)
                .status("unfreezed")
                .build();
        when(freezeRecordMapper.selectById(1L)).thenReturn(record);

        assertThrows(ServiceException.class, () -> freezeService.manualUnfreeze(1L));
    }

    @Test
    @DisplayName("manualUnfreeze - 正常解冻，更新状态为 unfreezed")
    void manualUnfreeze_success() {
        CpsFreezeRecordDO record = CpsFreezeRecordDO.builder()
                .id(1L)
                .status("frozen")
                .build();
        when(freezeRecordMapper.selectById(1L)).thenReturn(record);

        freezeService.manualUnfreeze(1L);

        verify(freezeRecordMapper).updateById(argThat(r ->
                "unfreezed".equals(r.getStatus()) && r.getActualUnfreezeTime() != null));
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
        when(freezeConfigMapper.selectActiveByPlatform("taobao")).thenReturn(platformConfig);

        CpsFreezeConfigDO result = freezeService.getActiveConfig("taobao");

        assertNotNull(result);
        assertEquals(7, result.getUnfreezeDays());
        assertEquals("taobao", result.getPlatformCode());
    }

    @Test
    @DisplayName("getActiveConfig - 无平台专属配置时返回全平台默认（null表示未找到）")
    void getActiveConfig_noConfig() {
        when(freezeConfigMapper.selectActiveByPlatform("pdd")).thenReturn(null);

        CpsFreezeConfigDO result = freezeService.getActiveConfig("pdd");

        assertNull(result);
    }

    // ==================== createFreezeConfig 测试 ====================

    @Test
    @DisplayName("createFreezeConfig - 正常创建，调用 insert 并返回ID")
    void createFreezeConfig_success() {
        CpsFreezeConfigSaveReqVO reqVO = new CpsFreezeConfigSaveReqVO();
        reqVO.setPlatformCode("taobao");
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

    // ==================== getFreezeConfigPage 测试 ====================

    @Test
    @DisplayName("getFreezeConfigPage - 调用 Mapper 分页查询")
    void getFreezeConfigPage_callsMapper() {
        CpsFreezeConfigPageReqVO reqVO = new CpsFreezeConfigPageReqVO();
        freezeService.getFreezeConfigPage(reqVO);
        verify(freezeConfigMapper).selectPage(reqVO);
    }

}
