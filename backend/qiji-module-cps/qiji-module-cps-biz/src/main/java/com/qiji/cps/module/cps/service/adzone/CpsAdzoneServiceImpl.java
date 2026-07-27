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
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        normalizeAdzone(createReqVO);
        validateAdzoneConfig(createReqVO);
        validateAdzoneUnique(null, createReqVO.getPlatformCode(), createReqVO.getAdzoneId());
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
        normalizeAdzone(updateReqVO);
        validateAdzoneConfig(updateReqVO);
        validateAdzoneUnique(updateReqVO.getId(), updateReqVO.getPlatformCode(),
                updateReqVO.getAdzoneId());
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
    public List<CpsAdzoneDO> getAdzoneListByPlatform(String platformCode) {
        return adzoneMapper.selectAllByPlatformCode(platformCode);
    }

    @Override
    public Long upsertAdzoneForOnboarding(CpsAdzoneSaveReqVO saveReqVO) {
        normalizeAdzone(saveReqVO);
        validateAdzoneConfig(saveReqVO);
        CpsAdzoneDO existing = adzoneMapper.selectByPlatformAndAdzoneId(
                saveReqVO.getPlatformCode(), saveReqVO.getAdzoneId());
        if (existing == null) {
            CpsAdzoneDO created = BeanUtils.toBean(saveReqVO, CpsAdzoneDO.class);
            adzoneMapper.insert(created);
            return created.getId();
        }
        saveReqVO.setId(existing.getId());
        CpsAdzoneDO updated = BeanUtils.toBean(saveReqVO, CpsAdzoneDO.class);
        adzoneMapper.updateById(updated);
        return existing.getId();
    }

    @Override
    public void deleteAdzonesNotIn(String platformCode, Set<String> retainedAdzoneIds) {
        Set<String> retained = new HashSet<>();
        if (retainedAdzoneIds != null) {
            retainedAdzoneIds.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(retained::add);
        }
        for (CpsAdzoneDO adzone : adzoneMapper.selectAllByPlatformCode(platformCode)) {
            String adzoneId = StringUtils.hasText(adzone.getAdzoneId())
                    ? adzone.getAdzoneId().trim() : null;
            if (!retained.contains(adzoneId)) {
                adzoneMapper.deleteById(adzone.getId());
            }
        }
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

    private void validateAdzoneUnique(Long id, String platformCode, String adzoneId) {
        CpsAdzoneDO existing = adzoneMapper.selectByPlatformAndAdzoneId(platformCode, adzoneId);
        if (existing != null && (id == null || !id.equals(existing.getId()))) {
            throw exception(ADZONE_CONFIG_INVALID, "同一平台下推广位 ID 不能重复");
        }
    }

    private void normalizeAdzone(CpsAdzoneSaveReqVO request) {
        request.setPlatformCode(normalizeCode(request.getPlatformCode()));
        request.setAdzoneId(trimToNull(request.getAdzoneId()));
        String adzoneType = normalizeCode(request.getAdzoneType());
        if (!Arrays.asList(CpsAdzoneTypeEnum.ARRAYS).contains(adzoneType)) {
            throw exception(ADZONE_CONFIG_INVALID, "推广位类型不合法");
        }
        String relationType = normalizeCode(request.getRelationType());
        if (relationType != null && !relationType.equals(adzoneType)) {
            throw exception(ADZONE_CONFIG_INVALID, "推广位类型与关联类型必须一致");
        }
        request.setAdzoneType(adzoneType);
        request.setRelationType(relationType);
        request.setExternalRelationId(trimToNull(request.getExternalRelationId()));
        request.setExternalSpecialId(trimToNull(request.getExternalSpecialId()));
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
