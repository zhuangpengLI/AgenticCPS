package com.qiji.cps.module.cps.mcp.security;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Complete CPS/CPX MCP tool risk registry. Unknown self-test tools are intentionally not registered.
 */
@Component
public class CpsMcpToolRiskRegistry {

    private static final Map<String, CpsMcpToolRisk> RISKS;

    static {
        Map<String, CpsMcpToolRisk> risks = new LinkedHashMap<>();
        registerReadOnly(risks,
                "cps_search_goods", "cps_compare_prices", "cps_query_orders", "cps_get_rebate_summary",
                "cps_recommend_by_scene", "cps_purchase_decision", "cps_list_selection_themes",
                "cps_get_rebate_balance", "cps_query_exchange_status", "cpx_list_tasks", "cpx_get_task_detail",
                "cpx_query_conversions", "cpx_recommend_tasks_by_scene", "cpx_search_articles");
        register(risks, CpsMcpToolRisk.ATTRIBUTION_WRITE,
                "cps_generate_link", "cps_recommend_from_selection_theme", "cpx_generate_tracking_link");
        register(risks, CpsMcpToolRisk.ASSET_WRITE, "cps_create_token_exchange");
        RISKS = Collections.unmodifiableMap(risks);
    }

    public CpsMcpToolRisk getRisk(String toolName) {
        return RISKS.get(toolName);
    }

    public Set<String> getRegisteredTools() {
        return RISKS.keySet();
    }

    private static void registerReadOnly(Map<String, CpsMcpToolRisk> risks, String... toolNames) {
        register(risks, CpsMcpToolRisk.READ_ONLY, toolNames);
    }

    private static void register(Map<String, CpsMcpToolRisk> risks, CpsMcpToolRisk risk, String... toolNames) {
        for (String toolName : toolNames) {
            risks.put(toolName, risk);
        }
    }

}
