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
 * El script SQL crea al administrador deshabilitado. Si se define la variable
 * BOOTSTRAP_ADMIN_PASSWORD, esta clase le asigna esa contrasena (cifrada con BCrypt)
 * y lo habilita. Opcionalmente crea usuarios de prueba.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final String CORREO_ADMIN = "admin@cultura.gob.pe";

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
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setHabilitado(true);
            usuarioRepository.save(admin);
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
