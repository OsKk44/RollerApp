package cl.rollerapp.backend.controller;

import cl.rollerapp.backend.model.AuditoriaAdministrativa;
import cl.rollerapp.backend.service.AuditoriaAdministrativaService;
import lombok.RequiredArgsConstructor;
import cl.rollerapp.backend.dto.PaginaResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')") @RequiredArgsConstructor
public class AuditoriaAdministrativaController {
    private final AuditoriaAdministrativaService service;
    private final cl.rollerapp.backend.service.EventoAuditoriaService eventos;
    @GetMapping
    public PaginaResponse<AuditoriaAdministrativa> listar(@RequestParam(defaultValue="0") int pagina,
                                                 @RequestParam(defaultValue="20") int cantidad) {
        return PaginaResponse.desde(service.listar(pagina, cantidad));
    }
    @GetMapping("/eventos")
    public PaginaResponse<cl.rollerapp.backend.model.EventoAuditoria> eventos(
        @RequestParam(defaultValue="0") int pagina,
        @RequestParam(defaultValue="30") int cantidad,
        @RequestParam(required=false) Long actor,
        @RequestParam(required=false) String accion,
        @RequestParam(required=false) String origen,
        @RequestParam(required=false) java.time.LocalDate desde,
        @RequestParam(required=false) java.time.LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Intervalo de fechas inválido");
        if (actor != null && actor <= 0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Actor inválido");
        if (accion != null && !java.util.Set.of("CONSULTAR", "CREAR_O_EJECUTAR", "MODIFICAR", "ELIMINAR").contains(accion))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Acción inválida");
        if (origen != null && !java.util.Set.of("WEB", "MOVIL", "DESCONOCIDO").contains(origen))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Origen inválido");
        return PaginaResponse.desde(eventos.buscar(pagina, cantidad, actor, accion, origen,
            desde == null ? null : desde.atStartOfDay(),
            hasta == null ? null : hasta.plusDays(1).atStartOfDay()));
    }
}
