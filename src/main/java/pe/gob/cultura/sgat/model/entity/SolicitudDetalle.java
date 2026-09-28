package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitud_detalles")
public class SolicitudDetalle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_activo_id", nullable = false)
    private TipoActivo tipoActivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activo_id")
    private Activo activo;

    @Column
    private String observaciones;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Solicitud getSolicitud() { return solicitud; }
    public void setSolicitud(Solicitud solicitud) { this.solicitud = solicitud; }

    public TipoActivo getTipoActivo() { return tipoActivo; }
    public void setTipoActivo(TipoActivo tipoActivo) { this.tipoActivo = tipoActivo; }

    public Activo getActivo() { return activo; }
    public void setActivo(Activo activo) { this.activo = activo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

}
