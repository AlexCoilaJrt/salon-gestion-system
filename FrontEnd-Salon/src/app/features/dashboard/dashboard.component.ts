import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth/services/auth.service';
import { TicketService, TicketResponse } from '../finanzas/services/ticket.service';
import { AgendaService, CitaResponse } from '../operaciones/pages/agenda/agenda.service';
import { CatalogoService, Producto } from '../catalogo/services/catalogo.service';
import { CajaService, SesionCaja } from '../finanzas/services/caja.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private ticketService = inject(TicketService);
  private agendaService = inject(AgendaService);
  private catalogoService = inject(CatalogoService);
  private cajaService = inject(CajaService);

  userName = signal('');
  
  // Stats
  ventasHoy = signal(0);
  citasHoy = signal(0);
  ticketsHoy = signal(0);

  // New Data
  citasDelDiaLista = signal<CitaResponse[]>([]);
  alertasStock = signal<Producto[]>([]);
  cajaAbierta = signal<any | null>(null);

  ngOnInit() {
    const user: any = this.authService.getCurrentUser();
    if (user) {
      this.userName.set(user.firstName || user.username || 'Usuario');
    }
    
    this.cargarEstadisticas();
  }

  cargarEstadisticas() {
    // Citas hoy
    this.agendaService.obtenerTodas().subscribe((citas: CitaResponse[]) => {
      const hoy = new Date().toISOString().split('T')[0];
      const citasDelDia = citas.filter((c: CitaResponse) => c.fechaHora.toString().startsWith(hoy));
      this.citasHoy.set(citasDelDia.length);
      
      // Sort upcoming appointments first
      citasDelDia.sort((a, b) => new Date(a.fechaHora).getTime() - new Date(b.fechaHora).getTime());
      this.citasDelDiaLista.set(citasDelDia);
    });

    // Ventas hoy
    this.ticketService.obtenerTodosTickets().subscribe((tickets: TicketResponse[]) => {
      const hoy = new Date().toISOString().split('T')[0];
      const ticketsDelDia = tickets.filter((t: TicketResponse) => t.activo && t.fechaEmision.toString().startsWith(hoy));
      
      this.ticketsHoy.set(ticketsDelDia.length);
      
      const total = ticketsDelDia.reduce((sum: number, t: TicketResponse) => sum + t.total, 0);
      this.ventasHoy.set(total);
    });

    // Alertas de Stock
    this.catalogoService.getProductosAlertasStock().subscribe(alertas => {
      this.alertasStock.set(alertas);
    });

    // Estado de Caja (Resumen)
    this.cajaService.obtenerResumenActual().subscribe((resumen: any) => {
      if (resumen) {
        this.cajaAbierta.set(resumen);
      }
    });
  }

  navigate(path: string) {
    this.router.navigate([path]);
  }
}
