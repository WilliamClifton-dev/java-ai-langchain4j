package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V11__extend_audit_event_context extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        String database = connection.getMetaData().getDatabaseProductName();
        boolean h2 = "H2".equalsIgnoreCase(database);
        boolean mysql = database.toLowerCase().contains("mysql");
        if (!h2 && !mysql) {
            throw new FlywayException("Unsupported audit migration database: " + database);
        }

        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "request_id")) {
                statement.execute("ALTER TABLE audit_event ADD COLUMN request_id VARCHAR(64)");
            }
            if (!columnIsNullable(connection, "user_id")) {
                String sql = h2
                        ? "ALTER TABLE audit_event ALTER COLUMN user_id DROP NOT NULL"
                        : "ALTER TABLE audit_event MODIFY COLUMN user_id VARCHAR(36) NULL";
                statement.execute(sql);
            }
            if (!indexExists(connection, "idx_audit_request_id")) {
                statement.execute("CREATE INDEX idx_audit_request_id ON audit_event (request_id)");
            }
        }
    }

    private boolean columnExists(Connection connection, String expectedColumn) throws SQLException {
        try (ResultSet columns = metadata(connection).getColumns(
                connection.getCatalog(), null, tableName(connection), "%")) {
            while (columns.next()) {
                if (expectedColumn.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
            return false;
        }
    }

    private boolean columnIsNullable(Connection connection, String expectedColumn)
            throws SQLException {
        try (ResultSet columns = metadata(connection).getColumns(
                connection.getCatalog(), null, tableName(connection), "%")) {
            while (columns.next()) {
                if (expectedColumn.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                }
            }
            return false;
        }
    }

    private boolean indexExists(Connection connection, String expectedIndex) throws SQLException {
        try (ResultSet indexes = metadata(connection).getIndexInfo(
                connection.getCatalog(), null, tableName(connection), false, false)) {
            while (indexes.next()) {
                if (expectedIndex.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
            return false;
        }
    }

    private String tableName(Connection connection) throws SQLException {
        try (ResultSet tables = metadata(connection).getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String candidate = tables.getString("TABLE_NAME");
                if ("audit_event".equalsIgnoreCase(candidate)) return candidate;
            }
        }
        throw new FlywayException("Required audit_event table does not exist");
    }

    private DatabaseMetaData metadata(Connection connection) throws SQLException {
        return connection.getMetaData();
    }
}
