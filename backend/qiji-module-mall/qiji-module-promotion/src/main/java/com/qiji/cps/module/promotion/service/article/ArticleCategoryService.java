package com.qiji.cps.module.promotion.service.article;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.promotion.controller.admin.article.vo.category.ArticleCategoryCreateReqVO;
import com.qiji.cps.module.promotion.controller.admin.article.vo.category.ArticleCategoryPageReqVO;
import com.qiji.cps.module.promotion.controller.admin.article.vo.category.ArticleCategoryUpdateReqVO;
import com.qiji.cps.module.promotion.dal.dataobject.article.ArticleCategoryDO;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 文章分类 Service 接口
 *
 * @author HUIHUI
 */
public interface ArticleCategoryService {

    /**
     * 创建文章分类
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createArticleCategory(@Valid ArticleCategoryCreateReqVO createReqVO);

    /**
     * 更新文章分类
     *
     * @param updateReqVO 更新信息
     */
    void updateArticleCategory(@Valid ArticleCategoryUpdateReqVO updateReqVO);

    /**
     * 删除文章分类
     *
     * @param id 编号
     */
    void deleteArticleCategory(Long id);

    /**
     * 获得文章分类
     *
     * @param id 编号
     * @return 文章分类
     */
    ArticleCategoryDO getArticleCategory(Long id);

    /**
     * 获得文章分类分页
     *
     * @param pageReqVO 分页查询
     * @return 文章分类分页
     */
    PageResult<ArticleCategoryDO> getArticleCategoryPage(ArticleCategoryPageReqVO pageReqVO);

    /**
     * 获得指定状态的文章分类列表
     *
     * @param status 状态
     * @return 文章分类列表
     */
    List<ArticleCategoryDO> getArticleCategoryListByStatus(Integer status);

}
