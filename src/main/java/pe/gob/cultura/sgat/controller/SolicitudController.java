package pe.gob.cultura.sgat.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
import pe.gob.cultura.sgat.dto.AtencionRequest;
import pe.gob.cultura.sgat.dto.SolicitudRequest;
import pe.gob.cultura.sgat.dto.SolicitudResponse;
import pe.gob.cultura.sgat.service.SolicitudService;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crear(@Valid @RequestBody SolicitudRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.crear(request, usuario.id());
    }

    @GetMapping("/mis-solicitudes")
    public List<SolicitudResponse> misSolicitudes(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.misSolicitudes(usuario.id());
    }

    @GetMapping
    public List<SolicitudResponse> listar(@RequestParam(required = false) String estado) {
        return solicitudService.listar(estado);
    }

    @PutMapping("/{id}")
    public SolicitudResponse atender(@PathVariable Long id, @Valid @RequestBody AtencionRequest request,
                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.atender(id, request, usuario.id());
    }
}
