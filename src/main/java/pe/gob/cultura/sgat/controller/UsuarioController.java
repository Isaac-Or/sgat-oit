package pe.gob.cultura.sgat.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.cultura.sgat.config.UsuarioAutenticado;
import pe.gob.cultura.sgat.dto.CrearUsuarioRequest;
import pe.gob.cultura.sgat.dto.UsuarioResponse;
import pe.gob.cultura.sgat.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado administrador) {
        return usuarioService.crear(request, administrador.id());
    }
}
