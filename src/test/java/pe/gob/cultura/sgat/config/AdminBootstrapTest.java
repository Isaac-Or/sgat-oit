package pe.gob.cultura.sgat.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.RolRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String HASH_INICIAL =
            "$2y$12$rbui/6O8hWLUGDYHhkeBT.zPPQB0eGIzY8m59zvIfq8VUR8J0ydAq";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void configurar() {
        passwordEncoder = new BCryptPasswordEncoder(4);
    }

    @Test
    void activaSoloLaCuentaInicialSembrada() {
        Usuario admin = adminDeshabilitado(HASH_INICIAL);
        when(usuarioRepository.findByCorreoInstitucional("admin@cultura.gob.pe"))
                .thenReturn(Optional.of(admin));
        AdminBootstrap bootstrap = new AdminBootstrap(
                usuarioRepository, rolRepository, passwordEncoder, "Password-Segura-2026", false);

        bootstrap.run();

        assertTrue(admin.isHabilitado());
        assertTrue(passwordEncoder.matches("Password-Segura-2026", admin.getPasswordHash()));
        verify(usuarioRepository).save(admin);
    }

    @Test
    void noReactivaUnAdminDeshabilitadoDespuesDeLaInicializacion() {
        Usuario admin = adminDeshabilitado(passwordEncoder.encode("Password-Anterior-2026"));
        when(usuarioRepository.findByCorreoInstitucional("admin@cultura.gob.pe"))
                .thenReturn(Optional.of(admin));
        AdminBootstrap bootstrap = new AdminBootstrap(
                usuarioRepository, rolRepository, passwordEncoder, "Password-Nueva-2026", false);

        bootstrap.run();

        assertFalse(admin.isHabilitado());
        verify(usuarioRepository, never()).save(any());
    }

    private Usuario adminDeshabilitado(String hash) {
        Rol adminRol = new Rol();
        adminRol.setCodigo("ROLE_ADMIN");
        adminRol.setActivoLogico(true);
        Usuario admin = new Usuario();
        admin.setCorreoInstitucional("admin@cultura.gob.pe");
        admin.setPasswordHash(hash);
        admin.setHabilitado(false);
        admin.setActivoLogico(true);
        admin.setRoles(Set.of(adminRol));
        return admin;
    }
}
