package com.qiji.cps.module.cps.controller.admin.cpx.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CpxArticleSaveReqVO {

    private Long id;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String category;
    private String summary;
    private String coverUrl;
    private String content;
    private String platformCode;
    private String promotionMethod;
    private Long relatedTaskId;
    private String tags;
    private Integer status;
    private LocalDateTime publishTime;
}
