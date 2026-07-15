package com.qiji.cps.module.cps.controller.app.withdraw.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppCpsWithdrawRespVO {
    private Long id;
    private String withdrawNo;
    private String withdrawType;
    private Long amountCent;
    private String status;
    private String transferStatus;
    private String reviewNote;
    private LocalDateTime transferTime;
    private LocalDateTime createTime;
}
