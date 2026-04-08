package cn.iocoder.yudao.module.cps.dal.dataobject.mcp;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CPS MCP API Key管理 DO
 *
 * @author CPS System
 */
@TableName("cps_mcp_api_key")
@KeySequence("cps_mcp_api_key_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsMcpApiKeyDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * API Key名称（标识用途）
     */
    private String name;
    /**
     * API Key值
     */
    private String keyValue;
    /**
     * 描述
     */
    private String description;
    /**
     * 状态（0禁用 1启用）
     */
    private Integer status;
    /**
     * 过期时间（NULL=永不过期）
     */
    private LocalDateTime expireTime;
    /**
     * 最后使用时间
     */
    private LocalDateTime lastUseTime;
    /**
     * 累计调用次数
     */
    private Long useCount;

}
