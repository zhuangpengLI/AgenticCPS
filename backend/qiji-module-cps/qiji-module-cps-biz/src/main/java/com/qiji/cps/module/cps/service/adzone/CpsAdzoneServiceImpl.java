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

import java.util.List;
import java.util.regex.Pattern;
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

    private static final String PLATFORM_TAOBAO = "taobao";
    private static final Pattern TAOBAO_PID_PATTERN = Pattern.compile("^mm_\\d+_\\d+_\\d+$");
    private static final Pattern TAOBAO_SPECIAL_ID_PATTERN = Pattern.compile("^\\d+$");

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
        if (!isMemberAdzone(reqVO)) {
            return;
        }
        if (reqVO.getRelationId() == null) {
            throw exception(ADZONE_CONFIG_INVALID, "会员专属推广位必须选择关联会员");
        }
        if (!PLATFORM_TAOBAO.equalsIgnoreCase(reqVO.getPlatformCode())) {
            return;
        }
        if (!StringUtils.hasText(reqVO.getAdzoneId())
                || !TAOBAO_PID_PATTERN.matcher(reqVO.getAdzoneId().trim()).matches()) {
            throw exception(ADZONE_CONFIG_INVALID, "淘宝会员 PID 必须使用 mm_数字_数字_数字 格式");
        }
        if (!StringUtils.hasText(reqVO.getExternalSpecialId())
                || !TAOBAO_SPECIAL_ID_PATTERN.matcher(reqVO.getExternalSpecialId().trim()).matches()) {
            throw exception(ADZONE_CONFIG_INVALID, "淘宝会员专属推广位必须填写数字会员运营ID specialId");
        }
    }

    private boolean isMemberAdzone(CpsAdzoneSaveReqVO reqVO) {
        return CpsAdzoneTypeEnum.MEMBER.getType().equalsIgnoreCase(reqVO.getAdzoneType())
                || CpsAdzoneTypeEnum.MEMBER.getType().equalsIgnoreCase(reqVO.getRelationType());
    }

}
