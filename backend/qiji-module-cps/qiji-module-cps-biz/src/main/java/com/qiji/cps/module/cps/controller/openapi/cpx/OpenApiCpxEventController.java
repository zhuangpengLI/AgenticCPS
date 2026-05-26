package com.qiji.cps.module.cps.controller.openapi.cpx;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.openapi.cpx.vo.CpxEventCreateReqVO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxEventDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskConstants;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import com.qiji.cps.module.cps.service.exchange.CpsOpenApiSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "OpenAPI - CPX 事件回调")
@RestController
@RequestMapping("/openapi/cpx")
@Validated
@PermitAll
public class OpenApiCpxEventController {

    @Resource
    private CpxTaskService taskService;

    @Resource
    private CpsOpenApiSignatureService signatureService;

    @PostMapping("/event")
    @Operation(summary = "通用 CPX 事件上报")
    public CommonResult<CpxEventDO> recordEvent(@RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                                HttpServletRequest request,
                                                @Valid @RequestBody CpxEventCreateReqVO createReqVO) {
        signatureService.verify(request, createReqVO);
        applyHeaderIdempotencyKey(createReqVO, idempotencyKey);
        return success(taskService.recordEvent(createReqVO));
    }

    @PostMapping("/impression")
    @Operation(summary = "CPM 曝光上报")
    public CommonResult<CpxEventDO> recordImpression(@RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                                     HttpServletRequest request,
                                                     @Valid @RequestBody CpxEventCreateReqVO createReqVO) {
        createReqVO.setEventType(CpxTaskConstants.EVENT_IMPRESSION);
        signatureService.verify(request, createReqVO);
        applyHeaderIdempotencyKey(createReqVO, idempotencyKey);
        return success(taskService.recordEvent(createReqVO));
    }

    @PostMapping("/click")
    @Operation(summary = "CPC/oCPC 点击上报")
    public CommonResult<CpxEventDO> recordClick(@RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                                HttpServletRequest request,
                                                @Valid @RequestBody CpxEventCreateReqVO createReqVO) {
        createReqVO.setEventType(CpxTaskConstants.EVENT_CLICK);
        signatureService.verify(request, createReqVO);
        applyHeaderIdempotencyKey(createReqVO, idempotencyKey);
        return success(taskService.recordEvent(createReqVO));
    }

    @PostMapping("/lead")
    @Operation(summary = "CPL 线索上报")
    public CommonResult<CpxEventDO> recordLead(@RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                               HttpServletRequest request,
                                               @Valid @RequestBody CpxEventCreateReqVO createReqVO) {
        createReqVO.setEventType(CpxTaskConstants.EVENT_LEAD);
        signatureService.verify(request, createReqVO);
        applyHeaderIdempotencyKey(createReqVO, idempotencyKey);
        return success(taskService.recordEvent(createReqVO));
    }

    @PostMapping("/conversion")
    @Operation(summary = "CPA/oCPA 转化动作回调")
    public CommonResult<CpxEventDO> recordConversion(@RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                                     HttpServletRequest request,
                                                     @Valid @RequestBody CpxEventCreateReqVO createReqVO) {
        createReqVO.setEventType(CpxTaskConstants.EVENT_ACTION);
        signatureService.verify(request, createReqVO);
        applyHeaderIdempotencyKey(createReqVO, idempotencyKey);
        return success(taskService.recordEvent(createReqVO));
    }

    private void applyHeaderIdempotencyKey(CpxEventCreateReqVO createReqVO, String idempotencyKey) {
        if (createReqVO.getIdempotencyKey() == null || createReqVO.getIdempotencyKey().isBlank()) {
            createReqVO.setIdempotencyKey(idempotencyKey);
        }
    }
}
