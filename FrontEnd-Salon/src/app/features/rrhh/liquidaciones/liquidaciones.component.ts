import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { CalendarModule } from 'primeng/calendar';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { MessageService } from 'primeng/api';
import { forkJoin } from 'rxjs';
import { LiquidacionesService, LiquidacionResponse, DetalleLiquidacion } from './liquidaciones.service';
import { IncentivoService } from '../services/incentivo.service';

@Component({
  selector: 'app-liquidaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule, ToastModule, CalendarModule, TooltipModule, DialogModule],
  providers: [MessageService],
  templateUrl: './liquidaciones.component.html'
})
export class LiquidacionesComponent implements OnInit {
  liquidaciones: LiquidacionResponse[] = [];
  loading = false;
  rangoFechas: Date[] | undefined;

  // Desglose de servicios de una liquidación
  displayDetalle = false;
  loadingDetalle = false;
  detalle: DetalleLiquidacion[] = [];
  liquidacionSeleccionada: LiquidacionResponse | null = null;

  constructor(
    private liquidacionesService: LiquidacionesService,
    private incentivoService: IncentivoService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    const now = new Date();
    this.rangoFechas = [new Date(now.getFullYear(), now.getMonth(), 1), new Date(now.getFullYear(), now.getMonth() + 1, 0)];
    this.cargarLiquidaciones();
  }

  cargarLiquidaciones(): void {
    this.loading = true;
    this.liquidacionesService.obtenerTodas().subscribe({
      next: (data) => {
        this.liquidaciones = data;
        this.loading = false;
      },
      error: () => {
        this.messageService.add({severity:'error', summary:'Error', detail:'No se pudo cargar las liquidaciones guardadas'});
        this.loading = false;
      }
    });
  }

  generarLiquidaciones(): void {
    if (!this.rangoFechas || !this.rangoFechas[0] || !this.rangoFechas[1]) {
      this.messageService.add({severity:'warn', summary:'Alerta', detail:'Selecciona un rango de fechas válido'});
      return;
    }

    this.loading = true;
    const inicioStr = this.formatDate(this.rangoFechas[0]);
    const finStr = this.formatDate(this.rangoFechas[1]);

    this.incentivoService.getLiquidaciones(inicioStr, finStr).subscribe({
      next: (data: any[]) => {
        // El backend crea la liquidación si no existe y recalcula las PENDIENTE con las ventas nuevas;
        // las PAGADAS/RETENIDAS se devuelven tal cual.
        const peticiones = data.map(item => this.liquidacionesService.crearLiquidacion({
          empleadoId: item.empleadoId,
          fechaInicio: inicioStr,
          fechaFin: finStr,
          totalVentas: item.totalVentas,
          totalComision: item.totalComision,
          sueldoFijoProporcional: item.sueldoFijoProporcional || 0,
          descuentos: item.descuentosAdelantos || 0,
          totalAPagar: item.totalPagar,
          estado: 'PENDIENTE',
          diasAsistidos: item.diasAsistidos || 0
        }));

        if (peticiones.length === 0) {
          this.loading = false;
          return;
        }

        forkJoin(peticiones).subscribe({
          next: (guardadas) => {
            guardadas.forEach(liq => {
              const index = this.liquidaciones.findIndex(l => l.id === liq.id);
              if (index !== -1) {
                this.liquidaciones[index] = liq;
              } else {
                this.liquidaciones.push(liq);
              }
            });
            this.liquidaciones = [...this.liquidaciones];
            this.loading = false;
            this.messageService.add({severity:'success', summary:'Calculado', detail:'Pagos calculados y actualizados con las ventas del periodo.'});
          },
          error: (err) => {
            console.error('Error al guardar liquidación:', err);
            this.loading = false;
            this.messageService.add({severity:'error', summary:'Error Backend', detail:'Fallo al registrar un pago. Revisa si el backend fue reiniciado.'});
          }
        });
      },
      error: () => {
        this.loading = false;
        this.messageService.add({severity:'error', summary:'Error', detail:'Fallo al calcular los pagos'});
      }
    });
  }

  verDetalle(liq: LiquidacionResponse): void {
    this.liquidacionSeleccionada = liq;
    this.detalle = [];
    this.displayDetalle = true;
    this.loadingDetalle = true;
    this.liquidacionesService.obtenerDetalle(liq.empleadoId, liq.fechaInicio, liq.fechaFin).subscribe({
      next: (lineas) => {
        this.detalle = lineas;
        this.loadingDetalle = false;
      },
      error: () => {
        this.loadingDetalle = false;
        this.messageService.add({severity:'error', summary:'Error', detail:'No se pudo cargar el detalle de servicios'});
      }
    });
  }

  get totalDetalleVentas(): number {
    return this.detalle.reduce((acc, d) => acc + (d.subtotal || 0), 0);
  }

  get totalDetalleComision(): number {
    return this.detalle.reduce((acc, d) => acc + (d.comision || 0), 0);
  }

  private formatDate(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  cambiarEstado(liq: LiquidacionResponse, nuevoEstado: string): void {
    this.liquidacionesService.cambiarEstado(liq.id, nuevoEstado).subscribe({
      next: (updated) => {
        const index = this.liquidaciones.findIndex(l => l.id === liq.id);
        if (index !== -1) {
          this.liquidaciones[index] = updated;
        }
        this.messageService.add({severity:'success', summary:'Actualizado', detail:`Liquidación marcada como ${nuevoEstado}`});
      },
      error: () => {
        this.messageService.add({severity:'error', summary:'Error', detail:'No se pudo cambiar el estado'});
      }
    });
  }

  getSeverity(estado: string): string {
    return estado === 'PAGADO' ? 'success' : (estado === 'PENDIENTE' ? 'warning' : 'danger');
  }

  puedePagar(liq: LiquidacionResponse): boolean {
    if (!liq.fechaFin) return true;
    const parts = liq.fechaFin.split('-');
    if (parts.length !== 3) return true;
    const fechaFin = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    fechaFin.setDate(fechaFin.getDate() + 5); // 5 días de seguridad
    
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    fechaFin.setHours(0, 0, 0, 0);
    
    return hoy >= fechaFin;
  }

  getMensajeBloqueo(liq: LiquidacionResponse): string {
    if (!liq.fechaFin) return '';
    const parts = liq.fechaFin.split('-');
    if (parts.length !== 3) return '';
    const fechaFin = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    fechaFin.setDate(fechaFin.getDate() + 5);
    
    const dd = String(fechaFin.getDate()).padStart(2, '0');
    const mm = String(fechaFin.getMonth() + 1).padStart(2, '0');
    const yyyy = fechaFin.getFullYear();
    
    return `Se habilitará el ${dd}/${mm}/${yyyy} (5 días de seguridad)`;
  }
}
