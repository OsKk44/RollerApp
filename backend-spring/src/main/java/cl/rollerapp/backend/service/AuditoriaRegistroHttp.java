package cl.rollerapp.backend.service;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Evita crear un segundo asiento genérico si ya existe un registro detallado. */
public final class AuditoriaRegistroHttp {
    public static final String MARCA = AuditoriaRegistroHttp.class.getName() + ".registrado";
    private AuditoriaRegistroHttp() {}
    public static void marcarRegistrado() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes request) {
            request.getRequest().setAttribute(MARCA, Boolean.TRUE);
        }
    }
}
