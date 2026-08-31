package com.qiji.cps.module.cps.controller.admin.refund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "退款报表导入结果")
@Data
public class CpsRefundReportImportRespVO {
    private Long id;
    private String batchNo;
    private String source;
    private String fileName;
    private String fileHash;
    private String status;
    private Integer totalRows;
    private Integer matchedRows;
    private Integer diffRows;
    private String failureReason;
}
