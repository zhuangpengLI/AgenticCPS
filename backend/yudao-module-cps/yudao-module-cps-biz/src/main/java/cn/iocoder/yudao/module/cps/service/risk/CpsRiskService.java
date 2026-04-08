package cn.iocoder.yudao.module.cps.service.risk;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.cps.controller.admin.risk.vo.CpsRiskRulePageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.risk.vo.CpsRiskRuleSaveReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.risk.CpsRiskRuleDO;

/**
 * CPS 风控 Service 接口
 *
 * <p>基础版风控：Redis 计数器频率限制 + DB 黑名单检测。</p>
 *
 * @author CPS System
 */
public interface CpsRiskService {

    /**
     * 风控检查：会员是否允许转链
     *
     * <p>检查顺序：
     * <ol>
     *   <li>会员ID黑名单检查</li>
     *   <li>IP黑名单检查（clientIp 不为 null 时）</li>
     *   <li>Redis 计数器频率限制检查</li>
     * </ol>
     * 全部通过返回 true，任一拦截返回 false。
     * </p>
     *
     * @param memberId 会员ID
     * @param clientIp 客户端IP（可为 null，跳过IP黑名单检查）
     * @return true=允许转链，false=被风控拦截
     */
    boolean checkTransferAllowed(Long memberId, String clientIp);

    // ==================== 规则管理 ====================

    /**
     * 创建风控规则
     *
     * @param reqVO 规则信息
     * @return 规则ID
     */
    Long createRule(CpsRiskRuleSaveReqVO reqVO);

    /**
     * 更新风控规则
     *
     * @param reqVO 规则信息
     */
    void updateRule(CpsRiskRuleSaveReqVO reqVO);

    /**
     * 删除风控规则
     *
     * @param id 规则ID
     */
    void deleteRule(Long id);

    /**
     * 分页查询风控规则
     *
     * @param reqVO 分页请求
     * @return 分页结果
     */
    PageResult<CpsRiskRuleDO> getRulePage(CpsRiskRulePageReqVO reqVO);

}
