package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V17__remove_redundant_performance_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        String database = connection.getMetaData().getDatabaseProductName();
        boolean h2 = "H2".equalsIgnoreCase(database);
        boolean mysql = database.toLowerCase().contains("mysql");
        if (!h2 && !mysql) {
            throw new FlywayException("Unsupported redundant index migration database: " + database);
        }

        dropIndexIfPresent(connection, h2, "assessment_attempt", "idx_assessment_attempt_idempotency");
        dropIndexIfPresent(connection, h2, "knowledge_chunk", "idx_knowledge_chunk_version");
    }

    private void dropIndexIfPresent(
            Connection connection, boolean h2, String expectedTable, String indexName
    ) throws SQLException {
        String tableName = findTableName(connection, expectedTable);
        if (!indexExists(connection, tableName, indexName)) {
            return;
        }

        String sql = h2
                ? "DROP INDEX " + indexName
                : "DROP INDEX " + indexName + " ON " + tableName;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTableName(Connection connection, String expectedTable) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String candidate = tables.getString("TABLE_NAME");
                if (expectedTable.equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        throw new FlywayException("Required " + expectedTable + " table does not exist");
    }

    private boolean indexExists(Connection connection, String tableName, String expectedIndex)
            throws SQLException {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(
                connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                if (expectedIndex.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
