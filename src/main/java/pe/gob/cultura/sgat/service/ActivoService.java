package pe.gob.cultura.sgat.service;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.dto.ActivoRequest;
import pe.gob.cultura.sgat.dto.ActivoResponse;
import pe.gob.cultura.sgat.model.entity.Activo;
import pe.gob.cultura.sgat.model.entity.TipoActivo;
import pe.gob.cultura.sgat.model.entity.Ubicacion;
import pe.gob.cultura.sgat.repository.ActivoRepository;
import pe.gob.cultura.sgat.repository.TipoActivoRepository;
import pe.gob.cultura.sgat.repository.UbicacionRepository;

@Service
public class ActivoService {

    private static final Set<String> ESTADOS = Set.of("DISPONIBLE", "ASIGNADO", "EN_MANTENIMIENTO", "DE_BAJA");
    private static final Set<String> CONDICIONES = Set.of("BUENO", "REGULAR", "MALO");

    private final ActivoRepository activoRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final UbicacionRepository ubicacionRepository;

    public ActivoService(ActivoRepository activoRepository, TipoActivoRepository tipoActivoRepository,
                         UbicacionRepository ubicacionRepository) {
        this.activoRepository = activoRepository;
        this.tipoActivoRepository = tipoActivoRepository;
        this.ubicacionRepository = ubicacionRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivoResponse> listar(String estado) {
        List<Activo> activos = (estado == null || estado.isBlank())
                ? activoRepository.findByActivoLogicoTrueOrderByCodigoPatrimonialAsc()
                : activoRepository.findByEstadoAndActivoLogicoTrueOrderByCodigoPatrimonialAsc(estado.trim().toUpperCase());
        return activos.stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public ActivoResponse crear(ActivoRequest request, Long usuarioId) {
        String codigo = request.codigoPatrimonial().trim();
        String serie = normalizarSerie(request.numeroSerie());
        if (activoRepository.existsByCodigoPatrimonial(codigo)) {
            throw conflicto("Ya existe un activo con ese código patrimonial");
        }
        if (serie != null && activoRepository.existsByNumeroSerie(serie)) {
            throw conflicto("Ya existe un activo con ese número de serie");
        }

        Activo activo = new Activo();
        aplicarDatos(activo, request);
        activo.setCodigoPatrimonial(codigo);
        activo.setNumeroSerie(serie);
        activo.setEstado(estadoValido(request.estado(), "DISPONIBLE"));
        activo.setCreadoPor(usuarioId);
        // El trigger de la BD registra automáticamente el estado inicial en el historial
        return aRespuesta(activoRepository.saveAndFlush(activo));
    }

    @Transactional
    public ActivoResponse actualizar(Long id, ActivoRequest request, Long usuarioId) {
        Activo activo = activoRepository.findById(id)
                .filter(Activo::isActivoLogico)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));

        String codigo = request.codigoPatrimonial().trim();
        String serie = normalizarSerie(request.numeroSerie());
        if (activoRepository.existsByCodigoPatrimonialAndIdNot(codigo, id)) {
            throw conflicto("Ya existe otro activo con ese código patrimonial");
        }
        if (serie != null && activoRepository.existsByNumeroSerieAndIdNot(serie, id)) {
            throw conflicto("Ya existe otro activo con ese número de serie");
        }

        aplicarDatos(activo, request);
        activo.setCodigoPatrimonial(codigo);
        activo.setNumeroSerie(serie);
        activo.setEstado(estadoValido(request.estado(), activo.getEstado()));
        // Requerido por el trigger de historial cuando cambia el estado
        activo.setModificadoPor(usuarioId);
        return aRespuesta(activoRepository.saveAndFlush(activo));
    }

    private void aplicarDatos(Activo activo, ActivoRequest request) {
        TipoActivo tipo = tipoActivoRepository.findById(request.tipoActivoId())
                .filter(TipoActivo::isActivoLogico)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de activo no válido"));
        Ubicacion ubicacion = ubicacionRepository.findById(request.ubicacionId())
                .filter(Ubicacion::isActivoLogico)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubicación no válida"));

        activo.setTipoActivo(tipo);
        activo.setUbicacion(ubicacion);
        activo.setMarca(request.marca().trim());
        activo.setModelo(request.modelo().trim());
        activo.setDescripcion(request.descripcion());
        activo.setCondicionFisica(condicionValida(request.condicionFisica(), activo.getCondicionFisica()));
    }

    private String normalizarSerie(String serie) {
        return (serie == null || serie.isBlank()) ? null : serie.trim();
    }

    private String estadoValido(String estado, String porDefecto) {
        if (estado == null || estado.isBlank()) {
            return porDefecto;
        }
        String valor = estado.trim().toUpperCase();
        if (!ESTADOS.contains(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: " + estado);
        }
        return valor;
    }

    private String condicionValida(String condicion, String porDefecto) {
        if (condicion == null || condicion.isBlank()) {
            return porDefecto;
        }
        String valor = condicion.trim().toUpperCase();
        if (!CONDICIONES.contains(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Condición física no válida: " + condicion);
        }
        return valor;
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ActivoResponse aRespuesta(Activo a) {
        return new ActivoResponse(
                a.getId(),
                a.getCodigoPatrimonial(),
                a.getNumeroSerie(),
                a.getMarca(),
                a.getModelo(),
                a.getTipoActivo().getCategoria().getNombre(),
                a.getTipoActivo().getNombre(),
                a.getUbicacion().getNombre(),
                a.getEstado(),
                a.getCondicionFisica());
    }
}
