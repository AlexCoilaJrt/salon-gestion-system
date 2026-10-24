import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guardián de permisos dinámicos por ruta.
 * - Si el usuario es ADMIN, siempre tiene pase libre total.
 * - Para cualquier otro rol, valida si cuenta con el permiso dinámico requerido en route.data['permission'].
 * - Si no cuenta con el permiso, lo redirige de inmediato a /dashboard.
 */
export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Verificar autenticación
  if (!authService.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const user = authService.getCurrentUser();
  const userRoles: string[] = user?.roles || [];
  const userPermissions: string[] = user?.permissions || [];

  // 2. REGLA CLAVE: El rol ADMIN siempre tiene acceso total a todos los módulos
  const isAdmin = userRoles.some(r => {
    const rolUpper = String(r).toUpperCase();
    return rolUpper === 'ADMIN' || rolUpper === 'ROLE_ADMIN' || rolUpper === 'ADMINISTRADOR';
  });

  if (isAdmin) {
    return true;
  }

  // 3. Permiso requerido configurado en la ruta
  const requiredPermission = route.data['permission'] as string | undefined;
  const alternativePermissions = route.data['permissions'] as string[] | undefined;

  // Si la ruta no exige ningún permiso (ej. dashboard), se permite el acceso
  if (!requiredPermission && (!alternativePermissions || alternativePermissions.length === 0)) {
    return true;
  }

  // 4. Validar dinámicamente si el rol del usuario cuenta con el permiso
  let hasAccess = false;
  if (requiredPermission && userPermissions.includes(requiredPermission)) {
    hasAccess = true;
  } else if (alternativePermissions && alternativePermissions.some(p => userPermissions.includes(p))) {
    hasAccess = true;
  }

  if (hasAccess) {
    return true;
  }

  // 5. Acceso denegado: redirigir a dashboard
  console.warn(`[PermissionGuard] Acceso restringido a '${state.url}'. Requiere privilegio: ${requiredPermission || alternativePermissions?.join(', ')}`);
  return router.createUrlTree(['/dashboard']);
};
