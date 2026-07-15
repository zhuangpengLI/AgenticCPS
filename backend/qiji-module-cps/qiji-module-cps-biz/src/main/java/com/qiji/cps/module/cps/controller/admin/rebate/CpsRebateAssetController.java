package com.qiji.cps.module.cps.controller.admin.rebate;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateAssetLedgerPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateAssetPolicySaveReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtAdjustReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtPageReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtSummaryRespVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetMigrationCheckArchiveDO;
import com.qiji.cps.module.cps.service.rebate.asset.CpsAssetOperatorContext;
import com.qiji.cps.module.cps.service.rebate.asset.CpsDebtAdjustAction;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetQueryService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetPolicyService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetMigrationService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetMigrationCheckReport;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetMigrationCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CPS 返利资产")
@RestController
@RequestMapping("/cps/rebate-asset")
@Validated
public class CpsRebateAssetController {

    @Resource
    private CpsRebateAssetQueryService queryService;
    @Resource
    private CpsRebateAssetService assetService;
    @Resource
    private CpsRebateAssetPolicyService policyService;
    @Resource
    private CpsRebateAssetMigrationService migrationService;
    @Resource
    private CpsRebateAssetMigrationCheckService migrationCheckService;

    @GetMapping("/policy")
    @Operation(summary = "获取当前租户资产策略")
    @PreAuthorize("@ss.hasPermission('cps:rebate-asset-policy:query')")
    public CommonResult<CpsRebateAssetPolicyDO> getPolicy() {
        return success(policyService.getPolicy());
    }

    @PutMapping("/policy")
    @Operation(summary = "保存当前租户资产策略")
    @PreAuthorize("@ss.hasPermission('cps:rebate-asset-policy:update')")
    public CommonResult<Boolean> savePolicy(@Valid @RequestBody CpsRebateAssetPolicySaveReqVO reqVO) {
        policyService.savePolicy(CpsRebateAssetPolicyDO.builder()
                .v2Enabled(reqVO.getV2Enabled()).readOnly(reqVO.getReadOnly())
                .largeDebtThresholdCent(reqVO.getLargeDebtThresholdCent())
                .reminderIntervalDays(reqVO.getReminderIntervalDays())
                .normalReminderDays(reqVO.getNormalReminderDays())
                .largeReminderDays(reqVO.getLargeReminderDays())
                .smsIntervalDays(reqVO.getSmsIntervalDays()).build());
        return success(true);
    }

    @PostMapping("/migration/opening-balances")
    @Operation(summary = "为当前租户历史账户追加期初资产流水")
    @PreAuthorize("@ss.hasPermission('cps:rebate-asset-policy:update')")
    public CommonResult<Integer> backfillOpeningBalances() {
        return success(migrationService.backfillOpeningBalances(String.valueOf(getLoginUserId())));
    }

    @PostMapping("/migration/check")
    @Operation(summary = "执行当前租户资产V2迁移只读预检并归档")
    @PreAuthorize("@ss.hasPermission('cps:rebate-asset-policy:update')")
    public CommonResult<CpsRebateAssetMigrationCheckReport> runMigrationCheck() {
        return success(migrationCheckService.runCheck(String.valueOf(getLoginUserId())));
    }

    @GetMapping("/migration/check-archives")
    @Operation(summary = "查询当前租户资产V2迁移预检归档")
    @PreAuthorize("@ss.hasPermission('cps:rebate-asset-policy:query')")
    public CommonResult<List<CpsRebateAssetMigrationCheckArchiveDO>> getMigrationCheckArchives() {
        return success(migrationCheckService.getArchives());
    }

    @GetMapping("/debt/page")
    @Operation(summary = "获取欠款分页")
    @PreAuthorize("@ss.hasPermission('cps:rebate-debt:query')")
    public CommonResult<PageResult<CpsRebateDebtDO>> getDebtPage(@Valid CpsRebateDebtPageReqVO reqVO) {
        return success(queryService.getDebtPage(reqVO));
    }

    @GetMapping("/debt/get")
    @Operation(summary = "获取欠款详情")
    @PreAuthorize("@ss.hasPermission('cps:rebate-debt:query')")
    public CommonResult<CpsRebateDebtDO> getDebt(@RequestParam Long id) {
        return success(queryService.getDebt(id));
    }

    @GetMapping("/debt/member-summary")
    @Operation(summary = "获取会员欠款汇总")
    @PreAuthorize("@ss.hasPermission('cps:rebate-debt:query')")
    public CommonResult<CpsRebateDebtSummaryRespVO> getMemberDebtSummary(@RequestParam Long memberId) {
        return success(queryService.getDebtSummary(memberId));
    }

    @PostMapping("/debt/adjust")
    @Operation(summary = "人工调整欠款")
    @PreAuthorize("@ss.hasPermission('cps:rebate-debt:adjust')")
    public CommonResult<Boolean> adjustDebt(@Valid @RequestBody CpsRebateDebtAdjustReqVO reqVO) {
        assetService.manualAdjustDebt(reqVO.getMemberId(), CpsDebtAdjustAction.valueOf(reqVO.getAction()),
                reqVO.getAmountCent(), "ADMIN_DEBT_ADJUST:" + reqVO.getMemberId() + ":" + reqVO.getIdempotencyKey(),
                CpsAssetOperatorContext.admin(String.valueOf(getLoginUserId()),
                        reqVO.getIdempotencyKey(), reqVO.getReason()));
        return success(true);
    }

    @GetMapping("/ledger/page")
    @Operation(summary = "获取不可变资产流水分页")
    @PreAuthorize("@ss.hasPermission('cps:rebate-asset-ledger:query')")
    public CommonResult<PageResult<CpsRebateAssetLedgerDO>> getLedgerPage(
            @Valid CpsRebateAssetLedgerPageReqVO reqVO) {
        return success(queryService.getLedgerPage(reqVO));
    }
}
