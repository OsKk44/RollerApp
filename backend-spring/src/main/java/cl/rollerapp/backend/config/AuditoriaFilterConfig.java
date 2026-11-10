package cl.rollerapp.backend.config;
import cl.rollerapp.backend.security.AuditoriaHttpFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class AuditoriaFilterConfig {
    @Bean FilterRegistrationBean<AuditoriaHttpFilter> auditoriaFilterRegistration(AuditoriaHttpFilter filtro) {
        FilterRegistrationBean<AuditoriaHttpFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }
}
