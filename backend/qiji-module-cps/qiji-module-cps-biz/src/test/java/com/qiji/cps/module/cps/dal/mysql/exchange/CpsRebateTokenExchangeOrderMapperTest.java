package com.qiji.cps.module.cps.dal.mysql.exchange;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CpsRebateTokenExchangeOrderMapperTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "exchange-test"),
                CpsRebateTokenExchangeOrderDO.class);
    }

    @Test
    void statusUpdateRequiresExpectedVersionAndAllowedSourceStatus() {
        CpsRebateTokenExchangeOrderMapper mapper = mock(
                CpsRebateTokenExchangeOrderMapper.class, CALLS_REAL_METHODS);
        when(mapper.update(any(CpsRebateTokenExchangeOrderDO.class), any(Wrapper.class))).thenReturn(1);
        CpsRebateTokenExchangeOrderDO update = CpsRebateTokenExchangeOrderDO.builder()
                .id(8L).status("SUCCESS").build();

        assertEquals(1, mapper.updateByIdAndStatusVersion(update, 4, List.of("CREDITED")));

        ArgumentCaptor<Wrapper<CpsRebateTokenExchangeOrderDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(any(CpsRebateTokenExchangeOrderDO.class), wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("status_version"));
        assertTrue(wrapper.getSqlSegment().contains("status"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(4));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(5));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("CREDITED"));
    }
}
