import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
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
import { RrhhService, Empleado } from '../../rrhh/services/rrhh.service';

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
  imports: [CommonModule, FormsModule, ToastModule, ButtonModule, InputNumberModule, AutoCompleteModule, DialogModule, SelectButtonModule, InputTextModule, DropdownModule],
  providers: [MessageService],
  templateUrl: './caja.component.html'
})
export class CajaComponent implements OnInit {
  private cajaService = inject(CajaService);
  private catalogoService = inject(CatalogoService);
  private rrhhService = inject(RrhhService);
  private messageService = inject(MessageService);

  sesionActiva: SesionCaja | null = null;
  loading: boolean = true;
  montoApertura: number = 0;

  catalogoCompleto: VentaItem[] = [];
  resultadosBusqueda: VentaItem[] = [];
  itemSeleccionado: any;
  empleados: Empleado[] = [];

  // Carrito / Ticket
  ticket: TicketItem[] = [];
  subtotalTicket: number = 0;
  descuentoTicket: number = 0;
  totalTicket: number = 0;

  // Dialogo de Cobro
  cobroDialog: boolean = false;
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

  ngOnInit() {
    this.verificarCaja();
    this.cargarEmpleados();
  }

  cargarEmpleados() {
    this.rrhhService.getEmpleados().subscribe(res => {
      this.empleados = res.filter(e => e.estado !== false);
    });
  }

  verificarCaja() {
    this.loading = true;
    this.cajaService.obtenerCajaActual().subscribe({
      next: (sesion) => {
        this.sesionActiva = sesion;
        this.cargarCatalogoParaVenta();
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
    
    // Cargar Servicios
    this.catalogoService.getServicios().subscribe(servicios => {
      servicios.filter(s => s.estado === true).forEach(s => {
        this.catalogoCompleto.push({
          id: s.id!,
          nombre: s.nombre,
          precio: s.precioBase,
          tipo: 'servicio',
          imagen: 'pi-briefcase'
        });
      });
    });

    // Cargar Productos (Solo VENTA DIRECTA)
    this.catalogoService.getProductos().subscribe(productos => {
      productos.filter(p => p.estado === true && p.ventaDirecta === true).forEach(p => {
        this.catalogoCompleto.push({
          id: p.id!,
          nombre: p.nombre,
          precio: p.precioVenta,
          tipo: 'producto',
          imagen: 'pi-box'
        });
      });
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
    this.calcularTotales();
  }

  calcularTotales() {
    this.subtotalTicket = this.ticket.reduce((sum, item) => sum + item.subtotal, 0);
    this.totalTicket = this.subtotalTicket - this.descuentoTicket;
  }

  abrirCobro() {
    if (this.ticket.length === 0) return;
    this.montoRecibido = this.totalTicket;
    this.vuelto = 0;
    this.metodoPago = 'Efectivo';
    this.referenciaPago = '';
    this.nombreCliente = '';
    this.cobroDialog = true;
  }

  calcularVuelto() {
    if (this.montoRecibido > this.totalTicket) {
      this.vuelto = this.montoRecibido - this.totalTicket;
    } else {
      this.vuelto = 0;
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
      cantidad: item.cantidad,
      servicioId: item.tipo === 'servicio' ? item.id : undefined,
      productoId: item.tipo === 'producto' ? item.id : undefined,
      empleadoId: item.empleadoId
    }));

    const request = {
      metodoPago: this.metodoPago.toUpperCase(),
      nombreClienteNoRegistrado: this.nombreCliente || 'Público General',
      detalles: detallesRequest
    };

    this.cajaService.emitirTicket(request).subscribe({
      next: (res) => {
        this.messageService.add({ severity: 'success', summary: 'Venta Completada', detail: 'El pago se procesó correctamente.' });
        this.cobroDialog = false;
        this.ticketGeneradoDialog = true;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al procesar la venta.' });
      }
    });
  }

  imprimirTicket() {
    // Simulación de impresión (más adelante crearemos el diseño del voucher térmico)
    window.print();
  }

  cerrarVenta() {
    this.ticketGeneradoDialog = false;
    this.vaciarTicket();
  }
}
