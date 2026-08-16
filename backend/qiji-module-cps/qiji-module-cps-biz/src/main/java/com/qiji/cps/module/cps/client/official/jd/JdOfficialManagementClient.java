package com.qiji.cps.module.cps.client.official.jd;

import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;

import java.util.List;

/** 京东联盟远端推广位、PID 与渠道关系管理能力。 */
public interface JdOfficialManagementClient {

    List<Position> createPositions(CreatePositionCommand command, CpsVendorConfig config);

    PositionPage queryPositions(QueryPositionCommand command, CpsVendorConfig config);

    String getPid(PidCommand command, CpsVendorConfig config);

    Long createChannelRelation(ChannelRelationCommand command, CpsVendorConfig config);

    record CreatePositionCommand(long unionId, String key, int unionType, int type,
                                 long siteId, List<String> names) {
    }

    record QueryPositionCommand(long unionId, String key, int unionType, int pageNo, int pageSize) {
    }

    record PidCommand(Long unionId, Long childUnionId, Integer promotionType,
                      String positionName, String mediaName) {
    }

    record ChannelRelationCommand(String inviteCode, String note, String channelNote) {
    }

    record Position(String positionId, Long siteId, String name, Integer type, String pid) {
    }

    record PositionPage(List<Position> items, long total, int pageNo, int pageSize) {
    }
}
