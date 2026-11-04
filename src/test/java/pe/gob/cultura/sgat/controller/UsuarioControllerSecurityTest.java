package pe.gob.cultura.sgat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.cultura.sgat.config.JwtAuthFilter;
import pe.gob.cultura.sgat.config.JwtService;
import pe.gob.cultura.sgat.config.SecurityConfig;
import pe.gob.cultura.sgat.dto.CrearUsuarioRequest;
import pe.gob.cultura.sgat.dto.PaginaResponse;
import pe.gob.cultura.sgat.dto.UsuarioResponse;
import pe.gob.cultura.sgat.model.entity.Permiso;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.UsuarioRepository;
import pe.gob.cultura.sgat.service.UsuarioService;

@WebMvcTest(
        controllers = UsuarioController.class,
        properties = "cors.allowed-origins=http://localhost")
@Import({SecurityConfig.class, JwtAuthFilter.class,
        UsuarioControllerSecurityTest.ActuatorProbeController.class})
class UsuarioControllerSecurityTest {

    private static final String REQUEST_VALIDO = """
            {
              "nombres": "Ana",
              "apellidos": "Pérez",
              "correoInstitucional": "ana.perez@cultura.gob.pe",
              "password": "Password-Segura-2026",
              "roles": ["ROLE_SOLICITANTE"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    void rechazaSolicitudSinAutenticacion() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void rechazaUsuarioSinRolAdminAunqueElTokenDeclareAdmin() throws Exception {
        autenticarComo(usuarioVigente(2L, "usuario@cultura.gob.pe", rol("ROLE_SOLICITANTE", true)));

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void rechazaCuentaDeshabilitadaConJwtTodaviaVigente() throws Exception {
        Usuario deshabilitado = usuarioVigente(3L, "baja@cultura.gob.pe", rol("ROLE_ADMIN", true));
        deshabilitado.setHabilitado(false);
        autenticarComo(deshabilitado);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void rechazaCuentaEliminadaLogicamenteConJwtTodaviaVigente() throws Exception {
        Usuario eliminado = usuarioVigente(4L, "eliminado@cultura.gob.pe", rol("ROLE_ADMIN", true));
        eliminado.setActivoLogico(false);
        autenticarComo(eliminado);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void rechazaCuentaSinRolesVigentes() throws Exception {
        autenticarComo(usuarioVigente(5L, "sinrol@cultura.gob.pe", rol("ROLE_ADMIN", false)));

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void creaUsuarioComoAdminSinExponerPasswordNiHash() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        UsuarioResponse response = new UsuarioResponse(
                10L, "Ana", "Pérez", "ana.perez@cultura.gob.pe", true,
                Set.of("ROLE_SOLICITANTE"));
        when(usuarioService.crear(any(CrearUsuarioRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.correoInstitucional").value("ana.perez@cultura.gob.pe"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rechazaRolInvalidoPorValidacionDeEntrada() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        String requestInvalido = REQUEST_VALIDO.replace("ROLE_SOLICITANTE", "ROLE_SUPERADMIN");

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestInvalido))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void rechazaRolNullPorValidacionDeEntrada() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        String requestInvalido = REQUEST_VALIDO.replace("\"ROLE_SOLICITANTE\"", "null");

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestInvalido))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void rechazaColeccionDeRolesVaciaPorValidacionDeEntrada() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        String requestInvalido = REQUEST_VALIDO.replace("[\"ROLE_SOLICITANTE\"]", "[]");

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestInvalido))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void permisoConCodigoRoleAdminNoConcedeRolAdmin() throws Exception {
        Rol solicitante = rol("ROLE_SOLICITANTE", true);
        solicitante.setPermisos(Set.of(permiso("ROLE_ADMIN")));
        autenticarComo(usuarioVigente(6L, "usuario@cultura.gob.pe", solicitante));

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).crear(any(), any());
    }

    @Test
    void protegeRutasDerivadasDeUsuariosParaRoleAdmin() throws Exception {
        autenticarComo(usuarioVigente(7L, "usuario@cultura.gob.pe", rol("ROLE_SOLICITANTE", true)));

        mockMvc.perform(get("/api/usuarios/7")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listaUsuariosComoAdministradorSinExponerDatosSensibles() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        UsuarioResponse usuario = new UsuarioResponse(
                10L, "Ana", "Pérez", "ana@cultura.gob.pe", true,
                Set.of("ROLE_SOLICITANTE"));
        PaginaResponse<UsuarioResponse> pagina = new PaginaResponse<>(
                List.of(usuario), 0, 20, 1, 1, true, true);
        when(usuarioService.listar(0, 20, 1L)).thenReturn(pagina);

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(10))
                .andExpect(jsonPath("$.contenido[0].correoInstitucional").value("ana@cultura.gob.pe"))
                .andExpect(jsonPath("$.contenido[0].password").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void respetaParametrosDePaginacion() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        when(usuarioService.listar(2, 5, 1L)).thenReturn(
                new PaginaResponse<>(List.of(), 2, 5, 12, 3, false, true));

        mockMvc.perform(get("/api/usuarios?page=2&size=5")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.tamanio").value(5))
                .andExpect(jsonPath("$.totalPaginas").value(3));

        verify(usuarioService).listar(2, 5, 1L);
    }

    @Test
    void rechazaListadoSinToken() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).listar(anyInt(), anyInt(), anyLong());
    }

    @Test
    void rechazaListadoConRolInsuficiente() throws Exception {
        autenticarComo(usuarioVigente(2L, "usuario@cultura.gob.pe", rol("ROLE_SOLICITANTE", true)));

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).listar(anyInt(), anyInt(), anyLong());
    }

    @Test
    void obtieneUsuarioPorIdComoAdministrador() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        when(usuarioService.obtener(10L, 1L)).thenReturn(new UsuarioResponse(
                10L, "Ana", "Pérez", "ana@cultura.gob.pe", true,
                Set.of("ROLE_SOLICITANTE")));

        mockMvc.perform(get("/api/usuarios/10")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void respondeNotFoundSinFiltrarDatosInternos() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));
        when(usuarioService.obtener(999L, 1L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        mockMvc.perform(get("/api/usuarios/999")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void permiteHealthCheckSinAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void niegaOtrosEndpointsActuatorInclusoAlAdmin() throws Exception {
        autenticarComo(usuarioVigente(1L, "admin@cultura.gob.pe", rol("ROLE_ADMIN", true)));

        mockMvc.perform(get("/actuator/env")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isForbidden());
    }

    private void autenticarComo(Usuario usuario) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("uid", Number.class)).thenReturn(usuario.getId());
        when(jwtService.leer("token-vigente")).thenReturn(claims);
        when(usuarioRepository.findByIdConRolesYPermisos(usuario.getId())).thenReturn(Optional.of(usuario));
    }

    private Usuario usuarioVigente(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreoInstitucional(correo);
        usuario.setHabilitado(true);
        usuario.setActivoLogico(true);
        usuario.setRoles(Set.of(rol));
        return usuario;
    }

    private Rol rol(String codigo, boolean activoLogico) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setActivoLogico(activoLogico);
        return rol;
    }

    private Permiso permiso(String codigo) {
        Permiso permiso = new Permiso();
        permiso.setCodigo(codigo);
        return permiso;
    }

    @RestController
    public static class ActuatorProbeController {

        @GetMapping("/actuator/health")
        public Map<String, String> health() {
            return Map.of("status", "UP");
        }

        @GetMapping("/actuator/env")
        public Map<String, String> env() {
            return Map.of("status", "PROTECTED");
        }
    }
}
