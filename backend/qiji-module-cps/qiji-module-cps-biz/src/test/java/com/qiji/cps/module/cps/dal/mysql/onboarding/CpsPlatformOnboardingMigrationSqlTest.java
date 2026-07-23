package com.qiji.cps.module.cps.dal.mysql.onboarding;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsPlatformOnboardingMigrationSqlTest {

    private static final String MYSQL_LENGTH_PREFIX = """
            CONCAT(CHAR_LENGTH(CAST(`tenant_id` AS CHAR)), ':', CAST(`tenant_id` AS CHAR), \
            CHAR_LENGTH(`platform_code`), ':', `platform_code`, \
            CHAR_LENGTH(`adzone_id`), ':', `adzone_id`)""";
    private static final String H2_LENGTH_PREFIX = """
            CONCAT(LENGTH(CAST("tenant_id" AS varchar)), ':', CAST("tenant_id" AS varchar), \
            LENGTH("platform_code"), ':', "platform_code", \
            LENGTH("adzone_id"), ':', "adzone_id")""";

    @Test
    void adzoneDuplicateReconciliation_shouldPrecedeGeneratedUniqueConstraint() throws Exception {
        String sql = readSql("cps-update.sql");

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

    @Test
    void adzoneActiveKey_shouldUseSafeInjectiveLengthPrefixAcrossSchemas() throws Exception {
        String baselineSql = compact(readSql("cps-all-in-one.sql"));
        String updateSql = compact(readSql("cps-update.sql")).replace("''", "'");
        String h2Sql = compact(readTestSchema());

        assertTrue(baselineSql.contains("`active_unique_key` varchar(191)"),
                "baseline adzone active key must stay within the MySQL 5.7 legacy index limit");
        assertTrue(updateSql.contains("`active_unique_key` varchar(191)"),
                "upgrade adzone active key must stay within the MySQL 5.7 legacy index limit");
        assertTrue(h2Sql.contains("\"active_unique_key\" varchar(191)"),
                "H2 schema must match the production key width");
        assertTrue(baselineSql.contains(MYSQL_LENGTH_PREFIX),
                "baseline must use an injective length-prefixed adzone key");
        assertTrue(updateSql.contains(MYSQL_LENGTH_PREFIX),
                "upgrade must use the same injective length-prefixed adzone key");
        assertTrue(h2Sql.contains(H2_LENGTH_PREFIX),
                "H2 must model the same tenant/platform/adzone prefix order");
    }

    private String compact(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private String readSql(String fileName) throws Exception {
        return Files.readString(findBackendSqlFile("sql/module/" + fileName), StandardCharsets.UTF_8);
    }

    private String readTestSchema() throws Exception {
        return Files.readString(findBackendSqlFile(
                "qiji-module-cps/qiji-module-cps-biz/src/test/resources/sql/create_tables.sql"),
                StandardCharsets.UTF_8);
    }

    private Path findBackendSqlFile(String backendRelativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path backendCandidate = current.resolve(backendRelativePath);
            if (Files.exists(backendCandidate)) {
                return backendCandidate;
            }
            Path repoCandidate = current.resolve("backend").resolve(backendRelativePath);
            if (Files.exists(repoCandidate)) {
                return repoCandidate;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate backend/" + backendRelativePath);
    }

}
