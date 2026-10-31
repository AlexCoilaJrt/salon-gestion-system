import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CajaService, SesionCaja } from '../services/caja.service';
import { CatalogoService, Producto, Servicio } from '../../catalogo/services/catalogo.service';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { PaginatorModule } from 'primeng/paginator';
import { CalendarModule } from 'primeng/calendar';
import { TooltipModule } from 'primeng/tooltip';
import { RrhhService, Empleado } from '../../rrhh/services/rrhh.service';
import { EmpresaService, EmpresaResponse } from '../../../core/services/empresa.service';
import { AuthService } from '../../../core/auth/services/auth.service';

export interface VentaItem {
  id: number;
  nombre: string;
  precio: number;
  tipo: 'producto' | 'servicio';
  imagen?: string;
}

export interface TicketItem extends VentaItem {
  cantidad: number;
  subtotal: number;
  empleadoId?: number;
  detalleId?: number;
}

@Component({
  selector: 'app-caja',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    ToastModule, 
    ConfirmDialogModule, 
    ButtonModule, 
    InputNumberModule, 
    AutoCompleteModule, 
    DialogModule, 
    SelectButtonModule, 
    InputTextModule, 
    DropdownModule, 
    TableModule, 
    PaginatorModule,
    CalendarModule,
    TooltipModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './caja.component.html'
})
export class CajaComponent implements OnInit {
  private cajaService = inject(CajaService);
  private catalogoService = inject(CatalogoService);
  private rrhhService = inject(RrhhService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private empresaService = inject(EmpresaService);
  private authService = inject(AuthService);

  get isAdmin(): boolean {
    const user = this.authService.getCurrentUser();
    const roles: any[] = user?.usuario?.roles || user?.roles || [];
    return roles.some(r => {
      const roleStr = typeof r === 'string' ? r.toUpperCase() : (r as any).nombre?.toUpperCase();
      return roleStr === 'ADMIN' || roleStr === 'ROLE_ADMIN' || roleStr === 'ADMINISTRADOR';
    });
  }

  empresa = signal<EmpresaResponse | null>(null);

  sesionActiva: SesionCaja | null = null;
  loading: boolean = true;
  montoApertura: number = 0;

  catalogoCompleto: VentaItem[] = [];
  resultadosBusqueda: VentaItem[] = [];
  serviciosFrecuentes: VentaItem[] = [];
  itemSeleccionado: any;
  empleados: Empleado[] = [];

  // Carrito / Ticket
  ticket: TicketItem[] = [];
  subtotalTicket: number = 0;
  descuentoTicket: number = 0;
  totalTicket: number = 0;

  // Dialogo de Cobro
  cobroDialog: boolean = false;
  ticketPendienteId: number | null = null; // ID del ticket si estamos cobrando un ticket en espera
  
  // Lista de tickets en espera
  ticketsPendientes: any[] = [];
  
  // Filtro y Paginación para Órdenes en Espera
  filtroEspera: string = '';
  paginaActualEspera: number = 0;
  itemsPorPaginaEspera: number = 10;
  opcionesItemsPorPagina = [5, 10, 15, 20];

  get ticketsPendientesFiltrados() {
    if (!this.filtroEspera.trim()) return this.ticketsPendientes;
    const term = this.filtroEspera.toLowerCase();
    return this.ticketsPendientes.filter(tp => 
      tp.clienteNombreCompleto?.toLowerCase().includes(term) ||
      tp.id?.toString().includes(term) ||
      tp.detalles?.some((d: any) => d.nombreItem?.toLowerCase().includes(term))
    );
  }

  get ticketsPendientesPaginados() {
    const filtrados = this.ticketsPendientesFiltrados;
    const start = this.paginaActualEspera * this.itemsPorPaginaEspera;
    return filtrados.slice(start, start + this.itemsPorPaginaEspera);
  }

  onPageChangeEspera(event: any) {
    this.paginaActualEspera = event.page;
    this.itemsPorPaginaEspera = event.rows;
  }

  // Dashboard & Movimientos de Caja
  vistaActual: 'pos' | 'espera' | 'dashboard' = 'pos';
  ticketsDashboard: any[] = [];
  resumenDashboard: any = null;

  // Nuevas métricas y filtros de período
  totalesPeriodo: {
    totalHoy: number;
    cantidadVentasHoy: number;
    totalSemana: number;
    cantidadVentasSemana: number;
    totalMes: number;
    cantidadVentasMes: number;
  } | null = null;
  movimientosResumen: any = null;
  periodoSeleccionado: 'TURNO' | 'HOY' | 'SEMANA' | 'MES' | 'FECHA' = 'TURNO';
  fechaFiltroMovimientos: Date | null = null;
  rankingTrabajadoras: any[] = [];
  cargandoMovimientos: boolean = false;
  trabajadoraSeleccionadaDetalle: any = null;
  detalleTrabajadoraDialog: boolean = false;

  // Filtro de búsqueda rápida en movimientos (cliente, ticket, empleado, servicio)
  filtroTransacciones: string = '';

  get ticketsDashboardFiltrados(): any[] {
    if (!this.filtroTransacciones || !this.filtroTransacciones.trim()) {
      return this.ticketsDashboard;
    }
    const term = this.filtroTransacciones.toLowerCase().trim();
    return this.ticketsDashboard.filter(t => 
      t.clienteNombreCompleto?.toLowerCase().includes(term) ||
      t.id?.toString().includes(term) ||
      t.metodoPago?.toLowerCase().includes(term) ||
      t.detalles?.some((d: any) => 
        d.empleadoNombreCompleto?.toLowerCase().includes(term) ||
        d.servicioNombre?.toLowerCase().includes(term) ||
        d.productoNombre?.toLowerCase().includes(term)
      )
    );
  }

  cambiarVista(vista: 'pos' | 'espera' | 'dashboard') {
    this.vistaActual = vista;
    if (vista === 'dashboard') {
      this.cargarDatosDashboard();
    }
  }

  cargarDatosDashboard() {
    this.cargarTotalesPeriodo();
    this.cargarMovimientosFiltrados();
    this.cajaService.obtenerResumenActual().subscribe({
      next: (res) => {
        this.resumenDashboard = res;
        this.montoDeclarado = res.totalEsperadoEfectivo; // Sync for close
      },
      error: () => {
        this.resumenDashboard = null;
      }
    });
  }

  cargarTotalesPeriodo() {
    this.cajaService.obtenerTotalesPeriodo().subscribe({
      next: (data) => this.totalesPeriodo = data,
      error: () => this.totalesPeriodo = null
    });
  }

  cambiarPeriodoMovimientos(periodo: 'TURNO' | 'HOY' | 'SEMANA' | 'MES' | 'FECHA') {
    this.periodoSeleccionado = periodo;
    if (periodo !== 'FECHA') {
      this.fechaFiltroMovimientos = null;
      this.cargarMovimientosFiltrados();
    }
  }

  onFechaFiltroChange() {
    if (this.fechaFiltroMovimientos) {
      this.periodoSeleccionado = 'FECHA';
      this.cargarMovimientosFiltrados();
    }
  }

  cargarMovimientosFiltrados() {
    this.cargandoMovimientos = true;
    let fechaStr: string | undefined = undefined;
    if (this.periodoSeleccionado === 'FECHA' && this.fechaFiltroMovimientos) {
      const d = this.fechaFiltroMovimientos;
      const anio = d.getFullYear();
      const mes = String(d.getMonth() + 1).padStart(2, '0');
      const dia = String(d.getDate()).padStart(2, '0');
      fechaStr = `${anio}-${mes}-${dia}`;
    }

    this.cajaService.obtenerMovimientosPeriodo(this.periodoSeleccionado, fechaStr).subscribe({
      next: (res) => {
        this.movimientosResumen = res;
        this.ticketsDashboard = res.tickets || [];
        this.rankingTrabajadoras = res.rankingTrabajadoras || [];
        this.cargandoMovimientos = false;
      },
      error: () => {
        this.cargandoMovimientos = false;
        this.ticketsDashboard = [];
        this.rankingTrabajadoras = [];
      }
    });
  }

  abrirDetalleTrabajadora(trab: any) {
    this.trabajadoraSeleccionadaDetalle = trab;
    this.detalleTrabajadoraDialog = true;
  }

  fechaActual: Date = new Date();
  metodoPago: string = 'Efectivo';
  metodosPago = [
    { label: 'Efectivo', value: 'Efectivo', icon: 'pi-money-bill' },
    { label: 'Yape / Plin', value: 'Transferencia', icon: 'pi-mobile' }
  ];
  montoRecibido: number = 0;
  vuelto: number = 0;
  referenciaPago: string = '';
  nombreCliente: string = '';

  // Dialogo de Éxito / Ticket
  ticketGeneradoDialog: boolean = false;
  ticketGenerado: any = null;
  ticketGeneradoId: number | null = null;

  ngOnInit() {
    this.verificarCaja();
    this.cargarEmpleados();
    this.cargarEmpresa();
  }

  cargarEmpresa() {
    this.empresaService.getEmpresaActiva().subscribe({
      next: (res) => this.empresa.set(res.data),
      error: () => this.empresa.set(null)
    });
  }

  cargarEmpleados() {
    this.rrhhService.getEmpleados().subscribe(res => {
      this.empleados = res
        .filter(e => e.estado !== false)
        .map(e => ({
          ...e,
          nombreCompleto: `${e.nombres} ${e.apellidos || ''}`.trim()
        }));
    });
  }

  cargarTicketsPendientes() {
    this.cajaService.obtenerTicketsPendientes().subscribe({
      next: (tickets) => this.ticketsPendientes = tickets
    });
  }

  verificarCaja() {
    this.loading = true;
    this.cajaService.obtenerCajaActual().subscribe({
      next: (sesion) => {
        this.sesionActiva = sesion;
        this.cargarCatalogoParaVenta();
        this.cargarTicketsPendientes();
        this.loading = false;
      },
      error: (err) => {
        // No hay caja abierta
        this.sesionActiva = null;
        this.loading = false;
      }
    });
  }

  abrirCaja() {
    if (this.montoApertura < 0) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'El monto no puede ser negativo.' });
      return;
    }

    this.loading = true;
    this.cajaService.abrirCaja({ montoInicial: this.montoApertura, usuarioId: 1 }).subscribe({
      next: (sesion) => {
        this.sesionActiva = sesion;
        this.cargarCatalogoParaVenta();
        this.cargarTicketsPendientes();
        this.loading = false;
        this.messageService.add({ severity: 'success', summary: 'Caja Abierta', detail: 'Turno iniciado correctamente.' });
      },
      error: (err) => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al abrir caja' });
      }
    });
  }

  cargarCatalogoParaVenta() {
    this.catalogoCompleto = [];
    
    forkJoin({
      servicios: this.catalogoService.getServicios(),
      productos: this.catalogoService.getProductos(),
      frecuentes: this.cajaService.obtenerServiciosFrecuentes()
    }).subscribe(({ servicios, productos, frecuentes }) => {
      // Cargar Servicios
      servicios.filter(s => s.estado === true).forEach(s => {
        this.catalogoCompleto.push({
          id: s.id!,
          nombre: s.nombre,
          precio: s.precioBase,
          tipo: 'servicio',
          imagen: 'pi-briefcase'
        });
      });

      // Cargar Productos (Solo VENTA DIRECTA)
      productos.filter(p => p.estado === true && p.ventaDirecta === true).forEach(p => {
        this.catalogoCompleto.push({
          id: p.id!,
          nombre: p.nombre,
          precio: p.precioVenta,
          tipo: 'producto',
          imagen: 'pi-box'
        });
      });

      // Extraer los top frecuentes basados en el backend (que trae el ID en servicioId)
      this.serviciosFrecuentes = [];
      if (frecuentes && frecuentes.length > 0) {
        frecuentes.forEach(f => {
          const item = this.catalogoCompleto.find(c => c.tipo === 'servicio' && c.id === f.servicioId);
          if (item && this.serviciosFrecuentes.length < 4) {
            this.serviciosFrecuentes.push(item);
          }
        });
      }

      // Fallback para instalaciones nuevas sin ventas: tomar los primeros 4 servicios del catálogo
      if (this.serviciosFrecuentes.length === 0) {
        this.serviciosFrecuentes = this.catalogoCompleto.filter(c => c.tipo === 'servicio').slice(0, 4);
      }
    });
  }

  buscarItem(event: any) {
    const query = event.query.toLowerCase();
    this.resultadosBusqueda = this.catalogoCompleto.filter(item => 
      item.nombre.toLowerCase().includes(query)
    );
  }

  seleccionarItem(event: any) {
    const item: VentaItem = event.value;
    this.agregarAlTicket(item);
    this.itemSeleccionado = null;
  }

  agregarAlTicket(item: VentaItem) {
    // Buscar si ya existe en el ticket
    const existente = this.ticket.find(t => t.id === item.id && t.tipo === item.tipo);
    
    if (existente) {
      existente.cantidad += 1;
      existente.subtotal = existente.cantidad * existente.precio;
    } else {
      this.ticket.push({
        ...item,
        cantidad: 1,
        subtotal: item.precio
      });
    }
    
    this.calcularTotales();
  }

  eliminarDelTicket(index: number) {
    this.ticket.splice(index, 1);
    this.calcularTotales();
  }

  vaciarTicket() {
    this.ticket = [];
    this.ticketPendienteId = null;
    this.calcularTotales();
  }

  calcularTotales() {
    this.subtotalTicket = this.ticket.reduce((sum, item) => sum + item.subtotal, 0);
    this.totalTicket = this.subtotalTicket - this.descuentoTicket;
  }

  abrirCobro() {
    if (this.ticket.length === 0) return;
    // this.ticketPendienteId = null; // Do NOT reset this, we might be charging a loaded pending ticket
    this.montoRecibido = this.totalTicket;
    this.vuelto = 0;
    this.metodoPago = 'Efectivo';
    this.referenciaPago = '';
    // No reseteamos nombreCliente por si viene de la orden
    if (!this.ticketPendienteId) {
      this.nombreCliente = '';
    }
    this.servicioSelloId = null;
    this.serviciosSellosSeleccionados = this.serviciosEnTicket.map(s => s.id);
    this.onEspecialistaIndividualChange();
    this.cobroDialog = true;
  }

  abrirCobroPendiente(ticketPendiente: any) {
    this.ticketPendienteId = ticketPendiente.id;
    this.totalTicket = ticketPendiente.total;
    this.montoRecibido = this.totalTicket;
    this.vuelto = 0;
    this.metodoPago = 'Efectivo';
    this.referenciaPago = '';
    this.nombreCliente = ticketPendiente.clienteNombreCompleto;
    this.servicioSelloId = null;
    
    // Cargar los detalles al ticket actual para validar empleados
    this.ticket = ticketPendiente.detalles.map((d: any) => ({
      detalleId: d.id,
      id: d.servicioId || d.productoId,
      nombre: d.servicioNombre || d.productoNombre,
      precio: d.precioUnitario,
      tipo: d.servicioId ? 'servicio' : 'producto',
      cantidad: d.cantidad,
      subtotal: d.subtotal,
      empleadoId: d.empleadoId
    }));
    this.subtotalTicket = ticketPendiente.total;
    this.descuentoTicket = 0;

    this.serviciosSellosSeleccionados = this.serviciosEnTicket.map(s => s.id);
    this.onEspecialistaIndividualChange();
    this.cobroDialog = true;
  }

  calcularVuelto() {
    if (this.montoRecibido > this.totalTicket) {
      this.vuelto = this.montoRecibido - this.totalTicket;
    } else {
      this.vuelto = 0;
    }
  }

  get ticketNumero(): string {
    if (!this.ticketGenerado?.id) return '000001';
    return this.ticketGenerado.id.toString().padStart(6, '0');
  }

  get serviciosEnTicket() {
    return this.ticket.filter(t => t.tipo === 'servicio');
  }
  
  servicioSelloId: number | null = null;
  serviciosSellosSeleccionados: number[] = [];
  especialistaTodosId: number | null = null;

  get todosSellosSeleccionados(): boolean {
    const servicios = this.serviciosEnTicket;
    return servicios.length > 0 && servicios.every(s => this.serviciosSellosSeleccionados.includes(s.id));
  }

  toggleTodosSellos() {
    if (this.todosSellosSeleccionados) {
      this.serviciosSellosSeleccionados = [];
    } else {
      this.serviciosSellosSeleccionados = this.serviciosEnTicket.map(s => s.id);
    }
  }

  toggleSelloServicio(servicioId: number) {
    const index = this.serviciosSellosSeleccionados.indexOf(servicioId);
    if (index > -1) {
      this.serviciosSellosSeleccionados.splice(index, 1);
    } else {
      this.serviciosSellosSeleccionados.push(servicioId);
    }
  }

  isSelloSeleccionado(servicioId: number): boolean {
    return this.serviciosSellosSeleccionados.includes(servicioId);
  }

  asignarEspecialistaATodos(empleadoId: number | null | undefined) {
    if (!empleadoId) return;
    this.especialistaTodosId = empleadoId;
    this.ticket.forEach(item => {
      if (item.tipo === 'servicio') {
        item.empleadoId = empleadoId;
      }
    });
    const emp = this.empleados.find(e => e.id === empleadoId);
    const nombre = emp ? (emp.nombreCompleto || emp.nombres) : 'Especialista';
    this.messageService.add({
      severity: 'info',
      summary: 'Especialista Asignada',
      detail: `${nombre} asignada a todos los servicios seleccionados.`,
      life: 2500
    });
  }

  onEspecialistaIndividualChange() {
    const servicios = this.serviciosEnTicket;
    if (servicios.length > 0 && servicios.every(s => s.empleadoId && s.empleadoId === servicios[0].empleadoId)) {
      this.especialistaTodosId = servicios[0].empleadoId || null;
    } else {
      this.especialistaTodosId = null;
    }
  }

  confirmarCobro() {
    if (this.metodoPago === 'Efectivo' && this.montoRecibido < this.totalTicket) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'El monto recibido es menor al total' });
      return;
    }

    if (this.metodoPago === 'Transferencia' && !this.referenciaPago.trim()) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Debe ingresar el número de operación de Yape/Plin' });
      return;
    }

    // Validación de Empleado en Servicios
    const serviciosSinEmpleado = this.ticket.filter(item => item.tipo === 'servicio' && !item.empleadoId);
    if (serviciosSinEmpleado.length > 0) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Debe asignar a la empleada que realizó los servicios.' });
      return;
    }

    const detallesRequest = this.ticket.map(item => ({
      id: item.detalleId,
      cantidad: item.cantidad,
      servicioId: item.tipo === 'servicio' ? item.id : undefined,
      productoId: item.tipo === 'producto' ? item.id : undefined,
      empleadoId: item.empleadoId
    }));

    const request = {
      metodoPago: this.metodoPago.toUpperCase(),
      nombreClienteNoRegistrado: this.nombreCliente || 'Público General',
      detalles: detallesRequest,
      servicioSelloId: this.serviciosSellosSeleccionados.length === 1 ? this.serviciosSellosSeleccionados[0] : undefined,
      serviciosSellosIds: this.serviciosSellosSeleccionados
    };

    if (this.ticketPendienteId) {
      this.cajaService.pagarTicket(this.ticketPendienteId, request).subscribe({
        next: (res) => {
          this.ticketGenerado = res;
          this.ticketGeneradoId = res.id;
          this.messageService.add({ severity: 'success', summary: 'Venta Completada', detail: 'El pago se procesó correctamente.' });
          this.cobroDialog = false;
          this.ticketGeneradoDialog = true;
          this.cargarTicketsPendientes();
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al procesar el pago.' });
        }
      });
    } else {
      this.cajaService.emitirTicket(request).subscribe({
        next: (res) => {
          this.ticketGenerado = res;
          this.ticketGeneradoId = res.id;
          this.messageService.add({ severity: 'success', summary: 'Venta Completada', detail: 'El pago se procesó correctamente.' });
          this.cobroDialog = false;
          this.ticketGeneradoDialog = true;
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al procesar la venta.' });
        }
      });
    }
  }

  // Cierre de Caja
  cierreDialog: boolean = false;
  resumenCaja: any = null;
  montoDeclarado: number = 0;
  observacionesCierre: string = '';

  abrirDialogoCierre() {
    if (this.ticketsPendientes.length > 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Cierre de Caja Bloqueado',
        detail: `No puedes cerrar caja porque hay ${this.ticketsPendientes.length} orden(es) en espera de pago. Debes cobrarlas o eliminarlas primero.`,
        life: 5000
      });
      this.cambiarVista('espera');
      return;
    }

    this.cajaService.obtenerResumenActual().subscribe({
      next: (res) => {
        this.resumenCaja = res;
        this.montoDeclarado = res.totalEsperadoEfectivo; // Sugerir el monto exacto por defecto
        this.observacionesCierre = '';
        this.cierreDialog = true;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo obtener el resumen de caja.' });
      }
    });
  }

  confirmarCierre() {
    if (this.ticketsPendientes.length > 0) {
      this.messageService.add({
        severity: 'error',
        summary: 'Cierre Bloqueado',
        detail: `Existen ${this.ticketsPendientes.length} órdenes en espera pendientes de cobro.`
      });
      return;
    }

    this.cajaService.cerrarCaja({
      montoDeclarado: this.montoDeclarado,
      observaciones: this.observacionesCierre
    }).subscribe({
      next: (res) => {
        this.messageService.add({ severity: 'success', summary: 'Caja Cerrada', detail: 'Tu turno ha sido cerrado correctamente.' });
        this.cierreDialog = false;
        this.sesionActiva = null; // Volver a la pantalla de Apertura
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al cerrar caja.' });
      }
    });
  }

  confirmarEliminarOrden(event: Event, tp: any) {
    event.stopPropagation();
    if (!this.isAdmin) {
      this.messageService.add({
        severity: 'error',
        summary: 'Acceso Denegado',
        detail: 'Solo el Administrador tiene permiso para eliminar órdenes en espera.'
      });
      return;
    }

    const cliente = tp.clienteNombreCompleto || 'Público General';
    const total = Number(tp.total || 0).toFixed(2);

    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `¿Estás seguro de eliminar la Orden #${tp.id} (${cliente}) por S/ ${total}? Esta acción cancelará la orden pendiente de pago y no se podrá deshacer.`,
      header: 'Confirmar Eliminación de Orden',
      icon: 'pi pi-exclamation-triangle text-red-500',
      acceptLabel: 'Sí, eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.eliminarOrden(tp.id);
      }
    });
  }

  eliminarOrden(id: number) {
    this.cajaService.eliminarTicketPendiente(id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Orden Eliminada',
          detail: `La orden #${id} fue eliminada correctamente.`
        });
        this.cargarTicketsPendientes();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: err.error?.message || 'No se pudo eliminar la orden en espera.'
        });
      }
    });
  }

  // Edición de Precios de Orden en Espera (Solo Administrador)
  editarPreciosDialog: boolean = false;
  ordenParaEditar: any = null;
  detallesEdicion: {
    id: number;
    nombre: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
  }[] = [];
  guardandoPrecios: boolean = false;

  abrirEditarPrecios(event: Event, tp: any) {
    event.stopPropagation();
    this.ordenParaEditar = tp;
    this.detallesEdicion = (tp.detalles || []).map((d: any) => {
      const cant = Number(d.cantidad) || 1;
      const pu = Number(d.precioUnitario) || 0;
      return {
        id: d.id,
        nombre: d.servicioNombre || d.productoNombre || 'Servicio',
        cantidad: cant,
        precioUnitario: pu,
        subtotal: pu * cant
      };
    });
    this.editarPreciosDialog = true;
  }

  recalcularSubtotalEdicion(item: any) {
    item.cantidad = Math.max(1, Number(item.cantidad) || 1);
    item.precioUnitario = Math.max(0, Number(item.precioUnitario) || 0);
    item.subtotal = item.cantidad * item.precioUnitario;
  }

  get totalEdicion(): number {
    return this.detallesEdicion.reduce((sum, item) => sum + (item.subtotal || 0), 0);
  }

  guardarNuevosPrecios() {
    if (!this.ordenParaEditar) return;
    this.guardandoPrecios = true;

    const request = {
      detalles: this.detallesEdicion.map(d => ({
        id: d.id,
        precioUnitario: d.precioUnitario,
        cantidad: d.cantidad
      }))
    };

    this.cajaService.actualizarPreciosTicket(this.ordenParaEditar.id, request).subscribe({
      next: (res) => {
        this.guardandoPrecios = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Precios Actualizados',
          detail: `Se corrigieron los precios de la orden #${this.ordenParaEditar.id}.`
        });
        this.editarPreciosDialog = false;
        this.ordenParaEditar = null;
        this.cargarTicketsPendientes();
      },
      error: (err) => {
        this.guardandoPrecios = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: err.error?.message || 'No se pudieron actualizar los precios de la orden.'
        });
      }
    });
  }

  imprimirTicket() {
    this.fechaActual = new Date();
    // Simulación de impresión
    setTimeout(() => {
      window.print();
    }, 100);
  }

  cerrarVenta() {
    this.ticketGeneradoDialog = false;
    this.vaciarTicket();
  }

  obtenerNombreEmpleado(empleadoId?: number | null): string {
    if (!empleadoId) return 'No Asignado';
    const emp = this.empleados.find(e => e.id === empleadoId);
    return emp ? emp.nombres + ' ' + emp.apellidos : 'Desconocido';
  }
}
