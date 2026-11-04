package pe.gob.cultura.sgat.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.dto.ActivoElegido;
import pe.gob.cultura.sgat.dto.AtencionRequest;
import pe.gob.cultura.sgat.dto.CancelarSolicitudRequest;
import pe.gob.cultura.sgat.dto.DetalleRequest;
import pe.gob.cultura.sgat.dto.DetalleResponse;
import pe.gob.cultura.sgat.dto.SolicitudRequest;
import pe.gob.cultura.sgat.dto.SolicitudResponse;
import pe.gob.cultura.sgat.model.entity.Activo;
import pe.gob.cultura.sgat.model.entity.ReservaActivo;
import pe.gob.cultura.sgat.model.entity.Solicitud;
import pe.gob.cultura.sgat.model.entity.SolicitudDetalle;
import pe.gob.cultura.sgat.model.entity.TipoActivo;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.ActivoRepository;
import pe.gob.cultura.sgat.repository.AsignacionRepository;
import pe.gob.cultura.sgat.repository.ReservaActivoRepository;
import pe.gob.cultura.sgat.repository.SolicitudRepository;
import pe.gob.cultura.sgat.repository.TipoActivoRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final ActivoRepository activoRepository;
    private final AsignacionRepository asignacionRepository;
    private final ReservaActivoRepository reservaActivoRepository;
    private final UsuarioRepository usuarioRepository;

    public SolicitudService(SolicitudRepository solicitudRepository, TipoActivoRepository tipoActivoRepository,
                            ActivoRepository activoRepository, AsignacionRepository asignacionRepository,
                            ReservaActivoRepository reservaActivoRepository, UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.tipoActivoRepository = tipoActivoRepository;
        this.activoRepository = activoRepository;
        this.asignacionRepository = asignacionRepository;
        this.reservaActivoRepository = reservaActivoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Registra la solicitud en estado PENDIENTE (HU-05). 
    @Transactional
    public SolicitudResponse crear(SolicitudRequest request, Long usuarioId) {
        Solicitud solicitud = new Solicitud();
        solicitud.setUsuarioSolicitante(usuarioRepository.getReferenceById(usuarioId));
        solicitud.setMotivo(request.motivo().trim());
        solicitud.setEstado("PENDIENTE");
        solicitud.setFechaSolicitud(OffsetDateTime.now());
        solicitud.setCreadoPor(usuarioId);

        Set<Long> tiposSolicitados = new HashSet<>();
        for (DetalleRequest item : request.detalles()) {
            if (!tiposSolicitados.add(item.tipoActivoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No repita el mismo tipo de equipo en una solicitud");
            }
            TipoActivo tipo = tipoActivoRepository.findById(item.tipoActivoId())
                    .filter(TipoActivo::isActivoLogico)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de activo no válido"));

            // Compuerta del BPM: ¿disponible y sin duplicidad?
            if (!activoRepository.existsDisponibleSinReservaNiAsignacion(tipo.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay equipos disponibles del tipo: " + tipo.getNombre());
            }
            if (solicitudRepository.existePendienteConTipo(usuarioId, tipo.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya tiene una solicitud pendiente para: " + tipo.getNombre());
            }

            SolicitudDetalle detalle = new SolicitudDetalle();
            detalle.setSolicitud(solicitud);
            detalle.setTipoActivo(tipo);
            detalle.setObservaciones(item.observaciones());
            solicitud.getDetalles().add(detalle);
        }
        return aRespuesta(solicitudRepository.saveAndFlush(solicitud));
    }

    // Solicitudes del colaborador autenticado (HU-07). 
    @Transactional(readOnly = true)
    public List<SolicitudResponse> misSolicitudes(Long usuarioId) {
        return solicitudRepository.findByUsuarioSolicitanteIdOrderByFechaSolicitudDesc(usuarioId)
                .stream().map(this::aRespuesta).toList();
    }

    // Bandeja del administrador, con filtro opcional por estado.
    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(String estado) {
        List<Solicitud> solicitudes = (estado == null || estado.isBlank())
                ? solicitudRepository.findAllByOrderByFechaSolicitudDesc()
                : solicitudRepository.findByEstadoOrderByFechaSolicitudDesc(estado.trim().toUpperCase());
        return solicitudes.stream().map(this::aRespuesta).toList();
    }

    // Aprueba reservando el activo de cada detalle, o rechaza sin crear reservas (HU-06).
    @Transactional
    public SolicitudResponse atender(Long id, AtencionRequest request, Long administradorId) {
        Solicitud solicitud = solicitudRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue atendida");
        }

        String observaciones = request.observaciones() == null ? null : request.observaciones().trim();

        if (request.aprobada()) {
            reservarActivos(solicitud, request.activos(), administradorId);
            solicitud.setEstado("APROBADA");
        } else {
            if (observaciones == null || observaciones.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique el motivo del rechazo");
            }
            solicitud.setEstado("RECHAZADA");
        }

        solicitud.setObservacionesAtencion(observaciones);
        solicitud.setUsuarioAtendedor(usuarioRepository.getReferenceById(administradorId));
        solicitud.setFechaAtencion(OffsetDateTime.now());
        solicitud.setModificadoPor(administradorId);
        return aRespuesta(solicitudRepository.saveAndFlush(solicitud));
    }

    @Transactional
    public SolicitudResponse cancelar(Long id, CancelarSolicitudRequest request, Long administradorId) {
        Solicitud solicitud = solicitudRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (!"APROBADA".equals(solicitud.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cancelar una solicitud aprobada");
        }

        List<ReservaActivo> reservas = reservaActivoRepository.findActivasBySolicitudIdForUpdate(id);
        if (reservas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La solicitud no tiene reservas activas para liberar");
        }

        String motivo = request.motivo().trim();
        Usuario administrador = usuarioRepository.getReferenceById(administradorId);
        reservas.forEach(reserva -> reserva.liberar(administrador, motivo));
        reservaActivoRepository.saveAll(reservas);

        solicitud.setEstado("CANCELADA");
        solicitud.setModificadoPor(administradorId);
        String observacionAnterior = solicitud.getObservacionesAtencion();
        solicitud.setObservacionesAtencion(
                (observacionAnterior == null || observacionAnterior.isBlank())
                        ? "Cancelación: " + motivo
                        : observacionAnterior + "\nCancelación: " + motivo);
        return aRespuesta(solicitudRepository.saveAndFlush(solicitud));
    }

    private void reservarActivos(
            Solicitud solicitud, List<ActivoElegido> elegidos, Long administradorId) {
        if (elegidos == null || elegidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione el activo para cada detalle");
        }
        Map<Long, Long> activoPorDetalle = new HashMap<>();
        for (ActivoElegido e : elegidos) {
            if (activoPorDetalle.put(e.detalleId(), e.activoId()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No repita un detalle en la selección de activos");
            }
        }
        if (new HashSet<>(activoPorDetalle.values()).size() != activoPorDetalle.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede asignar el mismo activo dos veces");
        }

        Set<Long> detalleIds = solicitud.getDetalles().stream()
                .map(SolicitudDetalle::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (!detalleIds.equals(activoPorDetalle.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar exactamente un activo para cada detalle de la solicitud");
        }

        List<Long> activoIds = activoPorDetalle.values().stream().sorted().toList();
        List<Activo> activosBloqueados = activoRepository.findAllByIdForUpdate(activoIds);
        if (activosBloqueados.size() != activoIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uno de los activos no existe");
        }
        Map<Long, Activo> activosPorId = activosBloqueados.stream()
                .collect(java.util.stream.Collectors.toMap(Activo::getId, activo -> activo));
        Usuario administrador = usuarioRepository.getReferenceById(administradorId);
        List<ReservaActivo> nuevasReservas = new ArrayList<>();

        for (SolicitudDetalle detalle : solicitud.getDetalles()) {
            Long activoId = activoPorDetalle.get(detalle.getId());
            Activo activo = activosPorId.get(activoId);
            if (!activo.isActivoLogico() || !"DISPONIBLE".equals(activo.getEstado())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El activo " + activo.getCodigoPatrimonial() + " no está disponible");
            }
            if (reservaActivoRepository.existsByActivoIdAndEstado(activoId, ReservaActivo.ACTIVA)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El activo " + activo.getCodigoPatrimonial() + " ya está reservado");
            }
            if (asignacionRepository.existsByActivoIdAndFechaDevolucionRealIsNull(activoId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El activo " + activo.getCodigoPatrimonial() + " tiene una asignación física abierta");
            }
            if (!activo.getTipoActivo().getId().equals(detalle.getTipoActivo().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El activo " + activo.getCodigoPatrimonial() + " no corresponde al tipo solicitado");
            }
            detalle.setActivo(activo);

            ReservaActivo reserva = new ReservaActivo();
            reserva.setActivo(activo);
            reserva.setSolicitudDetalle(detalle);
            reserva.setUsuarioResponsable(administrador);
            nuevasReservas.add(reserva);
        }
        reservaActivoRepository.saveAllAndFlush(nuevasReservas);
    }

    private SolicitudResponse aRespuesta(Solicitud s) {
        Usuario solicitante = s.getUsuarioSolicitante();
        List<DetalleResponse> detalles = s.getDetalles().stream()
                .map(d -> new DetalleResponse(
                        d.getId(),
                        d.getTipoActivo().getId(),
                        d.getTipoActivo().getNombre(),
                        d.getActivo() == null ? null : d.getActivo().getCodigoPatrimonial(),
                        d.getObservaciones()))
                .toList();
        return new SolicitudResponse(
                s.getId(),
                solicitante.getNombres() + " " + solicitante.getApellidos(),
                s.getFechaSolicitud(),
                s.getFechaAtencion(),
                s.getMotivo(),
                s.getEstado(),
                s.getObservacionesAtencion(),
                detalles);
    }
}
