package cn.iocoder.yudao.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户 APP - 商品搜索结果 VO（分页包装）
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 商品搜索结果 VO")
@Data
@Builder
public class AppCpsGoodsSearchResult {

    @Schema(description = "商品列表")
    private List<AppCpsGoodsRespVO> list;

    @Schema(description = "总数量（部分平台不支持，返回-1）")
    private Long total;

    @Schema(description = "当前页码")
    private Integer pageNo;

    @Schema(description = "每页大小")
    private Integer pageSize;

    @Schema(description = "下一页标识（部分平台使用游标翻页）")
    private String nextPageId;

}
