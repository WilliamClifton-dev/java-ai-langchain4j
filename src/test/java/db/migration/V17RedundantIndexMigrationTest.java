package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V17RedundantIndexMigrationTest {

    @Test
    void removesIndexesAlreadyCoveredByExistingUniqueConstraints() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v17_redundant_indexes;MODE=MySQL;DB_CLOSE_DELAY=-1")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE assessment_attempt ("
                        + "id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, "
                        + "idempotency_key_hash CHAR(64) NOT NULL, "
                        + "CONSTRAINT uk_attempt_idempotency UNIQUE (user_id, idempotency_key_hash))");
                statement.execute("CREATE INDEX idx_assessment_attempt_idempotency "
                        + "ON assessment_attempt (user_id, idempotency_key_hash)");
                statement.execute("CREATE TABLE knowledge_chunk ("
                        + "id VARCHAR(36) PRIMARY KEY, version_id VARCHAR(36) NOT NULL, ordinal INT NOT NULL, "
                        + "CONSTRAINT uk_chunk_ordinal UNIQUE (version_id, ordinal))");
                statement.execute("CREATE INDEX idx_knowledge_chunk_version "
                        + "ON knowledge_chunk (version_id, ordinal)");
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);

            new V17__remove_redundant_performance_indexes().migrate(context);

            assertThat(indexExists(connection.getMetaData(), "ASSESSMENT_ATTEMPT",
                    "IDX_ASSESSMENT_ATTEMPT_IDEMPOTENCY")).isFalse();
            assertThat(indexExists(connection.getMetaData(), "KNOWLEDGE_CHUNK",
                    "IDX_KNOWLEDGE_CHUNK_VERSION")).isFalse();
        }
    }

    private boolean indexExists(DatabaseMetaData metadata, String tableName, String indexName)
            throws Exception {
        try (ResultSet indexes = metadata.getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
            return false;
        }
    }
}
