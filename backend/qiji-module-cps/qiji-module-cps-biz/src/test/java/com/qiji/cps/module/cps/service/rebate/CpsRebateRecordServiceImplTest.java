package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateRecordServiceImplTest {

    @InjectMocks
    private CpsRebateRecordServiceImpl rebateRecordService;

    @Mock
    private CpsRebateRecordMapper rebateRecordMapper;
    @Mock
    private CpsRebateSettleService rebateSettleService;
    @Mock
    private MemberUserApi memberUserApi;

    @Test
    @DisplayName("deleteRebateRecord - 删除返利记录前校验记录存在")
    void deleteRebateRecord_validatesExistsThenDeletes() {
        when(rebateRecordMapper.selectById(3L)).thenReturn(CpsRebateRecordDO.builder().id(3L).build());

        rebateRecordService.deleteRebateRecord(3L);

        verify(rebateRecordMapper).deleteById(3L);
    }

    @Test
    @DisplayName("deleteRebateRecordList - 批量删除返利记录时逐个校验存在")
    void deleteRebateRecordList_validatesAndDeletesEachRecord() {
        when(rebateRecordMapper.selectById(2L)).thenReturn(CpsRebateRecordDO.builder().id(2L).build());
        when(rebateRecordMapper.selectById(3L)).thenReturn(CpsRebateRecordDO.builder().id(3L).build());

        rebateRecordService.deleteRebateRecordList(List.of(2L, 3L));

        verify(rebateRecordMapper).deleteById(2L);
        verify(rebateRecordMapper).deleteById(3L);
    }

    @Test
    @DisplayName("deleteRebateRecord - 返利记录不存在时抛出业务异常")
    void deleteRebateRecord_throwsWhenMissing() {
        when(rebateRecordMapper.selectById(404L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> rebateRecordService.deleteRebateRecord(404L));

        verify(rebateRecordMapper, never()).deleteById(404L);
    }

}
