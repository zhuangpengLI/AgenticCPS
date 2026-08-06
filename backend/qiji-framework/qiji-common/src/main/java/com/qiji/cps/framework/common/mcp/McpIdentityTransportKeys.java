package com.qiji.cps.framework.common.mcp;

/**
 * Shared keys for the signed identity carried by an AgenticCPS self-MCP call.
 */
public final class McpIdentityTransportKeys {

    public static final String META_IDENTITY_ENVELOPE = "CPS_MCP_IDENTITY_ENVELOPE";
    public static final String META_SELF_TEST_INVOCATION = "CPS_MCP_SELF_TEST_INVOCATION";

    /** Spring AI 1.1.x server adapters do not expose request _meta to ToolContext. */
    public static final String ARG_IDENTITY_PAYLOAD = "_cps_mcp_identity_payload";
    public static final String ARG_IDENTITY_SIGNATURE = "_cps_mcp_identity_signature";
    public static final String ARG_SELF_TEST_INVOCATION = "_cps_mcp_self_test";

    private McpIdentityTransportKeys() {
    }

}
