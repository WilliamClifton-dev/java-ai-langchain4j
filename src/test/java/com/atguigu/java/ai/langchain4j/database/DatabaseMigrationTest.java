package com.atguigu.java.ai.langchain4j.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ActiveProfiles("test")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class DatabaseMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayCreatesOrderedConversationSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            assertThat(tableExists(metadata, "coach_conversation")).isTrue();
            assertThat(tableExists(metadata, "coach_message")).isTrue();
            assertThat(uniqueIndexExists(
                    metadata,
                    "coach_message",
                    Set.of("conversation_id", "sequence_no")
            )).isTrue();
            assertThat(importedKeyExists(metadata, "coach_message", "coach_conversation")).isTrue();
            assertThat(tableExists(metadata, "user_account")).isTrue();
            assertThat(tableExists(metadata, "refresh_token")).isTrue();
            assertThat(uniqueIndexExists(metadata, "user_account", Set.of("normalized_email"))).isTrue();
            assertThat(uniqueIndexExists(metadata, "refresh_token", Set.of("token_hash"))).isTrue();
            assertThat(importedKeyExists(metadata, "refresh_token", "user_account")).isTrue();
            assertThat(tableExists(metadata, "user_profile")).isTrue();
            assertThat(tableExists(metadata, "safety_screening")).isTrue();
            assertThat(uniqueIndexExists(metadata, "safety_screening", Set.of("user_id", "version"))).isTrue();
            assertThat(importedKeyExists(metadata, "user_profile", "user_account")).isTrue();
            assertThat(importedKeyExists(metadata, "safety_screening", "user_account")).isTrue();
            assertThat(tableExists(metadata, "assessment_definition")).isTrue();
            assertThat(tableExists(metadata, "assessment_dimension")).isTrue();
            assertThat(tableExists(metadata, "assessment_item")).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "assessment_definition", Set.of("assessment_key", "version")
            )).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "assessment_item", Set.of("definition_id", "ordinal")
            )).isTrue();
            assertThat(importedKeyExists(metadata, "assessment_dimension", "assessment_definition")).isTrue();
            assertThat(importedKeyExists(metadata, "assessment_item", "assessment_definition")).isTrue();
            assertThat(tableExists(metadata, "assessment_attempt")).isTrue();
            assertThat(tableExists(metadata, "assessment_answer")).isTrue();
            assertThat(tableExists(metadata, "assessment_score")).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "assessment_attempt", Set.of("user_id", "idempotency_key_hash")
            )).isTrue();
            assertThat(importedKeyExists(metadata, "assessment_attempt", "user_account")).isTrue();
            assertThat(importedKeyExists(metadata, "assessment_attempt", "assessment_definition")).isTrue();
            assertThat(importedKeyExists(metadata, "assessment_answer", "assessment_attempt")).isTrue();
            assertThat(importedKeyExists(metadata, "assessment_score", "assessment_attempt")).isTrue();
            assertThat(tableExists(metadata, "weight_plan")).isTrue();
            assertThat(tableExists(metadata, "weight_plan_version")).isTrue();
            assertThat(uniqueIndexExists(metadata, "weight_plan", Set.of("user_id"))).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "weight_plan_version", Set.of("plan_id", "version_no")
            )).isTrue();
            assertThat(uniqueIndexExists(
                    metadata,
                    "weight_plan_version",
                    Set.of("plan_id", "draft_idempotency_key_hash")
            )).isTrue();
            assertThat(uniqueIndexExists(
                    metadata,
                    "weight_plan_version",
                    Set.of("plan_id", "activation_idempotency_key_hash")
            )).isTrue();
            assertThat(importedKeyExists(metadata, "weight_plan", "user_account")).isTrue();
            assertThat(importedKeyExists(metadata, "weight_plan_version", "weight_plan")).isTrue();
            assertThat(importedKeyExists(metadata, "weight_plan_version", "safety_screening")).isTrue();
            assertThat(importedKeyExists(metadata, "weight_plan_version", "assessment_attempt")).isTrue();
            assertThat(tableExists(metadata, "daily_metric")).isTrue();
            assertThat(tableExists(metadata, "nutrition_log")).isTrue();
            assertThat(tableExists(metadata, "training_log")).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "daily_metric", Set.of("user_id", "local_date")
            )).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "nutrition_log", Set.of("user_id", "local_date")
            )).isTrue();
            assertThat(importedKeyExists(metadata, "daily_metric", "user_account")).isTrue();
            assertThat(importedKeyExists(metadata, "nutrition_log", "user_account")).isTrue();
            assertThat(importedKeyExists(metadata, "training_log", "user_account")).isTrue();
            assertThat(tableExists(metadata, "weekly_review")).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "weekly_review", Set.of("user_id", "window_end", "version_no")
            )).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "weekly_review", Set.of("user_id", "window_end", "input_hash")
            )).isTrue();
            assertThat(importedKeyExists(metadata, "weekly_review", "user_account")).isTrue();
            assertThat(importedKeyExists(metadata, "weekly_review", "weight_plan_version")).isTrue();
            assertThat(tableExists(metadata, "knowledge_document")).isTrue();
            assertThat(tableExists(metadata, "knowledge_document_version")).isTrue();
            assertThat(tableExists(metadata, "knowledge_chunk")).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "knowledge_document", Set.of("source_key")
            )).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "knowledge_document_version", Set.of("document_id", "version_no")
            )).isTrue();
            assertThat(uniqueIndexExists(
                    metadata, "knowledge_chunk", Set.of("version_id", "ordinal")
            )).isTrue();
            assertThat(importedKeyExists(
                    metadata, "knowledge_document_version", "knowledge_document"
            )).isTrue();
            assertThat(importedKeyExists(metadata, "knowledge_chunk", "knowledge_document_version"))
                    .isTrue();
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        return actualTableName(metadata, tableName) != null;
    }

    private boolean uniqueIndexExists(
            DatabaseMetaData metadata,
            String tableName,
            Set<String> expectedColumns
    ) throws SQLException {
        String actualTableName = actualTableName(metadata, tableName);
        Map<String, Set<String>> indexColumns = new HashMap<>();
        try (ResultSet indexes = metadata.getIndexInfo(null, null, actualTableName, true, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                String columnName = indexes.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    indexColumns.computeIfAbsent(indexName, ignored -> new HashSet<>())
                            .add(columnName.toLowerCase());
                }
            }
        }
        return indexColumns.values().stream().anyMatch(expectedColumns::equals);
    }

    private boolean importedKeyExists(
            DatabaseMetaData metadata,
            String tableName,
            String referencedTable
    ) throws SQLException {
        String actualTableName = actualTableName(metadata, tableName);
        try (ResultSet keys = metadata.getImportedKeys(null, null, actualTableName)) {
            while (keys.next()) {
                if (referencedTable.equalsIgnoreCase(keys.getString("PKTABLE_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private String actualTableName(DatabaseMetaData metadata, String expectedName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String actualName = tables.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(actualName)) {
                    return actualName;
                }
            }
            return null;
        }
    }
}
