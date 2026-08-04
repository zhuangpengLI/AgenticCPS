package com.qiji.cps.module.ai.service.chat.tool;

import java.util.List;

/**
 * Business modules contribute their chat actions through this SPI.
 */
@FunctionalInterface
public interface AiChatToolActionProvider {

    List<AiChatToolAction> getToolActions();

}
