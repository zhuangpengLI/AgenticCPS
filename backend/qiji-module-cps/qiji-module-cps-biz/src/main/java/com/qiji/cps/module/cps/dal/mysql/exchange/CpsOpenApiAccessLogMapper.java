package com.qiji.cps.module.cps.dal.mysql.exchange;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsOpenApiAccessLogDO;
import org.apache.ibatis.annotations.Mapper;

/** CPS OpenAPI access audit log mapper; business code should append only. */
@Mapper
public interface CpsOpenApiAccessLogMapper extends BaseMapperX<CpsOpenApiAccessLogDO> {
}
