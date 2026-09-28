package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "activos")
public class Activo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_activo_id", nullable = false)
    private TipoActivo tipoActivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(name = "codigo_patrimonial", nullable = false, unique = true, length = 50)
    private String codigoPatrimonial;

    @Column(name = "numero_serie", unique = true, length = 100)
    private String numeroSerie;

    @Column(nullable = false, length = 100)
    private String marca;

    @Column(nullable = false, length = 150)
    private String modelo;

    @Column
    private String descripcion;

    @Column(nullable = false, length = 30)
    private String estado = "DISPONIBLE";

    @Column(name = "condicion_fisica", nullable = false, length = 20)
    private String condicionFisica = "BUENO";

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", insertable = false, updatable = false)
    private OffsetDateTime fechaModificacion;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "modificado_por")
    private Long modificadoPor;

    @Column(name = "activo_logico", nullable = false)
    private boolean activoLogico = true;

    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoActivo getTipoActivo() { return tipoActivo; }
    public void setTipoActivo(TipoActivo tipoActivo) { this.tipoActivo = tipoActivo; }

    public Ubicacion getUbicacion() { return ubicacion; }
    public void setUbicacion(Ubicacion ubicacion) { this.ubicacion = ubicacion; }

    public String getCodigoPatrimonial() { return codigoPatrimonial; }
    public void setCodigoPatrimonial(String codigoPatrimonial) { this.codigoPatrimonial = codigoPatrimonial; }

    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCondicionFisica() { return condicionFisica; }
    public void setCondicionFisica(String condicionFisica) { this.condicionFisica = condicionFisica; }

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

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

}
