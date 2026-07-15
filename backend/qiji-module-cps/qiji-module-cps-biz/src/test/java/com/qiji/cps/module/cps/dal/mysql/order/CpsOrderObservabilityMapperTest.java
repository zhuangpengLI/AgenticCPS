package com.qiji.cps.module.cps.dal.mysql.order;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class CpsOrderObservabilityMapperTest {

    @BeforeAll
    static void initializeLambdaMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "attribution-test"),
                CpsOrderAttributionLogDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "checkpoint-test"),
                CpsOrderSyncCheckpointDO.class);
    }

    @Test
    void attributionLogPage_buildsRequestedFilters() {
        CpsOrderAttributionLogMapper mapper = mock(CpsOrderAttributionLogMapper.class, CALLS_REAL_METHODS);
        AtomicReference<AbstractWrapper<?, ?, ?>> wrapperRef = capturePageWrapper(mapper);
        CpsOrderAttributionLogPageReqVO request = new CpsOrderAttributionLogPageReqVO();
        request.setPlatformCode("taobao");
        request.setResult("REJECTED");

        mapper.selectPage(request);

        AbstractWrapper<?, ?, ?> wrapper = wrapperRef.get();
        assertTrue(wrapper.getSqlSegment().contains("platform_code"));
        assertTrue(wrapper.getSqlSegment().contains("result"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("taobao"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("REJECTED"));
    }

    @Test
    void syncCheckpointPage_buildsRequestedFilters() {
        CpsOrderSyncCheckpointMapper mapper = mock(CpsOrderSyncCheckpointMapper.class, CALLS_REAL_METHODS);
        AtomicReference<AbstractWrapper<?, ?, ?>> wrapperRef = capturePageWrapper(mapper);
        CpsOrderSyncCheckpointPageReqVO request = new CpsOrderSyncCheckpointPageReqVO();
        request.setVendorCode("dataoke");
        request.setLastSyncStatus("PARTIAL");

        mapper.selectPage(request);

        AbstractWrapper<?, ?, ?> wrapper = wrapperRef.get();
        assertTrue(wrapper.getSqlSegment().contains("vendor_code"));
        assertTrue(wrapper.getSqlSegment().contains("last_sync_status"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("dataoke"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("PARTIAL"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> AtomicReference<AbstractWrapper<?, ?, ?>> capturePageWrapper(T mapper) {
        AtomicReference<AbstractWrapper<?, ?, ?>> wrapperRef = new AtomicReference<>();
        if (mapper instanceof CpsOrderAttributionLogMapper attributionMapper) {
            doAnswer(invocation -> completePage(invocation.getArgument(0), invocation.getArgument(1), wrapperRef))
                    .when(attributionMapper).selectPage(any(IPage.class), any(Wrapper.class));
        } else if (mapper instanceof CpsOrderSyncCheckpointMapper checkpointMapper) {
            doAnswer(invocation -> completePage(invocation.getArgument(0), invocation.getArgument(1), wrapperRef))
                    .when(checkpointMapper).selectPage(any(IPage.class), any(Wrapper.class));
        }
        return wrapperRef;
    }

    private static IPage<?> completePage(IPage<?> page, Wrapper<?> wrapper,
                                         AtomicReference<AbstractWrapper<?, ?, ?>> wrapperRef) {
        wrapperRef.set((AbstractWrapper<?, ?, ?>) wrapper);
        page.setRecords(List.of());
        page.setTotal(0L);
        return page;
    }
}
