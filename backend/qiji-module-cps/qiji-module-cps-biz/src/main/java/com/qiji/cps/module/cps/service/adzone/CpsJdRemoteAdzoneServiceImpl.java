package com.qiji.cps.module.cps.service.adzone;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.official.jd.JdOfficialManagementClient;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdChannelRelationReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPidReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPositionCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPositionQueryReqVO;
import com.qiji.cps.module.cps.enums.CpsAdzoneTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.VENDOR_NOT_EXISTS;

/** 将京东远端推广位同步到本地统一推广位表，供转链和订单归因复用。 */
@Service
public class CpsJdRemoteAdzoneServiceImpl implements CpsJdRemoteAdzoneService {

    private static final String VENDOR_CODE = "official";
    private static final String PLATFORM_CODE = "jd";

    @Resource
    private CpsPlatformClientFactory platformClientFactory;
    @Resource
    private CpsAdzoneService adzoneService;

    @Override
    public List<JdOfficialManagementClient.Position> createAndSyncPositions(CpsJdPositionCreateReqVO request) {
        Context context = context();
        long unionId = resolveUnionId(request.getUnionId(), context.config());
        List<JdOfficialManagementClient.Position> positions = context.client().createPositions(
                new JdOfficialManagementClient.CreatePositionCommand(unionId, request.getKey(),
                        request.getUnionType(), request.getType(), request.getSiteId(), request.getNames()),
                context.config());
        positions.forEach(this::syncPosition);
        return positions;
    }

    @Override
    public JdOfficialManagementClient.PositionPage queryAndSyncPositions(CpsJdPositionQueryReqVO request) {
        Context context = context();
        long unionId = resolveUnionId(request.getUnionId(), context.config());
        JdOfficialManagementClient.PositionPage page = context.client().queryPositions(
                new JdOfficialManagementClient.QueryPositionCommand(unionId, request.getKey(),
                        request.getUnionType(), request.getPageNo(), request.getPageSize()), context.config());
        page.items().forEach(this::syncPosition);
        return page;
    }

    @Override
    public String getAndSyncPid(CpsJdPidReqVO request) {
        Context context = context();
        Long unionId = request.getUnionId() != null ? request.getUnionId()
                : resolveUnionId(null, context.config());
        String pid = context.client().getPid(new JdOfficialManagementClient.PidCommand(unionId,
                request.getChildUnionId(), request.getPromotionType(), request.getPositionName(),
                request.getMediaName()), context.config());
        if (StringUtils.hasText(pid)) {
            syncPosition(new JdOfficialManagementClient.Position(null, null,
                    request.getPositionName(), null, pid));
        }
        return pid;
    }

    @Override
    public Long createChannelRelation(CpsJdChannelRelationReqVO request) {
        Context context = context();
        return context.client().createChannelRelation(new JdOfficialManagementClient.ChannelRelationCommand(
                request.getInviteCode(), request.getNote(), request.getChannelNote()), context.config());
    }

    private Context context() {
        CpsApiVendorClient vendorClient = platformClientFactory.getVendorClient(VENDOR_CODE, PLATFORM_CODE);
        CpsVendorConfig config = platformClientFactory.getVendorConfig(VENDOR_CODE, PLATFORM_CODE);
        if (!(vendorClient instanceof JdOfficialManagementClient client) || config == null) {
            throw exception(VENDOR_NOT_EXISTS);
        }
        return new Context(client, config);
    }

    private long resolveUnionId(Long requestUnionId, CpsVendorConfig config) {
        if (requestUnionId != null) {
            return requestUnionId;
        }
        String configured = config.getExtraConfig() == null ? null : config.getExtraConfig().get("unionId");
        if (!StringUtils.hasText(configured)) {
            throw new IllegalArgumentException("京东 unionId 未配置");
        }
        try {
            return Long.parseLong(configured.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("京东 unionId 格式不正确");
        }
    }

    private void syncPosition(JdOfficialManagementClient.Position position) {
        String adzoneId = StringUtils.hasText(position.pid()) ? position.pid() : position.positionId();
        if (!StringUtils.hasText(adzoneId)) {
            return;
        }
        CpsAdzoneSaveReqVO save = new CpsAdzoneSaveReqVO();
        save.setPlatformCode(PLATFORM_CODE);
        save.setAdzoneId(adzoneId);
        save.setAdzoneName(StringUtils.hasText(position.name()) ? position.name() : "京东推广位");
        save.setAdzoneType(CpsAdzoneTypeEnum.GENERAL.getType());
        save.setIsDefault(0);
        save.setStatus(1);
        adzoneService.upsertAdzoneForOnboarding(save);
    }

    private record Context(JdOfficialManagementClient client, CpsVendorConfig config) {
    }
}
