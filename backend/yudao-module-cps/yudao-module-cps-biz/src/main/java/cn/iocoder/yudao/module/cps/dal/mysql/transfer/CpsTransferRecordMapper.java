package cn.iocoder.yudao.module.cps.dal.mysql.transfer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

/**
 * CPS转链记录 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsTransferRecordMapper extends BaseMapperX<CpsTransferRecordDO> {

    /**
     * 分页查询转链记录
     */
    default PageResult<CpsTransferRecordDO> selectPage(CpsTransferRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eqIfPresent(CpsTransferRecordDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsTransferRecordDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsTransferRecordDO::getStatus, reqVO.getStatus())
                .likeIfPresent(CpsTransferRecordDO::getItemTitle, reqVO.getItemTitle())
                .betweenIfPresent(CpsTransferRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsTransferRecordDO::getId));
    }

    /**
     * 统计会员指定日期的转链次数（风控用）
     */
    default long countTodayByMember(Long memberId, LocalDate date) {
        return selectCount(new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eq(CpsTransferRecordDO::getMemberId, memberId)
                .between(CpsTransferRecordDO::getCreateTime,
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
    }

}
