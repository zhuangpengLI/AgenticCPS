package com.qiji.cps.module.cps.client.haina;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qiji.cps.haina")
public class HainaDecisionProperties {

    private boolean enabled = true;

    private String clientName = "haina";

    private String apiKey;

    private int maxResults = 10;
}
