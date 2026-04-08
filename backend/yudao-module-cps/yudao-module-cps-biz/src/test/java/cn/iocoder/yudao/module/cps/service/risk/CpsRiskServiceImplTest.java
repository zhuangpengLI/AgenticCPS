package cn.iocoder.yudao.module.cps.service.risk;

import cn.iocoder.yudao.module.cps.dal.dataobject.risk.CpsRiskRuleDO;
import cn.iocoder.yudao.module.cps.dal.mysql.risk.CpsRiskRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link CpsRiskServiceImpl} 单元测试
 *
 * <p>重点测试风控检查核心逻辑：黑名单拦截 + 频率限制。</p>
 *
 * @author CPS System
 */
@ExtendWith(MockitoExtension.class)
class CpsRiskServiceImplTest {

    @InjectMocks
    private CpsRiskServiceImpl riskService;

    @Mock
    private CpsRiskRuleMapper riskRuleMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ==================== 黑名单拦截测试 ====================

    @Test
    @DisplayName("checkTransferAllowed - 会员在黑名单中，返回 false")
    void checkTransferAllowed_memberBlacklisted() {
        when(riskRuleMapper.existsBlacklist("member", "100")).thenReturn(true);

        boolean result = riskService.checkTransferAllowed(100L, "1.2.3.4");

        assertFalse(result);
        // 黑名单命中后不应继续查 IP 或频率
        verify(riskRuleMapper, never()).existsBlacklist(eq("ip"), anyString());
    }

    @Test
    @DisplayName("checkTransferAllowed - IP在黑名单中，返回 false")
    void checkTransferAllowed_ipBlacklisted() {
        when(riskRuleMapper.existsBlacklist("member", "100")).thenReturn(false);
        when(riskRuleMapper.existsBlacklist("ip", "1.2.3.4")).thenReturn(true);

        boolean result = riskService.checkTransferAllowed(100L, "1.2.3.4");

        assertFalse(result);
    }

    @Test
    @DisplayName("checkTransferAllowed - clientIp 为 null 时跳过 IP 黑名单检查")
    void checkTransferAllowed_nullIpSkipped() {
        when(riskRuleMapper.existsBlacklist("member", "100")).thenReturn(false);
        CpsRiskRuleDO rule = CpsRiskRuleDO.builder().limitCount(100).build();
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(rule);
        when(valueOps.increment(anyString())).thenReturn(1L);

        boolean result = riskService.checkTransferAllowed(100L, null);

        assertTrue(result);
        // null IP 时不调用 IP 黑名单查询
        verify(riskRuleMapper, never()).existsBlacklist(eq("ip"), anyString());
    }

    // ==================== 频率限制测试 ====================

    @Test
    @DisplayName("checkTransferAllowed - 频率超限，返回 false")
    void checkTransferAllowed_rateLimitExceeded() {
        when(riskRuleMapper.existsBlacklist(any(), any())).thenReturn(false);
        CpsRiskRuleDO rule = CpsRiskRuleDO.builder().limitCount(5).build();
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(rule);
        when(valueOps.increment(anyString())).thenReturn(6L); // 超过 5 次

        boolean result = riskService.checkTransferAllowed(100L, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("checkTransferAllowed - 首次计数时设置 Redis TTL")
    void checkTransferAllowed_firstCountSetsTtl() {
        when(riskRuleMapper.existsBlacklist(any(), any())).thenReturn(false);
        CpsRiskRuleDO rule = CpsRiskRuleDO.builder().limitCount(100).build();
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(rule);
        when(valueOps.increment(anyString())).thenReturn(1L); // 第 1 次

        riskService.checkTransferAllowed(100L, null);

        // 首次计数时应设置 TTL
        verify(stringRedisTemplate).expire(anyString(), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("checkTransferAllowed - 非首次计数，不重复设置 TTL")
    void checkTransferAllowed_notFirstCount_noTtl() {
        when(riskRuleMapper.existsBlacklist(any(), any())).thenReturn(false);
        CpsRiskRuleDO rule = CpsRiskRuleDO.builder().limitCount(100).build();
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(rule);
        when(valueOps.increment(anyString())).thenReturn(50L); // 第 50 次

        riskService.checkTransferAllowed(100L, null);

        // 非首次不设置 TTL
        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("checkTransferAllowed - 全部通过，返回 true")
    void checkTransferAllowed_allowed() {
        when(riskRuleMapper.existsBlacklist(any(), any())).thenReturn(false);
        CpsRiskRuleDO rule = CpsRiskRuleDO.builder().limitCount(100).build();
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(rule);
        when(valueOps.increment(anyString())).thenReturn(1L);

        boolean result = riskService.checkTransferAllowed(100L, "1.2.3.4");

        assertTrue(result);
    }

    @Test
    @DisplayName("checkTransferAllowed - 无频率限制规则时直接通过")
    void checkTransferAllowed_noRateRule() {
        when(riskRuleMapper.existsBlacklist(any(), any())).thenReturn(false);
        when(riskRuleMapper.selectActiveRateLimit()).thenReturn(null); // 无规则

        boolean result = riskService.checkTransferAllowed(100L, "1.2.3.4");

        assertTrue(result);
        // 无规则时不应操作 Redis
        verify(stringRedisTemplate, never()).opsForValue();
    }

}
