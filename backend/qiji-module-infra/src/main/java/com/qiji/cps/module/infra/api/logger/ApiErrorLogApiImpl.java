package com.qiji.cps.module.infra.api.logger;

import com.qiji.cps.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.qiji.cps.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import com.qiji.cps.module.infra.service.logger.ApiErrorLogService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

/**
 * API 访问日志的 API 接口
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ApiErrorLogApiImpl implements ApiErrorLogCommonApi {

    @Resource
    private ApiErrorLogService apiErrorLogService;

    @Override
    public void createApiErrorLog(ApiErrorLogCreateReqDTO createDTO) {
        apiErrorLogService.createApiErrorLog(createDTO);
    }

}
