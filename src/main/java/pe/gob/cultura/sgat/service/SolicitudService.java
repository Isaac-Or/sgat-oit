package pe.gob.cultura.sgat.service;

import java.time.OffsetDateTime;
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
import pe.gob.cultura.sgat.dto.DetalleRequest;
import pe.gob.cultura.sgat.dto.DetalleResponse;
import pe.gob.cultura.sgat.dto.SolicitudRequest;
import pe.gob.cultura.sgat.dto.SolicitudResponse;
import pe.gob.cultura.sgat.model.entity.Activo;
import pe.gob.cultura.sgat.model.entity.Solicitud;
import pe.gob.cultura.sgat.model.entity.SolicitudDetalle;
import pe.gob.cultura.sgat.model.entity.TipoActivo;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.ActivoRepository;
import pe.gob.cultura.sgat.repository.SolicitudRepository;
import pe.gob.cultura.sgat.repository.TipoActivoRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;

    public SolicitudService(SolicitudRepository solicitudRepository, TipoActivoRepository tipoActivoRepository,
                            ActivoRepository activoRepository, UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.tipoActivoRepository = tipoActivoRepository;
        this.activoRepository = activoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Registra la solicitud en estado PENDIENTE (HU-05). */
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
            if (!activoRepository.existsByTipoActivoIdAndEstadoAndActivoLogicoTrue(tipo.getId(), "DISPONIBLE")) {
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

    /** Solicitudes del colaborador autenticado (HU-07). */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> misSolicitudes(Long usuarioId) {
        return solicitudRepository.findByUsuarioSolicitanteIdOrderByFechaSolicitudDesc(usuarioId)
                .stream().map(this::aRespuesta).toList();
    }

    /** Bandeja del administrador, con filtro opcional por estado. */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(String estado) {
        List<Solicitud> solicitudes = (estado == null || estado.isBlank())
                ? solicitudRepository.findAllByOrderByFechaSolicitudDesc()
                : solicitudRepository.findByEstadoOrderByFechaSolicitudDesc(estado.trim().toUpperCase());
        return solicitudes.stream().map(this::aRespuesta).toList();
    }

    /** Aprueba (eligiendo el activo físico de cada detalle) o rechaza una solicitud (HU-06). */
    @Transactional
    public SolicitudResponse atender(Long id, AtencionRequest request, Long administradorId) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue atendida");
        }

        String observaciones = request.observaciones() == null ? null : request.observaciones().trim();

        if (request.aprobada()) {
            asignarActivos(solicitud, request.activos());
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

    private void asignarActivos(Solicitud solicitud, List<ActivoElegido> elegidos) {
        if (elegidos == null || elegidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione el activo para cada detalle");
        }
        Map<Long, Long> activoPorDetalle = new HashMap<>();
        for (ActivoElegido e : elegidos) {
            activoPorDetalle.put(e.detalleId(), e.activoId());
        }
        if (new HashSet<>(activoPorDetalle.values()).size() != activoPorDetalle.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede asignar el mismo activo dos veces");
        }

        for (SolicitudDetalle detalle : solicitud.getDetalles()) {
            Long activoId = activoPorDetalle.get(detalle.getId());
            if (activoId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Falta seleccionar el activo del detalle " + detalle.getId());
            }
            Activo activo = activoRepository.findById(activoId)
                    .filter(Activo::isActivoLogico)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado: " + activoId));
            if (!"DISPONIBLE".equals(activo.getEstado())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El activo " + activo.getCodigoPatrimonial() + " no está disponible");
            }
            if (!activo.getTipoActivo().getId().equals(detalle.getTipoActivo().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El activo " + activo.getCodigoPatrimonial() + " no corresponde al tipo solicitado");
            }
            detalle.setActivo(activo);
        }
    }

    private SolicitudResponse aRespuesta(Solicitud s) {
        Usuario solicitante = s.getUsuarioSolicitante();
        List<DetalleResponse> detalles = s.getDetalles().stream()
                .map(d -> new DetalleResponse(
                        d.getId(),
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
