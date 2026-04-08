package cn.iocoder.yudao.module.cps.service.withdraw;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.cps.controller.admin.withdraw.vo.CpsWithdrawPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;

/**
 * CPS 提现申请 Service 接口
 *
 * @author CPS System
 */
public interface CpsWithdrawService {

    /**
     * 管理端分页查询提现申请
     *
     * @param reqVO 分页请求参数
     * @return 分页结果
     */
    PageResult<CpsWithdrawDO> getWithdrawPage(CpsWithdrawPageReqVO reqVO);

    /**
     * 获取提现申请详情
     *
     * @param id 提现ID
     * @return 提现申请
     */
    CpsWithdrawDO getWithdraw(Long id);

    /**
     * 审核通过提现申请
     *
     * @param id         提现ID
     * @param reviewNote 审核备注
     */
    void approveWithdraw(Long id, String reviewNote);

    /**
     * 驳回提现申请
     *
     * @param id         提现ID
     * @param reviewNote 驳回原因
     */
    void rejectWithdraw(Long id, String reviewNote);

}
