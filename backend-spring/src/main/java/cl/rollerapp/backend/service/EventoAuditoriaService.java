package cl.rollerapp.backend.service;

import cl.rollerapp.backend.model.EventoAuditoria;
import cl.rollerapp.backend.repository.EventoAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class EventoAuditoriaService {
    private final EventoAuditoriaRepository repository;

    /** La auditoría no depende de la transacción de negocio: registra también respuestas fallidas. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long actorId, String accion, String metodo, String recurso,
                         int estado, long duracionMs, String origen) {
        EventoAuditoria e = new EventoAuditoria();
        e.actorId = actorId;
        e.accion = accion;
        e.metodo = metodo;
        e.recurso = recurso;
        e.estado = estado;
        e.duracionMs = Math.max(0, duracionMs);
        e.origen = origen;
        repository.save(e);
    }

    @Transactional(readOnly = true)
    public Page<EventoAuditoria> buscar(int pagina, int cantidad, Long actor, String accion,
                                       String origen, LocalDateTime desde, LocalDateTime hasta) {
        return repository.buscar(actor, accion, origen, desde, hasta,
            PageRequest.of(Math.max(0, pagina), Math.max(1, Math.min(100, cantidad))));
    }
}
