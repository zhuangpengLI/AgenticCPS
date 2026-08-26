package com.qiji.cps.module.cps.service.selection;

/**
 * CPS 选品库固定状态与来源常量。
 */
public final class CpsSelectionConstants {

    private CpsSelectionConstants() {
    }

    public static final class ThemeStatus {
        public static final String DRAFT = "DRAFT";
        public static final String PUBLISHED = "PUBLISHED";
        public static final String OFFLINE = "OFFLINE";

        private ThemeStatus() {
        }
    }

    public static final class ThemeType {
        public static final String AI_SAVED_FILTER = "AI_SAVED_FILTER";

        private ThemeType() {
        }
    }

    public static final class ItemStatus {
        public static final String ENABLED = "ENABLED";
        public static final String DISABLED = "DISABLED";

        private ItemStatus() {
        }
    }

    public static final class SourceType {
        public static final String MANUAL = "MANUAL";
        public static final String AI_RECOMMEND = "AI_RECOMMEND";
        public static final String AUTO_REFRESH = "AUTO_REFRESH";
        public static final String VENDOR_PULL = "VENDOR_PULL";
        public static final String PROMOTION_TEMPLATE = "PROMOTION_TEMPLATE";

        private SourceType() {
        }
    }

    public static final class AiReviewStatus {
        public static final String CONFIRMED = "CONFIRMED";
        public static final String WITHDRAWN = "WITHDRAWN";

        private AiReviewStatus() {
        }
    }

    public static final class ImportTaskStatus {
        public static final String PROCESSING = "PROCESSING";
        public static final String SUCCESS = "SUCCESS";
        public static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
        public static final String FAILED = "FAILED";
        public static final String SKIPPED = "SKIPPED";

        private ImportTaskStatus() {
        }
    }
}
