package pe.gob.cultura.sgat.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            SecurityContextHolder.clearContext();
            try {
                Claims claims = jwtService.leer(header.substring(7));
                Long usuarioId = claims.get("uid", Number.class).longValue();

                Usuario usuario = usuarioRepository.findByIdConRolesYPermisos(usuarioId)
                        .filter(u -> u.isHabilitado() && u.isActivoLogico())
                        .orElse(null);

                if (usuario != null) {
                    List<Rol> rolesVigentes = usuario.getRoles().stream()
                            .filter(Rol::isActivoLogico)
                            .toList();

                    if (!rolesVigentes.isEmpty()) {
                        Set<SimpleGrantedAuthority> autoridadesRoles = rolesVigentes.stream()
                                .map(Rol::getCodigo)
                                .filter(Objects::nonNull)
                                .filter(codigo -> codigo.startsWith("ROLE_"))
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                        Set<SimpleGrantedAuthority> autoridadesPermisos = rolesVigentes.stream()
                                .flatMap(rol -> rol.getPermisos().stream())
                                .map(permiso -> permiso.getCodigo())
                                .filter(Objects::nonNull)
                                .filter(codigo -> !codigo.startsWith("ROLE_"))
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                        autoridadesRoles.addAll(autoridadesPermisos);
                        List<SimpleGrantedAuthority> autoridades = List.copyOf(autoridadesRoles);
                        UsuarioAutenticado principal = new UsuarioAutenticado(
                                usuario.getId(), usuario.getCorreoInstitucional());
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, autoridades));
                    }
                }
            } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
