package cn.iocoder.yudao.module.cps.client;

import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.iocoder.yudao.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CPS 平台客户端工厂（策略模式注册中心）
 *
 * <p>基于 Spring Bean 自动注入机制，所有实现了 {@link CpsPlatformClient} 的 Bean 在启动时
 * 自动注册到工厂，业务代码通过平台编码动态获取对应的适配器实例。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class CpsPlatformClientFactory {

    /**
     * 平台编码 → 客户端适配器映射表
     */
    private final Map<String, CpsPlatformClient> clientMap = new ConcurrentHashMap<>();

    /**
     * 所有平台客户端适配器（Spring 自动注入所有实现 Bean）
     */
    @Resource
    private List<CpsPlatformClient> platformClients;

    @Resource
    private CpsPlatformService platformService;

    @PostConstruct
    public void init() {
        if (platformClients != null) {
            for (CpsPlatformClient client : platformClients) {
                clientMap.put(client.getPlatformCode(), client);
                log.info("[CpsPlatformClientFactory] 注册平台适配器: {}", client.getPlatformCode());
            }
        }
        log.info("[CpsPlatformClientFactory] 共注册 {} 个平台适配器", clientMap.size());
    }

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
    public java.util.Set<String> getRegisteredPlatformCodes() {
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

}
