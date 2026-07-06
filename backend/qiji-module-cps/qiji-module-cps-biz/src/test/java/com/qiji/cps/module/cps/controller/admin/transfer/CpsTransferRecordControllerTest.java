package com.qiji.cps.module.cps.controller.admin.transfer;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.service.transfer.CpsTransferService;
import com.qiji.cps.module.member.dal.dataobject.user.MemberUserDO;
import com.qiji.cps.module.member.service.user.MemberUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsTransferRecordControllerTest {

    @InjectMocks
    private CpsTransferRecordController controller;

    @Mock
    private CpsTransferService transferService;

    @Mock
    private MemberUserService memberUserService;

    @Test
    void getTransferPage_enrichesMemberName() {
        CpsTransferRecordDO record = CpsTransferRecordDO.builder()
                .memberId(283L)
                .platformCode("taobao")
                .taoCommand("￥P9jBgNaLongTaoCommand￥")
                .build();
        record.setId(16L);
        when(transferService.getTransferPage(any())).thenReturn(new PageResult<>(List.of(record), 1L));
        when(memberUserService.getUserList(anyCollection())).thenReturn(List.of(MemberUserDO.builder()
                .id(283L)
                .nickname("张三")
                .build()));

        var result = controller.getTransferPage(new CpsTransferRecordPageReqVO());

        assertEquals("张三", result.getData().getList().get(0).getMemberName());
    }

    @Test
    void getTransferPage_filtersByMemberName() {
        when(memberUserService.getUserListByNickname("张")).thenReturn(List.of(MemberUserDO.builder()
                .id(283L)
                .nickname("张三")
                .build()));
        when(transferService.getTransferPage(any())).thenReturn(new PageResult<>(List.of(), 0L));
        CpsTransferRecordPageReqVO reqVO = new CpsTransferRecordPageReqVO();
        reqVO.setMemberName("张");

        controller.getTransferPage(reqVO);

        verify(transferService).getTransferPage(argThat(query ->
                query.getMemberIds().equals(List.of(283L))));
    }
}
