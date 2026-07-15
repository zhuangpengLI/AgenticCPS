package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventRecordReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingClickEventDO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingClickEventMapper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsMarketingClickEventServiceImplTest {

    @InjectMocks
    private CpsMarketingClickEventServiceImpl service;

    @Mock
    private CpsMarketingClickEventMapper clickEventMapper;
    @Mock
    private CpsMarketingShortLinkMapper shortLinkMapper;

    @Test
    @DisplayName("recordClick reuses existing event by dedupe key")
    void recordClick_reusesExistingEventByDedupeKey() {
        CpsMarketingClickEventDO existing = CpsMarketingClickEventDO.builder()
                .id(10L)
                .clickId("CLK-existing")
                .shortCode("Abc123XyZ9")
                .dedupeKey("dedupe")
                .build();
        when(clickEventMapper.selectByDedupeKey(anyString())).thenReturn(existing);

        CpsMarketingClickEventDO result = service.recordClick(buildRecordReq());

        assertEquals("CLK-existing", result.getClickId());
        verify(clickEventMapper, never()).insert(any(CpsMarketingClickEventDO.class));
    }

    @Test
    @DisplayName("recordClick hashes sensitive attribution and device inputs")
    void recordClick_hashesSensitiveAttributionAndDeviceInputs() {
        when(shortLinkMapper.selectByShortCode("Abc123XyZ9")).thenReturn(buildShortLink());
        when(clickEventMapper.selectByDedupeKey(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            CpsMarketingClickEventDO event = invocation.getArgument(0);
            event.setId(20L);
            return 1;
        }).when(clickEventMapper).insert(any(CpsMarketingClickEventDO.class));

        CpsMarketingClickEventDO result = service.recordClick(buildRecordReq());

        ArgumentCaptor<CpsMarketingClickEventDO> captor = ArgumentCaptor.forClass(CpsMarketingClickEventDO.class);
        verify(clickEventMapper).insert(captor.capture());
        CpsMarketingClickEventDO inserted = captor.getValue();
        assertEquals(20L, result.getId());
        assertNotNull(inserted.getClickId());
        assertTrue(inserted.getClickId().startsWith("CLK"));
        assertEquals(10L, inserted.getShortLinkId());
        assertEquals("camp-1", inserted.getCampaignId());
        assertEquals("creative-1", inserted.getCreativeId());
        assertEquals("wechat-group", inserted.getChannelCode());
        assertHash(inserted.getMemberAttributionHash(), "member-1001");
        assertHash(inserted.getIpHash(), "127.0.0.1");
        assertHash(inserted.getUserAgentHash(), "Mozilla/5.0");
        assertHash(inserted.getDeviceHash(), "device-fingerprint");
        assertHash(inserted.getDedupeKey(), "Abc123XyZ9");
    }

    private CpsMarketingClickEventRecordReqVO buildRecordReq() {
        CpsMarketingClickEventRecordReqVO reqVO = new CpsMarketingClickEventRecordReqVO();
        reqVO.setShortCode("Abc123XyZ9");
        reqVO.setIp("127.0.0.1");
        reqVO.setUserAgent("Mozilla/5.0");
        reqVO.setDeviceFingerprint("device-fingerprint");
        reqVO.setMemberAttributionKey("member-1001");
        reqVO.setTrustedSource("app-redirect");
        reqVO.setClickTime(LocalDateTime.of(2026, 7, 14, 18, 0));
        return reqVO;
    }

    private CpsMarketingShortLinkDO buildShortLink() {
        return CpsMarketingShortLinkDO.builder()
                .id(10L)
                .shortCode("Abc123XyZ9")
                .campaignId("camp-1")
                .creativeId("creative-1")
                .channelCode("wechat-group")
                .memberAttributionHash("existing-attribution-hash")
                .build();
    }

    private void assertHash(String value, String plaintext) {
        assertNotNull(value);
        assertEquals(64, value.length());
        assertNotEquals(plaintext, value);
    }
}
