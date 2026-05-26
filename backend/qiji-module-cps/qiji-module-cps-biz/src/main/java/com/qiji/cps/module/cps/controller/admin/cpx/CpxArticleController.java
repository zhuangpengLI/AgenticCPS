package com.qiji.cps.module.cps.controller.admin.cpx;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxArticleSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxArticleDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPX 资讯中心")
@RestController
@RequestMapping("/cpx/article")
@Validated
public class CpxArticleController {

    @Resource
    private CpxTaskService taskService;

    @PostMapping("/create")
    @Operation(summary = "发布 CPX 资讯/教程/攻略")
    @PreAuthorize("@ss.hasPermission('cpx:article:create')")
    public CommonResult<Long> createArticle(@Valid @RequestBody CpxArticleSaveReqVO createReqVO) {
        return success(taskService.createArticle(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 CPX 资讯/教程/攻略")
    @PreAuthorize("@ss.hasPermission('cpx:article:update')")
    public CommonResult<Boolean> updateArticle(@Valid @RequestBody CpxArticleSaveReqVO updateReqVO) {
        taskService.updateArticle(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取 CPX 资讯详情")
    @PreAuthorize("@ss.hasPermission('cpx:article:query')")
    public CommonResult<CpxArticleDO> getArticle(@RequestParam("id") Long id) {
        return success(taskService.getArticle(id));
    }

    @GetMapping("/list")
    @Operation(summary = "查询 CPX 资讯")
    @PreAuthorize("@ss.hasPermission('cpx:article:query')")
    public CommonResult<List<CpxArticleDO>> listArticles(@RequestParam(value = "keyword", required = false) String keyword,
                                                        @RequestParam(value = "category", required = false) String category,
                                                        @RequestParam(value = "promotionMethod", required = false) String promotionMethod,
                                                        @RequestParam(value = "limit", required = false) Integer limit) {
        return success(taskService.listAdminArticles(keyword, category, promotionMethod, limit));
    }
}
