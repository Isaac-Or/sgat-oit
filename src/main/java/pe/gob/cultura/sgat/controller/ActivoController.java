package pe.gob.cultura.sgat.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.cultura.sgat.config.UsuarioAutenticado;
import pe.gob.cultura.sgat.dto.ActivoRequest;
import pe.gob.cultura.sgat.dto.ActivoResponse;
import pe.gob.cultura.sgat.service.ActivoService;
import pe.gob.cultura.sgat.dto.HistorialResponse;

@RestController
@RequestMapping("/api/activos")
public class ActivoController {

    private final ActivoService activoService;

    public ActivoController(ActivoService activoService) {
        this.activoService = activoService;
    }

    /** Catálogo: el colaborador solo ve equipos DISPONIBLES; admin y técnico ven todo (HU-04). */
    @GetMapping
    public List<ActivoResponse> listar(@RequestParam(required = false) String estado,
                                       Authentication authentication) {
        boolean veTodo = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_TECNICO"));
        return activoService.listar(veTodo ? estado : "DISPONIBLE");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivoResponse crear(@Valid @RequestBody ActivoRequest request,
                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return activoService.crear(request, usuario.id());
    }

    @PutMapping("/{id}")
    public ActivoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActivoRequest request,
                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return activoService.actualizar(id, request, usuario.id());
    }
    
    @GetMapping("/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Long id) {
        return activoService.historial(id);
    }
}
