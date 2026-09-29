import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const clonedReq = token
    ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
    : req;

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
