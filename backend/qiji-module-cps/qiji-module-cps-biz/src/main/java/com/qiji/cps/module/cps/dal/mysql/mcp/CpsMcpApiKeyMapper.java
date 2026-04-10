package com.qiji.cps.module.cps.dal.mysql.mcp;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpApiKeyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS MCP API Key Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsMcpApiKeyMapper extends BaseMapperX<CpsMcpApiKeyDO> {

    default CpsMcpApiKeyDO selectByKeyValue(String keyValue) {
        return selectOne(CpsMcpApiKeyDO::getKeyValue, keyValue);
    }

}
