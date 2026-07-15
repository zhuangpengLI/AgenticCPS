package com.qiji.cps.module.cps.dal.mysql.order;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CpsOrderMapperTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "order-test"),
                CpsOrderDO.class);
    }

    @Test
    void selectByPlatformOrderIdScopesLookupToPlatform() {
        CpsOrderMapper mapper = mock(CpsOrderMapper.class, CALLS_REAL_METHODS);
        doReturn(null).when(mapper).selectOne(any(Wrapper.class));

        mapper.selectByPlatformOrderId("jd", "SAME-ORDER-1");

        ArgumentCaptor<Wrapper<CpsOrderDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("platform_code"));
        assertTrue(wrapper.getSqlSegment().contains("platform_order_id"));
        assertEquals(2, wrapper.getParamNameValuePairs().size());
        assertTrue(wrapper.getParamNameValuePairs().containsValue("jd"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("SAME-ORDER-1"));
    }

    @Test
    void updateByIdAndStatusVersionRequiresExpectedVersionAndIncrementsIt() {
        CpsOrderMapper mapper = mock(CpsOrderMapper.class, CALLS_REAL_METHODS);
        when(mapper.update(any(CpsOrderDO.class), any(Wrapper.class))).thenReturn(1);
        CpsOrderDO update = CpsOrderDO.builder().id(8L).orderStatus("refunded").build();

        assertEquals(1, mapper.updateByIdAndStatusVersion(update, 4));

        ArgumentCaptor<Wrapper<CpsOrderDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(any(CpsOrderDO.class), wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("id"));
        assertTrue(wrapper.getSqlSegment().contains("status_version"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(8L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(4));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(5));
    }

    @Test
    void selectPendingSettleOrdersExcludesOrdersThatAlreadyCreatedV2Freeze() {
        CpsOrderMapper mapper = mock(CpsOrderMapper.class, CALLS_REAL_METHODS);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        mapper.selectPendingSettleOrders(List.of("settled"), 20);

        ArgumentCaptor<Wrapper<CpsOrderDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        String sql = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("rebate_freeze_status IS NULL"));
        assertTrue(sql.contains("rebate_freeze_status"));
        assertTrue(sql.contains("rebate_settle_next_retry_time"));
        assertTrue(sql.contains("COALESCE(rebate_settle_next_retry_time, create_time) ASC"));
    }

    @Test
    void selectForUpdateByIdLocksCurrentOrderRow() {
        CpsOrderMapper mapper = mock(CpsOrderMapper.class, CALLS_REAL_METHODS);
        doReturn(null).when(mapper).selectOne(any(Wrapper.class));

        mapper.selectForUpdateById(8L);

        ArgumentCaptor<Wrapper<CpsOrderDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("FOR UPDATE"));
    }

    @Test
    void updateRebateFreezeUsesStatusVersionCompareAndSet() {
        CpsOrderMapper mapper = mock(CpsOrderMapper.class, CALLS_REAL_METHODS);
        when(mapper.update(any(CpsOrderDO.class), any(Wrapper.class))).thenReturn(1);
        CpsOrderDO update = CpsOrderDO.builder().id(8L).orderStatus("settled")
                .realRebate(new java.math.BigDecimal("8.00")).rebateFreezeStatus("frozen").build();

        assertEquals(1, mapper.updateRebateFreezeByStatusVersion(update, 4));

        ArgumentCaptor<Wrapper<CpsOrderDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(any(CpsOrderDO.class), wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("status_version"));
        assertTrue(wrapper.getSqlSegment().contains("order_status"));
        assertTrue(((LambdaUpdateWrapper<CpsOrderDO>) wrapperCaptor.getValue())
                .getSqlSet().contains("rebate_freeze_status"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(4));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(5));
    }
}
