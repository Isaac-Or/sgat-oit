package pe.gob.cultura.sgat.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import pe.gob.cultura.sgat.config.UsuarioAutenticado;
import pe.gob.cultura.sgat.dto.CambiarHabilitacionRequest;
import pe.gob.cultura.sgat.dto.CrearUsuarioRequest;
import pe.gob.cultura.sgat.dto.EditarUsuarioRequest;
import pe.gob.cultura.sgat.dto.PaginaResponse;
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

    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UsuarioAutenticado administrador) {
        return usuarioService.listar(page, size, administrador.id());
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id,
                                   @AuthenticationPrincipal UsuarioAutenticado administrador) {
        return usuarioService.obtener(id, administrador.id());
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody EditarUsuarioRequest request,
                                      @AuthenticationPrincipal UsuarioAutenticado administrador) {
        return usuarioService.actualizar(id, request, administrador.id());
    }

    @PutMapping("/{id}/habilitacion")
    public UsuarioResponse cambiarHabilitacion(
            @PathVariable Long id,
            @Valid @RequestBody CambiarHabilitacionRequest request,
            @AuthenticationPrincipal UsuarioAutenticado administrador) {
        return usuarioService.cambiarHabilitacion(id, request, administrador.id());
    }
}
