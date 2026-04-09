package cn.iocoder.yudao.module.cps.client;

import cn.iocoder.yudao.module.cps.client.dto.CpsVendorConfig;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.iocoder.yudao.module.cps.service.platform.CpsPlatformService;
import cn.iocoder.yudao.module.cps.service.vendor.CpsApiVendorService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CPS 平台客户端工厂（策略模式注册中心 + 双维度路由）
 *
 * <p>基于 Spring Bean 自动注入机制，所有实现了 {@link CpsPlatformClient} 的 Bean 在启动时
 * 自动注册到工厂，业务代码通过平台编码动态获取对应的适配器实例。</p>
 *
 * <p>新增供应商维度路由：通过 vendorCode + platformCode 双维度获取具体的
 * {@link CpsApiVendorClient} 实现，支持同一电商平台对接多个 API 供应商。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class CpsPlatformClientFactory {

    /**
     * 平台编码 → 客户端适配器映射表（面向业务层）
     */
    private final Map<String, CpsPlatformClient> clientMap = new ConcurrentHashMap<>();

    /**
     * vendorCode:platformCode → 供应商客户端映射表（面向底层实现）
     */
    private final Map<String, CpsApiVendorClient> vendorClientMap = new ConcurrentHashMap<>();

    /**
     * 所有平台客户端适配器（Spring 自动注入所有实现 Bean）
     */
    @Resource
    private List<CpsPlatformClient> platformClients;

    /**
     * 所有供应商客户端（Spring 自动注入所有实现 Bean）
     */
    @Resource
    private List<CpsApiVendorClient> vendorClients;

    @Resource
    private CpsPlatformService platformService;

    @Resource
    private CpsApiVendorService vendorService;

    @PostConstruct
    public void init() {
        // 注册面向业务的平台客户端
        if (platformClients != null) {
            for (CpsPlatformClient client : platformClients) {
                clientMap.put(client.getPlatformCode(), client);
                log.info("[CpsPlatformClientFactory] 注册平台适配器: {}", client.getPlatformCode());
            }
        }
        log.info("[CpsPlatformClientFactory] 共注册 {} 个平台适配器", clientMap.size());

        // 注册供应商客户端（vendorCode:platformCode → client）
        if (vendorClients != null) {
            for (CpsApiVendorClient vendor : vendorClients) {
                String key = buildVendorKey(vendor.getVendorCode(), vendor.getPlatformCode());
                vendorClientMap.put(key, vendor);
                log.info("[CpsPlatformClientFactory] 注册供应商客户端: {}:{}", vendor.getVendorCode(), vendor.getPlatformCode());
            }
        }
        log.info("[CpsPlatformClientFactory] 共注册 {} 个供应商客户端", vendorClientMap.size());
    }

    // ==================== 平台维度（面向业务层，保持不变） ====================

    /**
     * 根据平台编码获取平台客户端适配器
     *
     * @param platformCode 平台编码
     * @return 平台客户端，若不存在则返回 null
     */
    public CpsPlatformClient getClient(String platformCode) {
        CpsPlatformClient client = clientMap.get(platformCode);
        if (client == null) {
            log.warn("[CpsPlatformClientFactory] 未找到平台适配器: {}", platformCode);
        }
        return client;
    }

    /**
     * 根据平台编码获取平台客户端适配器（必须存在，否则抛出异常）
     *
     * @param platformCode 平台编码
     * @return 平台客户端
     */
    public CpsPlatformClient getRequiredClient(String platformCode) {
        CpsPlatformClient client = clientMap.get(platformCode);
        if (client == null) {
            throw new IllegalArgumentException("未找到平台适配器: " + platformCode);
        }
        return client;
    }

    /**
     * 获取所有已注册的平台编码列表
     *
     * @return 平台编码列表
     */
    public Set<String> getRegisteredPlatformCodes() {
        return clientMap.keySet();
    }

    /**
     * 获取已启用的平台客户端列表（从数据库过滤状态）
     *
     * @return 已启用平台的客户端列表
     */
    public List<CpsPlatformClient> getEnabledClients() {
        List<CpsPlatformDO> enabledPlatforms = platformService.getEnabledPlatformList();
        return enabledPlatforms.stream()
                .filter(p -> clientMap.containsKey(p.getPlatformCode()))
                .map(p -> clientMap.get(p.getPlatformCode()))
                .toList();
    }

    // ==================== 供应商维度（新增，面向底层实现） ====================

    /**
     * 根据供应商编码和平台编码获取供应商客户端
     *
     * @param vendorCode   供应商编码，如 "dataoke"
     * @param platformCode 平台编码，如 "taobao"
     * @return 供应商客户端，若不存在则返回 null
     */
    public CpsApiVendorClient getVendorClient(String vendorCode, String platformCode) {
        String key = buildVendorKey(vendorCode, platformCode);
        CpsApiVendorClient client = vendorClientMap.get(key);
        if (client == null) {
            log.warn("[CpsPlatformClientFactory] 未找到供应商客户端: {}:{}", vendorCode, platformCode);
        }
        return client;
    }

    /**
     * 获取平台当前激活的供应商客户端
     *
     * <p>读取 cps_platform 表的 active_vendor_code 字段，找到对应的 VendorClient</p>
     *
     * @param platformCode 平台编码
     * @return 当前激活的供应商客户端，若不存在则返回 null
     */
    public CpsApiVendorClient getActiveVendorClient(String platformCode) {
        CpsPlatformDO platform = platformService.getPlatformByCode(platformCode);
        if (platform == null) {
            log.warn("[CpsPlatformClientFactory] 未找到平台配置: {}", platformCode);
            return null;
        }
        String vendorCode = platform.getActiveVendorCode();
        if (vendorCode == null || vendorCode.isBlank()) {
            vendorCode = "dataoke"; // 默认使用大淘客
        }
        return getVendorClient(vendorCode, platformCode);
    }

    /**
     * 获取平台当前激活的供应商配置
     *
     * @param platformCode 平台编码
     * @return 供应商运行时配置
     */
    public CpsVendorConfig getActiveVendorConfig(String platformCode) {
        CpsPlatformDO platform = platformService.getPlatformByCode(platformCode);
        if (platform == null) {
            return null;
        }
        String vendorCode = platform.getActiveVendorCode();
        if (vendorCode == null || vendorCode.isBlank()) {
            vendorCode = "dataoke";
        }
        return vendorService.getVendorConfig(vendorCode, platformCode);
    }

    // ==================== 内部方法 ====================

    private String buildVendorKey(String vendorCode, String platformCode) {
        return vendorCode + ":" + platformCode;
    }

}
