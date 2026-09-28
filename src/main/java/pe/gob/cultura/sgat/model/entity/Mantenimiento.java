package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "mantenimientos")
public class Mantenimiento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_responsable_id")
    private Usuario tecnicoResponsable;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String estado = "PROGRAMADO";

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @Column
    private String diagnostico;

    @Column(name = "acciones_realizadas")
    private String accionesRealizadas;

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

    public Activo getActivo() { return activo; }
    public void setActivo(Activo activo) { this.activo = activo; }

    public Usuario getTecnicoResponsable() { return tecnicoResponsable; }
    public void setTecnicoResponsable(Usuario tecnicoResponsable) { this.tecnicoResponsable = tecnicoResponsable; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public OffsetDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(OffsetDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public OffsetDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(OffsetDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getAccionesRealizadas() { return accionesRealizadas; }
    public void setAccionesRealizadas(String accionesRealizadas) { this.accionesRealizadas = accionesRealizadas; }

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
