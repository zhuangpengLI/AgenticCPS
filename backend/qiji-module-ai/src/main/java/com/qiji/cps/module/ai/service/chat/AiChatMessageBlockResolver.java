package com.qiji.cps.module.ai.service.chat;

import java.util.List;
import java.util.Map;

/**
 * Converts an allow-listed tool response into UI blocks without coupling the
 * generic AI module to a business module.
 */
public interface AiChatMessageBlockResolver {

    boolean supports(String toolName);

    List<Map<String, Object>> resolve(String toolName, String resultPayload);
}
