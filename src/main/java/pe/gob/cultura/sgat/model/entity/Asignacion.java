package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "asignaciones")
public class Asignacion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_detalle_id", nullable = false, unique = true)
    private SolicitudDetalle solicitudDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_receptor_id", nullable = false)
    private Usuario usuarioReceptor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entregado_por", nullable = false)
    private Usuario entregadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recibido_devolucion_por")
    private Usuario recibidoDevolucionPor;

    @Column(name = "fecha_entrega", nullable = false)
    private OffsetDateTime fechaEntrega;

    @Column(name = "fecha_devolucion_estimada", nullable = false)
    private OffsetDateTime fechaDevolucionEstimada;

    @Column(name = "fecha_devolucion_real")
    private OffsetDateTime fechaDevolucionReal;

    @Column(name = "observaciones_entrega")
    private String observacionesEntrega;

    @Column(name = "observaciones_retorno")
    private String observacionesRetorno;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SolicitudDetalle getSolicitudDetalle() { return solicitudDetalle; }
    public void setSolicitudDetalle(SolicitudDetalle solicitudDetalle) { this.solicitudDetalle = solicitudDetalle; }

    public Activo getActivo() { return activo; }
    public void setActivo(Activo activo) { this.activo = activo; }

    public Usuario getUsuarioReceptor() { return usuarioReceptor; }
    public void setUsuarioReceptor(Usuario usuarioReceptor) { this.usuarioReceptor = usuarioReceptor; }

    public Usuario getEntregadoPor() { return entregadoPor; }
    public void setEntregadoPor(Usuario entregadoPor) { this.entregadoPor = entregadoPor; }

    public Usuario getRecibidoDevolucionPor() { return recibidoDevolucionPor; }
    public void setRecibidoDevolucionPor(Usuario recibidoDevolucionPor) { this.recibidoDevolucionPor = recibidoDevolucionPor; }

    public OffsetDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(OffsetDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public OffsetDateTime getFechaDevolucionEstimada() { return fechaDevolucionEstimada; }
    public void setFechaDevolucionEstimada(OffsetDateTime fechaDevolucionEstimada) { this.fechaDevolucionEstimada = fechaDevolucionEstimada; }

    public OffsetDateTime getFechaDevolucionReal() { return fechaDevolucionReal; }
    public void setFechaDevolucionReal(OffsetDateTime fechaDevolucionReal) { this.fechaDevolucionReal = fechaDevolucionReal; }

    public String getObservacionesEntrega() { return observacionesEntrega; }
    public void setObservacionesEntrega(String observacionesEntrega) { this.observacionesEntrega = observacionesEntrega; }

    public String getObservacionesRetorno() { return observacionesRetorno; }
    public void setObservacionesRetorno(String observacionesRetorno) { this.observacionesRetorno = observacionesRetorno; }

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

}
