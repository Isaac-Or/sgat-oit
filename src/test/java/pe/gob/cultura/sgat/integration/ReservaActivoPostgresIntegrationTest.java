package pe.gob.cultura.sgat.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.dto.ActivoElegido;
import pe.gob.cultura.sgat.dto.ActivoRequest;
import pe.gob.cultura.sgat.dto.ActivoResponse;
import pe.gob.cultura.sgat.dto.AtencionRequest;
import pe.gob.cultura.sgat.dto.CancelarSolicitudRequest;
import pe.gob.cultura.sgat.dto.DetalleRequest;
import pe.gob.cultura.sgat.dto.SolicitudRequest;
import pe.gob.cultura.sgat.dto.SolicitudResponse;
import pe.gob.cultura.sgat.model.entity.ReservaActivo;
import pe.gob.cultura.sgat.repository.ReservaActivoRepository;
import pe.gob.cultura.sgat.service.ActivoService;
import pe.gob.cultura.sgat.service.SolicitudService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReservaActivoPostgresIntegrationTest {

    private static final EmbeddedPostgresTestDatabase DATABASE =
            EmbeddedPostgresTestDatabase.start();

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::jdbcUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("jwt.secret", () -> "clave-reservas-local-que-supera-32-bytes-2026");
        registry.add("sgat.bootstrap.admin-password", () -> "");
        registry.add("sgat.bootstrap.demo-users", () -> "false");
    }

    @Autowired private ActivoService activoService;
    @Autowired private SolicitudService solicitudService;
    @Autowired private ReservaActivoRepository reservaRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private Long adminId;
    private Long solicitanteUnoId;
    private Long solicitanteDosId;
    private Long laptopId;
    private Long monitorId;
    private Long ubicacionId;

    @BeforeEach
    void prepararDatos() {
        jdbcTemplate.update("DELETE FROM asignaciones");
        jdbcTemplate.update("DELETE FROM mantenimientos");
        jdbcTemplate.update("DELETE FROM reservas_activos");
        jdbcTemplate.update("DELETE FROM solicitud_detalles");
        jdbcTemplate.update("DELETE FROM solicitudes");
        jdbcTemplate.update("DELETE FROM historial_estados_activo");
        jdbcTemplate.update("DELETE FROM activos");
        jdbcTemplate.update("DELETE FROM usuario_rol WHERE usuario_id IN "
                + "(SELECT id FROM usuarios WHERE correo_institucional <> 'admin@cultura.gob.pe')");
        jdbcTemplate.update("DELETE FROM usuarios WHERE correo_institucional <> 'admin@cultura.gob.pe'");

        adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE correo_institucional = 'admin@cultura.gob.pe'",
                Long.class);
        jdbcTemplate.update("UPDATE usuarios SET habilitado = TRUE WHERE id = ?", adminId);
        solicitanteUnoId = crearSolicitante("reserva.uno@cultura.gob.pe", "Reserva Uno");
        solicitanteDosId = crearSolicitante("reserva.dos@cultura.gob.pe", "Reserva Dos");
        laptopId = idTipo("Laptop");
        monitorId = idTipo("Monitor");
        ubicacionId = jdbcTemplate.queryForObject(
                "SELECT id FROM ubicaciones WHERE codigo = 'OIT'", Long.class);
        entityManager.clear();
    }

    @AfterAll
    static void cerrarPostgres() throws Exception {
        DATABASE.close();
    }

    @Test
    void dosAprobacionesConcurrentesDelMismoActivoSoloPermitenUnaReserva() throws Exception {
        ActivoResponse activo = crearActivo("CONCURRENTE-1", laptopId);
        SolicitudResponse primera = crearSolicitud(solicitanteUnoId, laptopId);
        SolicitudResponse segunda = crearSolicitud(solicitanteDosId, laptopId);

        List<ResultadoAprobacion> resultados = aprobarConcurrentemente(List.of(
                () -> aprobar(primera, List.of(activo.id())),
                () -> aprobar(segunda, List.of(activo.id()))));

        assertEquals(1, resultados.stream().filter(ResultadoAprobacion::exitosa).count());
        Throwable fallo = resultados.stream().map(ResultadoAprobacion::fallo)
                .filter(java.util.Objects::nonNull).findFirst().orElseThrow();
        assertTrue(fallo instanceof ResponseStatusException, fallo.toString());
        assertEquals(HttpStatus.CONFLICT, ((ResponseStatusException) fallo).getStatusCode());
        assertEquals(1, reservaRepository.countByActivoIdAndEstado(activo.id(), ReservaActivo.ACTIVA));
        assertEquals(1, contarSolicitudes("APROBADA"));
        assertEquals(1, contarSolicitudes("PENDIENTE"));
        assertSinEntregaFicticia(activo.id());
    }

    @Test
    void dosActivosDistintosSeReservanConcurrentemente() throws Exception {
        ActivoResponse primero = crearActivo("DISTINTO-1", laptopId);
        ActivoResponse segundo = crearActivo("DISTINTO-2", laptopId);
        SolicitudResponse solicitudUno = crearSolicitud(solicitanteUnoId, laptopId);
        SolicitudResponse solicitudDos = crearSolicitud(solicitanteDosId, laptopId);

        List<ResultadoAprobacion> resultados = aprobarConcurrentemente(List.of(
                () -> aprobar(solicitudUno, List.of(primero.id())),
                () -> aprobar(solicitudDos, List.of(segundo.id()))));

        assertTrue(resultados.stream().allMatch(ResultadoAprobacion::exitosa));
        assertEquals(2, contarReservas(ReservaActivo.ACTIVA));
        assertEquals(2, contarSolicitudes("APROBADA"));
        assertSinEntregaFicticia(primero.id());
        assertSinEntregaFicticia(segundo.id());
    }

    @Test
    void unaSolicitudConVariosActivosCreaTodasLasReservasAtomicamente() {
        ActivoResponse laptop = crearActivo("MULTIPLE-L", laptopId);
        ActivoResponse monitor = crearActivo("MULTIPLE-M", monitorId);
        SolicitudResponse solicitud = crearSolicitud(solicitanteUnoId, laptopId, monitorId);

        SolicitudResponse aprobada = aprobar(solicitud, List.of(laptop.id(), monitor.id()));

        assertEquals("APROBADA", aprobada.estado());
        assertEquals(2, reservaRepository.countBySolicitudDetalleSolicitudIdAndEstado(
                solicitud.id(), ReservaActivo.ACTIVA));
        assertSinEntregaFicticia(laptop.id());
        assertSinEntregaFicticia(monitor.id());
    }

    @Test
    void conflictoEnUnActivoRevierteTodaLaAprobacionSinReservaParcial() {
        ActivoResponse laptop = crearActivo("ROLLBACK-L", laptopId);
        ActivoResponse monitor = crearActivo("ROLLBACK-M", monitorId);
        SolicitudResponse multiple = crearSolicitud(solicitanteUnoId, laptopId, monitorId);
        SolicitudResponse bloqueadora = crearSolicitud(solicitanteDosId, monitorId);
        aprobar(bloqueadora, List.of(monitor.id()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> aprobar(multiple, List.of(laptop.id(), monitor.id())));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(0, reservaRepository.countByActivoIdAndEstado(laptop.id(), ReservaActivo.ACTIVA));
        assertEquals(1, reservaRepository.countByActivoIdAndEstado(monitor.id(), ReservaActivo.ACTIVA));
        assertEquals("PENDIENTE", estadoSolicitud(multiple.id()));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM solicitud_detalles WHERE solicitud_id = ? AND activo_id IS NOT NULL",
                Integer.class, multiple.id()));
    }

    @Test
    void reservaActivaDevuelveConflictoEnUnNuevoIntento() {
        ActivoResponse activo = crearActivo("CONFLICTO-1", laptopId);
        SolicitudResponse primera = crearSolicitud(solicitanteUnoId, laptopId);
        SolicitudResponse segunda = crearSolicitud(solicitanteDosId, laptopId);
        aprobar(primera, List.of(activo.id()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> aprobar(segunda, List.of(activo.id())));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("reservado"));
        assertEquals("PENDIENTE", estadoSolicitud(segunda.id()));
    }

    @Test
    void cancelacionLiberaYPermiteUnaNuevaReserva() {
        ActivoResponse activo = crearActivo("CANCELACION-1", laptopId);
        SolicitudResponse primera = crearSolicitud(solicitanteUnoId, laptopId);
        SolicitudResponse segunda = crearSolicitud(solicitanteDosId, laptopId);
        aprobar(primera, List.of(activo.id()));

        SolicitudResponse cancelada = solicitudService.cancelar(
                primera.id(), new CancelarSolicitudRequest("Necesidad retirada"), adminId);
        SolicitudResponse nuevaAprobacion = aprobar(segunda, List.of(activo.id()));

        assertEquals("CANCELADA", cancelada.estado());
        assertEquals("APROBADA", nuevaAprobacion.estado());
        assertEquals(1, reservaRepository.countByActivoIdAndEstado(activo.id(), ReservaActivo.LIBERADA));
        assertEquals(1, reservaRepository.countByActivoIdAndEstado(activo.id(), ReservaActivo.ACTIVA));
        assertNotNull(jdbcTemplate.queryForObject("""
                SELECT fecha_finalizacion FROM reservas_activos
                WHERE activo_id = ? AND estado = 'LIBERADA'
                """, java.time.OffsetDateTime.class, activo.id()));
        assertEquals(adminId, jdbcTemplate.queryForObject("""
                SELECT usuario_finalizador_id FROM reservas_activos
                WHERE activo_id = ? AND estado = 'LIBERADA'
                """, Long.class, activo.id()));
        assertSinEntregaFicticia(activo.id());
    }

    @Test
    void rechazoNoCreaReservasNiEntregas() {
        ActivoResponse activo = crearActivo("RECHAZO-1", laptopId);
        SolicitudResponse solicitud = crearSolicitud(solicitanteUnoId, laptopId);

        SolicitudResponse rechazada = solicitudService.atender(
                solicitud.id(), new AtencionRequest(false, "No corresponde", null), adminId);

        assertEquals("RECHAZADA", rechazada.estado());
        assertEquals(0, contarReservas(ReservaActivo.ACTIVA));
        assertSinEntregaFicticia(activo.id());
    }

    @Test
    void baseDeDatosImpideDosReservasDelMismoDetalle() {
        ActivoResponse activo = crearActivo("DETALLE-1", laptopId);
        SolicitudResponse solicitud = crearSolicitud(solicitanteUnoId, laptopId);
        aprobar(solicitud, List.of(activo.id()));
        Long detalleId = solicitud.detalles().get(0).id();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO reservas_activos
                    (activo_id, solicitud_detalle_id, estado, usuario_responsable_id)
                VALUES (?, ?, 'ACTIVA', ?)
                """, activo.id(), detalleId, adminId));
    }

    private Long crearSolicitante(String correo, String nombres) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO usuarios
                    (nombres, apellidos, correo_institucional, password_hash, habilitado, activo_logico)
                VALUES (?, 'Prueba', ?, 'hash-local-no-utilizado', TRUE, TRUE)
                RETURNING id
                """, Long.class, nombres, correo);
        jdbcTemplate.update("""
                INSERT INTO usuario_rol (usuario_id, rol_id, asignado_por)
                SELECT ?, id, ? FROM roles WHERE codigo = 'ROLE_SOLICITANTE'
                """, id, adminId);
        return id;
    }

    private Long idTipo(String nombre) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tipos_activo WHERE nombre = ?", Long.class, nombre);
    }

    private ActivoResponse crearActivo(String codigo, Long tipoId) {
        return activoService.crear(new ActivoRequest(
                tipoId, ubicacionId, codigo, "SER-" + codigo,
                "Marca", "Modelo", "Prueba de reserva",
                "DISPONIBLE", "BUENO"), adminId);
    }

    private SolicitudResponse crearSolicitud(Long usuarioId, Long... tipos) {
        List<DetalleRequest> detalles = java.util.Arrays.stream(tipos)
                .map(tipo -> new DetalleRequest(tipo, null))
                .toList();
        return solicitudService.crear(
                new SolicitudRequest("Prueba transaccional de reserva", detalles), usuarioId);
    }

    private SolicitudResponse aprobar(SolicitudResponse solicitud, List<Long> activos) {
        List<ActivoElegido> elegidos = new ArrayList<>();
        for (int i = 0; i < solicitud.detalles().size(); i++) {
            elegidos.add(new ActivoElegido(solicitud.detalles().get(i).id(), activos.get(i)));
        }
        return solicitudService.atender(
                solicitud.id(), new AtencionRequest(true, "Reserva aprobada", elegidos), adminId);
    }

    private List<ResultadoAprobacion> aprobarConcurrentemente(
            List<java.util.concurrent.Callable<SolicitudResponse>> operaciones) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(operaciones.size());
        CountDownLatch preparados = new CountDownLatch(operaciones.size());
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<ResultadoAprobacion>> futuros = operaciones.stream()
                    .map(operacion -> executor.submit(() -> {
                        preparados.countDown();
                        inicio.await(5, TimeUnit.SECONDS);
                        try {
                            return new ResultadoAprobacion(operacion.call(), null);
                        } catch (Throwable ex) {
                            return new ResultadoAprobacion(null, ex);
                        }
                    }))
                    .toList();
            assertTrue(preparados.await(5, TimeUnit.SECONDS));
            inicio.countDown();
            List<ResultadoAprobacion> resultados = new ArrayList<>();
            for (Future<ResultadoAprobacion> futuro : futuros) {
                resultados.add(futuro.get(15, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private int contarReservas(String estado) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM reservas_activos WHERE estado = ?",
                Integer.class, estado);
    }

    private int contarSolicitudes(String estado) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM solicitudes WHERE estado = ?",
                Integer.class, estado);
    }

    private String estadoSolicitud(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT estado FROM solicitudes WHERE id = ?", String.class, id);
    }

    private void assertSinEntregaFicticia(Long activoId) {
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM asignaciones WHERE activo_id = ?",
                Integer.class, activoId));
        assertEquals("DISPONIBLE", jdbcTemplate.queryForObject(
                "SELECT estado FROM activos WHERE id = ?", String.class, activoId));
    }

    private record ResultadoAprobacion(SolicitudResponse respuesta, Throwable fallo) {
        boolean exitosa() {
            return respuesta != null;
        }
    }
}
