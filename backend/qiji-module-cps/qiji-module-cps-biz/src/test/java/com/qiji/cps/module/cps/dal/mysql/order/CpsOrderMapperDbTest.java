package com.qiji.cps.module.cps.dal.mysql.order;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(CpsOrderMapperDbTest.TenantTestConfiguration.class)
class CpsOrderMapperDbTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantTestConfiguration {
        @Bean
        TenantLineInnerInterceptor tenantLineInnerInterceptor(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor, tenantInterceptor, 0);
            return tenantInterceptor;
        }
    }

    @Resource
    private CpsOrderMapper orderMapper;
    @Resource
    private CpsRebateAccountMapper accountMapper;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void orderUniquenessIsTenantAndPlatformScoped() {
        TenantContextHolder.setTenantId(1L);
        orderMapper.insert(newOrder("taobao", "SAME-ORDER"));

        assertThrows(DuplicateKeyException.class,
                () -> orderMapper.insert(newOrder("taobao", "SAME-ORDER")));
        assertDoesNotThrow(() -> orderMapper.insert(newOrder("jd", "SAME-ORDER")));

        TenantContextHolder.setTenantId(2L);
        assertDoesNotThrow(() -> orderMapper.insert(newOrder("taobao", "SAME-ORDER")));
        assertEquals(3L, orderMapper.selectCount(null));
    }

    @Test
    void logicallyDeletedOrderCanBeFoundAndRestoredWithoutCreatingDuplicate() {
        TenantContextHolder.setTenantId(1L);
        CpsOrderDO order = newOrder("taobao", "RESTORE-ORDER");
        orderMapper.insert(order);

        orderMapper.deleteById(order.getId());

        assertNull(orderMapper.selectByPlatformOrderId("taobao", "RESTORE-ORDER"));
        CpsOrderDO deleted = orderMapper.selectDeletedByPlatformOrderId("taobao", "RESTORE-ORDER");
        assertEquals(order.getId(), deleted.getId());
        assertEquals(Boolean.TRUE, deleted.getDeleted());

        assertEquals(1, orderMapper.restoreDeletedById(order.getId()));
        assertEquals(order.getId(), orderMapper.selectByPlatformOrderId("taobao", "RESTORE-ORDER").getId());
        assertEquals(1L, orderMapper.selectCount(null));
    }

    @Test
    void accountUniquenessIsTenantAndMemberScoped() {
        TenantContextHolder.setTenantId(1L);
        accountMapper.insert(newAccount(99L));
        assertThrows(DuplicateKeyException.class, () -> accountMapper.insert(newAccount(99L)));

        TenantContextHolder.setTenantId(2L);
        assertDoesNotThrow(() -> accountMapper.insert(newAccount(99L)));
        assertEquals(2L, accountMapper.selectCount(null));
    }

    private CpsOrderDO newOrder(String platformCode, String platformOrderId) {
        CpsOrderDO order = CpsOrderDO.builder()
                .platformCode(platformCode)
                .platformOrderId(platformOrderId)
                .orderStatus("paid")
                .statusVersion(0)
                .build();
        order.setTenantId(TenantContextHolder.getRequiredTenantId());
        return order;
    }

    private CpsRebateAccountDO newAccount(Long memberId) {
        CpsRebateAccountDO account = CpsRebateAccountDO.builder()
                .memberId(memberId)
                .totalRebate(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .frozenBalance(BigDecimal.ZERO)
                .debtBalance(BigDecimal.ZERO)
                .withdrawnAmount(BigDecimal.ZERO)
                .status(1)
                .version(0)
                .build();
        account.setTenantId(TenantContextHolder.getRequiredTenantId());
        return account;
    }
}
