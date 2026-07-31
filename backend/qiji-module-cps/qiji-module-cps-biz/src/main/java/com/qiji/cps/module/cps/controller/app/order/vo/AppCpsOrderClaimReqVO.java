package com.qiji.cps.module.cps.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.qiji.cps.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "用户 APP - CPS 订单申领 Request VO")
@Data
public class AppCpsOrderClaimReqVO {

    @NotBlank(message = "平台编码不能为空")
    @Size(max = 32, message = "平台编码长度不能超过32个字符")
    private String platformCode;

    @NotBlank(message = "订单号不能为空")
    @Size(max = 128, message = "订单号长度不能超过128个字符")
    private String platformOrderId;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime orderTime;

    private BigDecimal payAmount;

    @Size(max = 512, message = "商品名称长度不能超过512个字符")
    private String itemTitle;

    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键长度不能超过128个字符")
    private String idempotencyKey;
}
