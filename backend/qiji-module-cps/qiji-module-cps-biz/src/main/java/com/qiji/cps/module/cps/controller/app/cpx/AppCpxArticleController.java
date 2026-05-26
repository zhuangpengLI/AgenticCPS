package com.qiji.cps.module.cps.controller.app.cpx;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxArticleDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - CPX 资讯")
@RestController
@RequestMapping("/cpx/article")
@Validated
public class AppCpxArticleController {

    @Resource
    private CpxTaskService taskService;

    @GetMapping("/list")
    @Operation(summary = "查询 CPX 资讯、教程和任务攻略")
    public CommonResult<List<CpxArticleDO>> listArticles(@RequestParam(value = "keyword", required = false) String keyword,
                                                        @RequestParam(value = "category", required = false) String category,
                                                        @RequestParam(value = "promotionMethod", required = false) String promotionMethod,
                                                        @RequestParam(value = "limit", required = false) Integer limit) {
        return success(taskService.searchArticles(keyword, category, promotionMethod, limit));
    }
}
