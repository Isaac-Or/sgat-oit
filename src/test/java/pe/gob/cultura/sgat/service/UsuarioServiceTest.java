package pe.gob.cultura.sgat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.dto.CrearUsuarioRequest;
import pe.gob.cultura.sgat.dto.UsuarioResponse;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.RolRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;
    private Usuario administrador;

    @BeforeEach
    void configurar() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        usuarioService = new UsuarioService(usuarioRepository, rolRepository, passwordEncoder);
        administrador = usuario(ADMIN_ID, "admin@cultura.gob.pe", true, true, rol("ROLE_ADMIN", true));
    }

    @Test
    void creaUsuarioExitosamente() {
        CrearUsuarioRequest request = request(" Nuevo.Usuario@Cultura.gob.pe ", "ROLE_SOLICITANTE");
        Rol solicitante = rol("ROLE_SOLICITANTE", true);
        prepararCreacion(request, solicitante);

        UsuarioResponse response = usuarioService.crear(request, ADMIN_ID);

        assertEquals(10L, response.id());
        assertEquals("nuevo.usuario@cultura.gob.pe", response.correoInstitucional());
        assertEquals(Set.of("ROLE_SOLICITANTE"), response.roles());
        assertTrue(response.habilitado());
    }

    @Test
    void rechazaCorreoDuplicado() {
        CrearUsuarioRequest request = request("existente@cultura.gob.pe", "ROLE_SOLICITANTE");
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador));
        when(usuarioRepository.findByCorreoInstitucional("existente@cultura.gob.pe"))
                .thenReturn(Optional.of(new Usuario()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> usuarioService.crear(request, ADMIN_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaRolFueraDeLaListaPermitida() {
        CrearUsuarioRequest request = request("nuevo@cultura.gob.pe", "ROLE_SUPERADMIN");
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> usuarioService.crear(request, ADMIN_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(rolRepository, never()).findByCodigo(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaRolNullConBadRequest() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "Nombre", "Apellido", "nuevo@cultura.gob.pe", "Password-Segura-2026",
                Collections.singleton(null));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> usuarioService.crear(request, ADMIN_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(rolRepository, never()).findByCodigo(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaColeccionDeRolesVaciaConBadRequest() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "Nombre", "Apellido", "nuevo@cultura.gob.pe", "Password-Segura-2026", Set.of());
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> usuarioService.crear(request, ADMIN_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(rolRepository, never()).findByCodigo(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void almacenaLaPasswordUnicamenteComoHashBCrypt() {
        CrearUsuarioRequest request = request("nuevo@cultura.gob.pe", "ROLE_TECNICO");
        prepararCreacion(request, rol("ROLE_TECNICO", true));

        usuarioService.crear(request, ADMIN_ID);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        String hash = captor.getValue().getPasswordHash();
        assertNotEquals(request.password(), hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
        assertTrue(passwordEncoder.matches(request.password(), hash));
    }

    @Test
    void rechazaPasswordQueExcedeElLimiteDe72BytesDeBCrypt() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "Nombre", "Apellido", "nuevo@cultura.gob.pe", "á".repeat(40),
                Set.of("ROLE_TECNICO"));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador));
        when(usuarioRepository.findByCorreoInstitucional("nuevo@cultura.gob.pe"))
                .thenReturn(Optional.empty());
        when(rolRepository.findByCodigo("ROLE_TECNICO"))
                .thenReturn(Optional.of(rol("ROLE_TECNICO", true)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> usuarioService.crear(request, ADMIN_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    private void prepararCreacion(CrearUsuarioRequest request, Rol rolSolicitado) {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador));
        when(usuarioRepository.findByCorreoInstitucional(request.correoInstitucional().trim().toLowerCase()))
                .thenReturn(Optional.empty());
        when(rolRepository.findByCodigo(rolSolicitado.getCodigo())).thenReturn(Optional.of(rolSolicitado));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario usuario = invocacion.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });
    }

    private CrearUsuarioRequest request(String correo, String rol) {
        return new CrearUsuarioRequest(
                "Nombre", "Apellido", correo, "Password-Segura-2026", Set.of(rol));
    }

    private Usuario usuario(Long id, String correo, boolean habilitado,
                            boolean activoLogico, Rol... roles) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreoInstitucional(correo);
        usuario.setHabilitado(habilitado);
        usuario.setActivoLogico(activoLogico);
        usuario.setRoles(Set.of(roles));
        return usuario;
    }

    private Rol rol(String codigo, boolean activoLogico) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setActivoLogico(activoLogico);
        return rol;
    }
}
