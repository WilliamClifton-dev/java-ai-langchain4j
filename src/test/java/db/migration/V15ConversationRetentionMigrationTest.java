package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V15ConversationRetentionMigrationTest {

    @Test
    void backfillsActivityAndEnforcesRetentionColumnForH2() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v15_conversation_retention;MODE=MySQL;DB_CLOSE_DELAY=-1")) {
            LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE coach_conversation ("
                        + "id VARCHAR(36) PRIMARY KEY, updated_at TIMESTAMP(6) NOT NULL, "
                        + "last_message_at TIMESTAMP(6))");
                statement.execute("INSERT INTO coach_conversation (id, updated_at) VALUES "
                        + "('conversation-1', TIMESTAMP '2026-01-02 03:04:05')");
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);

            new V15__add_conversation_retention().migrate(context);

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT last_message_at FROM coach_conversation WHERE id='conversation-1'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getTimestamp(1)).isEqualTo(Timestamp.valueOf(updatedAt));
            }
            assertThat(columnIsNullable(connection.getMetaData())).isFalse();
            assertThat(indexExists(connection.getMetaData())).isTrue();
        }
    }

    @Test
    void completesWhenColumnAndIndexAlreadyExistAfterPartialMigration() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v15_conversation_retention_partial;MODE=MySQL;DB_CLOSE_DELAY=-1")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE coach_conversation ("
                        + "id VARCHAR(36) PRIMARY KEY, updated_at TIMESTAMP(6) NOT NULL, "
                        + "last_message_at TIMESTAMP(6))");
                statement.execute("INSERT INTO coach_conversation (id, updated_at) VALUES "
                        + "('conversation-1', TIMESTAMP '2026-01-02 03:04:05')");
                statement.execute("CREATE INDEX idx_coach_conversation_retention "
                        + "ON coach_conversation (last_message_at)");
            }
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);

            new V15__add_conversation_retention().migrate(context);

            assertThat(columnIsNullable(connection.getMetaData())).isFalse();
            assertThat(indexExists(connection.getMetaData())).isTrue();
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT last_message_at FROM coach_conversation WHERE id='conversation-1'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getTimestamp(1))
                        .isEqualTo(Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4, 5)));
            }
        }
    }

    private boolean columnIsNullable(DatabaseMetaData metadata) throws Exception {
        try (ResultSet columns = metadata.getColumns(null, null, "COACH_CONVERSATION", "LAST_MESSAGE_AT")) {
            return columns.next() && columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        }
    }

    private boolean indexExists(DatabaseMetaData metadata) throws Exception {
        try (ResultSet indexes = metadata.getIndexInfo(null, null, "COACH_CONVERSATION", false, false)) {
            while (indexes.next()) {
                if ("IDX_COACH_CONVERSATION_RETENTION".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
