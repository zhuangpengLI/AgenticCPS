package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.REBATE_RECORD_NOT_EXISTS;

/**
 * CPS 返利记录 Service 实现
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsRebateRecordServiceImpl implements CpsRebateRecordService {

    @Resource
    private CpsRebateRecordMapper rebateRecordMapper;

    @Resource
    private CpsRebateSettleService rebateSettleService;

    @Resource
    private MemberUserApi memberUserApi;

    @Override
    public PageResult<CpsRebateRecordDO> getRebateRecordPage(CpsRebateRecordPageReqVO pageReqVO) {
        fillMemberIdsForNicknameSearch(pageReqVO);
        PageResult<CpsRebateRecordDO> pageResult = rebateRecordMapper.selectPage(pageReqVO);
        enrichRecordMembers(pageResult.getList());
        return pageResult;
    }

    @Override
    public CpsRebateRecordDO getRebateRecord(Long id) {
        CpsRebateRecordDO record = rebateRecordMapper.selectById(id);
        enrichRecordMembers(record == null ? Collections.emptyList() : List.of(record));
        return record;
    }

    @Override
    public void deleteRebateRecord(Long id) {
        validateRebateRecordExists(id);
        rebateRecordMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRebateRecordList(List<Long> ids) {
        ids.forEach(this::deleteRebateRecord);
    }

    @Override
    public PageResult<CpsRebateRecordDO> getMemberRebateRecordPage(Long memberId, Integer pageNo, Integer pageSize) {
        CpsRebateRecordPageReqVO reqVO = new CpsRebateRecordPageReqVO();
        reqVO.setMemberId(memberId);
        reqVO.setPageNo(pageNo != null ? pageNo : 1);
        reqVO.setPageSize(pageSize != null ? pageSize : 10);
        return rebateRecordMapper.selectPage(reqVO);
    }

    @Override
    public BigDecimal getMemberPendingRebate(Long memberId) {
        BigDecimal pendingRebate = rebateRecordMapper.sumMemberPendingRebate(memberId);
        return pendingRebate != null ? pendingRebate : BigDecimal.ZERO;
    }

    @Override
    public boolean reverseRebate(Long orderId) {
        log.info("[reverseRebate] 触发订单退款回扣, orderId={}", orderId);
        return rebateSettleService.reverseRebate(orderId);
    }

    private void fillMemberIdsForNicknameSearch(CpsRebateRecordPageReqVO pageReqVO) {
        if (isBlank(pageReqVO.getMemberName())) {
            return;
        }
        pageReqVO.setMemberIds(findMemberIdsByNickname(pageReqVO.getMemberName()));
    }

    private List<Long> findMemberIdsByNickname(String memberName) {
        try {
            List<MemberUserRespDTO> users = memberUserApi.getUserListByNickname(memberName);
            if (users == null || users.isEmpty()) {
                return List.of(-1L);
            }
            List<Long> ids = users.stream().map(MemberUserRespDTO::getId).filter(Objects::nonNull).toList();
            return ids.isEmpty() ? List.of(-1L) : ids;
        } catch (Exception e) {
            log.warn("[findMemberIdsByNickname] 按会员名查询会员失败: memberName={}", memberName, e);
            return List.of(-1L);
        }
    }

    private void enrichRecordMembers(Collection<CpsRebateRecordDO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> memberIds = records.stream()
                .map(CpsRebateRecordDO::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return;
        }
        try {
            Map<Long, MemberUserRespDTO> userMap = memberUserApi.getUserMap(memberIds);
            if (userMap == null || userMap.isEmpty()) {
                return;
            }
            records.forEach(record -> {
                MemberUserRespDTO user = userMap.get(record.getMemberId());
                if (user != null && !isBlank(user.getNickname())) {
                    record.setMemberNickname(user.getNickname());
                }
            });
        } catch (Exception e) {
            log.warn("[enrichRecordMembers] 补充返利记录会员昵称失败: memberIds={}", memberIds, e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateRebateRecordExists(Long id) {
        if (rebateRecordMapper.selectById(id) == null) {
            throw exception(REBATE_RECORD_NOT_EXISTS);
        }
    }

}
