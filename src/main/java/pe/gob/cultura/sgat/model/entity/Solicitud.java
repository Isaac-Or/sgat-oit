package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_solicitante_id", nullable = false)
    private Usuario usuarioSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_atendedor_id")
    private Usuario usuarioAtendedor;

    @Column(name = "fecha_solicitud", nullable = false)
    private OffsetDateTime fechaSolicitud;

    @Column(name = "fecha_atencion")
    private OffsetDateTime fechaAtencion;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "observaciones_atencion")
    private String observacionesAtencion;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", insertable = false, updatable = false)
    private OffsetDateTime fechaModificacion;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "modificado_por")
    private Long modificadoPor;

    @Column(name = "activo_logico", nullable = false)
    private boolean activoLogico = true;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolicitudDetalle> detalles = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuarioSolicitante() { return usuarioSolicitante; }
    public void setUsuarioSolicitante(Usuario usuarioSolicitante) { this.usuarioSolicitante = usuarioSolicitante; }

    public Usuario getUsuarioAtendedor() { return usuarioAtendedor; }
    public void setUsuarioAtendedor(Usuario usuarioAtendedor) { this.usuarioAtendedor = usuarioAtendedor; }

    public OffsetDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(OffsetDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public OffsetDateTime getFechaAtencion() { return fechaAtencion; }
    public void setFechaAtencion(OffsetDateTime fechaAtencion) { this.fechaAtencion = fechaAtencion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacionesAtencion() { return observacionesAtencion; }
    public void setObservacionesAtencion(String observacionesAtencion) { this.observacionesAtencion = observacionesAtencion; }

    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public OffsetDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(OffsetDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public Long getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Long creadoPor) { this.creadoPor = creadoPor; }

    public Long getModificadoPor() { return modificadoPor; }
    public void setModificadoPor(Long modificadoPor) { this.modificadoPor = modificadoPor; }

    public boolean isActivoLogico() { return activoLogico; }
    public void setActivoLogico(boolean activoLogico) { this.activoLogico = activoLogico; }

    public List<SolicitudDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<SolicitudDetalle> detalles) { this.detalles = detalles; }

}
