package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneBatchCreateRespVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.enums.CpsAdzoneTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.IntStream;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

/**
 * CPS推广位 Service 实现类
 *
 * @author CPS System
 */
@Service
@Validated
public class CpsAdzoneServiceImpl implements CpsAdzoneService {

    @Resource
    private CpsAdzoneMapper adzoneMapper;

    @Override
    public Long createAdzone(CpsAdzoneSaveReqVO createReqVO) {
        validateAdzoneConfig(createReqVO);
        CpsAdzoneDO adzone = BeanUtils.toBean(createReqVO, CpsAdzoneDO.class);
        adzoneMapper.insert(adzone);
        return adzone.getId();
    }

    @Override
    public CpsAdzoneBatchCreateRespVO batchCreateAdzones(List<CpsAdzoneSaveReqVO> items) {
        List<CpsAdzoneBatchCreateRespVO.ItemResult> results = IntStream.range(0, items.size())
                .mapToObj(index -> createOneForBatch(index, items.get(index)))
                .toList();
        int successCount = (int) results.stream()
                .filter(result -> Boolean.TRUE.equals(result.getSuccess()))
                .count();
        return CpsAdzoneBatchCreateRespVO.builder()
                .totalCount(items.size())
                .successCount(successCount)
                .failureCount(items.size() - successCount)
                .results(results)
                .build();
    }

    @Override
    public void updateAdzone(CpsAdzoneSaveReqVO updateReqVO) {
        validateAdzoneExists(updateReqVO.getId());
        validateAdzoneConfig(updateReqVO);
        CpsAdzoneDO updateObj = BeanUtils.toBean(updateReqVO, CpsAdzoneDO.class);
        adzoneMapper.updateById(updateObj);
    }

    @Override
    public void deleteAdzone(Long id) {
        validateAdzoneExists(id);
        adzoneMapper.deleteById(id);
    }

    @Override
    public CpsAdzoneDO getAdzone(Long id) {
        return adzoneMapper.selectById(id);
    }

    @Override
    public PageResult<CpsAdzoneDO> getAdzonePage(CpsAdzonePageReqVO pageReqVO) {
        return adzoneMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CpsAdzoneDO> getAdzoneListByPlatformCode(String platformCode) {
        return adzoneMapper.selectListByPlatformCode(platformCode);
    }

    @Override
    public CpsAdzoneDO getMemberAdzone(String platformCode, Long memberId) {
        if (platformCode == null || memberId == null) {
            return null;
        }
        return adzoneMapper.selectActiveMemberAdzone(platformCode, memberId);
    }

    private void validateAdzoneExists(Long id) {
        if (adzoneMapper.selectById(id) == null) {
            throw exception(ADZONE_NOT_EXISTS);
        }
    }

    private CpsAdzoneBatchCreateRespVO.ItemResult createOneForBatch(int index, CpsAdzoneSaveReqVO reqVO) {
        try {
            Long id = createAdzone(reqVO);
            return CpsAdzoneBatchCreateRespVO.ItemResult.builder()
                    .index(index)
                    .adzoneId(reqVO.getAdzoneId())
                    .id(id)
                    .success(true)
                    .build();
        } catch (Exception ex) {
            return CpsAdzoneBatchCreateRespVO.ItemResult.builder()
                    .index(index)
                    .adzoneId(reqVO.getAdzoneId())
                    .success(false)
                    .failureReason(ex.getMessage())
                    .build();
        }
    }

    private void validateAdzoneConfig(CpsAdzoneSaveReqVO reqVO) {
        List<CpsAdzoneAttributionValidator.Violation> violations =
                CpsAdzoneAttributionValidator.validate(
                        reqVO.getPlatformCode(), reqVO.getAdzoneType(), reqVO.getRelationType(),
                        reqVO.getRelationId(), reqVO.getAdzoneId(), reqVO.getExternalRelationId(),
                        reqVO.getExternalSpecialId());
        if (violations.isEmpty()) {
            return;
        }
        CpsAdzoneAttributionValidator.Violation first = violations.get(0);
        if (first.type() == CpsAdzoneAttributionValidator.ViolationType.RELATION_REQUIRED) {
            String attributionType = CpsAdzoneAttributionValidator.isTaobaoChannel(
                    reqVO.getPlatformCode(), reqVO.getAdzoneType(), reqVO.getRelationType())
                    ? CpsAdzoneTypeEnum.CHANNEL.getType() : CpsAdzoneTypeEnum.MEMBER.getType();
            throw exception(ADZONE_RELATION_REQUIRED, attributionType);
        }
        throw exception(ADZONE_CONFIG_INVALID, first.message());
    }

}
