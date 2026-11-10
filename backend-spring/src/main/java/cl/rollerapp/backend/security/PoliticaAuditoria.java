package cl.rollerapp.backend.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lista cerrada de operaciones de negocio que merecen un asiento administrativo. */
public final class PoliticaAuditoria {
    private PoliticaAuditoria() {}
    public record Cambio(String accion, String recurso, Long recursoId) {}
    private static final Pattern NUMERO = Pattern.compile("/(?<id>[0-9]+)(?:/|$)");

    public static Cambio clasificar(String metodo, String ruta) {
        if (metodo == null || ruta == null) return null;
        String accion = null;
        String recurso = null;
        // Productos y servicios gestionados tienen auditoría detallada propia.
        if (ruta.matches("/api/productos(?:/[0-9]+(?:/estado)?)?")) return null;
        if (ruta.matches("/api/admin/servicios(?:/[0-9]+(?:/disponibilidad)?)?")) return null;

        if (ruta.matches("/api/telas(?:/[0-9]+(?:/(?:precio|paso-luz))?)?")) {
            recurso = "TELA";
            accion = operacion(metodo, ruta, "ACTUALIZAR_TELA");
        } else if (ruta.matches("/api/mecanismos(?:/[0-9]+)?")) {
            recurso = "MECANISMO";
            accion = operacion(metodo, ruta, "ACTUALIZAR_MECANISMO");
        } else if (ruta.matches("/api/servicios/[0-9]+/comision-tecnico") && "PUT".equals(metodo)) {
            recurso = "SERVICIO"; accion = "CAMBIAR_COMISION_TECNICO";
        } else if (ruta.matches("/api/agenda/admin/bloques") && "POST".equals(metodo)) {
            recurso = "AGENDA_BLOQUE"; accion = "CREAR_BLOQUE";
        } else if (ruta.matches("/api/agenda/admin/bloques/[0-9]+/cancelar") && "PUT".equals(metodo)) {
            recurso = "AGENDA_BLOQUE"; accion = "CANCELAR_BLOQUE";
        } else if (ruta.matches("/api/agenda/bloques/[0-9]+/(?:iniciar|completar)") && "PUT".equals(metodo)) {
            recurso = "AGENDA_BLOQUE"; accion = ruta.endsWith("/iniciar") ? "INICIAR_ATENCION" : "COMPLETAR_ATENCION";
        } else if (ruta.equals("/api/facturacion/campos") && "POST".equals(metodo)) {
            recurso = "CONFIG_FACTURACION"; accion = "CREAR_CAMPO_FACTURACION";
        } else if (ruta.matches("/api/facturacion/campos/[0-9]+") && "PUT".equals(metodo)) {
            recurso = "CONFIG_FACTURACION"; accion = "EDITAR_CAMPO_FACTURACION";
        } else if (ruta.matches("/api/admin/comisiones/[0-9]+/liquidar") && "PATCH".equals(metodo)) {
            recurso = "COMISION"; accion = "LIQUIDAR_COMISION";
        } else if (ruta.matches("/api/admin/comunicaciones/[0-9]+/reintentar") && "POST".equals(metodo)) {
            recurso = "COMUNICACION"; accion = "REINTENTAR_ENVIO";
        }
        if (accion == null) return null;
        Matcher coincidencia = NUMERO.matcher(ruta);
        Long id = 0L;
        if (coincidencia.find()) {
            try { id = Long.valueOf(coincidencia.group("id")); }
            catch (NumberFormatException ignored) { return null; }
        }
        return new Cambio(accion, recurso, id);
    }

    private static String operacion(String metodo, String ruta, String editar) {
        return switch (metodo) {
            case "POST" -> "CREAR_" + (editar.endsWith("TELA") ? "TELA" : "MECANISMO");
            case "PUT", "PATCH" -> editar;
            case "DELETE" -> "ARCHIVAR_" + (editar.endsWith("TELA") ? "TELA" : "MECANISMO");
            default -> null;
        };
    }

    public static boolean registrarSolicitud(String metodo, String ruta, int estado) {
        if (ruta == null || metodo == null) return false;
        // Intentos de autenticación y errores de autorización son eventos de seguridad.
        if (ruta.equals("/api/auth/login") && "POST".equals(metodo)) return true;
        if (estado == 401 || estado == 403) return true;
        // Las consultas normales no son eventos relevantes de auditoría.
        return ("POST".equals(metodo) || "PUT".equals(metodo) || "PATCH".equals(metodo)
            || "DELETE".equals(metodo)) && estado >= 400;
    }
}
