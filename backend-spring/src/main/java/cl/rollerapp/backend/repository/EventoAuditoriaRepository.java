package cl.rollerapp.backend.repository;

import cl.rollerapp.backend.model.EventoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    @Query("SELECT e FROM EventoAuditoria e WHERE (:actor IS NULL OR e.actorId = :actor) " +
           "AND (:accion IS NULL OR e.accion = :accion) " +
           "AND (:origen IS NULL OR e.origen = :origen) " +
           "AND (:desde IS NULL OR e.creadoEn >= :desde) " +
           "AND (:hasta IS NULL OR e.creadoEn < :hasta) " +
           "ORDER BY e.creadoEn DESC, e.id DESC")
    Page<EventoAuditoria> buscar(@Param("actor") Long actor, @Param("accion") String accion,
        @Param("origen") String origen, @Param("desde") java.time.LocalDateTime desde,
        @Param("hasta") java.time.LocalDateTime hasta, Pageable pagina);
}
