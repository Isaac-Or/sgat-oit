
package pe.gob.cultura.sgat.service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.cultura.sgat.dto.CrearUsuarioRequest;
import pe.gob.cultura.sgat.dto.UsuarioResponse;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.RolRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Set<String> ROLES_VALIDOS =
            Set.of("ROLE_ADMIN", "ROLE_TECNICO", "ROLE_SOLICITANTE");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse crear(
            CrearUsuarioRequest request,
            Long administradorId) {

        Usuario administrador = usuarioRepository.findById(administradorId)
                .filter(u -> u.isHabilitado() && u.isActivoLogico())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Administrador no autorizado"));

        boolean tieneRolAdmin = administrador.getRoles().stream()
                .anyMatch(r -> r.isActivoLogico()
                        && "ROLE_ADMIN".equals(r.getCodigo()));

        if (!tieneRolAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Permisos insuficientes");
        }

        Set<String> codigosRoles = request.roles();
        if (codigosRoles == null || codigosRoles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe indicar al menos un rol");
        }

        for (String codigo : codigosRoles) {
            if (codigo == null || !ROLES_VALIDOS.contains(codigo)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Rol no permitido");
            }
        }

        String correo = request.correoInstitucional()
                .trim().toLowerCase(java.util.Locale.ROOT);

        if (usuarioRepository.findByCorreoInstitucional(correo).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        Set<Rol> roles = new HashSet<>();

        for (String codigo : codigosRoles) {
            Rol rol = rolRepository.findByCodigo(codigo)
                    .filter(Rol::isActivoLogico)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Rol no válido"));

            roles.add(rol);
        }

        Usuario usuario = new Usuario();
        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setCorreoInstitucional(correo);
        usuario.setPasswordHash(cifrarPassword(request.password()));
        usuario.setHabilitado(true);
        usuario.setActivoLogico(true);
        usuario.setCreadoPor(administradorId);
        usuario.setRoles(roles);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);

        return new UsuarioResponse(
                guardado.getId(),
                guardado.getNombres(),
                guardado.getApellidos(),
                guardado.getCorreoInstitucional(),
                guardado.isHabilitado(),
                guardado.getRoles().stream()
                        .map(Rol::getCodigo)
                        .collect(Collectors.toSet())
        );
    }

    private String cifrarPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña excede el máximo permitido");
        }
        return passwordEncoder.encode(password);
    }
}
