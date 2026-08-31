package com.qiji.cps.module.cps.dal.dataobject.refund;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

@TableName("cps_refund_report_import")
@KeySequence("cps_refund_report_import_seq")
@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
@Builder @NoArgsConstructor @AllArgsConstructor
public class CpsRefundReportImportDO extends TenantBaseDO {
    @TableId private Long id;
    private String batchNo;
    private String source;
    private String fileName;
    private String fileHash;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String status;
    private Integer totalRows;
    private Integer matchedRows;
    private Integer diffRows;
    private String failureReason;
}
