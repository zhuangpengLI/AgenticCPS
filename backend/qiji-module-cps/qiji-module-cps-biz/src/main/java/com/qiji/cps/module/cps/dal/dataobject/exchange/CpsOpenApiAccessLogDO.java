package com.qiji.cps.module.cps.dal.dataobject.exchange;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** CPS OpenAPI append-only access audit log. */
@TableName("cps_openapi_access_log")
@KeySequence("cps_openapi_access_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOpenApiAccessLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String appId;
    private String requestMethod;
    private String requestUri;
    private String idempotencyKey;
    private String requestHeaders;
    /** 0 failed, 1 successful. Signature verification currently records failures only. */
    private Integer status;
    private String failureReason;
    private String clientIp;

}
