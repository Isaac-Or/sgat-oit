package pe.gob.cultura.sgat.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.cultura.sgat.dto.TipoActivoResponse;
import pe.gob.cultura.sgat.dto.UbicacionResponse;
import pe.gob.cultura.sgat.service.CatalogoService;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/tipos-activo")
    public List<TipoActivoResponse> tiposActivo() {
        return catalogoService.tiposActivo();
    }

    @GetMapping("/ubicaciones")
    public List<UbicacionResponse> ubicaciones() {
        return catalogoService.ubicaciones();
    }
}
