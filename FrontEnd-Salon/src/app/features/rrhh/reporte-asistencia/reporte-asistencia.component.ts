import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AsistenciaService, AsistenciaResponse } from '../services/asistencia.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { CalendarModule } from 'primeng/calendar';

@Component({
  selector: 'app-reporte-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule, CalendarModule],
  templateUrl: './reporte-asistencia.component.html'
})
export class ReporteAsistenciaComponent implements OnInit {
  private asistenciaService = inject(AsistenciaService);

  registros: AsistenciaResponse[] = [];
  loading: boolean = false;
  
  fechaInicio: Date = new Date();
  fechaFin: Date = new Date();

  ngOnInit() {
    // Por defecto, carga la última semana
    this.fechaInicio.setDate(this.fechaInicio.getDate() - 7);
    this.loadReporte();
  }

  loadReporte() {
    this.loading = true;
    const inicioStr = this.fechaInicio.toISOString().split('T')[0];
    const finStr = this.fechaFin.toISOString().split('T')[0];

    this.asistenciaService.getReporte(inicioStr, finStr).subscribe({
      next: (data) => {
        this.registros = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  abrirKiosko() {
    // Abre el Kiosko en una nueva pestaña (ideal para poner en pantalla completa en otro monitor)
    window.open('/kiosko', '_blank');
  }
}
