import { Routes } from '@angular/router';
import { LoginComponent } from './features/seguridad/pages/login/login.component';
import { MainLayoutComponent } from './core/layout/main-layout/main-layout.component';
import { RolesComponent } from './features/security/roles/roles.component';
import { UsuariosComponent } from './features/security/usuarios/usuarios.component';
import { authGuard } from './core/auth/guards/auth.guard';
import { permissionGuard } from './core/auth/guards/permission.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
      { 
        path: 'dashboard', 
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        canActivate: [permissionGuard]
      },
      { 
        path: 'roles', 
        component: RolesComponent,
        canActivate: [permissionGuard],
        data: { permission: 'VER_ROLES_Y_PERMISOS' }
      },
      { 
        path: 'usuarios', 
        component: UsuariosComponent,
        canActivate: [permissionGuard],
        data: { permission: 'VER_USUARIOS' }
      },
      { 
        path: 'empleados', 
        loadComponent: () => import('./features/rrhh/empleados/empleados.component').then(m => m.EmpleadosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_GESTION_DE_EMPLEADOS' }
      },
      { 
        path: 'especialidades', 
        loadComponent: () => import('./features/rrhh/especialidades/especialidades.component').then(m => m.EspecialidadesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_ESPECIALIDADES' }
      },
      { 
        path: 'turnos', 
        loadComponent: () => import('./features/rrhh/turnos/turnos.component').then(m => m.TurnosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_TURNOS' }
      },
      { 
        path: 'asistencia', 
        loadComponent: () => import('./features/rrhh/reporte-asistencia/reporte-asistencia.component').then(m => m.ReporteAsistenciaComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_CONTROL_DE_ASISTENCIA' }
      },
      { 
        path: 'comisiones', 
        loadComponent: () => import('./features/rrhh/comisiones/comisiones.component').then(m => m.ComisionesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_COMISIONES' }
      },
      { 
        path: 'incentivos', 
        loadComponent: () => import('./features/rrhh/incentivos/incentivos.component').then(m => m.IncentivosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_CAMPANAS_E_INCENTIVOS' }
      },
      { 
        path: 'monitor-comisiones', 
        loadComponent: () => import('./features/rrhh/monitor-comisiones/monitor-comisiones.component').then(m => m.MonitorComisionesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_MONITOR_DE_COMISIONES' }
      },
      { 
        path: 'liquidaciones', 
        loadComponent: () => import('./features/rrhh/liquidaciones/liquidaciones.component').then(m => m.LiquidacionesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_LIQUIDACIONES' }
      },
      { 
        path: 'agenda', 
        loadComponent: () => import('./features/operaciones/pages/agenda/agenda.component').then(m => m.AgendaComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_AGENDA_Y_CITAS' }
      },
      { 
        path: 'pos', 
        loadComponent: () => import('./features/operaciones/pages/pos/pos.component').then(m => m.PosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_PUNTO_DE_VENTA_POS' }
      },
      { 
        path: 'generar-orden', 
        loadComponent: () => import('./features/operaciones/pages/generar-orden/generar-orden.component').then(m => m.GenerarOrdenComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_PUNTO_DE_VENTA_POS' }
      },
      { 
        path: 'categorias', 
        loadComponent: () => import('./features/catalogo/categorias/categorias.component').then(m => m.CategoriasComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_CATEGORIAS' }
      },
      { 
        path: 'servicios', 
        loadComponent: () => import('./features/catalogo/servicios/servicios.component').then(m => m.ServiciosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_SERVICIOS' }
      },
      { 
        path: 'fidelizacion/cartillas', 
        loadComponent: () => import('./features/fidelizacion/pages/gestor-cartillas/gestor-cartillas.component').then(m => m.GestorCartillasComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_SERVICIOS' }
      },
      { 
        path: 'productos', 
        loadComponent: () => import('./features/catalogo/productos/productos.component').then(m => m.ProductosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_PRODUCTOS' }
      },
      { 
        path: 'insumos', 
        loadComponent: () => import('./features/catalogo/productos/productos.component').then(m => m.ProductosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_PRODUCTOS' }
      },
      { 
        path: 'inventario', 
        loadComponent: () => import('./features/catalogo/inventario/inventario.component').then(m => m.InventarioComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_CONTROL_DE_INVENTARIO' }
      },
      { 
        path: 'caja', 
        loadComponent: () => import('./features/finanzas/caja/caja.component').then(m => m.CajaComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_CAJA' }
      },
      { 
        path: 'egresos', 
        loadComponent: () => import('./features/finanzas/egresos/egresos.component').then(m => m.EgresosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_EGRESOS' }
      },
      { 
        path: 'facturacion', 
        loadComponent: () => import('./features/finanzas/facturacion/facturacion.component').then(m => m.FacturacionComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_FACTURACION' }
      },
      { 
        path: 'reportes', 
        loadComponent: () => import('./features/analitica/pages/reportes-rendimiento/reportes-rendimiento.component').then(m => m.ReportesRendimientoComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_REPORTES_DE_RENDIMIENTO' }
      },
      { 
        path: 'reportes-ventas', 
        loadComponent: () => import('./features/analitica/pages/reporte-ventas-detallado/reporte-ventas-detallado.component').then(m => m.ReporteVentasDetalladoComponent),
        canActivate: [permissionGuard],
        data: { permission: 'VER_REPORTES_DE_RENDIMIENTO' }
      }
    ]
  },
  { path: 'kiosko', loadComponent: () => import('./features/rrhh/asistencia/asistencia.component').then(m => m.AsistenciaComponent) },
  { path: '**', redirectTo: '/login' }
];
