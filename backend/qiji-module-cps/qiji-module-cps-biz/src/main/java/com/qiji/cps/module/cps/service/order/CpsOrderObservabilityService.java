package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailurePageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncFailureDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;

/** CPS 订单归因与同步状态只读查询服务。 */
public interface CpsOrderObservabilityService {

    PageResult<CpsOrderAttributionLogDO> getAttributionLogPage(CpsOrderAttributionLogPageReqVO reqVO);

    PageResult<CpsOrderSyncCheckpointDO> getSyncCheckpointPage(CpsOrderSyncCheckpointPageReqVO reqVO);

    PageResult<CpsOrderSyncFailureDO> getSyncFailurePage(CpsOrderSyncFailurePageReqVO reqVO);
}
