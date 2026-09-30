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
  consolidado: any[] = [];
  vistaActiva: 'detallado' | 'consolidado' = 'detallado';
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
        this.generarConsolidado();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  generarConsolidado() {
    const mapa = new Map<number, any>();
    
    this.registros.forEach(reg => {
      if (!mapa.has(reg.empleadoId)) {
        mapa.set(reg.empleadoId, {
          empleadoNombreCompleto: reg.empleadoNombreCompleto,
          diasLaborados: 0,
          tardanzasVeces: 0,
          minutosTardanzaTotal: 0,
          faltas: 0
        });
      }
      
      const emp = mapa.get(reg.empleadoId);
      if (reg.tipo === 'ASISTIO' || reg.tipo === 'TARDANZA') {
        emp.diasLaborados++;
      }
      if (reg.tipo === 'TARDANZA') {
        emp.tardanzasVeces++;
        emp.minutosTardanzaTotal += (reg.tardanzaMinutos || 0);
      }
      if (reg.tipo === 'FALTA') {
        emp.faltas++;
      }
    });

    this.consolidado = Array.from(mapa.values());
  }

  abrirKiosko() {
    // Abre el Kiosko en una nueva pestaña (ideal para poner en pantalla completa en otro monitor)
    window.open('/kiosko', '_blank');
  }
}
