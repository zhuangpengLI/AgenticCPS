package cn.iocoder.yudao.module.cps.dal.mysql.freeze;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeRecordPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import cn.iocoder.yudao.module.cps.enums.CpsFreezeStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CPS冻结解冻记录 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsFreezeRecordMapper extends BaseMapperX<CpsFreezeRecordDO> {

    /**
     * 查询已到达解冻时间且状态为 frozen 的记录（批量自动解冻）
     */
    default List<CpsFreezeRecordDO> selectPendingUnfreeze(int limit) {
        return selectList(new LambdaQueryWrapperX<CpsFreezeRecordDO>()
                .eq(CpsFreezeRecordDO::getStatus, CpsFreezeStatusEnum.FROZEN.getStatus())
                .le(CpsFreezeRecordDO::getUnfreezeTime, LocalDateTime.now())
                .last("LIMIT " + limit));
    }

    /**
     * 分页查询冻结记录
     */
    default PageResult<CpsFreezeRecordDO> selectPage(CpsFreezeRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsFreezeRecordDO>()
                .eqIfPresent(CpsFreezeRecordDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsFreezeRecordDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CpsFreezeRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsFreezeRecordDO::getId));
    }

}
