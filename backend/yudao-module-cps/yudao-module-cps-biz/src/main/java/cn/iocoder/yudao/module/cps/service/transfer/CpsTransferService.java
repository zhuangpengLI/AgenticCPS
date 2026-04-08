package cn.iocoder.yudao.module.cps.service.transfer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;

/**
 * CPS 转链记录 Service 接口
 *
 * @author CPS System
 */
public interface CpsTransferService {

    /**
     * 管理端分页查询转链记录
     *
     * @param reqVO 分页请求参数
     * @return 分页结果
     */
    PageResult<CpsTransferRecordDO> getTransferPage(CpsTransferRecordPageReqVO reqVO);

    /**
     * 统计指定会员当日转链次数（风控使用）
     *
     * @param memberId 会员ID
     * @return 当日转链次数
     */
    long countTodayByMember(Long memberId);

}
