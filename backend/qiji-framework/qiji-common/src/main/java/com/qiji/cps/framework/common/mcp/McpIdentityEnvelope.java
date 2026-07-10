package com.qiji.cps.framework.common.mcp;

/**
 * Transport-safe signed MCP identity payload.
 */
public record McpIdentityEnvelope(String payload, String signature) {
}
