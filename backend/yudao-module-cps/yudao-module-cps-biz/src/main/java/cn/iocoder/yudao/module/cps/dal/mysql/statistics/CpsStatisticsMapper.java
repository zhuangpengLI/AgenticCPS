package cn.iocoder.yudao.module.cps.dal.mysql.statistics;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.cps.dal.dataobject.statistics.CpsStatisticsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS统计数据 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsStatisticsMapper extends BaseMapperX<CpsStatisticsDO> {

}
