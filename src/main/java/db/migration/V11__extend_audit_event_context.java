package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V11__extend_audit_event_context extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        String database = connection.getMetaData().getDatabaseProductName();
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE audit_event ADD COLUMN request_id VARCHAR(64)");
            if ("H2".equalsIgnoreCase(database)) {
                statement.execute("ALTER TABLE audit_event ALTER COLUMN user_id DROP NOT NULL");
            } else if (database.toLowerCase().contains("mysql")) {
                statement.execute("ALTER TABLE audit_event MODIFY COLUMN user_id VARCHAR(36) NULL");
            } else {
                throw new FlywayException("Unsupported audit migration database: " + database);
            }
            statement.execute("CREATE INDEX idx_audit_request_id ON audit_event (request_id)");
        }
    }
}
