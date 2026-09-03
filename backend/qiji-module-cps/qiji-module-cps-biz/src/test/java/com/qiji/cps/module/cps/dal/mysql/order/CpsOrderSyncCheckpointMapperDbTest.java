package com.qiji.cps.module.cps.dal.mysql.order;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(CpsOrderSyncCheckpointMapperDbTest.TenantTestConfiguration.class)
class CpsOrderSyncCheckpointMapperDbTest extends BaseDbUnitTest {

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
    private CpsOrderSyncCheckpointMapper checkpointMapper;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void updateByIdUsesAndAdvancesOptimisticLockVersion() {
        TenantContextHolder.setTenantId(1L);
        CpsOrderSyncCheckpointDO checkpoint = CpsOrderSyncCheckpointDO.builder()
                .platformCode("taobao")
                .vendorCode("official")
                .orderScene(1)
                .queryType("INCREMENTAL")
                .lastSyncStatus("RUNNING")
                .version(0)
                .build();
        checkpoint.setTenantId(1L);
        checkpointMapper.insert(checkpoint);

        checkpoint.setLastSyncStatus("SUCCESS");
        assertEquals(1, checkpointMapper.updateById(checkpoint));
        assertEquals(1, checkpoint.getVersion());
        assertEquals(1, checkpointMapper.selectById(checkpoint.getId()).getVersion());

        CpsOrderSyncCheckpointDO stale = CpsOrderSyncCheckpointDO.builder()
                .id(checkpoint.getId())
                .lastSyncStatus("STALE")
                .version(0)
                .build();
        assertEquals(0, checkpointMapper.updateById(stale));
    }
}
