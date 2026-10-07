import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { CalendarModule } from 'primeng/calendar';
import { MessageService } from 'primeng/api';
import { LiquidacionesService, LiquidacionResponse } from './liquidaciones.service';
import { IncentivoService } from '../services/incentivo.service';

@Component({
  selector: 'app-liquidaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule, ToastModule, CalendarModule],
  providers: [MessageService],
  templateUrl: './liquidaciones.component.html'
})
export class LiquidacionesComponent implements OnInit {
  liquidaciones: LiquidacionResponse[] = [];
  loading = false;
  rangoFechas: Date[] | undefined;

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
        // Combinamos la data precalculada con lo que ya está guardado (si existe)
        data.forEach(item => {
          item.fechaInicio = inicioStr;
          item.fechaFin = finStr;
          item.estado = 'RETENIDO'; // Estado por defecto para las nuevas
          
          // Verificamos si ya existe
          const existente = this.liquidaciones.find(l => l.empleadoId === item.empleadoId && l.fechaInicio === inicioStr && l.fechaFin === finStr);
          if (!existente) {
             // Guardar en la DB
             this.liquidacionesService.crearLiquidacion({
               empleadoId: item.empleadoId,
               fechaInicio: inicioStr,
               fechaFin: finStr,
               totalVentas: item.totalVentas,
               totalComision: item.totalComision,
               sueldoFijoProporcional: item.sueldoFijoProporcional || 0,
               descuentos: item.descuentosAdelantos || 0,
               totalAPagar: item.totalPagar,
               estado: 'RETENIDO',
               diasAsistidos: item.diasAsistidos || 0
             }).subscribe(nueva => {
               this.liquidaciones.push(nueva);
             });
          }
        });
        
        setTimeout(() => {
          this.loading = false;
          this.messageService.add({severity:'success', summary:'Calculado', detail:'Pagos pre-calculados y registrados con éxito.'});
        }, 1000);
      },
      error: () => {
        this.loading = false;
        this.messageService.add({severity:'error', summary:'Error', detail:'Fallo al calcular los pagos'});
      }
    });
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
    return estado === 'PAGADO' ? 'success' : 'warning';
  }
}
