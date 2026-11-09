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
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { forkJoin } from 'rxjs';
import { LiquidacionesService, LiquidacionResponse, DetalleLiquidacion } from './liquidaciones.service';
import { IncentivoService } from '../services/incentivo.service';
import { AdelantoService, AdelantoResponse, AdelantoRequest } from '../services/adelanto.service';

@Component({
  selector: 'app-liquidaciones',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    TagModule,
    ToastModule,
    CalendarModule,
    TooltipModule,
    DialogModule,
    InputTextModule,
    InputNumberModule,
    DropdownModule,
    CheckboxModule
  ],
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
  busquedaDetalle = '';
  liquidacionSeleccionada: LiquidacionResponse | null = null;

  // Gestión de Adelantos
  displayModalAdelanto = false;
  loadingAdelantos = false;
  guardandoAdelanto = false;
  adelantoEnEdicion: AdelantoResponse | null = null;
  filtroVerTodosAdelantos = false;
  adelantosEmpleado: AdelantoResponse[] = [];
  liquidacionAdelanto: LiquidacionResponse | null = null;

  nuevoAdelanto = {
    monto: null as number | null,
    fecha: new Date(),
    metodoPago: 'EFECTIVO',
    motivo: '',
    descontarDeCaja: false
  };

  metodosPago = [
    { label: 'Efectivo', value: 'EFECTIVO' },
    { label: 'Yape', value: 'YAPE' },
    { label: 'Plin', value: 'PLIN' },
    { label: 'Transferencia Bancaria', value: 'TRANSFERENCIA' }
  ];

  constructor(
    private liquidacionesService: LiquidacionesService,
    private incentivoService: IncentivoService,
    private adelantoService: AdelantoService,
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
    this.busquedaDetalle = '';
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

  // --- Métodos de Adelantos ---
  abrirModalAdelantos(liq: LiquidacionResponse): void {
    this.liquidacionAdelanto = liq;
    this.adelantoEnEdicion = null;
    this.filtroVerTodosAdelantos = false;

    this.resetFormularioAdelanto();
    this.displayModalAdelanto = true;
    this.recargarAdelantos();
  }

  recargarAdelantos(): void {
    if (!this.liquidacionAdelanto) return;
    this.loadingAdelantos = true;
    const pInicio = this.filtroVerTodosAdelantos ? undefined : this.liquidacionAdelanto.fechaInicio;
    const pFin = this.filtroVerTodosAdelantos ? undefined : this.liquidacionAdelanto.fechaFin;

    this.adelantoService.obtenerAdelantos(this.liquidacionAdelanto.empleadoId, pInicio, pFin).subscribe({
      next: (data) => {
        this.adelantosEmpleado = data;
        this.loadingAdelantos = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los adelantos.' });
        this.loadingAdelantos = false;
      }
    });
  }

  toggleFiltroAdelantos(): void {
    this.filtroVerTodosAdelantos = !this.filtroVerTodosAdelantos;
    this.recargarAdelantos();
  }

  editarAdelanto(a: AdelantoResponse): void {
    this.adelantoEnEdicion = a;
    let fechaObj = new Date();
    if (a.fecha) {
      const partes = a.fecha.split('-');
      if (partes.length === 3) {
        fechaObj = new Date(parseInt(partes[0]), parseInt(partes[1]) - 1, parseInt(partes[2]), 12, 0, 0);
      }
    }

    this.nuevoAdelanto = {
      monto: a.monto,
      fecha: fechaObj,
      metodoPago: a.metodoPago || 'EFECTIVO',
      motivo: a.motivo || '',
      descontarDeCaja: false
    };
  }

  cancelarEdicion(): void {
    this.adelantoEnEdicion = null;
    this.resetFormularioAdelanto();
  }

  resetFormularioAdelanto(): void {
    let fechaDefecto = new Date();
    if (this.liquidacionAdelanto?.fechaInicio && this.liquidacionAdelanto?.fechaFin) {
      const partesFin = this.liquidacionAdelanto.fechaFin.split('-');
      const fechaFinLiq = new Date(parseInt(partesFin[0]), parseInt(partesFin[1]) - 1, parseInt(partesFin[2]));
      const partesIni = this.liquidacionAdelanto.fechaInicio.split('-');
      const fechaIniLiq = new Date(parseInt(partesIni[0]), parseInt(partesIni[1]) - 1, parseInt(partesIni[2]));

      const hoy = new Date();
      hoy.setHours(0, 0, 0, 0);
      fechaFinLiq.setHours(0, 0, 0, 0);
      fechaIniLiq.setHours(0, 0, 0, 0);

      if (hoy >= fechaIniLiq && hoy <= fechaFinLiq) {
        fechaDefecto = new Date();
      } else if (hoy > fechaFinLiq) {
        fechaDefecto = fechaFinLiq;
      } else {
        fechaDefecto = fechaIniLiq;
      }
    }

    this.nuevoAdelanto = {
      monto: null,
      fecha: fechaDefecto,
      metodoPago: 'EFECTIVO',
      motivo: '',
      descontarDeCaja: false
    };
  }

  guardarNuevoAdelanto(): void {
    if (!this.liquidacionAdelanto) return;
    if (!this.nuevoAdelanto.monto || this.nuevoAdelanto.monto <= 0) {
      this.messageService.add({ severity: 'warn', summary: 'Atención', detail: 'Ingresa un monto válido para el adelanto.' });
      return;
    }

    this.guardandoAdelanto = true;
    const req: AdelantoRequest = {
      empleadoId: this.liquidacionAdelanto.empleadoId,
      monto: this.nuevoAdelanto.monto,
      fecha: this.formatDate(this.nuevoAdelanto.fecha || new Date()),
      metodoPago: this.nuevoAdelanto.metodoPago,
      motivo: this.nuevoAdelanto.motivo?.trim() || '',
      descontarDeCaja: this.nuevoAdelanto.descontarDeCaja
    };

    if (this.adelantoEnEdicion) {
      this.adelantoService.actualizarAdelanto(this.adelantoEnEdicion.id, req).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Actualizado',
            detail: `Adelanto de S/ ${req.monto.toFixed(2)} actualizado correctamente.`
          });
          this.guardandoAdelanto = false;
          this.cancelarEdicion();
          this.recargarAdelantos();
          this.cargarLiquidaciones();
        },
        error: (err) => {
          this.guardandoAdelanto = false;
          const msg = err.error?.message || 'Error al actualizar el adelanto.';
          this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
        }
      });
    } else {
      this.adelantoService.registrarAdelanto(req).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Registrado',
            detail: `Adelanto de S/ ${req.monto.toFixed(2)} registrado correctamente.`
          });
          this.guardandoAdelanto = false;
          this.resetFormularioAdelanto();
          this.recargarAdelantos();
          this.cargarLiquidaciones();
        },
        error: (err) => {
          this.guardandoAdelanto = false;
          const msg = err.error?.message || 'Error al registrar el adelanto.';
          this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
        }
      });
    }
  }

  eliminarAdelanto(adelanto: AdelantoResponse): void {
    if (!confirm(`¿Estás seguro de eliminar el adelanto de S/ ${adelanto.monto.toFixed(2)}? Esta acción reintegrará el saldo y eliminará el egreso si fue registrado en caja.`)) {
      return;
    }

    this.adelantoService.eliminarAdelanto(adelanto.id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Eliminado', detail: 'Adelanto eliminado correctamente.' });
        if (this.adelantoEnEdicion?.id === adelanto.id) {
          this.cancelarEdicion();
        }
        this.recargarAdelantos();
        this.cargarLiquidaciones();
      },
      error: (err) => {
        const msg = err.error?.message || 'No se pudo eliminar el adelanto.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
      }
    });
  }

  anularAdelanto(adelanto: AdelantoResponse): void {
    if (!confirm(`¿Estás seguro de anular el adelanto de S/ ${adelanto.monto.toFixed(2)}?`)) {
      return;
    }

    this.adelantoService.anularAdelanto(adelanto.id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'info', summary: 'Anulado', detail: 'Adelanto anulado correctamente.' });
        this.recargarAdelantos();
        this.cargarLiquidaciones();
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo anular el adelanto.' });
      }
    });
  }

  get adelantosEnPeriodo(): AdelantoResponse[] {
    if (!this.liquidacionAdelanto) return [];
    const inicio = this.liquidacionAdelanto.fechaInicio;
    const fin = this.liquidacionAdelanto.fechaFin;
    return this.adelantosEmpleado.filter(a => a.fecha >= inicio && a.fecha <= fin);
  }

  get adelantosEnPeriodoCount(): number {
    return this.adelantosEnPeriodo.length;
  }

  get totalAdelantosActivos(): number {
    if (!this.liquidacionAdelanto) return 0;
    if (!this.filtroVerTodosAdelantos) {
      return this.adelantosEmpleado.reduce((acc, curr) => acc + (curr.monto || 0), 0);
    }
    return this.adelantosEnPeriodo.reduce((acc, curr) => acc + (curr.monto || 0), 0);
  }

  get saldoRestanteAdelanto(): number {
    if (!this.liquidacionAdelanto) return 0;
    const ganancia = (this.liquidacionAdelanto.totalComision || 0) + (this.liquidacionAdelanto.sueldoFijoProporcional || 0);
    return Math.max(0, ganancia - this.totalAdelantosActivos);
  }

  // Filtra por servicio/producto, cliente o número de ticket (sin distinguir mayúsculas ni tildes)
  get detalleFiltrado(): DetalleLiquidacion[] {
    const termino = this.normalizar(this.busquedaDetalle);
    if (!termino) return this.detalle;
    return this.detalle.filter(d =>
      this.normalizar(d.descripcion).includes(termino) ||
      this.normalizar(d.cliente).includes(termino) ||
      String(d.ticketId).includes(termino.replace('#', ''))
    );
  }

  // Totales del periodo completo (no dependen de la búsqueda)
  get totalVentasPeriodo(): number {
    return this.detalle.reduce((acc, d) => acc + (d.subtotal || 0), 0);
  }

  get totalComisionPeriodo(): number {
    return this.detalle.reduce((acc, d) => acc + (d.comision || 0), 0);
  }

  // Totales de lo que se está mostrando con la búsqueda
  get totalDetalleVentas(): number {
    return this.detalleFiltrado.reduce((acc, d) => acc + (d.subtotal || 0), 0);
  }

  get totalDetalleComision(): number {
    return this.detalleFiltrado.reduce((acc, d) => acc + (d.comision || 0), 0);
  }

  private normalizar(texto: string | null | undefined): string {
    return (texto || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
  }

  public formatDate(date: Date): string {
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
