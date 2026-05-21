package com.qiji.cps.module.cps.controller.openapi.recommend;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendReqVO;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendRespVO;
import com.qiji.cps.module.cps.service.exchange.CpsOpenApiSignatureService;
import com.qiji.cps.module.cps.service.recommend.CpsSceneRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "开放接口 - CPS场景推荐")
@RestController
@RequestMapping("/openapi/cps/recommend")
@Validated
@PermitAll
public class OpenApiCpsRecommendController {

    @Resource
    private CpsSceneRecommendationService sceneRecommendationService;

    @Resource
    private CpsOpenApiSignatureService signatureService;

    @PostMapping("/by-scene")
    @Operation(summary = "AIoT场景商品推荐")
    public CommonResult<OpenApiCpsSceneRecommendRespVO> recommendByScene(
            @Valid @RequestBody OpenApiCpsSceneRecommendReqVO reqVO,
            HttpServletRequest request) {
        signatureService.verify(request, reqVO);
        return success(sceneRecommendationService.recommendByScene(reqVO));
    }
}
