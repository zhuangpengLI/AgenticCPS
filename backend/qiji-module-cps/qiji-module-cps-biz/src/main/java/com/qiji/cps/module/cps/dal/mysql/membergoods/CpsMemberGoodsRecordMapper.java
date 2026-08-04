package com.qiji.cps.module.cps.dal.mysql.membergoods;

import com.qiji.cps.framework.common.pojo.PageParam;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.membergoods.CpsMemberGoodsRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CpsMemberGoodsRecordMapper extends BaseMapperX<CpsMemberGoodsRecordDO> {

    default CpsMemberGoodsRecordDO selectActiveByIdentity(Long memberId, String recordType, String identityKey) {
        return selectOne(new LambdaQueryWrapperX<CpsMemberGoodsRecordDO>()
                .eq(CpsMemberGoodsRecordDO::getMemberId, memberId)
                .eq(CpsMemberGoodsRecordDO::getRecordType, recordType)
                .eq(CpsMemberGoodsRecordDO::getIdentityKey, identityKey));
    }

    default PageResult<CpsMemberGoodsRecordDO> selectMemberPage(PageParam pageParam, Long memberId,
                                                                 String recordType, String platformCode) {
        return selectPage(pageParam, new LambdaQueryWrapperX<CpsMemberGoodsRecordDO>()
                .eq(CpsMemberGoodsRecordDO::getMemberId, memberId)
                .eq(CpsMemberGoodsRecordDO::getRecordType, recordType)
                .eqIfPresent(CpsMemberGoodsRecordDO::getPlatformCode, platformCode)
                .orderByDesc(CpsMemberGoodsRecordDO::getUpdateTime)
                .orderByDesc(CpsMemberGoodsRecordDO::getId));
    }

    default List<CpsMemberGoodsRecordDO> selectMemberList(Long memberId, String recordType) {
        return selectList(new LambdaQueryWrapperX<CpsMemberGoodsRecordDO>()
                .eq(CpsMemberGoodsRecordDO::getMemberId, memberId)
                .eq(CpsMemberGoodsRecordDO::getRecordType, recordType)
                .orderByDesc(CpsMemberGoodsRecordDO::getUpdateTime)
                .orderByDesc(CpsMemberGoodsRecordDO::getId));
    }

    default int deleteMemberRecords(Long memberId, String recordType) {
        return delete(new LambdaQueryWrapperX<CpsMemberGoodsRecordDO>()
                .eq(CpsMemberGoodsRecordDO::getMemberId, memberId)
                .eq(CpsMemberGoodsRecordDO::getRecordType, recordType));
    }

    default int deleteActiveByIdentity(Long memberId, String recordType, String identityKey) {
        return delete(new LambdaQueryWrapperX<CpsMemberGoodsRecordDO>()
                .eq(CpsMemberGoodsRecordDO::getMemberId, memberId)
                .eq(CpsMemberGoodsRecordDO::getRecordType, recordType)
                .eq(CpsMemberGoodsRecordDO::getIdentityKey, identityKey));
    }
}
