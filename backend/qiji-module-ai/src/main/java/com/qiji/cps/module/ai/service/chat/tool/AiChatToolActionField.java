package com.qiji.cps.module.ai.service.chat.tool;

import lombok.Data;

import java.util.List;

@Data
public class AiChatToolActionField {

    private String name;
    private String label;
    private String type;
    private Boolean required;
    private String placeholder;
    private String defaultValue;
    private List<AiChatToolActionOption> options;

}
