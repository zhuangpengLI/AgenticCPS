package com.qiji.cps.module.cps.controller.app.marketing;

import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingClickEventRecordReqVO;
import com.qiji.cps.module.cps.service.marketing.CpsMarketingClickEventService;
import com.qiji.cps.module.cps.service.marketing.CpsMarketingShortLinkService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/cps/s")
public class AppCpsMarketingShortLinkRedirectController {

    @Resource
    private CpsMarketingShortLinkService shortLinkService;
    @Resource
    private CpsMarketingClickEventService clickEventService;

    @GetMapping("/{shortCode}")
    public RedirectView redirect(@PathVariable("shortCode") String shortCode, HttpServletRequest request) {
        String targetUrl = shortLinkService.resolveTargetUrl(shortCode);
        if (StringUtils.hasText(targetUrl)) {
            clickEventService.recordClick(buildRecordReq(shortCode, request));
        }
        RedirectView redirectView = new RedirectView(StringUtils.hasText(targetUrl) ? targetUrl : "/");
        redirectView.setStatusCode(StringUtils.hasText(targetUrl) ? HttpStatus.FOUND : HttpStatus.NOT_FOUND);
        return redirectView;
    }

    private CpsMarketingClickEventRecordReqVO buildRecordReq(String shortCode, HttpServletRequest request) {
        CpsMarketingClickEventRecordReqVO reqVO = new CpsMarketingClickEventRecordReqVO();
        reqVO.setShortCode(shortCode);
        reqVO.setIp(firstText(request.getHeader("X-Forwarded-For"), request.getRemoteAddr()));
        reqVO.setUserAgent(request.getHeader("User-Agent"));
        reqVO.setTrustedSource("app-short-link-redirect");
        return reqVO;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
