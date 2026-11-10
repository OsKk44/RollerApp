import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

// Marca el tipo de cliente; la identidad siempre se comprueba mediante el JWT en el servidor.
export const origenAuditoriaInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl + '/')) return next(req);
  return next(req.clone({ setHeaders: { 'X-RollerApp-Cliente': 'web' } }));
};
