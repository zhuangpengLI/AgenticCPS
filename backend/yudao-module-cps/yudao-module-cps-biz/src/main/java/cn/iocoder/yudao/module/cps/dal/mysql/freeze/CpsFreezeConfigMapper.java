package cn.iocoder.yudao.module.cps.dal.mysql.freeze;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS冻结解冻配置 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsFreezeConfigMapper extends BaseMapperX<CpsFreezeConfigDO> {

}
