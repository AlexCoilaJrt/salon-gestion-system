import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

import { environment } from '../../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // Reescribe dinámicamente http://localhost:8080/api a /api cuando se ejecuta en Docker,
  // tablets por Wi-Fi o servidores en la nube para permitir conexiones remotas.
  let targetUrl = req.url;
  if (targetUrl.startsWith('http://localhost:8080/api')) {
    targetUrl = targetUrl.replace('http://localhost:8080/api', environment.apiUrl);
  }

  let headers = req.headers;
  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  const clonedReq = req.clone({
    url: targetUrl,
    headers: headers
  });

  return next(clonedReq).pipe(
    catchError(error => {
      // Si el token expiró o es inválido, limpiar sesión y redirigir al login
      if (error.status === 401 || error.status === 403) {
        authService.clearLocalSession();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
