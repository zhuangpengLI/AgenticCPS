package cn.iocoder.yudao.module.cps.dal.mysql.freeze;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.cps.controller.admin.freeze.vo.CpsFreezeConfigPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS冻结解冻配置 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsFreezeConfigMapper extends BaseMapperX<CpsFreezeConfigDO> {

    /**
     * 查询分页列表
     */
    default PageResult<CpsFreezeConfigDO> selectPage(CpsFreezeConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsFreezeConfigDO>()
                .eqIfPresent(CpsFreezeConfigDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsFreezeConfigDO::getStatus, reqVO.getStatus())
                .orderByDesc(CpsFreezeConfigDO::getId));
    }

    /**
     * 查询指定平台的启用配置（优先取平台专属，其次取全平台）
     *
     * @param platformCode 平台编码
     * @return 启用中的配置，优先平台专属 > 全平台默认
     */
    default CpsFreezeConfigDO selectActiveByPlatform(String platformCode) {
        // 先查平台专属配置
        CpsFreezeConfigDO config = selectOne(new LambdaQueryWrapperX<CpsFreezeConfigDO>()
                .eq(CpsFreezeConfigDO::getPlatformCode, platformCode)
                .eq(CpsFreezeConfigDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
        if (config != null) {
            return config;
        }
        // 再查全平台配置（platform_code IS NULL）
        return selectOne(new LambdaQueryWrapperX<CpsFreezeConfigDO>()
                .isNull(CpsFreezeConfigDO::getPlatformCode)
                .eq(CpsFreezeConfigDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

}
