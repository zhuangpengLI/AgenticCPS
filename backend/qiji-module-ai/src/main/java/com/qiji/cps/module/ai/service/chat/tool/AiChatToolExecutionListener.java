package com.qiji.cps.module.ai.service.chat.tool;

@FunctionalInterface
public interface AiChatToolExecutionListener {

    AiChatToolExecutionListener NOOP = event -> { };

    void onEvent(AiChatToolExecutionEvent event);

}
