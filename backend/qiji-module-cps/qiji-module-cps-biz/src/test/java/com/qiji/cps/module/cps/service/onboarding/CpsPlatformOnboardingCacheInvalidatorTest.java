package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.module.cps.config.CpsCacheConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CpsPlatformOnboardingCacheInvalidatorTest {

    private Cache platformCache;
    private Cache vendorCache;
    private Cache rebateCache;
    private CpsPlatformOnboardingCacheInvalidator invalidator;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = mock(CacheManager.class);
        platformCache = mock(Cache.class);
        vendorCache = mock(Cache.class);
        rebateCache = mock(Cache.class);
        when(cacheManager.getCache(CpsCacheConfig.CACHE_PLATFORM)).thenReturn(platformCache);
        when(cacheManager.getCache(CpsCacheConfig.CACHE_API_VENDOR)).thenReturn(vendorCache);
        when(cacheManager.getCache(CpsCacheConfig.CACHE_REBATE_CONFIG)).thenReturn(rebateCache);
        invalidator = new CpsPlatformOnboardingCacheInvalidator(cacheManager);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictAfterCommit_insideTransaction_shouldNotEvictBeforeCommit() {
        TransactionSynchronizationManager.initSynchronization();

        invalidator.evictAfterCommit("taobao");

        verify(platformCache, never()).evict("taobao");
        verify(vendorCache, never()).clear();
        verify(rebateCache, never()).clear();
    }

    @Test
    void evictAfterCommit_afterSuccessfulCommit_shouldEvictPlatformVendorAndRebateCaches() {
        TransactionSynchronizationManager.initSynchronization();
        invalidator.evictAfterCommit("taobao");

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(platformCache).evict("taobao");
        verify(vendorCache).clear();
        verify(rebateCache).clear();
    }

    @Test
    void evictAfterCommit_afterRollback_shouldNotEvict() {
        TransactionSynchronizationManager.initSynchronization();
        invalidator.evictAfterCommit("taobao");

        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(platformCache, never()).evict("taobao");
        verify(vendorCache, never()).clear();
        verify(rebateCache, never()).clear();
    }

}
