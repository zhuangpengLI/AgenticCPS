package com.qiji.cps.module.cps.service.recommend;

import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendReqVO;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendRespVO;

public interface CpsSceneRecommendationService {

    OpenApiCpsSceneRecommendRespVO recommendByScene(OpenApiCpsSceneRecommendReqVO request);
}
