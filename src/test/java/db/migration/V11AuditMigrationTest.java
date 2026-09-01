package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V11AuditMigrationTest {

    @Test
    void completesAfterAnEarlierAttemptAppliedOnlyTheRequestIdColumn() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v11_partial;DB_CLOSE_DELAY=-1")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE audit_event (
                            id BIGINT PRIMARY KEY,
                            user_id VARCHAR(36) NOT NULL,
                            request_id VARCHAR(64)
                        )
                        """);
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);

            assertThatCode(() -> new V11__extend_audit_event_context().migrate(context))
                    .doesNotThrowAnyException();

            DatabaseMetaData metadata = connection.getMetaData();
            assertThat(columnIsNullable(metadata, "AUDIT_EVENT", "USER_ID")).isTrue();
            assertThat(indexExists(metadata, "AUDIT_EVENT", "IDX_AUDIT_REQUEST_ID")).isTrue();
        }
    }

    private boolean columnIsNullable(
            DatabaseMetaData metadata, String tableName, String columnName
    ) throws Exception {
        try (ResultSet columns = metadata.getColumns(null, null, tableName, columnName)) {
            return columns.next()
                    && columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        }
    }

    private boolean indexExists(
            DatabaseMetaData metadata, String tableName, String indexName
    ) throws Exception {
        try (ResultSet indexes = metadata.getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
            return false;
        }
    }
}
