package com.qiji.cps.module.cps.service.vendor;

import com.qiji.cps.framework.common.enums.CommonStatusEnum;
import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.controller.admin.vendor.vo.CpsApiVendorSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import com.qiji.cps.module.cps.dal.mysql.vendor.CpsApiVendorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.VENDOR_CAPABILITY_NOT_READY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link CpsApiVendorServiceImpl} 单元测试
 *
 * @author CPS System
 */
@ExtendWith(MockitoExtension.class)
class CpsApiVendorServiceImplTest {

    @InjectMocks
    private CpsApiVendorServiceImpl vendorService;

    @Mock
    private CpsApiVendorMapper vendorMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("创建供应商 - 正常流程")
    void testCreateVendor_success() {
        CpsApiVendorSaveReqVO reqVO = new CpsApiVendorSaveReqVO();
        reqVO.setVendorCode("dataoke");
        reqVO.setVendorName("大淘客");
        reqVO.setVendorType("aggregator");
        reqVO.setPlatformCode("taobao");
        reqVO.setAppKey("testKey");
        reqVO.setAppSecret("testSecret");
        reqVO.setApiBaseUrl("https://openapi.dataoke.com/api");
        reqVO.setStatus(1);

        // 不存在相同组合
        when(vendorMapper.selectByVendorAndPlatform("dataoke", "taobao")).thenReturn(null);
        when(vendorMapper.insert(any(CpsApiVendorDO.class))).thenReturn(1);

        Long id = vendorService.createVendor(reqVO);
        verify(vendorMapper).insert(org.mockito.ArgumentMatchers.<CpsApiVendorDO>argThat(vendor ->
                "dataoke".equals(vendor.getVendorCode()) &&
                "taobao".equals(vendor.getPlatformCode())
        ));
    }

    @Test
    @DisplayName("创建供应商 - 重复组合应抛异常")
    void testCreateVendor_duplicate() {
        CpsApiVendorSaveReqVO reqVO = new CpsApiVendorSaveReqVO();
        reqVO.setVendorCode("dataoke");
        reqVO.setPlatformCode("taobao");

        CpsApiVendorDO existing = new CpsApiVendorDO();
        existing.setId(999L);
        when(vendorMapper.selectByVendorAndPlatform("dataoke", "taobao")).thenReturn(existing);

        assertThrows(ServiceException.class, () -> vendorService.createVendor(reqVO));
    }

    @Test
    @DisplayName("创建供应商 - 未完成业务能力验收的 official skeleton 禁止启用")
    void testCreateVendor_rejectsOfficialSkeletonEnable() throws Exception {
        setVendorClients(List.of(new StubVendorClient("official", "taobao", "official",
                Set.of(CpsVendorCapability.CONNECTION_TEST))));
        CpsApiVendorSaveReqVO reqVO = buildOfficialReqVO("taobao", CommonStatusEnum.ENABLE.getStatus());

        when(vendorMapper.selectByVendorAndPlatform("official", "taobao")).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> vendorService.createVendor(reqVO));
        assertEquals(VENDOR_CAPABILITY_NOT_READY.getCode(), exception.getCode());
        verify(vendorMapper, never()).insert(any(CpsApiVendorDO.class));
    }

    @Test
    @DisplayName("更新供应商 - 不存在应抛异常")
    void testUpdateVendor_notExists() {
        CpsApiVendorSaveReqVO reqVO = new CpsApiVendorSaveReqVO();
        reqVO.setId(1L);
        reqVO.setVendorCode("dataoke");
        reqVO.setPlatformCode("taobao");

        when(vendorMapper.selectById(1L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> vendorService.updateVendor(reqVO));
    }

    @Test
    @DisplayName("更新供应商 - 未完成业务能力验收的 official skeleton 禁止启用")
    void testUpdateVendor_rejectsOfficialSkeletonEnable() throws Exception {
        setVendorClients(List.of(new StubVendorClient("official", "jd", "official",
                Set.of(CpsVendorCapability.CONNECTION_TEST))));
        CpsApiVendorSaveReqVO reqVO = buildOfficialReqVO("jd", CommonStatusEnum.ENABLE.getStatus());
        reqVO.setId(1L);

        CpsApiVendorDO existing = new CpsApiVendorDO();
        existing.setId(1L);
        existing.setVendorCode("official");
        existing.setPlatformCode("jd");
        when(vendorMapper.selectById(1L)).thenReturn(existing);
        when(vendorMapper.selectByVendorAndPlatform("official", "jd")).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class, () -> vendorService.updateVendor(reqVO));
        assertEquals(VENDOR_CAPABILITY_NOT_READY.getCode(), exception.getCode());
        verify(vendorMapper, never()).updateById(any(CpsApiVendorDO.class));
    }

    @Test
    @DisplayName("删除供应商 - 正常流程")
    void testDeleteVendor_success() {
        CpsApiVendorDO existing = new CpsApiVendorDO();
        existing.setId(1L);
        when(vendorMapper.selectById(1L)).thenReturn(existing);

        vendorService.deleteVendor(1L);
        verify(vendorMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除供应商 - 不存在应抛异常")
    void testDeleteVendor_notExists() {
        when(vendorMapper.selectById(1L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> vendorService.deleteVendor(1L));
    }

    @Test
    @DisplayName("buildVendorConfig - 正常转换")
    void testBuildVendorConfig_success() {
        CpsApiVendorDO vendorDO = CpsApiVendorDO.builder()
                .vendorCode("dataoke")
                .vendorType("aggregator")
                .platformCode("taobao")
                .appKey("key123")
                .appSecret("secret456")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .authToken("token789")
                .defaultAdzoneId("mm_123")
                .extraConfig("{\"timeout\": \"5000\"}")
                .build();

        CpsVendorConfig config = vendorService.buildVendorConfig(vendorDO);
        assertNotNull(config);
        assertEquals("dataoke", config.getVendorCode());
        assertEquals("aggregator", config.getVendorType());
        assertEquals("taobao", config.getPlatformCode());
        assertEquals("key123", config.getAppKey());
        assertEquals("secret456", config.getAppSecret());
        assertEquals("https://openapi.dataoke.com/api", config.getApiBaseUrl());
        assertEquals("token789", config.getAuthToken());
        assertEquals("mm_123", config.getDefaultAdzoneId());
        assertNotNull(config.getExtraConfig());
        assertEquals("5000", config.getExtraConfig().get("timeout"));
    }

    @Test
    @DisplayName("buildVendorConfig - null 输入返回 null")
    void testBuildVendorConfig_null() {
        assertNull(vendorService.buildVendorConfig(null));
    }

    @Test
    @DisplayName("buildVendorConfig - 空 extraConfig 返回空 Map")
    void testBuildVendorConfig_emptyExtra() {
        CpsApiVendorDO vendorDO = CpsApiVendorDO.builder()
                .vendorCode("dataoke")
                .vendorType("aggregator")
                .platformCode("taobao")
                .appKey("key")
                .appSecret("secret")
                .apiBaseUrl("https://example.com")
                .extraConfig(null)
                .build();

        CpsVendorConfig config = vendorService.buildVendorConfig(vendorDO);
        assertNotNull(config.getExtraConfig());
        assertTrue(config.getExtraConfig().isEmpty());
    }

    private CpsApiVendorSaveReqVO buildOfficialReqVO(String platformCode, Integer status) {
        CpsApiVendorSaveReqVO reqVO = new CpsApiVendorSaveReqVO();
        reqVO.setVendorCode("official");
        reqVO.setVendorName("官方API");
        reqVO.setVendorType("official");
        reqVO.setPlatformCode(platformCode);
        reqVO.setAppKey("app-key");
        reqVO.setAppSecret("app-secret");
        reqVO.setApiBaseUrl("https://api.example.com");
        reqVO.setStatus(status);
        return reqVO;
    }

    private void setVendorClients(List<CpsApiVendorClient> vendorClients) throws Exception {
        Field field = CpsApiVendorServiceImpl.class.getDeclaredField("vendorClients");
        field.setAccessible(true);
        field.set(vendorService, vendorClients);
    }

    private record StubVendorClient(String vendorCode, String platformCode, String vendorType,
                                    Set<CpsVendorCapability> capabilities) implements CpsApiVendorClient {

        @Override
        public String getVendorCode() {
            return vendorCode;
        }

        @Override
        public String getPlatformCode() {
            return platformCode;
        }

        @Override
        public String getVendorType() {
            return vendorType;
        }

        @Override
        public Set<CpsVendorCapability> getCapabilities() {
            return capabilities;
        }

        @Override
        public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
            return null;
        }

        @Override
        public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
            return null;
        }

        @Override
        public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
            return List.of();
        }

        @Override
        public boolean testConnection(CpsVendorConfig config) {
            return true;
        }
    }

}
