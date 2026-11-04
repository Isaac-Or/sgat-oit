package pe.gob.cultura.sgat.integration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;

final class EmbeddedPostgresTestDatabase implements AutoCloseable {

    private static final Path BASELINE = Path.of("db", "001_schema_baseline.sql");

    private final EmbeddedPostgres postgres;
    private final DataSource dataSource;
    private final AtomicBoolean closed = new AtomicBoolean();

    private EmbeddedPostgresTestDatabase(EmbeddedPostgres postgres, DataSource dataSource) {
        this.postgres = postgres;
        this.dataSource = dataSource;
    }

    static EmbeddedPostgresTestDatabase start() {
        EmbeddedPostgres postgres = null;
        try {
            postgres = EmbeddedPostgres.builder()
                    .setPort(0)
                    .setServerConfig("listen_addresses", "127.0.0.1")
                    .setCleanDataDirectory(true)
                    .setRegisterShutdownHook(true)
                    .start();

            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE sgat_oit");
            }

            DataSource dataSource = postgres.getDatabase("postgres", "sgat_oit");
            String baseline = Files.readString(BASELINE, StandardCharsets.UTF_8);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(baseline);
            }
            return new EmbeddedPostgresTestDatabase(postgres, dataSource);
        } catch (Exception ex) {
            cerrarSilenciosamente(postgres);
            throw new IllegalStateException("No se pudo iniciar PostgreSQL embebido", ex);
        }
    }

    DataSource dataSource() {
        return dataSource;
    }

    String jdbcUrl() {
        return postgres.getJdbcUrl("postgres", "sgat_oit");
    }

    int port() {
        return postgres.getPort();
    }

    DatabaseInfo databaseInfo() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     select current_database(), current_setting('server_version'),
                            current_setting('listen_addresses'), inet_server_port()
                     """)) {
            resultSet.next();
            return new DatabaseInfo(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getInt(4));
        }
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            postgres.close();
        }
    }

    private static void cerrarSilenciosamente(EmbeddedPostgres postgres) {
        if (postgres == null) {
            return;
        }
        try {
            postgres.close();
        } catch (IOException ignored) {
            // Conserva la causa original del fallo de arranque.
        }
    }

    record DatabaseInfo(String database, String version, String listenAddresses, int port) {
    }
}
