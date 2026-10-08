import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardService, ReporteVentaDetalleDTO } from '../../services/dashboard.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { TagModule } from 'primeng/tag';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';

@Component({
  selector: 'app-reporte-ventas-detallado',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, CalendarModule, TagModule, IconFieldModule, InputIconModule],
  templateUrl: './reporte-ventas-detallado.component.html'
})
export class ReporteVentasDetalladoComponent implements OnInit {
  
  private dashboardService = inject(DashboardService);
  
  reporte: ReporteVentaDetalleDTO[] = [];
  loading = false;
  
  cols = [
    { field: 'fechaHora', header: 'Fecha y Hora' },
    { field: 'ticketId', header: 'N° Ticket' },
    { field: 'tipoItem', header: 'Tipo' },
    { field: 'itemNombre', header: 'Ítem (Servicio/Producto)' },
    { field: 'empleadoNombre', header: 'Realizado por' },
    { field: 'clienteNombre', header: 'Cliente' },
    { field: 'cantidad', header: 'Cant.' },
    { field: 'precioUnitario', header: 'Precio Unit.' },
    { field: 'subtotal', header: 'Subtotal' },
    { field: 'estadoTicket', header: 'Estado' }
  ];
  
  fechaInicio: Date = new Date();
  fechaFin: Date = new Date();
  
  searchTerm = '';

  ngOnInit(): void {
    // Por defecto cargar desde inicio del mes actual
    this.fechaInicio = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
    this.cargarReporte();
  }

  cargarReporte() {
    this.loading = true;
    
    // Convert to YYYY-MM-DD
    const strInicio = this.fechaInicio.toISOString().split('T')[0];
    const strFin = this.fechaFin.toISOString().split('T')[0];

    this.dashboardService.getReporteVentas(strInicio, strFin).subscribe({
      next: (data) => {
        this.reporte = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }
}
