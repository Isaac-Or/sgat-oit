package pe.gob.cultura.sgat.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Claims;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.gob.cultura.sgat.config.JwtAuthFilter;
import pe.gob.cultura.sgat.config.JwtService;
import pe.gob.cultura.sgat.config.SecurityConfig;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.UsuarioRepository;
import pe.gob.cultura.sgat.service.AsignacionService;
import pe.gob.cultura.sgat.service.SolicitudService;

@WebMvcTest(
        controllers = {SolicitudController.class, AsignacionController.class},
        properties = "cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173")
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AsignacionControllerSecurityTest {

    private static final String ENTREGA = """
            {"fechaDevolucionEstimada":"2027-01-15T12:00:00-05:00","observaciones":null}
            """;
    private static final String DEVOLUCION = """
            {"condicionFisica":"BUENO","estadoActivo":"DISPONIBLE","observaciones":null}
            """;

    @Autowired private MockMvc mockMvc;
    @MockBean private SolicitudService solicitudService;
    @MockBean private AsignacionService asignacionService;
    @MockBean private JwtService jwtService;
    @MockBean private UsuarioRepository usuarioRepository;

    @Test
    void entregaRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/solicitudes/1/entrega")
                        .contentType(MediaType.APPLICATION_JSON).content(ENTREGA))
                .andExpect(status().isUnauthorized());
        verify(asignacionService, never()).entregar(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void entregaRechazaRolInsuficiente() throws Exception {
        autenticarComo("ROLE_SOLICITANTE");
        mockMvc.perform(post("/api/solicitudes/1/entrega")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON).content(ENTREGA))
                .andExpect(status().isForbidden());
    }

    @Test
    void devolucionRequiereAutenticacion() throws Exception {
        mockMvc.perform(put("/api/asignaciones/1/devolucion")
                        .contentType(MediaType.APPLICATION_JSON).content(DEVOLUCION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devolucionYListadoRechazanRolInsuficiente() throws Exception {
        autenticarComo("ROLE_TECNICO");
        mockMvc.perform(put("/api/asignaciones/1/devolucion")
                        .header("Authorization", "Bearer token-vigente")
                        .contentType(MediaType.APPLICATION_JSON).content(DEVOLUCION))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/asignaciones")
                        .header("Authorization", "Bearer token-vigente"))
                .andExpect(status().isForbidden());
    }

    private void autenticarComo(String codigoRol) {
        Rol rol = new Rol();
        rol.setCodigo(codigoRol);
        rol.setActivoLogico(true);
        Usuario usuario = new Usuario();
        usuario.setId(50L);
        usuario.setCorreoInstitucional("usuario@cultura.gob.pe");
        usuario.setHabilitado(true);
        usuario.setActivoLogico(true);
        usuario.setRoles(Set.of(rol));
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("uid", Number.class)).thenReturn(50L);
        when(jwtService.leer("token-vigente")).thenReturn(claims);
        when(usuarioRepository.findByIdConRolesYPermisos(50L)).thenReturn(Optional.of(usuario));
    }
}
