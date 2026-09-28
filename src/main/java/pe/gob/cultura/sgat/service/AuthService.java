package pe.gob.cultura.sgat.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.config.JwtService;
import pe.gob.cultura.sgat.dto.LoginRequest;
import pe.gob.cultura.sgat.dto.LoginResponse;
import pe.gob.cultura.sgat.model.entity.Rol;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = request.correo().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(this::credencialesInvalidas);

        // Mismo mensaje para cualquier fallo, para no revelar si el correo existe
        if (!usuario.isHabilitado() || !usuario.isActivoLogico()
                || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw credencialesInvalidas();
        }

        List<String> roles = usuario.getRoles().stream().map(Rol::getCodigo).sorted().toList();
        String token = jwtService.generar(usuario.getId(), usuario.getCorreoInstitucional(), roles);
        return new LoginResponse(token, "Bearer", usuario.getCorreoInstitucional(), roles);
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }
}
