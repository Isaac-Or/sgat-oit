package pe.gob.cultura.sgat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.gob.cultura.sgat.config.JwtService;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres-1234";

    @Test
    void generaYLeeUnTokenValido() {
        JwtService jwt = new JwtService(SECRETO, 5);
        String token = jwt.generar(7L, "admin@cultura.gob.pe", List.of("ROLE_ADMIN"));

        Claims claims = jwt.leer(token);

        assertEquals("admin@cultura.gob.pe", claims.getSubject());
        assertEquals(7L, claims.get("uid", Number.class).longValue());
        assertTrue(claims.get("roles", List.class).contains("ROLE_ADMIN"));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        String token = new JwtService(SECRETO, 5).generar(1L, "a@cultura.gob.pe", List.of());
        JwtService otro = new JwtService("otro-secreto-distinto-con-mas-de-32-caracteres-99", 5);

        assertThrows(JwtException.class, () -> otro.leer(token));
    }

    @Test
    void rechazaUnTokenExpirado() {
        JwtService jwt = new JwtService(SECRETO, -1);
        String token = jwt.generar(1L, "a@cultura.gob.pe", List.of());

        assertThrows(JwtException.class, () -> jwt.leer(token));
    }
}
