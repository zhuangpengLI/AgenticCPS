package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.module.cps.client.official.jd.JdOfficialManagementClient;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdChannelRelationReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPidReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPositionCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPositionQueryReqVO;

import java.util.List;

/** 京东联盟远端推广位、PID 与渠道关系服务。 */
public interface CpsJdRemoteAdzoneService {

    List<JdOfficialManagementClient.Position> createAndSyncPositions(CpsJdPositionCreateReqVO request);

    JdOfficialManagementClient.PositionPage queryAndSyncPositions(CpsJdPositionQueryReqVO request);

    String getAndSyncPid(CpsJdPidReqVO request);

    Long createChannelRelation(CpsJdChannelRelationReqVO request);
}
