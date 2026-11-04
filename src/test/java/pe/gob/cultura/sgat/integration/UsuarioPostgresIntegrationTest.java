package pe.gob.cultura.sgat.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.dto.CambiarHabilitacionRequest;
import pe.gob.cultura.sgat.dto.CrearUsuarioRequest;
import pe.gob.cultura.sgat.dto.EditarUsuarioRequest;
import pe.gob.cultura.sgat.dto.LoginRequest;
import pe.gob.cultura.sgat.dto.LoginResponse;
import pe.gob.cultura.sgat.dto.PaginaResponse;
import pe.gob.cultura.sgat.dto.UsuarioResponse;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.RolRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;
import pe.gob.cultura.sgat.service.AuthService;
import pe.gob.cultura.sgat.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class UsuarioPostgresIntegrationTest {

    private static final String ADMIN_CORREO = "admin@cultura.gob.pe";
    private static final String ADMIN_PASSWORD = "Admin-Seguro-2026";
    private static final String USER_PASSWORD = "Usuario-Seguro-2026";
    private static final EmbeddedPostgresTestDatabase DATABASE =
            EmbeddedPostgresTestDatabase.start();

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::jdbcUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "1000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "500");
        registry.add("jwt.secret",
                () -> "clave-integracion-local-que-supera-32-bytes-2026");
        registry.add("sgat.bootstrap.admin-password", () -> "");
        registry.add("sgat.bootstrap.demo-users", () -> "false");
    }

    @Autowired private UsuarioService usuarioService;
    @Autowired private AuthService authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mockMvc;

    private Long adminId;

    @BeforeEach
    void restaurarDatos() {
        jdbcTemplate.update("UPDATE roles SET activo_logico = TRUE");
        jdbcTemplate.update("DELETE FROM usuario_rol WHERE usuario_id IN "
                + "(SELECT id FROM usuarios WHERE correo_institucional <> ?)", ADMIN_CORREO);
        jdbcTemplate.update("DELETE FROM usuarios WHERE correo_institucional <> ?", ADMIN_CORREO);
        adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE correo_institucional = ?",
                Long.class, ADMIN_CORREO);
        jdbcTemplate.update("DELETE FROM usuario_rol WHERE usuario_id = ?", adminId);
        jdbcTemplate.update("""
                INSERT INTO usuario_rol (usuario_id, rol_id, asignado_por)
                SELECT ?, id, ? FROM roles WHERE codigo = 'ROLE_ADMIN'
                """, adminId, adminId);
        jdbcTemplate.update("""
                UPDATE usuarios
                SET nombres = 'Administrador', apellidos = 'ZZZ',
                    password_hash = ?, habilitado = TRUE, activo_logico = TRUE,
                    creado_por = NULL, modificado_por = NULL
                WHERE id = ?
                """, passwordEncoder.encode(ADMIN_PASSWORD), adminId);
        entityManager.clear();
    }

    @AfterAll
    static void cerrarPostgres() throws Exception {
        DATABASE.close();
    }

    @Test
    @Order(1)
    void persisteUsuarioYRelacionUsuarioRolConPasswordBcrypt() {
        UsuarioResponse creado = crearUsuario(
                "Ana", "Perez", "ANA.PEREZ@CULTURA.GOB.PE", Set.of("ROLE_TECNICO"));

        assertEquals("ana.perez@cultura.gob.pe", creado.correoInstitucional());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM usuario_rol WHERE usuario_id = ?", Integer.class,
                creado.id()));
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM usuarios WHERE id = ?", String.class, creado.id());
        assertNotEquals(USER_PASSWORD, hash);
        assertTrue(passwordEncoder.matches(USER_PASSWORD, hash));
    }

    @Test
    @Order(2)
    void editaDatosRolesYAdministradorDeModificacion() {
        UsuarioResponse creado = crearUsuario(
                "Ana", "Perez", "ana@cultura.gob.pe", Set.of("ROLE_SOLICITANTE"));

        UsuarioResponse editado = usuarioService.actualizar(
                creado.id(),
                new EditarUsuarioRequest(
                        "Ana Maria", "Perez Lopez", "ANA.NUEVA@CULTURA.GOB.PE",
                        Set.of("ROLE_TECNICO", "ROLE_SOLICITANTE")),
                adminId);

        assertEquals("ana.nueva@cultura.gob.pe", editado.correoInstitucional());
        assertEquals(Set.of("ROLE_TECNICO", "ROLE_SOLICITANTE"), editado.roles());
        assertEquals(adminId, jdbcTemplate.queryForObject(
                "SELECT modificado_por FROM usuarios WHERE id = ?", Long.class, creado.id()));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM usuario_rol WHERE usuario_id = ?", Integer.class,
                creado.id()));
    }

    @Test
    @Order(3)
    void entityGraphCargaRolesYPermisosFueraDeLaSesion() {
        UsuarioResponse creado = crearUsuario(
                "Tania", "Tecnica", "tania@cultura.gob.pe", Set.of("ROLE_TECNICO"));

        Usuario usuario = usuarioRepository.findByIdConRolesYPermisos(creado.id()).orElseThrow();
        entityManager.clear();
        Set<String> permisos = usuario.getRoles().iterator().next().getPermisos().stream()
                .map(permiso -> permiso.getCodigo())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(permisos.contains("ACTIVO_EDITAR"));
        assertTrue(permisos.contains("MANTENIMIENTO_REGISTRAR"));
        assertFalse(permisos.contains("USUARIO_GESTIONAR"));
    }

    @Test
    @Order(4)
    void paginaYOrdenaEstablementeUsuariosActivos() {
        crearUsuario("Zoe", "Bravo", "zoe@cultura.gob.pe", Set.of("ROLE_SOLICITANTE"));
        crearUsuario("Ana", "Alfa", "ana@cultura.gob.pe", Set.of("ROLE_SOLICITANTE"));
        crearUsuario("Bea", "Alfa", "bea@cultura.gob.pe", Set.of("ROLE_SOLICITANTE"));
        crearUsuario("Carlos", "Delta", "carlos@cultura.gob.pe", Set.of("ROLE_TECNICO"));

        PaginaResponse<UsuarioResponse> primera = usuarioService.listar(0, 2, adminId);
        PaginaResponse<UsuarioResponse> segunda = usuarioService.listar(1, 2, adminId);

        assertEquals(List.of("Ana", "Bea"), primera.contenido().stream()
                .map(UsuarioResponse::nombres).toList());
        assertEquals(List.of("Zoe", "Carlos"), segunda.contenido().stream()
                .map(UsuarioResponse::nombres).toList());
        assertEquals(5, primera.totalElementos());
        assertEquals(3, primera.totalPaginas());
    }

    @Test
    @Order(5)
    void restriccionUnicaDePostgresqlRechazaCorreoDuplicado() {
        crearUsuario("Ana", "Perez", "unico@cultura.gob.pe", Set.of("ROLE_SOLICITANTE"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO usuarios
                    (nombres, apellidos, correo_institucional, password_hash,
                     habilitado, activo_logico)
                VALUES ('Duplicada', 'Prueba', 'unico@cultura.gob.pe', ?, TRUE, TRUE)
                """, passwordEncoder.encode(USER_PASSWORD)));
    }

    @Test
    @Order(6)
    void concurrenciaEnCorreoDuplicadoDejaUnUsuarioYDevuelveConflicto() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<ResultadoCreacion>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                int numero = i;
                futuros.add(executor.submit(() -> {
                    preparados.countDown();
                    inicio.await(5, TimeUnit.SECONDS);
                    try {
                        UsuarioResponse usuario = usuarioService.crear(
                                new CrearUsuarioRequest(
                                        "Concurrente" + numero, "Prueba",
                                        "concurrente@cultura.gob.pe", USER_PASSWORD,
                                        Set.of("ROLE_SOLICITANTE")),
                                adminId);
                        return new ResultadoCreacion(usuario, null);
                    } catch (Throwable ex) {
                        return new ResultadoCreacion(null, ex);
                    }
                }));
            }
            assertTrue(preparados.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            List<ResultadoCreacion> resultados = new ArrayList<>();
            for (Future<ResultadoCreacion> futuro : futuros) {
                resultados.add(futuro.get(15, TimeUnit.SECONDS));
            }
            assertEquals(1, resultados.stream().filter(r -> r.usuario() != null).count());
            Throwable fallo = resultados.stream().map(ResultadoCreacion::fallo)
                    .filter(java.util.Objects::nonNull).findFirst().orElseThrow();
            assertTrue(fallo instanceof ResponseStatusException, fallo.toString());
            assertEquals(HttpStatus.CONFLICT, ((ResponseStatusException) fallo).getStatusCode());
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM usuarios WHERE correo_institucional = ?",
                    Integer.class, "concurrente@cultura.gob.pe"));
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @Order(7)
    void bloqueoPesimistaSerializaLaAdministracionDeUsuarios() throws Exception {
        UsuarioResponse creado = crearUsuario(
                "Bloqueada", "Prueba", "bloqueada@cultura.gob.pe",
                Set.of("ROLE_SOLICITANTE"));
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bloqueoTomado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        try {
            CompletableFuture<Void> bloqueador = CompletableFuture.runAsync(() ->
                    transactions.executeWithoutResult(status -> {
                        rolRepository.findByCodigoForUpdate("ROLE_ADMIN").orElseThrow();
                        bloqueoTomado.countDown();
                        esperar(liberar);
                    }), executor);
            assertTrue(bloqueoTomado.await(5, TimeUnit.SECONDS));

            CompletableFuture<UsuarioResponse> operacion = CompletableFuture.supplyAsync(() ->
                    usuarioService.cambiarHabilitacion(
                            creado.id(), new CambiarHabilitacionRequest(false), adminId), executor);

            assertThrows(TimeoutException.class,
                    () -> operacion.get(Duration.ofMillis(400).toMillis(), TimeUnit.MILLISECONDS));
            liberar.countDown();
            assertFalse(operacion.get(5, TimeUnit.SECONDS).habilitado());
            bloqueador.get(5, TimeUnit.SECONDS);
        } finally {
            liberar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @Order(8)
    void protegeAlUltimoAdministradorActivo() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(
                        adminId,
                        new EditarUsuarioRequest(
                                "Administrador", "ZZZ", ADMIN_CORREO, Set.of("ROLE_TECNICO")),
                        adminId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(1L, usuarioRepository.countAdministradoresActivos());
    }

    @Test
    @Order(9)
    void deshabilitacionRevocaJwtYRehabilitacionRestauraLogin() throws Exception {
        UsuarioResponse tecnico = crearUsuario(
                "Tania", "Tecnica", "jwt@cultura.gob.pe", Set.of("ROLE_TECNICO"));
        LoginResponse login = authService.login(new LoginRequest(
                tecnico.correoInstitucional(), USER_PASSWORD));

        mockMvc.perform(get("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.token()))
                .andExpect(status().isForbidden());

        usuarioService.cambiarHabilitacion(
                tecnico.id(), new CambiarHabilitacionRequest(false), adminId);

        ResponseStatusException loginDeshabilitado = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest(
                        tecnico.correoInstitucional(), USER_PASSWORD)));
        assertEquals(HttpStatus.UNAUTHORIZED, loginDeshabilitado.getStatusCode());
        mockMvc.perform(get("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.token()))
                .andExpect(status().isUnauthorized());

        usuarioService.cambiarHabilitacion(
                tecnico.id(), new CambiarHabilitacionRequest(true), adminId);
        assertNotNull(authService.login(new LoginRequest(
                tecnico.correoInstitucional(), USER_PASSWORD)).token());
    }

    @Test
    @Order(10)
    void jwtUsaRolesYPermisosVigentesCargadosPorEntityGraph() throws Exception {
        LoginResponse loginAdmin = authService.login(new LoginRequest(ADMIN_CORREO, ADMIN_PASSWORD));

        mockMvc.perform(get("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAdmin.token()))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "UPDATE roles SET activo_logico = FALSE WHERE codigo = 'ROLE_ADMIN'");
        entityManager.clear();

        mockMvc.perform(get("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAdmin.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(100)
    void healthReflejaBaseDisponibleYNoDisponibleYCierraElProceso() throws Exception {
        String jdbcUrl = DATABASE.jdbcUrl();
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        DATABASE.close();

        assertThrows(SQLException.class,
                () -> DriverManager.getConnection(jdbcUrl, "postgres", ""));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    private UsuarioResponse crearUsuario(
            String nombres, String apellidos, String correo, Set<String> roles) {
        return usuarioService.crear(
                new CrearUsuarioRequest(
                        nombres, apellidos, correo, USER_PASSWORD, roles),
                adminId);
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tiempo de espera agotado");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Espera interrumpida", ex);
        }
    }

    private record ResultadoCreacion(UsuarioResponse usuario, Throwable fallo) {
    }
}
