package pe.gob.cultura.sgat.service;

import java.util.List;
import java.util.HashSet;
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
import pe.gob.cultura.sgat.repository.AsignacionRepository;
import pe.gob.cultura.sgat.repository.HistorialEstadoActivoRepository;
import pe.gob.cultura.sgat.repository.ReservaActivoRepository;
import pe.gob.cultura.sgat.repository.TipoActivoRepository;
import pe.gob.cultura.sgat.repository.UbicacionRepository;
import pe.gob.cultura.sgat.dto.HistorialResponse;

@Service
public class ActivoService {

    private static final Set<String> ESTADOS = Set.of("DISPONIBLE", "ASIGNADO", "EN_MANTENIMIENTO", "DE_BAJA");
    private static final Set<String> CONDICIONES = Set.of("BUENO", "REGULAR", "MALO");

    private final ActivoRepository activoRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final HistorialEstadoActivoRepository historialRepository;
    private final ReservaActivoRepository reservaActivoRepository;
    private final AsignacionRepository asignacionRepository;

    public ActivoService(ActivoRepository activoRepository, TipoActivoRepository tipoActivoRepository,
                         UbicacionRepository ubicacionRepository, HistorialEstadoActivoRepository historialRepository,
                         ReservaActivoRepository reservaActivoRepository,
                         AsignacionRepository asignacionRepository) {
        this.activoRepository = activoRepository;
        this.tipoActivoRepository = tipoActivoRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.historialRepository = historialRepository;
        this.reservaActivoRepository = reservaActivoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivoResponse> listar(String estado) {
        List<Activo> activos = (estado == null || estado.isBlank())
                ? activoRepository.findByActivoLogicoTrueOrderByCodigoPatrimonialAsc()
                : activoRepository.findByEstadoAndActivoLogicoTrueOrderByCodigoPatrimonialAsc(estado.trim().toUpperCase());
        Set<Long> reservados = new HashSet<>(reservaActivoRepository.findIdsActivosConReservaActiva());
        return activos.stream().map(a -> aRespuesta(a, reservados.contains(a.getId()))).toList();
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
        String estadoInicial = estadoValido(request.estado(), "DISPONIBLE");
        if ("ASIGNADO".equals(estadoInicial)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estado ASIGNADO solo se establece mediante una entrega física");
        }
        activo.setEstado(estadoInicial);
        activo.setCreadoPor(usuarioId);
        // El trigger de la BD registra automáticamente el estado inicial en el historial
        return aRespuesta(activoRepository.saveAndFlush(activo), false);
    }

    @Transactional
    public ActivoResponse actualizar(Long id, ActivoRequest request, Long usuarioId) {
        Activo activo = activoRepository.findByIdForUpdate(id)
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

        String estadoAnterior = activo.getEstado();
        String estadoSolicitado = estadoValido(request.estado(), estadoAnterior);
        boolean reservaActiva = reservaActivoRepository.existsByActivoIdAndEstado(
                id, pe.gob.cultura.sgat.model.entity.ReservaActivo.ACTIVA);
        boolean asignacionAbierta = asignacionRepository
                .existsByActivoIdAndFechaDevolucionRealIsNull(id);
        boolean tieneAsignaciones = asignacionRepository.existsByActivoId(id);
        boolean cambiaTipo = !activo.getTipoActivo().getId().equals(request.tipoActivoId());

        if (!"ASIGNADO".equals(estadoAnterior) && "ASIGNADO".equals(estadoSolicitado)) {
            throw conflicto("El estado ASIGNADO solo se establece mediante una entrega física");
        }
        if (asignacionAbierta && !"ASIGNADO".equals(estadoSolicitado)) {
            throw conflicto("El activo tiene una asignación abierta; registre primero la devolución");
        }
        if (reservaActiva && !"DISPONIBLE".equals(estadoSolicitado)) {
            throw conflicto("El activo reservado debe conservar el estado DISPONIBLE");
        }
        if (cambiaTipo && (reservaActiva || tieneAsignaciones)) {
            throw conflicto("No se puede cambiar el tipo de un activo reservado o con historial de préstamos");
        }

        aplicarDatos(activo, request);
        activo.setCodigoPatrimonial(codigo);
        activo.setNumeroSerie(serie);
        activo.setEstado(estadoSolicitado);
        // Requerido por el trigger de historial cuando cambia el estado
        activo.setModificadoPor(usuarioId);
        Activo actualizado = activoRepository.saveAndFlush(activo);
        boolean reservado = reservaActivoRepository.existsByActivoIdAndEstado(
                actualizado.getId(), pe.gob.cultura.sgat.model.entity.ReservaActivo.ACTIVA);
        return aRespuesta(actualizado, reservado);
    }
        @Transactional(readOnly = true)
    public List<HistorialResponse> historial(Long activoId) {
        if (!activoRepository.existsById(activoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado");
        }
        return historialRepository.findByActivoIdOrderByFechaCambioDesc(activoId).stream()
                .map(h -> new HistorialResponse(
                        h.getId(),
                        h.getEstadoAnterior(),
                        h.getEstadoNuevo(),
                        h.getFechaCambio(),
                        h.getMotivo(),
                        h.getCambiadoPor().getNombres() + " " + h.getCambiadoPor().getApellidos()))
                .toList();
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
        if (request.descripcion() != null || activo.getId() == null) {
            activo.setDescripcion(request.descripcion());
        }
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

    private ActivoResponse aRespuesta(Activo a, boolean reservado) {
        return new ActivoResponse(
                a.getId(),
                a.getTipoActivo().getId(),
                a.getUbicacion().getId(),
                a.getCodigoPatrimonial(),
                a.getNumeroSerie(),
                a.getMarca(),
                a.getModelo(),
                a.getDescripcion(),
                a.getTipoActivo().getCategoria().getNombre(),
                a.getTipoActivo().getNombre(),
                a.getUbicacion().getNombre(),
                a.getEstado(),
                a.getCondicionFisica(),
                reservado);
    }
}
