package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetMigrationCheckArchiveDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeConfigMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckArchiveMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetPolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CpsRebateAssetPolicyServiceImpl implements CpsRebateAssetPolicyService {

    private static final long DEFAULT_LARGE_DEBT_THRESHOLD_CENT = 10_000L;
    private static final int DEFAULT_REMINDER_INTERVAL_DAYS = 7;
    private static final int DEFAULT_NORMAL_REMINDER_DAYS = 30;
    private static final int DEFAULT_LARGE_REMINDER_DAYS = 180;
    private static final int DEFAULT_SMS_INTERVAL_DAYS = 30;

    private final CpsRebateAssetPolicyMapper policyMapper;
    private final CpsFreezeConfigMapper freezeConfigMapper;
    private final CpsRebateAssetMigrationCheckService migrationCheckService;
    private final CpsRebateAssetMigrationCheckArchiveMapper migrationCheckArchiveMapper;

    @Override
    public CpsRebateAssetPolicyDO getPolicy() {
        CpsRebateAssetPolicyDO policy = policyMapper.selectCurrentTenant();
        return policy == null ? defaultPolicy() : normalize(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public CpsRebateAssetPolicyDO initializePolicy() {
        CpsRebateAssetPolicyDO policy = policyMapper.selectCurrentTenant();
        if (policy == null) {
            policy = defaultPolicy();
            policyMapper.insert(policy);
        } else {
            normalize(policy);
        }
        ensureTenantDefaultFreezeRule();
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public CpsRebateAssetPolicyDO confirmMigrationReady(String approvalRef) {
        String normalizedApprovalRef = approvalRef == null ? "" : approvalRef.trim();
        if (normalizedApprovalRef.isEmpty()) {
            throw new IllegalArgumentException("发布B变更单号不能为空");
        }
        CpsRebateAssetPolicyDO policy = policyMapper.selectCurrentTenant();
        if (policy == null) {
            throw new IllegalStateException("请先初始化租户资产策略");
        }
        normalize(policy);
        if (Boolean.TRUE.equals(policy.getV2Enabled())) {
            return policy;
        }
        CpsRebateAssetMigrationCheckArchiveDO latest =
                migrationCheckArchiveMapper.selectLatestByTenantId(TenantContextHolder.getRequiredTenantId());
        if (latest == null || !latest.isReady()) {
            throw new IllegalStateException("最近一次资产预检未通过，不能确认发布B核验");
        }
        policy.setMigrationReady(true);
        policy.setLatestReadyCheckBatchNo(latest.getBatchNo());
        policy.setReadyCheckTime(latest.getExecutedAt());
        policy.setMigrationApprovalRef(normalizedApprovalRef);
        policyMapper.updateById(policy);
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void savePolicy(CpsRebateAssetPolicyDO policy) {
        normalize(policy);
        CpsRebateAssetPolicyDO existing = policyMapper.selectCurrentTenant();
        if (existing == null) {
            if (Boolean.TRUE.equals(policy.getV2Enabled())) {
                throw new IllegalStateException("返利资产启用前必须先创建策略，并由发布B完成迁移核验");
            }
            policy.setId(null);
            policy.setMigrationReady(false);
            policyMapper.insert(policy);
            ensureTenantDefaultFreezeRule();
            return;
        }
        if (Boolean.TRUE.equals(existing.getV2Enabled()) && !Boolean.TRUE.equals(policy.getV2Enabled())) {
            throw new IllegalStateException("返利资产产生写入后禁止回退旧逻辑，请使用只读熔断并向前修复");
        }
        if (!Boolean.TRUE.equals(existing.getV2Enabled()) && Boolean.TRUE.equals(policy.getV2Enabled())
                && !Boolean.TRUE.equals(existing.getMigrationReady())) {
            throw new IllegalStateException("发布B唯一键、期初资产流水和冻结对账尚未核验，禁止启用返利资产");
        }
        policy.setId(existing.getId());
        policy.setMigrationReady(existing.getMigrationReady());
        policy.setLatestReadyCheckBatchNo(existing.getLatestReadyCheckBatchNo());
        policy.setReadyCheckTime(existing.getReadyCheckTime());
        policy.setMigrationApprovalRef(existing.getMigrationApprovalRef());
        if (!Boolean.TRUE.equals(existing.getV2Enabled()) && Boolean.TRUE.equals(policy.getV2Enabled())) {
            validateReleaseBApproval(existing);
            CpsRebateAssetMigrationCheckReport report =
                    migrationCheckService.runCheck("SYSTEM:ASSET_POLICY_ENABLE");
            if (!report.isReady()) {
                throw new IllegalStateException("启用事务内资产预检发现新差异，禁止启用返利资产");
            }
            policy.setLatestReadyCheckBatchNo(report.getBatchNo());
            policy.setReadyCheckTime(report.getExecutedAt());
            ensureTenantDefaultFreezeRule();
        }
        policyMapper.updateById(policy);
    }

    @Override
    public void assertWritable() {
        CpsRebateAssetPolicyDO policy = getPolicy();
        if (!Boolean.TRUE.equals(policy.getV2Enabled())) {
            throw new IllegalStateException("当前租户尚未启用返利资产写入");
        }
        if (Boolean.TRUE.equals(policy.getReadOnly())) {
            throw new IllegalStateException("当前租户返利资产已切换为只读模式");
        }
    }

    private CpsRebateAssetPolicyDO defaultPolicy() {
        return CpsRebateAssetPolicyDO.builder().v2Enabled(false).migrationReady(false).readOnly(false)
                .largeDebtThresholdCent(DEFAULT_LARGE_DEBT_THRESHOLD_CENT)
                .reminderIntervalDays(DEFAULT_REMINDER_INTERVAL_DAYS)
                .normalReminderDays(DEFAULT_NORMAL_REMINDER_DAYS)
                .largeReminderDays(DEFAULT_LARGE_REMINDER_DAYS)
                .smsIntervalDays(DEFAULT_SMS_INTERVAL_DAYS).build();
    }

    private CpsRebateAssetPolicyDO normalize(CpsRebateAssetPolicyDO policy) {
        if (policy.getV2Enabled() == null) policy.setV2Enabled(false);
        if (policy.getMigrationReady() == null) policy.setMigrationReady(false);
        if (policy.getReadOnly() == null) policy.setReadOnly(false);
        if (policy.getLargeDebtThresholdCent() == null) {
            policy.setLargeDebtThresholdCent(DEFAULT_LARGE_DEBT_THRESHOLD_CENT);
        }
        if (policy.getReminderIntervalDays() == null) policy.setReminderIntervalDays(DEFAULT_REMINDER_INTERVAL_DAYS);
        if (policy.getNormalReminderDays() == null) policy.setNormalReminderDays(DEFAULT_NORMAL_REMINDER_DAYS);
        if (policy.getLargeReminderDays() == null) policy.setLargeReminderDays(DEFAULT_LARGE_REMINDER_DAYS);
        if (policy.getSmsIntervalDays() == null) policy.setSmsIntervalDays(DEFAULT_SMS_INTERVAL_DAYS);
        if (policy.getLargeDebtThresholdCent() <= 0L || policy.getReminderIntervalDays() <= 0
                || policy.getNormalReminderDays() <= 0 || policy.getLargeReminderDays() <= 0
                || policy.getSmsIntervalDays() <= 0) {
            throw new IllegalArgumentException("资产策略金额阈值和提醒周期必须大于0");
        }
        return policy;
    }

    private void ensureTenantDefaultFreezeRule() {
        var enabledRules = freezeConfigMapper.selectEnabledRules();
        var legacyFallback = enabledRules == null ? null : enabledRules.stream()
                .filter(rule -> rule.getPlatformCode() == null
                        && (rule.getMinAmountCent() == null || rule.getMinAmountCent() == 0L)
                        && rule.getMaxAmountCent() == null)
                .findFirst().orElse(null);
        if (legacyFallback != null) {
            if (Objects.equals(legacyFallback.getUnfreezeDays(), 15)
                    && legacyFallback.getRemark() != null
                    && legacyFallback.getRemark().contains("默认配置")) {
                CpsFreezeConfigDO update = CpsFreezeConfigDO.builder().id(legacyFallback.getId())
                        .minAmountCent(1000L).unfreezeDays(7)
                        .remark("当前租户全平台返利满10元冻结，资格时间后7天解冻").build();
                freezeConfigMapper.updateById(update);
            }
            return;
        }
        boolean hasFallback = enabledRules != null && enabledRules.stream()
                .anyMatch(rule -> rule.getPlatformCode() == null
                        && Objects.equals(rule.getMinAmountCent(), 1000L)
                        && rule.getMaxAmountCent() == null);
        if (hasFallback) {
            return;
        }
        freezeConfigMapper.insert(CpsFreezeConfigDO.builder()
                .platformCode(null).minAmountCent(1000L).maxAmountCent(null)
                .unfreezeDays(7).status(1)
                .remark("当前租户全平台返利满10元冻结，资格时间后7天解冻")
                .build());
    }

    private void validateReleaseBApproval(CpsRebateAssetPolicyDO existing) {
        long tenantId = TenantContextHolder.getRequiredTenantId();
        CpsRebateAssetMigrationCheckArchiveDO latest =
                migrationCheckArchiveMapper.selectLatestByTenantId(tenantId);
        if (latest == null || !latest.isReady()
                || !Objects.equals(existing.getLatestReadyCheckBatchNo(), latest.getBatchNo())
                || !Objects.equals(existing.getReadyCheckTime(), latest.getExecutedAt())) {
            throw new IllegalStateException("migration_ready 未绑定当前租户最新通过的预检批次，禁止启用返利资产");
        }
    }
}
