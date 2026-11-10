package cl.rollerapp.backend.security;

import cl.rollerapp.backend.service.EventoAuditoriaService;
import cl.rollerapp.backend.service.AuditoriaAdministrativaService;
import cl.rollerapp.backend.service.AuditoriaRegistroHttp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Trazabilidad de API para cualquier cliente; no almacena payloads, IP ni parámetros sensibles. */
@Component @RequiredArgsConstructor
public class AuditoriaHttpFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaHttpFilter.class);
    private final EventoAuditoriaService auditoria;
    private final AuditoriaAdministrativaService cambios;

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/admin/auditoria")
            || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws IOException, ServletException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long actor = auth != null && auth.getPrincipal() instanceof UsuarioPrincipal p ? p.getId() : null;
            String method = request.getMethod();
            String action = switch (method) {
                case "POST" -> "CREAR_O_EJECUTAR";
                case "PUT", "PATCH" -> "MODIFICAR";
                case "DELETE" -> "ELIMINAR";
                default -> "CONSULTAR";
            };
            // Solo se conserva la ruta sin query ni identificadores variables; evita filtrar correos, tokens o códigos.
            String route = request.getRequestURI().replaceAll("(?i)/[0-9a-f]{8}-[0-9a-f-]{27,}", "/:id")
                .replaceAll("/[0-9]+(?=/|$)", "/:id")
                .replaceAll("(?i)/(?:[a-z0-9_-]{32,})(?=/|$)", "/:valor");
            if (route.length() > 180) route = route.substring(0, 180);
            // Cabecera informativa controlada por el cliente: NO es una prueba de identidad ni procedencia.
            String header = request.getHeader("X-RollerApp-Cliente");
            String origen = "movil".equalsIgnoreCase(header) ? "MOVIL"
                : "web".equalsIgnoreCase(header) ? "WEB" : "DESCONOCIDO";
            boolean esAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean exitosa = response.getStatus() >= 200 && response.getStatus() < 300;
            boolean yaRegistrada = Boolean.TRUE.equals(request.getAttribute(AuditoriaRegistroHttp.MARCA));
            PoliticaAuditoria.Cambio cambio = PoliticaAuditoria.clasificar(method, request.getRequestURI());
            if (esAdmin && actor != null && exitosa && !yaRegistrada && cambio != null) {
                try {
                    cambios.registrar(actor, cambio.accion(), cambio.recurso(), cambio.recursoId(),
                        "Operación administrativa confirmada");
                } catch (Exception ex) {
                    log.error("Fallo al persistir cambio administrativo: {} {}", method, route, ex);
                }
            }
            if (PoliticaAuditoria.registrarSolicitud(method, request.getRequestURI(), response.getStatus())) {
                try {
                    auditoria.registrar(actor, action, method, route, response.getStatus(),
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), origen);
                } catch (Exception ex) {
                    log.error("Fallo al persistir auditoría HTTP: {} {}", method, route, ex);
                }
            }
        }
    }
}
