package pe.gob.cultura.sgat.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tipos_activo")
public class TipoActivo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column
    private String descripcion;

    @Column(name = "activo_logico", nullable = false)
    private boolean activoLogico = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isActivoLogico() { return activoLogico; }
    public void setActivoLogico(boolean activoLogico) { this.activoLogico = activoLogico; }

}
