import { Routes } from '@angular/router';
import { LoginComponent } from './features/seguridad/pages/login/login.component';
import { MainLayoutComponent } from './core/layout/main-layout/main-layout.component';
import { RolesComponent } from './features/security/roles/roles.component';
import { UsuariosComponent } from './features/security/usuarios/usuarios.component';
import { authGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: '/roles', pathMatch: 'full' },
      { path: 'roles', component: RolesComponent },
      { path: 'usuarios', component: UsuariosComponent },
      { path: 'empleados', loadComponent: () => import('./features/rrhh/empleados/empleados.component').then(m => m.EmpleadosComponent) },
      { path: 'especialidades', loadComponent: () => import('./features/rrhh/especialidades/especialidades.component').then(m => m.EspecialidadesComponent) },
      { path: 'turnos', loadComponent: () => import('./features/rrhh/turnos/turnos.component').then(m => m.TurnosComponent) },
      { path: 'asistencia', loadComponent: () => import('./features/rrhh/reporte-asistencia/reporte-asistencia.component').then(m => m.ReporteAsistenciaComponent) },
      { path: 'comisiones', loadComponent: () => import('./features/rrhh/comisiones/comisiones.component').then(m => m.ComisionesComponent) },
      { path: 'incentivos', loadComponent: () => import('./features/rrhh/incentivos/incentivos.component').then(m => m.IncentivosComponent) },
      { path: 'monitor-comisiones', loadComponent: () => import('./features/rrhh/monitor-comisiones/monitor-comisiones.component').then(m => m.MonitorComisionesComponent) },
      { path: 'categorias', loadComponent: () => import('./features/catalogo/categorias/categorias.component').then(m => m.CategoriasComponent) },
      { path: 'servicios', loadComponent: () => import('./features/catalogo/servicios/servicios.component').then(m => m.ServiciosComponent) },
      { path: 'productos', loadComponent: () => import('./features/catalogo/productos/productos.component').then(m => m.ProductosComponent) },
      { path: 'insumos', loadComponent: () => import('./features/catalogo/productos/productos.component').then(m => m.ProductosComponent) },
      { path: 'inventario', loadComponent: () => import('./features/catalogo/inventario/inventario.component').then(m => m.InventarioComponent) },
      { path: 'caja', loadComponent: () => import('./features/finanzas/caja/caja.component').then(m => m.CajaComponent) }
    ]
  },
  { path: 'kiosko', loadComponent: () => import('./features/rrhh/asistencia/asistencia.component').then(m => m.AsistenciaComponent) },
  { path: '**', redirectTo: '/login' }
];
