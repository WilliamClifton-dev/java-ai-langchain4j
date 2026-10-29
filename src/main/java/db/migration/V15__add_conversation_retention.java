package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V15__add_conversation_retention extends BaseJavaMigration {

    private static final String TABLE_NAME = "coach_conversation";
    private static final String COLUMN_NAME = "last_message_at";
    private static final String INDEX_NAME = "idx_coach_conversation_retention";

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        String database = connection.getMetaData().getDatabaseProductName();
        boolean h2 = "H2".equalsIgnoreCase(database);
        boolean mysql = database.toLowerCase().contains("mysql");
        if (!h2 && !mysql) {
            throw new FlywayException("Unsupported conversation retention migration database: " + database);
        }

        String tableName = findTableName(connection);
        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, tableName, COLUMN_NAME)) {
                statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_NAME + " TIMESTAMP(6)");
            }

            statement.execute("UPDATE " + tableName + " SET " + COLUMN_NAME + " = updated_at WHERE "
                    + COLUMN_NAME + " IS NULL");

            if (columnIsNullable(connection, tableName, COLUMN_NAME)) {
                String sql = h2
                        ? "ALTER TABLE " + tableName + " ALTER COLUMN " + COLUMN_NAME + " SET NOT NULL"
                        : "ALTER TABLE " + tableName + " MODIFY COLUMN " + COLUMN_NAME + " TIMESTAMP(6) NOT NULL";
                statement.execute(sql);
            }

            if (!indexExists(connection, tableName, INDEX_NAME)) {
                statement.execute("CREATE INDEX " + INDEX_NAME + " ON " + tableName + " (" + COLUMN_NAME + ")");
            }
        }
    }

    private String findTableName(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String candidate = tables.getString("TABLE_NAME");
                if (TABLE_NAME.equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        throw new FlywayException("Required " + TABLE_NAME + " table does not exist");
    }

    private boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), null, tableName, "%")) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnIsNullable(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), null, tableName, "%")) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                }
            }
        }
        throw new FlywayException("Required " + tableName + "." + columnName + " column does not exist");
    }

    private boolean indexExists(Connection connection, String tableName, String indexName)
            throws SQLException {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(
                connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
