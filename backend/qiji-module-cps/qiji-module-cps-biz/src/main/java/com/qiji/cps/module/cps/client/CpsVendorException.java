package com.qiji.cps.module.cps.client;

/**
 * Unified upstream vendor failure used by order synchronization.
 */
public class CpsVendorException extends RuntimeException {

    private final String code;
    private final String vendorCode;
    private final String platformCode;
    private final CpsVendorCapability capability;
    private final String upstreamCode;
    private final String upstreamMessage;

    public CpsVendorException(String message) {
        super(message);
        this.code = "VENDOR_ERROR";
        this.vendorCode = null;
        this.platformCode = null;
        this.capability = null;
        this.upstreamCode = null;
        this.upstreamMessage = null;
    }

    public CpsVendorException(String message, Throwable cause) {
        super(message, cause);
        this.code = "VENDOR_ERROR";
        this.vendorCode = null;
        this.platformCode = null;
        this.capability = null;
        this.upstreamCode = null;
        this.upstreamMessage = null;
    }

    private CpsVendorException(String code, String vendorCode, String platformCode,
                               CpsVendorCapability capability, String message) {
        this(code, vendorCode, platformCode, capability, null, null, message);
    }

    private CpsVendorException(String code, String vendorCode, String platformCode,
                               CpsVendorCapability capability, String upstreamCode,
                               String upstreamMessage, String message) {
        super(message);
        this.code = code;
        this.vendorCode = vendorCode;
        this.platformCode = platformCode;
        this.capability = capability;
        this.upstreamCode = upstreamCode;
        this.upstreamMessage = upstreamMessage;
    }

    public static CpsVendorException unavailable(String platformCode) {
        return new CpsVendorException("CPS vendor unavailable for platform " + platformCode);
    }

    public static CpsVendorException capabilityUnsupported(String vendorCode, String platformCode,
                                                           CpsVendorCapability capability) {
        return new CpsVendorException("CAPABILITY_UNSUPPORTED", vendorCode, platformCode, capability,
                "CPS vendor capability unsupported [vendor=" + vendorCode
                        + ", platform=" + platformCode
                        + ", capability=" + capability + "]");
    }

    public static CpsVendorException upstreamRejected(String vendorCode, String platformCode,
                                                      CpsVendorCapability capability,
                                                      String upstreamCode, String upstreamMessage) {
        String message = upstreamMessage == null || upstreamMessage.isBlank()
                ? "上游供应商拒绝了请求" : upstreamMessage;
        return new CpsVendorException("UPSTREAM_REJECTED", vendorCode, platformCode, capability,
                upstreamCode, message, message);
    }

    public String getCode() {
        return code;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public CpsVendorCapability getCapability() {
        return capability;
    }

    public String getUpstreamCode() {
        return upstreamCode;
    }

    public String getUpstreamMessage() {
        return upstreamMessage;
    }
}
