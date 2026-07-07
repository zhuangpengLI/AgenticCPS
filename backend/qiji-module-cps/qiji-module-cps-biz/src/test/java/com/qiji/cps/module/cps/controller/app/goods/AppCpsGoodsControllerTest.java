package com.qiji.cps.module.cps.controller.app.goods;

import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.controller.app.goods.vo.AppCpsLinkReqVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCpsGoodsControllerTest {

    @InjectMocks
    private AppCpsGoodsController controller;

    @Mock
    private CpsGoodsService cpsGoodsService;
    @Mock
    private CpsTransferRecordMapper transferRecordMapper;

    @Test
    @DisplayName("generateLink inserts transfer record for order attribution")
    void generateLink_insertsTransferRecordForOrderAttribution() {
        AppCpsLinkReqVO reqVO = new AppCpsLinkReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setGoodsId("ITEM-1");
        reqVO.setAdzoneId("mm_111_222_333");
        when(cpsGoodsService.resolvePromotionAdzoneId("taobao", 1001L, "mm_111_222_333"))
                .thenReturn("mm_111_222_333");
        when(cpsGoodsService.generatePromotionLink("taobao", "ITEM-1", null, 1001L, "mm_111_222_333"))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://s.click/1")
                        .longUrl("https://item.taobao.com/item.htm?id=ITEM-1")
                        .tpwd("abc")
                        .build());

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var response = controller.generateLink(reqVO);

            assertEquals("https://s.click/1", response.getData().getShortUrl());
        }

        ArgumentCaptor<CpsTransferRecordDO> captor = ArgumentCaptor.forClass(CpsTransferRecordDO.class);
        verify(transferRecordMapper).insert(captor.capture());
        CpsTransferRecordDO record = captor.getValue();
        assertEquals(1001L, record.getMemberId());
        assertEquals("taobao", record.getPlatformCode());
        assertEquals("ITEM-1", record.getItemId());
        assertEquals("ITEM-1", record.getOriginalContent());
        assertEquals("mm_111_222_333", record.getAdzoneId());
        assertEquals("https://s.click/1", record.getPromotionUrl());
        assertEquals("abc", record.getTaoCommand());
        assertEquals(1, record.getStatus());
    }

    @Test
    @DisplayName("generateLink does not insert transfer record when link generation fails")
    void generateLink_doesNotInsertTransferRecordWhenLinkGenerationFails() {
        AppCpsLinkReqVO reqVO = new AppCpsLinkReqVO();
        reqVO.setPlatformCode("taobao");
        reqVO.setGoodsId("ITEM-1");
        reqVO.setAdzoneId("mm_111_222_333");
        when(cpsGoodsService.resolvePromotionAdzoneId("taobao", 1001L, "mm_111_222_333"))
                .thenReturn("mm_111_222_333");
        when(cpsGoodsService.generatePromotionLink("taobao", "ITEM-1", null, 1001L, "mm_111_222_333"))
                .thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var response = controller.generateLink(reqVO);

            assertNull(response.getData());
        }

        verify(transferRecordMapper, never()).insert(any(CpsTransferRecordDO.class));
    }
}
