package pe.gob.cultura.sgat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.dto.CambiarHabilitacionRequest;
import pe.gob.cultura.sgat.dto.EditarUsuarioRequest;
import pe.gob.cultura.sgat.dto.UsuarioResponse;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.RolRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioAdministracionServiceTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    private UsuarioService usuarioService;
    private Rol rolAdmin;
    private Usuario administrador;

    @BeforeEach
    void configurar() {
        usuarioService = new UsuarioService(
                usuarioRepository, rolRepository, new BCryptPasswordEncoder(4));
        rolAdmin = rol("ROLE_ADMIN", true);
        administrador = usuario(ADMIN_ID, "admin@cultura.gob.pe", true, rolAdmin);
    }

    @Test
    void editaUsuarioSinModificarPasswordYRegistraAdministrador() {
        Rol solicitante = rol("ROLE_SOLICITANTE", true);
        Rol tecnico = rol("ROLE_TECNICO", true);
        Usuario objetivo = usuario(2L, "anterior@cultura.gob.pe", true, solicitante);
        objetivo.setNombres("Nombre anterior");
        objetivo.setApellidos("Apellido anterior");
        objetivo.setPasswordHash("hash-bcrypt-original");
        prepararOperacion(objetivo);
        when(usuarioRepository.findByCorreoInstitucional("nuevo@cultura.gob.pe"))
                .thenReturn(Optional.empty());
        when(rolRepository.findByCodigo("ROLE_TECNICO")).thenReturn(Optional.of(tecnico));

        UsuarioResponse response = usuarioService.actualizar(
                2L,
                new EditarUsuarioRequest(
                        " Nuevo ", " Apellido ", " Nuevo@Cultura.gob.pe ",
                        Set.of("ROLE_TECNICO")),
                ADMIN_ID);

        assertEquals("Nuevo", objetivo.getNombres());
        assertEquals("Apellido", objetivo.getApellidos());
        assertEquals("nuevo@cultura.gob.pe", objetivo.getCorreoInstitucional());
        assertEquals("hash-bcrypt-original", objetivo.getPasswordHash());
        assertEquals(ADMIN_ID, objetivo.getModificadoPor());
        assertEquals(Set.of("ROLE_TECNICO"), response.roles());
        verify(usuarioRepository).saveAndFlush(objetivo);
    }

    @Test
    void rechazaCorreoDuplicadoAlEditar() {
        Usuario objetivo = usuario(2L, "actual@cultura.gob.pe", true,
                rol("ROLE_SOLICITANTE", true));
        Usuario existente = usuario(3L, "duplicado@cultura.gob.pe", true,
                rol("ROLE_TECNICO", true));
        prepararOperacionSinGuardado(objetivo);
        when(usuarioRepository.findByCorreoInstitucional("duplicado@cultura.gob.pe"))
                .thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(
                        2L,
                        editar("duplicado@cultura.gob.pe", Set.of("ROLE_SOLICITANTE")),
                        ADMIN_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void respondeNotFoundAlEditarUsuarioInexistente() {
        prepararAdministradorYBloqueo();
        when(usuarioRepository.findActivoByIdForUpdate(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(
                        99L, editar("usuario@cultura.gob.pe", Set.of("ROLE_TECNICO")), ADMIN_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificaRolesCuandoExistenYEstanActivos() {
        Rol tecnico = rol("ROLE_TECNICO", true);
        Rol solicitante = rol("ROLE_SOLICITANTE", true);
        Usuario objetivo = usuario(2L, "usuario@cultura.gob.pe", true, tecnico);
        prepararOperacion(objetivo);
        when(usuarioRepository.findByCorreoInstitucional("usuario@cultura.gob.pe"))
                .thenReturn(Optional.of(objetivo));
        when(rolRepository.findByCodigo("ROLE_TECNICO")).thenReturn(Optional.of(tecnico));
        when(rolRepository.findByCodigo("ROLE_SOLICITANTE")).thenReturn(Optional.of(solicitante));

        UsuarioResponse response = usuarioService.actualizar(
                2L,
                editar("usuario@cultura.gob.pe", Set.of("ROLE_TECNICO", "ROLE_SOLICITANTE")),
                ADMIN_ID);

        assertEquals(Set.of("ROLE_TECNICO", "ROLE_SOLICITANTE"), response.roles());
    }

    @Test
    void rechazaRolDesconocidoAlEditar() {
        Usuario objetivo = usuario(2L, "usuario@cultura.gob.pe", true,
                rol("ROLE_SOLICITANTE", true));
        prepararOperacionSinGuardado(objetivo);
        when(usuarioRepository.findByCorreoInstitucional("usuario@cultura.gob.pe"))
                .thenReturn(Optional.of(objetivo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(
                        2L, editar("usuario@cultura.gob.pe", Set.of("ROLE_SUPERADMIN")), ADMIN_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaRolInactivoAlEditar() {
        Usuario objetivo = usuario(2L, "usuario@cultura.gob.pe", true,
                rol("ROLE_SOLICITANTE", true));
        prepararOperacionSinGuardado(objetivo);
        when(usuarioRepository.findByCorreoInstitucional("usuario@cultura.gob.pe"))
                .thenReturn(Optional.of(objetivo));
        when(rolRepository.findByCodigo("ROLE_TECNICO"))
                .thenReturn(Optional.of(rol("ROLE_TECNICO", false)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(
                        2L, editar("usuario@cultura.gob.pe", Set.of("ROLE_TECNICO")), ADMIN_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void protegeAlUltimoAdministradorAlQuitarSuRol() {
        Rol tecnico = rol("ROLE_TECNICO", true);
        administrador.setNombres("Administrador");
        administrador.setApellidos("Principal");
        prepararOperacionSinGuardado(administrador);
        when(usuarioRepository.findByCorreoInstitucional("admin@cultura.gob.pe"))
                .thenReturn(Optional.of(administrador));
        when(rolRepository.findByCodigo("ROLE_TECNICO")).thenReturn(Optional.of(tecnico));
        when(usuarioRepository.countAdministradoresActivos()).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(
                        ADMIN_ID, editar("admin@cultura.gob.pe", Set.of("ROLE_TECNICO")), ADMIN_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void deshabilitaUsuarioYRegistraAdministrador() {
        Usuario objetivo = usuario(2L, "usuario@cultura.gob.pe", true,
                rol("ROLE_SOLICITANTE", true));
        prepararOperacion(objetivo);

        UsuarioResponse response = usuarioService.cambiarHabilitacion(
                2L, new CambiarHabilitacionRequest(false), ADMIN_ID);

        assertFalse(objetivo.isHabilitado());
        assertFalse(response.habilitado());
        assertEquals(ADMIN_ID, objetivo.getModificadoPor());
        verify(usuarioRepository).saveAndFlush(objetivo);
    }

    @Test
    void impideAutodeshabilitacion() {
        prepararOperacionSinGuardado(administrador);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.cambiarHabilitacion(
                        ADMIN_ID, new CambiarHabilitacionRequest(false), ADMIN_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void impideDeshabilitarAlUltimoAdministradorActivo() {
        Usuario objetivo = usuario(2L, "otro.admin@cultura.gob.pe", true, rolAdmin);
        prepararOperacionSinGuardado(objetivo);
        when(usuarioRepository.countAdministradoresActivos()).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.cambiarHabilitacion(
                        2L, new CambiarHabilitacionRequest(false), ADMIN_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rehabilitaUsuario() {
        Usuario objetivo = usuario(2L, "usuario@cultura.gob.pe", false,
                rol("ROLE_SOLICITANTE", true));
        prepararOperacion(objetivo);

        UsuarioResponse response = usuarioService.cambiarHabilitacion(
                2L, new CambiarHabilitacionRequest(true), ADMIN_ID);

        assertTrue(objetivo.isHabilitado());
        assertTrue(response.habilitado());
        assertEquals(ADMIN_ID, objetivo.getModificadoPor());
    }

    private void prepararAdministradorYBloqueo() {
        when(rolRepository.findByCodigoForUpdate("ROLE_ADMIN")).thenReturn(Optional.of(rolAdmin));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador));
    }

    private void prepararOperacionSinGuardado(Usuario objetivo) {
        prepararAdministradorYBloqueo();
        when(usuarioRepository.findActivoByIdForUpdate(objetivo.getId()))
                .thenReturn(Optional.of(objetivo));
    }

    private void prepararOperacion(Usuario objetivo) {
        prepararOperacionSinGuardado(objetivo);
        when(usuarioRepository.saveAndFlush(objetivo)).thenReturn(objetivo);
    }

    private EditarUsuarioRequest editar(String correo, Set<String> roles) {
        return new EditarUsuarioRequest("Nombre", "Apellido", correo, roles);
    }

    private Usuario usuario(Long id, String correo, boolean habilitado, Rol... roles) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombres("Nombre");
        usuario.setApellidos("Apellido");
        usuario.setCorreoInstitucional(correo);
        usuario.setPasswordHash("hash-bcrypt");
        usuario.setHabilitado(habilitado);
        usuario.setActivoLogico(true);
        usuario.setRoles(Set.of(roles));
        return usuario;
    }

    private Rol rol(String codigo, boolean activo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setActivoLogico(activo);
        return rol;
    }
}
