package com.ima.admin;

import com.ima.config.Settings;
import com.ima.domain.Checks;
import com.ima.security.ApiKeyHasher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.util.UUID;

/** Operator-only bootstrap; never exposed through an unauthenticated HTTP route. */
public final class ProvisionApplication {
    private ProvisionApplication() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 3) {
            throw new IllegalArgumentException("usage: ProvisionApplication APP_ID DISPLAY_NAME OWNER_REFERENCE");
        }
        Settings settings = new Settings(System.getenv());
        String applicationId = Checks.identifier(arguments[0], "applicationId");
        String displayName = Checks.text(arguments[1], "displayName", 160);
        String owner = Checks.identifier(arguments[2], "ownerReference");
        String token = settings.required("IMA_BOOTSTRAP_API_KEY");
        ApiKeyHasher hasher = new ApiKeyHasher();
        String digest = hasher.digest(token);
        long now = Clock.systemUTC().millis();
        int days = settings.integer("IMA_BOOTSTRAP_KEY_DAYS", 30, 1, 365);
        Class.forName("com.mysql.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(settings.required("IMA_MYSQL_URL"),
                settings.required("IMA_MYSQL_USER"), settings.required("IMA_MYSQL_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                insertApplication(connection, applicationId, displayName, owner, now);
                insertCredential(connection, applicationId, digest, now, now + days * 86400000L);
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            }
        }
        System.out.println("Application and initial credential created; no credential material is printed.");
    }

    private static void insertApplication(Connection connection, String id, String name, String owner, long now)
            throws SQLException {
        String sql = "INSERT INTO applications (application_id, display_name, owner_reference, enabled, "
                + "retention_days, session_gap_millis, created_at, updated_at, revision) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, name);
            statement.setString(3, owner);
            statement.setBoolean(4, true);
            statement.setInt(5, 365);
            statement.setLong(6, 1800000);
            statement.setLong(7, now);
            statement.setLong(8, now);
            statement.setLong(9, 0);
            statement.executeUpdate();
        }
    }

    private static void insertCredential(Connection connection, String app, String digest, long now, long expires)
            throws SQLException {
        String sql = "INSERT INTO api_credentials (key_id, application_id, key_digest, scopes, expires_at, created_at, revoked) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, app);
            statement.setString(3, digest);
            statement.setString(4, "INGEST,READ,ADMIN");
            statement.setLong(5, expires);
            statement.setLong(6, now);
            statement.setBoolean(7, false);
            statement.executeUpdate();
        }
    }
}
