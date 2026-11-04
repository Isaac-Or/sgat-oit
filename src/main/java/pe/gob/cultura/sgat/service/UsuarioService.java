
package pe.gob.cultura.sgat.service;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.cultura.sgat.dto.CambiarHabilitacionRequest;
import pe.gob.cultura.sgat.dto.CrearUsuarioRequest;
import pe.gob.cultura.sgat.dto.EditarUsuarioRequest;
import pe.gob.cultura.sgat.dto.PaginaResponse;
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

        validarAdministrador(administradorId);

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
                .trim().toLowerCase(Locale.ROOT);

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

        Usuario guardado = guardarConCorreoUnico(usuario);

        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(int pagina, int tamanio, Long administradorId) {
        validarAdministrador(administradorId);
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Parámetros de paginación inválidos");
        }

        Sort ordenEstable = Sort.by(
                Sort.Order.asc("apellidos"),
                Sort.Order.asc("nombres"),
                Sort.Order.asc("id"));
        Page<Usuario> resultado = usuarioRepository.findByActivoLogicoTrue(
                PageRequest.of(pagina, tamanio, ordenEstable));
        List<UsuarioResponse> contenido = resultado.getContent().stream()
                .map(this::aRespuesta)
                .toList();

        return new PaginaResponse<>(
                contenido,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isFirst(),
                resultado.isLast());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id, Long administradorId) {
        validarAdministrador(administradorId);
        Usuario usuario = usuarioRepository.findByIdAndActivoLogicoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return aRespuesta(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(
            Long id, EditarUsuarioRequest request, Long administradorId) {
        bloquearAdministracionUsuarios();
        validarAdministrador(administradorId);
        Usuario usuario = usuarioRepository.findActivoByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String correo = request.correoInstitucional().trim().toLowerCase(Locale.ROOT);
        usuarioRepository.findByCorreoInstitucional(correo)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "El correo ya está registrado");
                });

        Set<Rol> roles = resolverRoles(request.roles());
        if (esAdministradorActivo(usuario)
                && roles.stream().noneMatch(this::esRolAdministradorVigente)) {
            asegurarQueNoSeaElUltimoAdministrador();
        }

        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setCorreoInstitucional(correo);
        usuario.setRoles(roles);
        usuario.setModificadoPor(administradorId);
        return aRespuesta(guardarConCorreoUnico(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarHabilitacion(
            Long id, CambiarHabilitacionRequest request, Long administradorId) {
        if (request.habilitado() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe indicar el estado de habilitación");
        }

        bloquearAdministracionUsuarios();
        validarAdministrador(administradorId);
        Usuario usuario = usuarioRepository.findActivoByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        boolean habilitar = request.habilitado();

        if (usuario.isHabilitado() == habilitar) {
            return aRespuesta(usuario);
        }
        if (!habilitar && id.equals(administradorId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No puede deshabilitar su propia cuenta");
        }
        if (!habilitar && esAdministradorActivo(usuario)) {
            asegurarQueNoSeaElUltimoAdministrador();
        }

        usuario.setHabilitado(habilitar);
        usuario.setModificadoPor(administradorId);
        return aRespuesta(usuarioRepository.saveAndFlush(usuario));
    }

    private String cifrarPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña excede el máximo permitido");
        }
        return passwordEncoder.encode(password);
    }

    private Usuario guardarConCorreoUnico(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionUnicidad(ex)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "El correo ya está registrado", ex);
            }
            throw ex;
        }
    }

    private boolean esViolacionUnicidad(Throwable error) {
        Throwable causa = error;
        while (causa != null) {
            if (causa instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private Set<Rol> resolverRoles(Set<String> codigosRoles) {
        validarCodigosRoles(codigosRoles);
        Set<Rol> roles = new HashSet<>();
        for (String codigo : codigosRoles) {
            Rol rol = rolRepository.findByCodigo(codigo)
                    .filter(Rol::isActivoLogico)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Rol no válido"));
            roles.add(rol);
        }
        return roles;
    }

    private void validarCodigosRoles(Set<String> codigosRoles) {
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
    }

    private void bloquearAdministracionUsuarios() {
        rolRepository.findByCodigoForUpdate("ROLE_ADMIN")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Configuración de roles no disponible"));
    }

    private void asegurarQueNoSeaElUltimoAdministrador() {
        if (usuarioRepository.countAdministradoresActivos() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operación dejaría al sistema sin administradores activos");
        }
    }

    private boolean esAdministradorActivo(Usuario usuario) {
        return usuario.isHabilitado()
                && usuario.isActivoLogico()
                && usuario.getRoles().stream().anyMatch(this::esRolAdministradorVigente);
    }

    private boolean esRolAdministradorVigente(Rol rol) {
        return rol.isActivoLogico() && "ROLE_ADMIN".equals(rol.getCodigo());
    }

    private void validarAdministrador(Long administradorId) {
        Usuario administrador = usuarioRepository.findById(administradorId)
                .filter(u -> u.isHabilitado() && u.isActivoLogico())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Administrador no autorizado"));

        boolean tieneRolAdmin = administrador.getRoles().stream()
                .anyMatch(rol -> rol.isActivoLogico()
                        && "ROLE_ADMIN".equals(rol.getCodigo()));
        if (!tieneRolAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Permisos insuficientes");
        }
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        Set<String> rolesVigentes = usuario.getRoles().stream()
                .filter(Rol::isActivoLogico)
                .map(Rol::getCodigo)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreoInstitucional(),
                usuario.isHabilitado(),
                rolesVigentes);
    }
}
