package com.qiji.cps.module.ai.dal.mysql.knowledge;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.ai.controller.admin.knowledge.vo.knowledge.AiKnowledgePageReqVO;
import com.qiji.cps.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI 知识库 Mapper
 *
 * @author xiaoxin
 */
@Mapper
public interface AiKnowledgeMapper extends BaseMapperX<AiKnowledgeDO> {

    default PageResult<AiKnowledgeDO> selectPage(AiKnowledgePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<AiKnowledgeDO>()
                .likeIfPresent(AiKnowledgeDO::getName, pageReqVO.getName())
                .eqIfPresent(AiKnowledgeDO::getStatus, pageReqVO.getStatus())
                .betweenIfPresent(AiKnowledgeDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(AiKnowledgeDO::getId));
    }

    default List<AiKnowledgeDO> selectListByStatus(Integer status) {
        return selectList(AiKnowledgeDO::getStatus, status);
    }

}
