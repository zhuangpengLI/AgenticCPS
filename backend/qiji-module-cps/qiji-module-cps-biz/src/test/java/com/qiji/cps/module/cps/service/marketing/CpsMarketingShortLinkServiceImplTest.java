package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingShortLinkCreateReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingShortLinkMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsMarketingShortLinkServiceImplTest {

    @InjectMocks
    private CpsMarketingShortLinkServiceImpl service;

    @Mock
    private CpsMarketingShortLinkMapper shortLinkMapper;

    @Test
    @DisplayName("createShortLink - 生成不可枚举短码并只保存会员归因摘要")
    void createShortLink_generatesOpaqueCodeAndHashesAttribution() {
        CpsMarketingShortLinkCreateReqVO reqVO = buildCreateReq();
        when(shortLinkMapper.selectByRequestHash(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            CpsMarketingShortLinkDO link = invocation.getArgument(0);
            link.setId(99L);
            return 1;
        }).when(shortLinkMapper).insert(any(CpsMarketingShortLinkDO.class));

        CpsMarketingShortLinkDO result = service.createShortLink(reqVO);

        ArgumentCaptor<CpsMarketingShortLinkDO> captor = ArgumentCaptor.forClass(CpsMarketingShortLinkDO.class);
        verify(shortLinkMapper).insert(captor.capture());
        CpsMarketingShortLinkDO inserted = captor.getValue();
        assertEquals(99L, result.getId());
        assertEquals("https://example.com/promo?item=123", inserted.getTargetUrl());
        assertTrue(inserted.getShortCode().matches("[A-Za-z0-9]{10,20}"));
        assertEquals(64, inserted.getMemberAttributionHash().length());
        assertNotEquals("member-1001", inserted.getMemberAttributionHash());
        assertEquals(inserted.getShortCode(), result.getShortCode());
        assertNull(inserted.getLastAccessTime());
    }

    @Test
    @DisplayName("createShortLink - 同一营销请求复用既有短链")
    void createShortLink_reusesExistingByRequestHash() {
        CpsMarketingShortLinkDO existing = CpsMarketingShortLinkDO.builder()
                .id(10L)
                .shortCode("Abc123XyZ9")
                .targetUrl("https://example.com/promo?item=123")
                .requestHash("hash")
                .status(1)
                .build();
        when(shortLinkMapper.selectByRequestHash(anyString())).thenReturn(existing);

        CpsMarketingShortLinkDO result = service.createShortLink(buildCreateReq());

        assertEquals(10L, result.getId());
        assertEquals("Abc123XyZ9", result.getShortCode());
        verify(shortLinkMapper, never()).insert(any(CpsMarketingShortLinkDO.class));
    }

    @Test
    @DisplayName("resolveTargetUrl - 有效短码返回目标链接并记录访问")
    void resolveTargetUrl_returnsTargetAndIncrementsAccess() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 16, 0);
        service.setClockForTest(() -> now);
        when(shortLinkMapper.selectByShortCode("Abc123XyZ9")).thenReturn(CpsMarketingShortLinkDO.builder()
                .id(10L)
                .shortCode("Abc123XyZ9")
                .targetUrl("https://example.com/promo?item=123")
                .status(1)
                .expireTime(now.plusDays(1))
                .accessCount(7L)
                .build());

        String targetUrl = service.resolveTargetUrl("Abc123XyZ9");

        assertEquals("https://example.com/promo?item=123", targetUrl);
        ArgumentCaptor<CpsMarketingShortLinkDO> captor = ArgumentCaptor.forClass(CpsMarketingShortLinkDO.class);
        verify(shortLinkMapper).updateById(captor.capture());
        assertEquals(10L, captor.getValue().getId());
        assertEquals(8L, captor.getValue().getAccessCount());
        assertEquals(now, captor.getValue().getLastAccessTime());
    }

    @Test
    @DisplayName("resolveTargetUrl - 失效或过期短码不跳转")
    void resolveTargetUrl_returnsNullForExpiredLink() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 16, 0);
        service.setClockForTest(() -> now);
        when(shortLinkMapper.selectByShortCode("Expired001")).thenReturn(CpsMarketingShortLinkDO.builder()
                .id(11L)
                .shortCode("Expired001")
                .targetUrl("https://example.com/promo?item=456")
                .status(1)
                .expireTime(now.minusSeconds(1))
                .accessCount(1L)
                .build());

        String targetUrl = service.resolveTargetUrl("Expired001");

        assertNull(targetUrl);
        verify(shortLinkMapper, never()).updateById(any(CpsMarketingShortLinkDO.class));
    }

    private CpsMarketingShortLinkCreateReqVO buildCreateReq() {
        CpsMarketingShortLinkCreateReqVO reqVO = new CpsMarketingShortLinkCreateReqVO();
        reqVO.setTargetUrl("https://example.com/promo?item=123");
        reqVO.setPlatformCode("taobao");
        reqVO.setVendorCode("dataoke");
        reqVO.setTransferRecordId(100L);
        reqVO.setCampaignId("camp-1");
        reqVO.setCreativeId("creative-1");
        reqVO.setChannelCode("wechat-group");
        reqVO.setMemberAttributionKey("member-1001");
        reqVO.setExpireTime(LocalDateTime.of(2026, 8, 1, 0, 0));
        return reqVO;
    }
}
