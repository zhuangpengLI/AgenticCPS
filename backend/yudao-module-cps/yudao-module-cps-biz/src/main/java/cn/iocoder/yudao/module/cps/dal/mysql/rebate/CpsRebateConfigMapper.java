package cn.iocoder.yudao.module.cps.dal.mysql.rebate;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CPS返利配置 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsRebateConfigMapper extends BaseMapperX<CpsRebateConfigDO> {

    default List<CpsRebateConfigDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<CpsRebateConfigDO>()
                .eq(CpsRebateConfigDO::getStatus, status)
                .orderByDesc(CpsRebateConfigDO::getPriority));
    }

}
