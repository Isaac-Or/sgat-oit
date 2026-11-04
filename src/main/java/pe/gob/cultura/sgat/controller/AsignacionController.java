package pe.gob.cultura.sgat.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.cultura.sgat.config.UsuarioAutenticado;
import pe.gob.cultura.sgat.dto.AsignacionResponse;
import pe.gob.cultura.sgat.dto.DevolucionAsignacionRequest;
import pe.gob.cultura.sgat.service.AsignacionService;

@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @GetMapping
    public List<AsignacionResponse> listar(
            @RequestParam(defaultValue = "true") boolean abiertas) {
        return asignacionService.listar(abiertas);
    }

    @PutMapping("/{id}/devolucion")
    public AsignacionResponse devolver(
            @PathVariable Long id,
            @Valid @RequestBody DevolucionAsignacionRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asignacionService.devolver(id, request, usuario.id());
    }
}
