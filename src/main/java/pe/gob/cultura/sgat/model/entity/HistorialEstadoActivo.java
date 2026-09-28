package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historial_estados_activo")
public class HistorialEstadoActivo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    @Column(name = "estado_anterior", length = 30)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private String estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cambiado_por", nullable = false)
    private Usuario cambiadoPor;

    @Column(name = "fecha_cambio", insertable = false, updatable = false)
    private OffsetDateTime fechaCambio;

    @Column
    private String motivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Activo getActivo() { return activo; }
    public void setActivo(Activo activo) { this.activo = activo; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Usuario getCambiadoPor() { return cambiadoPor; }
    public void setCambiadoPor(Usuario cambiadoPor) { this.cambiadoPor = cambiadoPor; }

    public OffsetDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(OffsetDateTime fechaCambio) { this.fechaCambio = fechaCambio; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

}
