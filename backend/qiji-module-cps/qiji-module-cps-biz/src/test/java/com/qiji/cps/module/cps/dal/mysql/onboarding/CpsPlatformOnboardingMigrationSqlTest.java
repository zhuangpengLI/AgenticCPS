package com.qiji.cps.module.cps.dal.mysql.onboarding;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsPlatformOnboardingMigrationSqlTest {

    @Test
    void adzoneDuplicateReconciliation_shouldPrecedeGeneratedUniqueConstraint() throws Exception {
        String sql = Files.readString(findUpdateSql(), StandardCharsets.UTF_8);

        int reconciliation = sql.indexOf("UPDATE `cps_adzone` AS `older`");
        int generatedColumn = sql.indexOf(
                "ALTER TABLE `cps_adzone` ADD COLUMN `active_unique_key`");
        int uniqueIndex = sql.indexOf(
                "ALTER TABLE `cps_adzone` ADD UNIQUE INDEX `uk_cps_adzone_active`");

        assertTrue(reconciliation >= 0, "migration must reconcile active adzone duplicates");
        assertTrue(sql.contains("`older`.`id` < `newer`.`id`"),
                "migration must preserve the newest/highest-id active adzone");
        assertTrue(sql.contains("SET `older`.`deleted` = b'1'"),
                "migration must soft-delete older active duplicates");
        assertTrue(sql.contains("WHERE `older`.`deleted` = b'0'")
                        && sql.contains("AND `newer`.`deleted` = b'0'"),
                "reconciliation must be safe to rerun against active rows only");
        assertTrue(sql.contains("`older`.`updater` = 'platform-onboarding-migration'")
                        && sql.contains("`older`.`update_time` = CURRENT_TIMESTAMP"),
                "soft-deleted duplicates must retain migration audit evidence");
        assertTrue(reconciliation < generatedColumn,
                "duplicate reconciliation must precede the generated active key");
        assertTrue(generatedColumn < uniqueIndex,
                "generated active key must precede its unique index");
    }

    private Path findUpdateSql() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path backendCandidate = current.resolve("sql/module/cps-update.sql");
            if (Files.exists(backendCandidate)) {
                return backendCandidate;
            }
            Path repoCandidate = current.resolve("backend/sql/module/cps-update.sql");
            if (Files.exists(repoCandidate)) {
                return repoCandidate;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate backend/sql/module/cps-update.sql");
    }

}
