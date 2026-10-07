import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CajaService, SesionCaja } from '../services/caja.service';
import { CatalogoService, Producto, Servicio } from '../../catalogo/services/catalogo.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { DialogModule } from 'primeng/dialog';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { PaginatorModule } from 'primeng/paginator';
import { RrhhService, Empleado } from '../../rrhh/services/rrhh.service';
import { EmpresaService, EmpresaResponse } from '../../../core/services/empresa.service';

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
}

@Component({
  selector: 'app-caja',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastModule, ButtonModule, InputNumberModule, AutoCompleteModule, DialogModule, SelectButtonModule, InputTextModule, DropdownModule, TableModule, PaginatorModule],
  providers: [MessageService],
  templateUrl: './caja.component.html'
})
export class CajaComponent implements OnInit {
  private cajaService = inject(CajaService);
  private catalogoService = inject(CatalogoService);
  private rrhhService = inject(RrhhService);
  private messageService = inject(MessageService);
  private empresaService = inject(EmpresaService);

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

  // Dashboard de Caja
  vistaActual: 'pos' | 'espera' | 'dashboard' = 'pos';
  ticketsDashboard: any[] = [];
  resumenDashboard: any = null;

  cambiarVista(vista: 'pos' | 'espera' | 'dashboard') {
    this.vistaActual = vista;
    if (vista === 'dashboard') {
      this.cargarDatosDashboard();
    }
  }

  cargarDatosDashboard() {
    this.cajaService.obtenerTicketsCajaActual().subscribe({
      next: (tickets) => this.ticketsDashboard = tickets
    });
    this.cajaService.obtenerResumenActual().subscribe({
      next: (res) => {
        this.resumenDashboard = res;
        this.montoDeclarado = res.totalEsperadoEfectivo; // Sync for close
      }
    });
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
      this.empleados = res.filter(e => e.estado !== false);
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
      cantidad: item.cantidad,
      servicioId: item.tipo === 'servicio' ? item.id : undefined,
      productoId: item.tipo === 'producto' ? item.id : undefined,
      empleadoId: item.empleadoId
    }));

    const request = {
      metodoPago: this.metodoPago.toUpperCase(),
      nombreClienteNoRegistrado: this.nombreCliente || 'Público General',
      detalles: detallesRequest,
      servicioSelloId: this.servicioSelloId || undefined
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
