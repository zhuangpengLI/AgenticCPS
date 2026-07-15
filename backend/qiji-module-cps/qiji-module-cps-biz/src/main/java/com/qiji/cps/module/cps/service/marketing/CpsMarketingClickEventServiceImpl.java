package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventPageReqVO;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventRecordReqVO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingClickEventDO;
import com.qiji.cps.module.cps.dal.dataobject.marketing.CpsMarketingShortLinkDO;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingClickEventMapper;
import com.qiji.cps.module.cps.dal.mysql.marketing.CpsMarketingShortLinkMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Validated
public class CpsMarketingClickEventServiceImpl implements CpsMarketingClickEventService {

    private static final int STATUS_ENABLED = 1;
    private static final String DELIMITER = "\u001F";

    @Resource
    private CpsMarketingClickEventMapper clickEventMapper;
    @Resource
    private CpsMarketingShortLinkMapper shortLinkMapper;

    private Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @Override
    public CpsMarketingClickEventDO recordClick(CpsMarketingClickEventRecordReqVO reqVO) {
        LocalDateTime clickTime = reqVO.getClickTime() == null ? nowSupplier.get() : reqVO.getClickTime();
        String memberAttributionHash = hashIfPresent(reqVO.getMemberAttributionKey());
        String ipHash = hashIfPresent(reqVO.getIp());
        String userAgentHash = hashIfPresent(reqVO.getUserAgent());
        String deviceHash = hashIfPresent(reqVO.getDeviceFingerprint());
        String dedupeKey = buildDedupeKey(reqVO.getShortCode(), memberAttributionHash, ipHash, userAgentHash,
                deviceHash, clickTime);

        CpsMarketingClickEventDO existing = clickEventMapper.selectByDedupeKey(dedupeKey);
        if (existing != null) {
            return existing;
        }

        CpsMarketingShortLinkDO shortLink = shortLinkMapper.selectByShortCode(reqVO.getShortCode());
        CpsMarketingClickEventDO event = CpsMarketingClickEventDO.builder()
                .clickId("CLK" + UUID.randomUUID().toString().replace("-", ""))
                .shortCode(reqVO.getShortCode())
                .shortLinkId(shortLink == null ? null : shortLink.getId())
                .campaignId(shortLink == null ? null : shortLink.getCampaignId())
                .creativeId(shortLink == null ? null : shortLink.getCreativeId())
                .channelCode(shortLink == null ? null : shortLink.getChannelCode())
                .memberAttributionHash(firstText(memberAttributionHash,
                        shortLink == null ? null : shortLink.getMemberAttributionHash()))
                .ipHash(ipHash)
                .userAgentHash(userAgentHash)
                .deviceHash(deviceHash)
                .dedupeKey(dedupeKey)
                .trustedSource(reqVO.getTrustedSource())
                .status(STATUS_ENABLED)
                .clickTime(clickTime)
                .build();
        clickEventMapper.insert(event);
        return event;
    }

    @Override
    public PageResult<CpsMarketingClickEventDO> getClickEventPage(CpsMarketingClickEventPageReqVO reqVO) {
        return clickEventMapper.selectPage(reqVO);
    }

    void setClockForTest(Supplier<LocalDateTime> nowSupplier) {
        this.nowSupplier = nowSupplier;
    }

    private String buildDedupeKey(String shortCode, String memberAttributionHash, String ipHash,
                                  String userAgentHash, String deviceHash, LocalDateTime clickTime) {
        LocalDateTime clickMinute = clickTime.truncatedTo(ChronoUnit.MINUTES);
        return sha256Hex(String.join(DELIMITER,
                blankToEmpty(shortCode),
                blankToEmpty(memberAttributionHash),
                blankToEmpty(ipHash),
                blankToEmpty(userAgentHash),
                blankToEmpty(deviceHash),
                clickMinute.toString()));
    }

    private String hashIfPresent(String value) {
        return StringUtils.hasText(value) ? sha256Hex(value) : null;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
