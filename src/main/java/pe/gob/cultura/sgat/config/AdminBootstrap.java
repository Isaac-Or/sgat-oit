package pe.gob.cultura.sgat.config;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.RolRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

/**
 * El script SQL crea al administrador deshabilitado con un hash marcador. Si se
 * define la variable BOOTSTRAP_ADMIN_PASSWORD, esta clase activa esa cuenta inicial
 * una sola vez. No reactiva una cuenta deshabilitada posteriormente. Opcionalmente
 * crea usuarios de prueba.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final String CORREO_ADMIN = "admin@cultura.gob.pe";
    private static final String HASH_INICIAL_ADMIN =
            "$2y$12$rbui/6O8hWLUGDYHhkeBT.zPPQB0eGIzY8m59zvIfq8VUR8J0ydAq";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final boolean usuariosDemo;

    public AdminBootstrap(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${sgat.bootstrap.admin-password}") String adminPassword,
                          @Value("${sgat.bootstrap.demo-users}") boolean usuariosDemo) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.usuariosDemo = usuariosDemo;
    }

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        usuarioRepository.findByCorreoInstitucional(CORREO_ADMIN).ifPresent(admin -> {
            boolean esCuentaInicial = !admin.isHabilitado()
                    && admin.isActivoLogico()
                    && HASH_INICIAL_ADMIN.equals(admin.getPasswordHash())
                    && admin.getRoles().stream().anyMatch(rol ->
                            rol.isActivoLogico() && "ROLE_ADMIN".equals(rol.getCodigo()));
            if (esCuentaInicial) {
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setHabilitado(true);
                usuarioRepository.save(admin);
            }
        });

        if (usuariosDemo) {
            crearUsuario("solicitante@cultura.gob.pe", "Colaborador", "Demo", "ROLE_SOLICITANTE");
            crearUsuario("tecnico@cultura.gob.pe", "Tecnico", "Demo", "ROLE_TECNICO");
        }
    }

    private void crearUsuario(String correo, String nombres, String apellidos, String codigoRol) {
        if (usuarioRepository.findByCorreoInstitucional(correo).isPresent()) {
            return;
        }
        Rol rol = rolRepository.findByCodigo(codigoRol).orElseThrow();
        Usuario u = new Usuario();
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setCorreoInstitucional(correo);
        u.setPasswordHash(passwordEncoder.encode(adminPassword));
        Set<Rol> roles = new HashSet<>();
        roles.add(rol);
        u.setRoles(roles);
        usuarioRepository.save(u);
    }
}
