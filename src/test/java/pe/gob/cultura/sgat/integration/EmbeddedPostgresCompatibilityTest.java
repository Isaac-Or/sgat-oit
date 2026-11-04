package pe.gob.cultura.sgat.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class EmbeddedPostgresCompatibilityTest {

    @Test
    void iniciaPostgresql16EnLoopbackYPuertoAleatorioYCargaElBaseline() throws Exception {
        String jdbcUrl;

        try (EmbeddedPostgresTestDatabase database = EmbeddedPostgresTestDatabase.start()) {
            EmbeddedPostgresTestDatabase.DatabaseInfo info = database.databaseInfo();
            jdbcUrl = database.jdbcUrl();

            assertEquals("sgat_oit", info.database());
            assertTrue(info.version().startsWith("16.15"), info.version());
            assertEquals("127.0.0.1", info.listenAddresses());
            assertEquals(database.port(), info.port());
            assertTrue(info.port() > 0);
            try (Connection connection = database.dataSource().getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                         select count(*)
                         from pg_catalog.pg_indexes
                         where schemaname = 'public'
                           and tablename = 'reservas_activos'
                           and indexname = 'ux_reserva_activo_activa'
                         """)) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
        }

        assertThrows(SQLException.class,
                () -> DriverManager.getConnection(jdbcUrl, "postgres", ""));
    }
}
