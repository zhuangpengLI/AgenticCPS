package cn.iocoder.yudao.module.cps.service.risk;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.risk.vo.CpsRiskRulePageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.risk.vo.CpsRiskRuleSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.risk.CpsRiskRuleDO;
import cn.iocoder.yudao.module.cps.dal.mysql.risk.CpsRiskRuleMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.cps.enums.CpsErrorCodeConstants.RISK_RULE_NOT_EXISTS;

/**
 * CPS 风控 Service 实现类
 *
 * <p>基础版风控策略：
 * <ol>
 *   <li>会员黑名单：从 DB 查询是否存在对应 blacklist 规则</li>
 *   <li>IP 黑名单：同上，按 IP 查询</li>
 *   <li>频率限制：Redis INCR 计数器，key = cps:risk:rate:{memberId}:{date}，TTL=1天</li>
 * </ol>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsRiskServiceImpl implements CpsRiskService {

    /** Redis 频率限制计数器 key 前缀 */
    private static final String RATE_LIMIT_KEY_PREFIX = "cps:risk:rate:";

    @Resource
    private CpsRiskRuleMapper riskRuleMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ==================== 风控检查 ====================

    @Override
    public boolean checkTransferAllowed(Long memberId, String clientIp) {
        // 1. 会员黑名单检查
        if (riskRuleMapper.existsBlacklist("member", String.valueOf(memberId))) {
            log.warn("[RiskCheck] 会员黑名单拦截 memberId={}", memberId);
            return false;
        }
        // 2. IP 黑名单检查（clientIp 不为 null 时才检查）
        if (clientIp != null && riskRuleMapper.existsBlacklist("ip", clientIp)) {
            log.warn("[RiskCheck] IP黑名单拦截 ip={}", clientIp);
            return false;
        }
        // 3. 频率限制（Redis INCR 计数器，每日重置）
        CpsRiskRuleDO rateRule = riskRuleMapper.selectActiveRateLimit();
        if (rateRule != null && rateRule.getLimitCount() != null) {
            String redisKey = RATE_LIMIT_KEY_PREFIX + memberId + ":" + LocalDate.now();
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            // 首次计数时设置 TTL（1天）
            if (count != null && count == 1) {
                stringRedisTemplate.expire(redisKey, Duration.ofDays(1));
            }
            if (count != null && count > rateRule.getLimitCount()) {
                log.warn("[RiskCheck] 频率超限 memberId={} count={} limit={}",
                        memberId, count, rateRule.getLimitCount());
                return false;
            }
        }
        return true;
    }

    // ==================== 规则管理 ====================

    @Override
    public Long createRule(CpsRiskRuleSaveReqVO reqVO) {
        CpsRiskRuleDO rule = BeanUtils.toBean(reqVO, CpsRiskRuleDO.class);
        riskRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateRule(CpsRiskRuleSaveReqVO reqVO) {
        // 校验规则存在
        validateRuleExists(reqVO.getId());
        // 更新
        CpsRiskRuleDO updateObj = BeanUtils.toBean(reqVO, CpsRiskRuleDO.class);
        riskRuleMapper.updateById(updateObj);
    }

    @Override
    public void deleteRule(Long id) {
        // 校验规则存在
        validateRuleExists(id);
        // 删除
        riskRuleMapper.deleteById(id);
    }

    @Override
    public PageResult<CpsRiskRuleDO> getRulePage(CpsRiskRulePageReqVO reqVO) {
        return riskRuleMapper.selectPage(reqVO);
    }

    // ==================== 私有方法 ====================

    private void validateRuleExists(Long id) {
        if (riskRuleMapper.selectById(id) == null) {
            throw exception(RISK_RULE_NOT_EXISTS);
        }
    }

}
