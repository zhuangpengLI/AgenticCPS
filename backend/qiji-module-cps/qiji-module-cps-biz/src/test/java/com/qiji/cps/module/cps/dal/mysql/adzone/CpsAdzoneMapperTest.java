package com.qiji.cps.module.cps.dal.mysql.adzone;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CpsAdzoneMapperTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "adzone-test"),
                CpsAdzoneDO.class);
    }

    @Test
    void selectByExternalRelationIdOnlyReturnsMemberBinding() {
        CpsAdzoneMapper mapper = mock(CpsAdzoneMapper.class, CALLS_REAL_METHODS);
        doReturn(null).when(mapper).selectOne(any(Wrapper.class));

        mapper.selectActiveMemberAdzoneByExternalRelationId("taobao", "RELATION-1");

        ArgumentCaptor<Wrapper<CpsAdzoneDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("relation_type"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("member"));
    }
}
