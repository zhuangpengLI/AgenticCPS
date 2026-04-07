package cn.iocoder.yudao.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * CPS 商品搜索结果 DTO（分页结果）
 *
 * @author CPS System
 */
@Data
@Builder
public class CpsGoodsSearchResult {

    /**
     * 商品列表
     */
    private List<CpsGoodsItem> list;

    /**
     * 总数量（部分平台不支持，返回-1）
     */
    private Long total;

    /**
     * 下一页标识（部分平台使用游标翻页）
     */
    private String nextPageId;

    /**
     * 当前页码
     */
    private Integer pageNo;

    /**
     * 每页大小
     */
    private Integer pageSize;

}
