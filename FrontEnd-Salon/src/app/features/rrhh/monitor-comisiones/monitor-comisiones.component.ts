import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreeTableModule } from 'primeng/treetable';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TreeNode } from 'primeng/api';
import { IncentivoService, MonitorComision } from '../services/incentivo.service';

@Component({
  selector: 'app-monitor-comisiones',
  standalone: true,
  imports: [CommonModule, TreeTableModule, TagModule, ButtonModule],
  templateUrl: './monitor-comisiones.component.html'
})
export class MonitorComisionesComponent implements OnInit {
  nodes: TreeNode[] = [];
  loading = false;
  
  private cd = inject(ChangeDetectorRef);

  constructor(private incentivoService: IncentivoService) {}

  ngOnInit(): void {
    this.loadMonitor();
  }

  loadMonitor(): void {
    this.loading = true;
    this.incentivoService.getMonitorComisiones().subscribe({
      next: (data) => {
        this.nodes = this.transformToTreeNodes(data);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  transformToTreeNodes(monitores: MonitorComision[]): TreeNode[] {
    return monitores.map(monitor => {
      return {
        data: {
          empleadoId: monitor.empleadoId,
          nombre: monitor.empleadoNombreCompleto,
          asistenciaActiva: monitor.asistenciaActiva,
          basePorcentaje: monitor.basePorcentaje,
          baseMontoFijo: monitor.baseMontoFijo,
          incentivosAplicados: monitor.incentivosAplicados,
          totalComisionPorcentaje: monitor.totalComisionPorcentaje,
          totalComisionMontoFijo: monitor.totalComisionMontoFijo,
          ventasHoy: monitor.ventasHoy,
          comisionesGanadasHoy: monitor.comisionesGanadasHoy,
          isRoot: true
        },
        children: monitor.detallesServicios ? monitor.detallesServicios.map(detalle => ({
          data: {
            nombre: detalle.servicioNombre,
            categoriaNombre: detalle.categoriaNombre,
            especialidadAplicada: detalle.especialidadAplicada,
            tipoPago: detalle.tipoPago,
            precioCobrado: detalle.precioCobrado,
            porcentajeAplicado: detalle.porcentajeAplicado,
            comisionesGanadasHoy: detalle.comisionGanada,
            fechaHora: detalle.fechaHora,
            isRoot: false
          }
        })) : []
      };
    });
  }
}
