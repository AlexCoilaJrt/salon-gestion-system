import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TreeTableModule } from 'primeng/treetable';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { TooltipModule } from 'primeng/tooltip';
import { TreeNode } from 'primeng/api';
import { IncentivoService, MonitorComision } from '../services/incentivo.service';

@Component({
  selector: 'app-monitor-comisiones',
  standalone: true,
  imports: [CommonModule, FormsModule, TreeTableModule, TagModule, ButtonModule, CalendarModule, TooltipModule],
  templateUrl: './monitor-comisiones.component.html'
})
export class MonitorComisionesComponent implements OnInit {
  nodes: TreeNode[] = [];
  loading = false;
  
  // Modos de filtro: 'hoy' | 'semana' | 'dia' | 'rango'
  modoFiltro: 'hoy' | 'semana' | 'dia' | 'rango' = 'hoy';
  fechaSeleccionada: Date = new Date();
  fechaCalendarDia: Date = new Date();
  rangoFechas: Date[] = [];
  fechaInicioStr: string = '';
  fechaFinStr: string = '';
  fechaDisplay: string = '';

  // Resumen métricas del período
  ventasTotalesPeriodo: number = 0;
  comisionesTotalesPeriodo: number = 0;
  empleadosConVentas: number = 0;
  empleadosActivosCount: number = 0;

  private cd = inject(ChangeDetectorRef);

  constructor(private incentivoService: IncentivoService) {}

  ngOnInit(): void {
    this.setFiltroHoy(false);
    this.loadMonitor();
  }

  formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  setFiltroHoy(recargar: boolean = true): void {
    this.modoFiltro = 'hoy';
    const hoy = new Date();
    this.fechaSeleccionada = hoy;
    this.fechaCalendarDia = hoy;
    const str = this.formatDate(hoy);
    this.fechaInicioStr = str;
    this.fechaFinStr = str;
    this.actualizarFechaDisplay();
    if (recargar) {
      this.loadMonitor();
    }
  }

  setFiltroSemana(referencia: Date = new Date()): void {
    this.modoFiltro = 'semana';
    this.fechaSeleccionada = referencia;
    const curr = new Date(referencia);
    const day = curr.getDay(); // 0 es domingo
    const diff = curr.getDate() - day + (day === 0 ? -6 : 1); // ajustar a lunes
    const lunes = new Date(curr.setDate(diff));
    const domingo = new Date(lunes);
    domingo.setDate(lunes.getDate() + 6);

    this.fechaInicioStr = this.formatDate(lunes);
    this.fechaFinStr = this.formatDate(domingo);
    this.actualizarFechaDisplay(lunes, domingo);
    this.loadMonitor();
  }

  setFiltroDia(fecha: Date): void {
    if (!fecha) return;
    this.modoFiltro = 'dia';
    this.fechaSeleccionada = fecha;
    this.fechaCalendarDia = fecha;
    const str = this.formatDate(fecha);
    this.fechaInicioStr = str;
    this.fechaFinStr = str;
    this.actualizarFechaDisplay();
    this.loadMonitor();
  }

  onDiaChange(fecha: Date): void {
    if (fecha) {
      const hoy = new Date();
      if (this.formatDate(fecha) === this.formatDate(hoy)) {
        this.setFiltroHoy();
      } else {
        this.setFiltroDia(fecha);
      }
    }
  }

  onRangoChange(rango: Date[]): void {
    if (rango && rango[0] && rango[1]) {
      this.modoFiltro = 'rango';
      this.fechaInicioStr = this.formatDate(rango[0]);
      this.fechaFinStr = this.formatDate(rango[1]);
      this.actualizarFechaDisplay(rango[0], rango[1]);
      this.loadMonitor();
    }
  }

  navegarPeriodo(direccion: number): void {
    if (this.modoFiltro === 'hoy' || this.modoFiltro === 'dia') {
      const nueva = new Date(this.fechaSeleccionada);
      nueva.setDate(nueva.getDate() + direccion);
      const hoy = new Date();
      if (this.formatDate(nueva) === this.formatDate(hoy)) {
        this.setFiltroHoy();
      } else {
        this.setFiltroDia(nueva);
      }
    } else if (this.modoFiltro === 'semana') {
      const inicio = new Date(this.fechaInicioStr + 'T00:00:00');
      inicio.setDate(inicio.getDate() + (direccion * 7));
      this.setFiltroSemana(inicio);
    }
  }

  actualizarFechaDisplay(fInicio?: Date, fFin?: Date): void {
    const meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    const diasSemana = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

    if (this.modoFiltro === 'hoy') {
      const d = this.fechaSeleccionada;
      this.fechaDisplay = `Hoy · ${diasSemana[d.getDay()]}, ${d.getDate()} de ${meses[d.getMonth()]} de ${d.getFullYear()}`;
    } else if (this.modoFiltro === 'dia') {
      const d = this.fechaSeleccionada;
      this.fechaDisplay = `${diasSemana[d.getDay()]}, ${d.getDate()} de ${meses[d.getMonth()]} de ${d.getFullYear()}`;
    } else if (this.modoFiltro === 'semana') {
      if (fInicio && fFin) {
        this.fechaDisplay = `Semana: ${fInicio.getDate()} ${meses[fInicio.getMonth()].substring(0,3)} - ${fFin.getDate()} ${meses[fFin.getMonth()]} ${fFin.getFullYear()}`;
      } else {
        this.fechaDisplay = `Semana del ${this.fechaInicioStr} al ${this.fechaFinStr}`;
      }
    } else if (this.modoFiltro === 'rango') {
      if (fInicio && fFin) {
        this.fechaDisplay = `Rango: ${fInicio.getDate()}/${fInicio.getMonth()+1}/${fInicio.getFullYear()} - ${fFin.getDate()}/${fFin.getMonth()+1}/${fFin.getFullYear()}`;
      } else {
        this.fechaDisplay = `Rango: ${this.fechaInicioStr} al ${this.fechaFinStr}`;
      }
    }
  }

  get columnaVentasTitulo(): string {
    if (this.modoFiltro === 'hoy') return 'Ventas Hoy (S/)';
    if (this.modoFiltro === 'semana') return 'Ventas Semana (S/)';
    if (this.modoFiltro === 'dia') return 'Ventas del Día (S/)';
    return 'Ventas Período (S/)';
  }

  get columnaGananciaTitulo(): string {
    if (this.modoFiltro === 'hoy') return 'Ganancia Hoy (S/)';
    if (this.modoFiltro === 'semana') return 'Ganancia Semana (S/)';
    if (this.modoFiltro === 'dia') return 'Ganancia del Día (S/)';
    return 'Ganancia Período (S/)';
  }

  loadMonitor(): void {
    this.loading = true;
    this.incentivoService.getMonitorComisiones(this.fechaInicioStr, this.fechaFinStr).subscribe({
      next: (data) => {
        this.nodes = this.transformToTreeNodes(data);
        this.ventasTotalesPeriodo = data.reduce((acc, m) => acc + (m.ventasHoy || 0), 0);
        this.comisionesTotalesPeriodo = data.reduce((acc, m) => acc + (m.comisionesGanadasHoy || 0), 0);
        this.empleadosConVentas = data.filter(m => (m.ventasHoy || 0) > 0).length;
        this.empleadosActivosCount = data.filter(m => m.asistenciaActiva).length;
        this.loading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cd.markForCheck();
      }
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
