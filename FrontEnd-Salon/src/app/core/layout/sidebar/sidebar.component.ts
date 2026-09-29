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
        { label: 'Agenda y Citas', icon: 'pi pi-calendar', route: '/agenda', requiredPermissions: ['GESTION_CITAS'] },
        { label: 'Punto de Venta POS', icon: 'pi pi-calculator', route: '/pos', requiredPermissions: ['PUNTO_VENTA'] }
      ]
    },
    {
      title: 'RRHH',
      items: [
        { label: 'Control de Asistencia', icon: 'pi pi-clock', route: '/asistencia' }, // Sin permiso, todos lo ven
        { label: 'Gestión de Empleados', icon: 'pi pi-id-card', route: '/empleados', active: true, requiredPermissions: ['GESTION_EMPLEADOS'] },
        { label: 'Especialidades', icon: 'pi pi-star', route: '/especialidades', requiredPermissions: ['GESTION_ESPECIALIDADES'] },
        { label: 'Turnos', icon: 'pi pi-sun', route: '/turnos', requiredPermissions: ['GESTION_EMPLEADOS'] },
        { label: 'Comisiones', icon: 'pi pi-money-bill', route: '/comisiones', requiredPermissions: ['VER_REPORTES'] },
        { label: 'Campañas e Incentivos', icon: 'pi pi-sparkles', route: '/incentivos', requiredPermissions: ['VER_REPORTES'] },
        { label: 'Monitor de Comisiones', icon: 'pi pi-chart-line', route: '/monitor-comisiones', requiredPermissions: ['VER_REPORTES'] }
      ]
    },
    {
      title: 'CATÁLOGO',
      items: [
        { label: 'Categorías', icon: 'pi pi-tags', route: '/categorias', requiredPermissions: ['GESTION_CATALOGO'] },
        { label: 'Servicios', icon: 'pi pi-briefcase', route: '/servicios', requiredPermissions: ['GESTION_CATALOGO'] },
        { label: 'Insumos (Uso Interno)', icon: 'pi pi-box', route: '/insumos', requiredPermissions: ['GESTION_CATALOGO'] },
        { label: 'Productos Retail', icon: 'pi pi-shopping-bag', route: '/productos', requiredPermissions: ['GESTION_CATALOGO'] },
        { label: 'Control de Inventario', icon: 'pi pi-list', route: '/inventario', requiredPermissions: ['GESTION_CATALOGO'] }
      ]
    },
    {
      title: 'FINANZAS',
      items: [
        { label: 'Caja', icon: 'pi pi-wallet', route: '/caja', requiredPermissions: ['PUNTO_VENTA'] },
        { label: 'Facturación', icon: 'pi pi-file', route: '/facturacion', requiredPermissions: ['PUNTO_VENTA'] },
        { label: 'Egresos', icon: 'pi pi-chart-line', route: '/egresos', requiredPermissions: ['VER_REPORTES'] }
      ]
    },
    {
      title: 'ANALÍTICA & SEGURIDAD',
      items: [
        { label: 'Reportes de Rendimiento', icon: 'pi pi-chart-bar', route: '/reportes', requiredPermissions: ['VER_REPORTES'] },
        { label: 'Usuarios', icon: 'pi pi-users', route: '/usuarios', requiredPermissions: ['GESTION_USUARIOS'] },
        { label: 'Roles y Permisos', icon: 'pi pi-shield', route: '/roles', requiredPermissions: ['GESTION_ROLES'] }
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
