package pe.gob.cultura.sgat.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.cultura.sgat.dto.TipoActivoResponse;
import pe.gob.cultura.sgat.dto.UbicacionResponse;
import pe.gob.cultura.sgat.repository.TipoActivoRepository;
import pe.gob.cultura.sgat.repository.UbicacionRepository;

@Service
public class CatalogoService {

    private final TipoActivoRepository tipoActivoRepository;
    private final UbicacionRepository ubicacionRepository;

    public CatalogoService(TipoActivoRepository tipoActivoRepository, UbicacionRepository ubicacionRepository) {
        this.tipoActivoRepository = tipoActivoRepository;
        this.ubicacionRepository = ubicacionRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoActivoResponse> tiposActivo() {
        return tipoActivoRepository.findByActivoLogicoTrueOrderByNombreAsc().stream()
                .map(t -> new TipoActivoResponse(t.getId(), t.getNombre(), t.getCategoria().getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UbicacionResponse> ubicaciones() {
        return ubicacionRepository.findByActivoLogicoTrueOrderByNombreAsc().stream()
                .map(u -> new UbicacionResponse(u.getId(), u.getCodigo(), u.getNombre()))
                .toList();
    }
}
