package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.module.cps.config.CpsCacheConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CpsPlatformOnboardingCacheInvalidator {

    private final CacheManager cacheManager;

    public CpsPlatformOnboardingCacheInvalidator(
            @Qualifier("cpsCacheManager") CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAfterCommit(String platformCode) {
        afterCommit(() -> evict(platformCode));
    }

    public void evictPlatformAfterCommit(String platformCode) {
        afterCommit(() -> evictPlatform(platformCode));
    }

    public void evictVendorAfterCommit() {
        afterCommit(this::evictVendor);
    }

    public void evictRebateAfterCommit() {
        afterCommit(this::evictRebate);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
    }

    private void evict(String platformCode) {
        evictPlatform(platformCode);
        evictVendor();
        evictRebate();
    }

    private void evictPlatform(String platformCode) {
        Cache platformCache = cacheManager.getCache(CpsCacheConfig.CACHE_PLATFORM);
        if (platformCache != null && platformCode != null) {
            platformCache.evict(platformCode);
        }
    }

    private void evictVendor() {
        Cache vendorCache = cacheManager.getCache(CpsCacheConfig.CACHE_API_VENDOR);
        if (vendorCache != null) {
            // Vendor cache keys contain both vendor and platform. Clear the small configuration
            // cache instead of attempting to evict a platform-only key that can never match.
            vendorCache.clear();
        }
    }

    private void evictRebate() {
        Cache rebateCache = cacheManager.getCache(CpsCacheConfig.CACHE_REBATE_CONFIG);
        if (rebateCache != null) {
            rebateCache.clear();
        }
    }

}
