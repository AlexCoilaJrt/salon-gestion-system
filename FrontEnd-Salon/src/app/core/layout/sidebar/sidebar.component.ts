import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EmpresaService, EmpresaResponse } from '../../services/empresa.service';

import { AuthService } from '../../auth/services/auth.service';

interface MenuCategory {
  title: string;
  items: MenuItem[];
  expanded?: boolean;
}

interface MenuItem {
  label: string;
  icon: string;
  route: string;
  active?: boolean;
  requiredPermissions?: string[];
}

import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent implements OnInit {
  
  private empresaService = inject(EmpresaService);
  private authService = inject(AuthService);
  
  // Usamos signals o variables para los datos dinámicos
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

  // Menú dinámico basado en Permisos
  menuCategories: MenuCategory[] = [
    {
      title: 'OPERACIONES',
      items: [
        { label: 'Agenda y Citas', icon: 'pi pi-calendar', route: '/agenda', requiredPermissions: ['VER_AGENDA_Y_CITAS'] },
        { label: 'Punto de Venta POS', icon: 'pi pi-calculator', route: '/pos', requiredPermissions: ['VER_PUNTO_DE_VENTA_POS'] }
      ]
    },
    {
      title: 'RRHH',
      items: [
        { label: 'Control de Asistencia', icon: 'pi pi-clock', route: '/asistencia', requiredPermissions: ['VER_CONTROL_DE_ASISTENCIA'] },
        { label: 'Gestión de Empleados', icon: 'pi pi-id-card', route: '/empleados', active: true, requiredPermissions: ['VER_GESTION_DE_EMPLEADOS'] },
        { label: 'Especialidades', icon: 'pi pi-star', route: '/especialidades', requiredPermissions: ['VER_ESPECIALIDADES'] },
        { label: 'Turnos', icon: 'pi pi-sun', route: '/turnos', requiredPermissions: ['VER_TURNOS'] },
        { label: 'Comisiones', icon: 'pi pi-money-bill', route: '/comisiones', requiredPermissions: ['VER_COMISIONES'] },
        { label: 'Campañas e Incentivos', icon: 'pi pi-sparkles', route: '/incentivos', requiredPermissions: ['VER_CAMPANAS_E_INCENTIVOS'] },
        { label: 'Monitor de Comisiones', icon: 'pi pi-chart-line', route: '/monitor-comisiones', requiredPermissions: ['VER_MONITOR_DE_COMISIONES'] }
      ]
    },
    {
      title: 'CATÁLOGO',
      items: [
        { label: 'Categorías', icon: 'pi pi-tags', route: '/categorias', requiredPermissions: ['VER_CATEGORIAS'] },
        { label: 'Servicios', icon: 'pi pi-briefcase', route: '/servicios', requiredPermissions: ['VER_SERVICIOS'] },
        { label: 'Insumos (Uso Interno)', icon: 'pi pi-box', route: '/insumos', requiredPermissions: ['VER_PRODUCTOS'] },
        { label: 'Productos Retail', icon: 'pi pi-shopping-bag', route: '/productos', requiredPermissions: ['VER_PRODUCTOS'] },
        { label: 'Control de Inventario', icon: 'pi pi-list', route: '/inventario', requiredPermissions: ['VER_CONTROL_DE_INVENTARIO'] }
      ]
    },
    {
      title: 'FINANZAS',
      items: [
        { label: 'Caja', icon: 'pi pi-wallet', route: '/caja', requiredPermissions: ['VER_CAJA'] },
        { label: 'Facturación', icon: 'pi pi-file', route: '/facturacion', requiredPermissions: ['VER_FACTURACION'] },
        { label: 'Egresos', icon: 'pi pi-chart-line', route: '/egresos', requiredPermissions: ['VER_EGRESOS'] },
        { label: 'Liquidaciones', icon: 'pi pi-money-bill', route: '/liquidaciones', requiredPermissions: ['VER_LIQUIDACIONES'] }
      ]
    },
    {
      title: 'ANALÍTICA & SEGURIDAD',
      items: [
        { label: 'Reportes de Rendimiento', icon: 'pi pi-chart-bar', route: '/reportes', requiredPermissions: ['VER_REPORTES_DE_RENDIMIENTO'] },
        { label: 'Usuarios', icon: 'pi pi-users', route: '/usuarios', requiredPermissions: ['VER_USUARIOS'] },
        { label: 'Roles y Permisos', icon: 'pi pi-shield', route: '/roles', requiredPermissions: ['VER_ROLES_Y_PERMISOS'] }
      ]
    }
  ];

  calculateFilteredMenu() {
    const user = this.authService.getCurrentUser();
    const userRoles = user?.roles || [];
    const userPermissions = user?.permissions || [];
    
    // Si es ADMIN, por defecto puede ver todo (Bypass de seguridad)
    const isAdmin = userRoles.includes('ADMIN');

    const filtered = this.menuCategories.map(category => {
      // Filtrar items permitidos
      const allowedItems = category.items.filter(item => {
        if (!item.requiredPermissions || item.requiredPermissions.length === 0) return true;
        if (isAdmin) return true;
        return item.requiredPermissions.some(perm => userPermissions.includes(perm));
      });

      // Validar si algún item está activo en la ruta actual
      const isAnyItemActive = allowedItems.some(item => window.location.pathname.includes(item.route));

      return {
        ...category,
        expanded: isAnyItemActive, // Abierto SOLO si estoy en esa sección
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
