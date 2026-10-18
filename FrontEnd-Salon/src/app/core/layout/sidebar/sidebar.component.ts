import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';
import { EmpresaService, EmpresaResponse } from '../../services/empresa.service';
import { AuthService } from '../../auth/services/auth.service';
import { LayoutService } from '../../services/layout.service';

interface MenuItem {
  label: string;
  shortLabel?: string;
  icon: string;
  route: string;
  active?: boolean;
  requiredPermissions?: string[];
}

interface MenuCategory {
  title: string;
  shortTitle: string;
  items: MenuItem[];
  expanded?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TooltipModule],
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent implements OnInit {
  public layoutService = inject(LayoutService);
  private empresaService = inject(EmpresaService);
  private authService = inject(AuthService);
  
  empresaData = signal<EmpresaResponse | null>(null);
  filteredMenuCategories = signal<MenuCategory[]>([]);

  ngOnInit() {
    this.empresaService.empresa$.subscribe(empresa => {
      if (empresa) {
        this.empresaData.set(empresa);
      }
    });

    this.empresaService.getEmpresaActiva().subscribe({
      error: (err) => console.error('Error al obtener datos de la empresa', err)
    });

    this.calculateFilteredMenu();
  }

  // Menú dinámico basado en Permisos con títulos cortos para modo compacto
  menuCategories: MenuCategory[] = [
    {
      title: 'INICIO',
      shortTitle: 'INI',
      items: [
        { label: 'Dashboard', shortLabel: 'Inicio', icon: 'pi pi-home', route: '/dashboard' }
      ]
    },
    {
      title: 'OPERACIONES',
      shortTitle: 'OPS',
      items: [
        { label: 'Agenda y Citas', shortLabel: 'Agenda', icon: 'pi pi-calendar', route: '/agenda', requiredPermissions: ['VER_AGENDA_Y_CITAS'] },
        { label: 'Generar Cartilla', shortLabel: 'Cartilla', icon: 'pi pi-file-edit', route: '/generar-orden', requiredPermissions: ['VER_PUNTO_DE_VENTA_POS'] }
      ]
    },
    {
      title: 'RRHH',
      shortTitle: 'RRHH',
      items: [
        { label: 'Control de Asistencia', shortLabel: 'Asistencia', icon: 'pi pi-clock', route: '/asistencia', requiredPermissions: ['VER_CONTROL_DE_ASISTENCIA'] },
        { label: 'Gestión de Empleados', shortLabel: 'Empleados', icon: 'pi pi-id-card', route: '/empleados', active: true, requiredPermissions: ['VER_GESTION_DE_EMPLEADOS'] },
        { label: 'Especialidades', shortLabel: 'Especialid.', icon: 'pi pi-star', route: '/especialidades', requiredPermissions: ['VER_ESPECIALIDADES'] },
        { label: 'Turnos', shortLabel: 'Turnos', icon: 'pi pi-sun', route: '/turnos', requiredPermissions: ['VER_TURNOS'] },
        { label: 'Comisiones', shortLabel: 'Comisiones', icon: 'pi pi-money-bill', route: '/comisiones', requiredPermissions: ['VER_COMISIONES'] },
        { label: 'Campañas e Incentivos', shortLabel: 'Incentivos', icon: 'pi pi-sparkles', route: '/incentivos', requiredPermissions: ['VER_CAMPANAS_E_INCENTIVOS'] },
        { label: 'Monitor de Comisiones', shortLabel: 'Monitor', icon: 'pi pi-chart-line', route: '/monitor-comisiones', requiredPermissions: ['VER_MONITOR_DE_COMISIONES'] }
      ]
    },
    {
      title: 'CATÁLOGO',
      shortTitle: 'CAT',
      items: [
        { label: 'Categorías', shortLabel: 'Categorías', icon: 'pi pi-tags', route: '/categorias', requiredPermissions: ['VER_CATEGORIAS'] },
        { label: 'Servicios', shortLabel: 'Servicios', icon: 'pi pi-briefcase', route: '/servicios', requiredPermissions: ['VER_SERVICIOS'] },
        { label: 'Fidelización y Promos', shortLabel: 'Fideliz.', icon: 'pi pi-id-card', route: '/fidelizacion/cartillas', requiredPermissions: ['VER_SERVICIOS'] },
        { label: 'Insumos (Uso Interno)', shortLabel: 'Insumos', icon: 'pi pi-box', route: '/insumos', requiredPermissions: ['VER_PRODUCTOS'] },
        { label: 'Productos Retail', shortLabel: 'Productos', icon: 'pi pi-shopping-bag', route: '/productos', requiredPermissions: ['VER_PRODUCTOS'] },
        { label: 'Control de Inventario', shortLabel: 'Inventario', icon: 'pi pi-list', route: '/inventario', requiredPermissions: ['VER_CONTROL_DE_INVENTARIO'] }
      ]
    },
    {
      title: 'FINANZAS',
      shortTitle: 'FIN',
      items: [
        { label: 'Caja', shortLabel: 'Caja', icon: 'pi pi-wallet', route: '/caja', requiredPermissions: ['VER_CAJA'] },
        { label: 'Facturación', shortLabel: 'Facturas', icon: 'pi pi-file', route: '/facturacion', requiredPermissions: ['VER_FACTURACION'] },
        { label: 'Egresos', shortLabel: 'Egresos', icon: 'pi pi-chart-line', route: '/egresos', requiredPermissions: ['VER_EGRESOS'] },
        { label: 'Liquidaciones', shortLabel: 'Liquidac.', icon: 'pi pi-money-bill', route: '/liquidaciones', requiredPermissions: ['VER_LIQUIDACIONES'] }
      ]
    },
    {
      title: 'ANALÍTICA & SEGURIDAD',
      shortTitle: 'SEG',
      items: [
        { label: 'Reportes de Rendimiento', shortLabel: 'Rendim.', icon: 'pi pi-chart-bar', route: '/reportes', requiredPermissions: ['VER_REPORTES_DE_RENDIMIENTO'] },
        { label: 'Reporte de Ventas', shortLabel: 'Ventas', icon: 'pi pi-list', route: '/reportes-ventas', requiredPermissions: ['VER_REPORTES_DE_RENDIMIENTO'] },
        { label: 'Usuarios', shortLabel: 'Usuarios', icon: 'pi pi-users', route: '/usuarios', requiredPermissions: ['VER_USUARIOS'] },
        { label: 'Roles y Permisos', shortLabel: 'Roles', icon: 'pi pi-shield', route: '/roles', requiredPermissions: ['VER_ROLES_Y_PERMISOS'] }
      ]
    }
  ];

  calculateFilteredMenu() {
    const user = this.authService.getCurrentUser();
    const userRoles = user?.roles || [];
    const userPermissions = user?.permissions || [];
    
    const isAdmin = userRoles.includes('ADMIN');

    const filtered = this.menuCategories.map(category => {
      const allowedItems = category.items.filter(item => {
        if (!item.requiredPermissions || item.requiredPermissions.length === 0) return true;
        if (isAdmin) return true;
        return item.requiredPermissions.some(perm => userPermissions.includes(perm));
      });

      const isAnyItemActive = allowedItems.some(item => window.location.pathname.includes(item.route));

      return {
        ...category,
        expanded: isAnyItemActive,
        items: allowedItems
      };
    }).filter(category => category.items.length > 0);

    this.filteredMenuCategories.set(filtered);
  }

  toggleCategory(index: number) {
    this.filteredMenuCategories.update(categories => {
      categories[index].expanded = !categories[index].expanded;
      return [...categories];
    });
  }
}
