import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  // Estado de la barra lateral (plegada o expandida)
  isSidebarCollapsed = signal<boolean>(
    typeof localStorage !== 'undefined' && localStorage.getItem('sidebar_collapsed') === 'true'
  );

  toggleSidebar(): void {
    this.isSidebarCollapsed.update(collapsed => {
      const nuevoEstado = !collapsed;
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('sidebar_collapsed', String(nuevoEstado));
      }
      return nuevoEstado;
    });
  }

  setSidebarCollapsed(collapsed: boolean): void {
    this.isSidebarCollapsed.set(collapsed);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('sidebar_collapsed', String(collapsed));
    }
  }
}
