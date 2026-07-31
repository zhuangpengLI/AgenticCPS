package com.qiji.cps.module.cps.service.activity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsRebateActivitySchemaTest {

    private static final Pattern JUMP_URL_COLUMN = Pattern.compile("`jump_url`\\s+varchar\\((\\d+)\\)",
            Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("cps_rebate_activity.jump_url supports third-party landing URLs")
    void jumpUrlColumnSupportsThirdPartyLandingUrls() throws Exception {
        Path sqlFile = findBackendSqlFile();
        String sql = Files.readString(sqlFile, StandardCharsets.UTF_8);

        Matcher matcher = JUMP_URL_COLUMN.matcher(sql);
        assertTrue(matcher.find(), "cps_rebate_activity.jump_url column definition should exist");

        int length = Integer.parseInt(matcher.group(1));
        assertTrue(length >= 2048, "jump_url should support at least 2048 characters");
        assertTrue(sql.contains("`promotion_activity_id` varchar(128)"),
                "activity conversion ID must be stored separately from row identity");
        assertTrue(sql.contains("`vendor_metadata` text"),
                "vendor activity conversion metadata must survive synchronization");
        assertTrue(sql.contains("'taobao-88vip-youku', '热门', 'search', ''"),
                "the seeded 88VIP card must not use an empty URL target");
    }

    private Path findBackendSqlFile() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path backendCandidate = current.resolve("sql/module/cps-all-in-one.sql");
            if (Files.exists(backendCandidate)) {
                return backendCandidate;
            }
            Path repoCandidate = current.resolve("backend/sql/module/cps-all-in-one.sql");
            if (Files.exists(repoCandidate)) {
                return repoCandidate;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate backend/sql/module/cps-all-in-one.sql");
    }
}
