package cl.rollerapp.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Metadatos de auditoría, sin cuerpos, cabeceras de autenticación ni datos personales de formularios. */
@Entity
@Table(name = "auditoria_eventos", indexes = {
    @Index(name = "idx_ae_fecha", columnList = "creado_en"),
    @Index(name = "idx_ae_actor_fecha", columnList = "actor_id,creado_en"),
    @Index(name = "idx_ae_accion_fecha", columnList = "accion,creado_en")
})
public class EventoAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "actor_id") public Long actorId;
    @Column(nullable = false, length = 24) public String accion;
    @Column(nullable = false, length = 12) public String metodo;
    @Column(nullable = false, length = 180) public String recurso;
    @Column(nullable = false) public int estado;
    @Column(name = "duracion_ms", nullable = false) public long duracionMs;
    @Column(nullable = false, length = 12) public String origen;
    @Column(name = "creado_en", nullable = false, updatable = false) public LocalDateTime creadoEn;
    @PrePersist void preparar() { if (creadoEn == null) creadoEn = LocalDateTime.now(); }
}
