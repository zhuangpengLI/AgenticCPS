package com.qiji.cps.module.cps.service.order.status;

import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;

import java.util.Locale;

public final class CpsPlatformOrderStatusMapper {

    private CpsPlatformOrderStatusMapper() {
    }

    public static String mapStatus(CpsOrderDTO dto) {
        if (dto == null) {
            return CpsOrderStatusEnum.CREATED.getStatus();
        }
        if (Integer.valueOf(1).equals(dto.getRefundTag())) {
            return CpsOrderStatusEnum.REFUNDED.getStatus();
        }
        Integer platformStatus = dto.getPlatformStatus();
        if (platformStatus == null) {
            return CpsOrderStatusEnum.CREATED.getStatus();
        }
        return switch (normalizePlatform(dto.getPlatformCode())) {
            case "taobao" -> mapTaobaoStatus(platformStatus);
            case "jd", "jingdong" -> mapJdStatus(platformStatus);
            case "pdd", "pinduoduo" -> mapPddStatus(platformStatus);
            case "douyin" -> mapDouyinStatus(platformStatus);
            case "didi", "meituan", "local_life", "local-life" -> mapLocalLifeStatus(platformStatus);
            default -> CpsOrderStatusEnum.CREATED.getStatus();
        };
    }

    public static boolean isLegalMigration(String platformCode, String currentStatus, String incomingStatus) {
        if (incomingStatus == null || currentStatus == null || currentStatus.equals(incomingStatus)) {
            return true;
        }
        if (isReversalStatus(incomingStatus)) {
            return true;
        }
        return statusRank(incomingStatus) >= statusRank(currentStatus);
    }

    private static String mapTaobaoStatus(Integer status) {
        return switch (status) {
            case 0 -> CpsOrderStatusEnum.CREATED.getStatus();
            case 1, 12 -> CpsOrderStatusEnum.PAID.getStatus();
            case 2, 14 -> CpsOrderStatusEnum.RECEIVED.getStatus();
            case 3 -> CpsOrderStatusEnum.SETTLED.getStatus();
            case 4 -> CpsOrderStatusEnum.REBATE_RECEIVED.getStatus();
            case -1, 13 -> CpsOrderStatusEnum.INVALID.getStatus();
            default -> CpsOrderStatusEnum.CREATED.getStatus();
        };
    }

    private static String mapJdStatus(Integer status) {
        return switch (status) {
            case 0 -> CpsOrderStatusEnum.CREATED.getStatus();
            case 1, 16 -> CpsOrderStatusEnum.PAID.getStatus();
            case 2 -> CpsOrderStatusEnum.RECEIVED.getStatus();
            case 3, 17, 18 -> CpsOrderStatusEnum.SETTLED.getStatus();
            case 4 -> CpsOrderStatusEnum.REBATE_RECEIVED.getStatus();
            case -1 -> CpsOrderStatusEnum.INVALID.getStatus();
            default -> CpsOrderStatusEnum.CREATED.getStatus();
        };
    }

    private static String mapPddStatus(Integer status) {
        return switch (status) {
            case 0 -> CpsOrderStatusEnum.CREATED.getStatus();
            case 1 -> CpsOrderStatusEnum.PAID.getStatus();
            case 2 -> CpsOrderStatusEnum.RECEIVED.getStatus();
            case 3 -> CpsOrderStatusEnum.SETTLED.getStatus();
            case 4 -> CpsOrderStatusEnum.REBATE_RECEIVED.getStatus();
            case -1 -> CpsOrderStatusEnum.INVALID.getStatus();
            default -> CpsOrderStatusEnum.CREATED.getStatus();
        };
    }

    private static String mapDouyinStatus(Integer status) {
        return switch (status) {
            case 0 -> CpsOrderStatusEnum.CREATED.getStatus();
            case 1 -> CpsOrderStatusEnum.PAID.getStatus();
            case 2 -> CpsOrderStatusEnum.RECEIVED.getStatus();
            case 3 -> CpsOrderStatusEnum.SETTLED.getStatus();
            case 4 -> CpsOrderStatusEnum.REBATE_RECEIVED.getStatus();
            case -1 -> CpsOrderStatusEnum.INVALID.getStatus();
            default -> CpsOrderStatusEnum.CREATED.getStatus();
        };
    }

    private static String mapLocalLifeStatus(Integer status) {
        return switch (status) {
            case 0 -> CpsOrderStatusEnum.CREATED.getStatus();
            case 1 -> CpsOrderStatusEnum.PAID.getStatus();
            case 2 -> CpsOrderStatusEnum.RECEIVED.getStatus();
            case 3, 7 -> CpsOrderStatusEnum.SETTLED.getStatus();
            case 4 -> CpsOrderStatusEnum.REBATE_RECEIVED.getStatus();
            case -1, 6, 8 -> CpsOrderStatusEnum.INVALID.getStatus();
            default -> CpsOrderStatusEnum.CREATED.getStatus();
        };
    }

    private static String normalizePlatform(String platformCode) {
        return platformCode == null ? "" : platformCode.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isReversalStatus(String status) {
        return CpsOrderStatusEnum.REFUNDED.getStatus().equals(status)
                || CpsOrderStatusEnum.INVALID.getStatus().equals(status);
    }

    private static int statusRank(String status) {
        if (status == null) {
            return -1;
        }
        if (CpsOrderStatusEnum.CREATED.getStatus().equals(status)) {
            return 0;
        }
        if (CpsOrderStatusEnum.PAID.getStatus().equals(status)) {
            return 1;
        }
        if (CpsOrderStatusEnum.RECEIVED.getStatus().equals(status)) {
            return 2;
        }
        if (CpsOrderStatusEnum.SETTLED.getStatus().equals(status)) {
            return 3;
        }
        if (CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(status)) {
            return 4;
        }
        return -1;
    }
}
