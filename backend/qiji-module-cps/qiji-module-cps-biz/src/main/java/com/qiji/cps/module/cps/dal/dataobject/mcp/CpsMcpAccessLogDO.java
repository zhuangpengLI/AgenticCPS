package com.qiji.cps.module.cps.dal.dataobject.mcp;

import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * CPS MCP访问日志 DO
 *
 * @author CPS System
 */
@TableName("cps_mcp_access_log")
@KeySequence("cps_mcp_access_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsMcpAccessLogDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * API Key ID（NULL=匿名访问）
     */
    private Long apiKeyId;
    /**
     * 调用的Tool名称
     */
    private String toolName;
    /**
     * 请求参数（JSON）
     */
    private String requestParams;
    /**
     * 响应数据摘要
     */
    private String responseData;
    /**
     * 调用状态（0失败 1成功）
     */
    private Integer status;
    /**
     * 错误信息（失败时记录）
     */
    private String errorMessage;
    /**
     * 耗时（毫秒）
     */
    private Integer durationMs;
    /**
     * 客户端IP
     */
    private String clientIp;

}
