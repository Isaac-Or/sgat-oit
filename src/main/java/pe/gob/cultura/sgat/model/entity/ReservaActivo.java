package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reservas_activos")
public class ReservaActivo {

    public static final String ACTIVA = "ACTIVA";
    public static final String LIBERADA = "LIBERADA";
    public static final String CONSUMIDA = "CONSUMIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_detalle_id", nullable = false, unique = true)
    private SolicitudDetalle solicitudDetalle;

    @Column(nullable = false, length = 20)
    private String estado = ACTIVA;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_responsable_id", nullable = false)
    private Usuario usuarioResponsable;

    @Column(name = "fecha_finalizacion")
    private OffsetDateTime fechaFinalizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_finalizador_id")
    private Usuario usuarioFinalizador;

    @Column(name = "motivo_finalizacion")
    private String motivoFinalizacion;

    public void liberar(Usuario responsable, String motivo) {
        finalizar(LIBERADA, responsable, motivo);
    }

    /** La futura entrega debe invocar esto en la misma transacción que crea la asignación física. */
    public void consumir(Usuario responsable, String motivo) {
        finalizar(CONSUMIDA, responsable, motivo);
    }

    private void finalizar(String nuevoEstado, Usuario responsable, String motivo) {
        if (!ACTIVA.equals(estado)) {
            throw new IllegalStateException("La reserva ya no está activa");
        }
        estado = nuevoEstado;
        fechaFinalizacion = OffsetDateTime.now();
        usuarioFinalizador = responsable;
        motivoFinalizacion = motivo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Activo getActivo() { return activo; }
    public void setActivo(Activo activo) { this.activo = activo; }

    public SolicitudDetalle getSolicitudDetalle() { return solicitudDetalle; }
    public void setSolicitudDetalle(SolicitudDetalle solicitudDetalle) { this.solicitudDetalle = solicitudDetalle; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Usuario getUsuarioResponsable() { return usuarioResponsable; }
    public void setUsuarioResponsable(Usuario usuarioResponsable) { this.usuarioResponsable = usuarioResponsable; }

    public OffsetDateTime getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(OffsetDateTime fechaFinalizacion) { this.fechaFinalizacion = fechaFinalizacion; }

    public Usuario getUsuarioFinalizador() { return usuarioFinalizador; }
    public void setUsuarioFinalizador(Usuario usuarioFinalizador) { this.usuarioFinalizador = usuarioFinalizador; }

    public String getMotivoFinalizacion() { return motivoFinalizacion; }
    public void setMotivoFinalizacion(String motivoFinalizacion) { this.motivoFinalizacion = motivoFinalizacion; }
}
