package pe.gob.cultura.sgat.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.cultura.sgat.dto.AsignacionResponse;
import pe.gob.cultura.sgat.dto.DevolucionAsignacionRequest;
import pe.gob.cultura.sgat.dto.EntregaSolicitudRequest;
import pe.gob.cultura.sgat.model.entity.Activo;
import pe.gob.cultura.sgat.model.entity.Asignacion;
import pe.gob.cultura.sgat.model.entity.ReservaActivo;
import pe.gob.cultura.sgat.model.entity.Solicitud;
import pe.gob.cultura.sgat.model.entity.Usuario;
import pe.gob.cultura.sgat.repository.ActivoRepository;
import pe.gob.cultura.sgat.repository.AsignacionRepository;
import pe.gob.cultura.sgat.repository.ReservaActivoRepository;
import pe.gob.cultura.sgat.repository.SolicitudRepository;
import pe.gob.cultura.sgat.repository.UsuarioRepository;

@Service
public class AsignacionService {

    private final SolicitudRepository solicitudRepository;
    private final ReservaActivoRepository reservaRepository;
    private final ActivoRepository activoRepository;
    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;

    public AsignacionService(SolicitudRepository solicitudRepository,
                             ReservaActivoRepository reservaRepository,
                             ActivoRepository activoRepository,
                             AsignacionRepository asignacionRepository,
                             UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.reservaRepository = reservaRepository;
        this.activoRepository = activoRepository;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public List<AsignacionResponse> entregar(
            Long solicitudId, EntregaSolicitudRequest request, Long administradorId) {
        Solicitud solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (!"APROBADA".equals(solicitud.getEstado())) {
            throw conflicto("Solo se puede entregar una solicitud aprobada");
        }

        OffsetDateTime fechaEntrega = OffsetDateTime.now();
        if (!request.fechaDevolucionEstimada().isAfter(fechaEntrega)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha estimada de devolución debe ser posterior a la entrega");
        }

        List<ReservaActivo> reservas = reservaRepository
                .findActivasBySolicitudIdForUpdate(solicitudId);
        Set<Long> detallesEsperados = solicitud.getDetalles().stream()
                .map(detalle -> detalle.getId())
                .collect(Collectors.toSet());
        Set<Long> detallesReservados = reservas.stream()
                .map(reserva -> reserva.getSolicitudDetalle().getId())
                .collect(Collectors.toSet());
        if (reservas.size() != solicitud.getDetalles().size()
                || !detallesEsperados.equals(detallesReservados)) {
            throw conflicto("Cada detalle debe conservar una reserva activa antes de la entrega");
        }

        List<Long> activoIds = reservas.stream()
                .map(reserva -> reserva.getActivo().getId())
                .sorted()
                .toList();
        if (new HashSet<>(activoIds).size() != activoIds.size()) {
            throw conflicto("La solicitud contiene reservas duplicadas para un activo");
        }
        List<Activo> activos = activoRepository.findAllByIdForUpdate(activoIds);
        if (activos.size() != activoIds.size()) {
            throw conflicto("Uno de los activos reservados ya no existe");
        }

        Usuario receptor = solicitud.getUsuarioSolicitante();
        if (!receptor.isActivoLogico() || !receptor.isHabilitado()) {
            throw conflicto("El receptor de la solicitud no tiene una cuenta vigente");
        }
        Usuario administrador = usuarioRepository.getReferenceById(administradorId);
        String observaciones = normalizar(request.observaciones());
        List<Asignacion> asignaciones = new ArrayList<>();

        for (ReservaActivo reserva : reservas) {
            Activo activo = activos.stream()
                    .filter(item -> item.getId().equals(reserva.getActivo().getId()))
                    .findFirst()
                    .orElseThrow(() -> conflicto("El activo reservado ya no existe"));
            if (!activo.isActivoLogico() || !"DISPONIBLE".equals(activo.getEstado())) {
                throw conflicto("El activo " + activo.getCodigoPatrimonial()
                        + " ya no está disponible para su entrega");
            }
            if (asignacionRepository.existsByActivoIdAndFechaDevolucionRealIsNull(activo.getId())) {
                throw conflicto("El activo " + activo.getCodigoPatrimonial()
                        + " ya tiene una asignación física abierta");
            }

            Asignacion asignacion = new Asignacion();
            asignacion.setSolicitudDetalle(reserva.getSolicitudDetalle());
            asignacion.setActivo(activo);
            asignacion.setUsuarioReceptor(receptor);
            asignacion.setEntregadoPor(administrador);
            asignacion.setFechaEntrega(fechaEntrega);
            asignacion.setFechaDevolucionEstimada(request.fechaDevolucionEstimada());
            asignacion.setObservacionesEntrega(observaciones);
            asignacion.setCreadoPor(administradorId);
            asignaciones.add(asignacion);

            reserva.consumir(administrador,
                    "Entrega física de la solicitud " + solicitudId, fechaEntrega);
            activo.setEstado("ASIGNADO");
            activo.setModificadoPor(administradorId);
        }

        reservaRepository.saveAll(reservas);
        activoRepository.saveAll(activos);
        return asignacionRepository.saveAllAndFlush(asignaciones).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public AsignacionResponse devolver(
            Long asignacionId, DevolucionAsignacionRequest request, Long administradorId) {
        Asignacion asignacion = asignacionRepository.findByIdForUpdate(asignacionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Asignación no encontrada"));
        if (asignacion.getFechaDevolucionReal() != null) {
            throw conflicto("La asignación ya fue devuelta");
        }

        Activo activo = activoRepository.findByIdForUpdate(asignacion.getActivo().getId())
                .orElseThrow(() -> conflicto("El activo de la asignación ya no existe"));
        if (!"ASIGNADO".equals(activo.getEstado())) {
            throw conflicto("El activo no se encuentra en estado ASIGNADO");
        }
        if ("MALO".equals(request.condicionFisica())
                && "DISPONIBLE".equals(request.estadoActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un activo en condición MALO debe pasar a mantenimiento");
        }

        Usuario administrador = usuarioRepository.getReferenceById(administradorId);
        asignacion.setFechaDevolucionReal(OffsetDateTime.now());
        asignacion.setRecibidoDevolucionPor(administrador);
        asignacion.setObservacionesRetorno(normalizar(request.observaciones()));
        asignacion.setModificadoPor(administradorId);
        activo.setCondicionFisica(request.condicionFisica());
        activo.setEstado(request.estadoActivo());
        activo.setModificadoPor(administradorId);

        activoRepository.save(activo);
        return aRespuesta(asignacionRepository.saveAndFlush(asignacion));
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponse> listar(boolean soloAbiertas) {
        List<Asignacion> asignaciones = soloAbiertas
                ? asignacionRepository.findByFechaDevolucionRealIsNullOrderByFechaEntregaDesc()
                : asignacionRepository.findAllByOrderByFechaEntregaDesc();
        return asignaciones.stream().map(this::aRespuesta).toList();
    }

    private AsignacionResponse aRespuesta(Asignacion asignacion) {
        Activo activo = asignacion.getActivo();
        return new AsignacionResponse(
                asignacion.getId(),
                asignacion.getSolicitudDetalle().getSolicitud().getId(),
                asignacion.getSolicitudDetalle().getId(),
                activo.getId(), activo.getCodigoPatrimonial(),
                activo.getTipoActivo().getNombre(),
                asignacion.getUsuarioReceptor().getId(),
                nombre(asignacion.getUsuarioReceptor()),
                nombre(asignacion.getEntregadoPor()),
                asignacion.getRecibidoDevolucionPor() == null
                        ? null : nombre(asignacion.getRecibidoDevolucionPor()),
                asignacion.getFechaEntrega(), asignacion.getFechaDevolucionEstimada(),
                asignacion.getFechaDevolucionReal(), asignacion.getObservacionesEntrega(),
                asignacion.getObservacionesRetorno(), activo.getEstado(),
                activo.getCondicionFisica(), asignacion.getFechaDevolucionReal() == null);
    }

    private String nombre(Usuario usuario) {
        return usuario.getNombres() + " " + usuario.getApellidos();
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}
