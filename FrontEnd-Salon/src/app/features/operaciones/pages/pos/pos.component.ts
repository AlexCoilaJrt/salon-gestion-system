import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CatalogoService, Servicio, Producto } from '../../../catalogo/services/catalogo.service';
import { TicketService, TicketResponse } from '../../../finanzas/services/ticket.service';
import { RrhhService, Empleado } from '../../../rrhh/services/rrhh.service';
import { AgendaService, ClienteResponse } from '../agenda/agenda.service';
import { FidelizacionService, ClienteCartillaDTO } from '../../../fidelizacion/services/fidelizacion.service';

interface PosItem {
  id: number;
  tipo: 'SERVICIO' | 'PRODUCTO';
  nombre: string;
  descripcion: string;
  precio: number;
  categoriaNombre: string;
  duracion?: number;
  stock?: number;
  imageUrl?: string;
}

interface CartItem {
  id: string; // unique string to differentiate same service with different employee
  item: PosItem;
  cantidad: number;
  empleadoId?: number;
  empleadoNombre?: string;
  subtotal: number;
}

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastModule, DialogModule, DropdownModule, OverlayPanelModule, SelectButtonModule, ButtonModule, InputTextModule],
  providers: [MessageService],
  templateUrl: './pos.component.html',
  styleUrl: './pos.component.scss'
})
export class PosComponent implements OnInit {
  private catalogoService = inject(CatalogoService);
  private ticketService = inject(TicketService);
  private rrhhService = inject(RrhhService);
  private agendaService = inject(AgendaService);
  private fidelizacionService = inject(FidelizacionService);
  private messageService = inject(MessageService);

  // Datos
  allItems = signal<PosItem[]>([]);
  empleados = signal<Empleado[]>([]);
  clientes = signal<ClienteResponse[]>([]);
  
  // Filtros
  searchQuery = signal('');
  selectedCategory = signal<string>('Todos');
  categories = computed(() => {
    const cats = new Set<string>();
    this.allItems().forEach(item => {
      if (item.categoriaNombre) cats.add(item.categoriaNombre);
    });
    return ['Todos', ...Array.from(cats)];
  });

  filteredItems = computed(() => {
    let items = this.allItems();
    if (this.selectedCategory() !== 'Todos') {
      items = items.filter(i => i.categoriaNombre === this.selectedCategory());
    }
    if (this.searchQuery()) {
      const q = this.searchQuery().toLowerCase();
      items = items.filter(i => i.nombre.toLowerCase().includes(q) || i.descripcion?.toLowerCase().includes(q));
    }
    return items;
  });

  // Carrito / Ticket
  cart = signal<CartItem[]>([]);
  
  subtotal = computed(() => this.cart().reduce((acc, curr) => acc + curr.subtotal, 0));
  descuento = signal(0);
  // Asumiendo IGV 18% incluido (Base = Subtotal / 1.18, IGV = Subtotal - Base)
  impuestos = computed(() => {
    const s = this.subtotal() - this.descuento();
    return s - (s / 1.18);
  });
  total = computed(() => this.subtotal() - this.descuento());

  isProcessing = signal(false);
  showTicketModal = signal(false);
  lastTicket = signal<TicketResponse | null>(null);

  // State
  selectedCliente = signal<ClienteResponse | null>(null);
  metodoPagoOptions = [
    { label: 'Efectivo', value: 'EFECTIVO' },
    { label: 'Yape / Plin', value: 'YAPE' },
    { label: 'Tarjeta (POS)', value: 'TARJETA' },
    { label: 'Transferencia', value: 'TRANSFERENCIA' }
  ];
  selectedMetodoPago = signal<string>('EFECTIVO');

  premiosDisponibles = signal<ClienteCartillaDTO[]>([]);
  premiosAplicados = signal<number[]>([]);

  // Registro Rápido de Cliente
  showNuevoClienteDialog = signal(false);
  nuevoCliente = { nombres: '', apellidos: '', telefono: '', email: '' };

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    // Cargar Clientes
    this.agendaService.obtenerClientes().subscribe(res => {
      this.clientes.set(res);
    });
    // Cargar Empleados
    this.rrhhService.getEmpleados().subscribe(res => {
      this.empleados.set(res.filter(e => e.estado !== false));
    });

    // Cargar Servicios y Productos
    let loadedItems: PosItem[] = [];
    
    this.catalogoService.getServicios().subscribe(servicios => {
      const parsedServicios = servicios
        .filter(s => s.estado !== false)
        .map(s => ({
          id: s.id!,
          tipo: 'SERVICIO' as const,
          nombre: s.nombre,
          descripcion: s.descripcion || '',
          precio: s.precioBase,
          categoriaNombre: s.categoriaNombre || 'Sin Categoría',
          duracion: s.duracionMinutos,
          imageUrl: (s as any).imageUrl || '' // vacio si no hay, para mostrar placeholder
        }));
      loadedItems = [...loadedItems, ...parsedServicios];
      this.allItems.set(loadedItems);
    });

    this.catalogoService.getProductos().subscribe(productos => {
      const parsedProductos = productos
        .filter(p => p.ventaDirecta && p.estado !== false && p.stockActual > 0)
        .map(p => ({
          id: p.id!,
          tipo: 'PRODUCTO' as const,
          nombre: p.nombre,
          descripcion: p.marca || 'Retail',
          precio: p.precioVenta,
          categoriaNombre: p.categoriaNombre || 'Productos',
          stock: p.stockActual,
          imageUrl: p.imageUrl || ''
        }));
      loadedItems = [...loadedItems, ...parsedProductos];
      this.allItems.set(loadedItems);
    });
  }

  setCategory(cat: string) {
    this.selectedCategory.set(cat);
  }

  addToCart(item: PosItem) {
    // Por defecto asigamos al primer empleado si es servicio (MVP) o el usuario debería seleccionarlo.
    // Para simplificar, si es servicio tomamos el primer empleado de la lista.
    let empleadoId: number | undefined = undefined;
    let empleadoNombre: string | undefined = undefined;
    
    if (item.tipo === 'SERVICIO' && this.empleados().length > 0) {
      empleadoId = this.empleados()[0].id;
      empleadoNombre = this.empleados()[0].nombres;
    }

    const cartId = item.tipo === 'SERVICIO' ? `${item.tipo}-${item.id}-${empleadoId}` : `${item.tipo}-${item.id}`;
    
    this.cart.update(current => {
      const existing = current.find(c => c.id === cartId);
      if (existing) {
        if (item.tipo === 'PRODUCTO' && item.stock && existing.cantidad >= item.stock) {
          this.messageService.add({ severity: 'warn', summary: 'Stock Insuficiente', detail: `Solo quedan ${item.stock} unidades de ${item.nombre}.` });
          return current;
        }
        existing.cantidad += 1;
        existing.subtotal = existing.cantidad * existing.item.precio;
        return [...current];
      }
      return [...current, {
        id: cartId,
        item,
        cantidad: 1,
        empleadoId,
        empleadoNombre,
        subtotal: item.precio
      }];
    });
  }

  updateQuantity(cartItem: CartItem, delta: number) {
    this.cart.update(current => {
      const target = current.find(c => c.id === cartItem.id);
      if (target) {
        if (delta > 0 && target.item.tipo === 'PRODUCTO' && target.item.stock && target.cantidad >= target.item.stock) {
          this.messageService.add({ severity: 'warn', summary: 'Stock Insuficiente', detail: 'No hay más stock disponible para este producto.' });
          return current;
        }
        target.cantidad += delta;
        if (target.cantidad <= 0) {
          return current.filter(c => c.id !== cartItem.id);
        }
        target.subtotal = target.cantidad * target.item.precio;
      }
      return [...current];
    });
  }

  updateCartItemEmpleado(cartItem: CartItem, empleadoId: number) {
    const emp = this.empleados().find(e => e.id === empleadoId);
    if (emp) {
      this.cart.update(current => {
        const target = current.find(c => c.id === cartItem.id);
        if (target) {
          target.empleadoId = emp.id;
          target.empleadoNombre = emp.nombres;
        }
        return [...current];
      });
    }
  }

  removeFromCart(cartItem: CartItem) {
    this.cart.update(current => current.filter(c => c.id !== cartItem.id));
  }

  servicioSelloId = signal<number | null>(null);
  
  serviciosEnTicket = computed(() => {
    // Unique services in the cart
    const map = new Map<number, any>();
    for (const c of this.cart()) {
      if (c.item.tipo === 'SERVICIO' && !c.id.startsWith('PREMIO')) {
        map.set(c.item.id, c.item);
      }
    }
    return Array.from(map.values());
  });

  servicioMasCaro = computed(() => {
    const servicios = this.serviciosEnTicket();
    if (servicios.length === 0) return null;
    return servicios.reduce((prev, current) => (prev.precio > current.precio) ? prev : current);
  });

  cobrarVenta() {
    if (this.cart().length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Carrito Vacío', detail: 'Agrega al menos un item para cobrar.' });
      return;
    }

    if (this.selectedCliente() && this.serviciosEnTicket().length > 1 && !this.servicioSelloId()) {
      this.messageService.add({ severity: 'warn', summary: 'Sello Requerido', detail: 'Por favor pregúntele al cliente y seleccione a qué servicio desea aplicar el sello de fidelización.' });
      return;
    }

    this.isProcessing.set(true);

    const request = {
      metodoPago: this.selectedMetodoPago(),
      clienteId: this.selectedCliente() ? this.selectedCliente()!.id : null,
      nombreClienteNoRegistrado: this.selectedCliente() ? this.selectedCliente()!.nombreCompleto : 'Público General',
      detalles: this.cart().map(c => ({
        servicioId: c.item.tipo === 'SERVICIO' && !c.id.startsWith('PREMIO') ? c.item.id : null,
        productoId: c.item.tipo === 'PRODUCTO' ? c.item.id : null,
        empleadoId: c.empleadoId,
        cantidad: c.cantidad
      })),
      servicioSelloId: this.servicioSelloId() || undefined,
      premiosFidelizacionIds: this.premiosAplicados()
    };

    this.ticketService.emitirTicket(request).subscribe({
      next: (res) => {
        this.messageService.add({ severity: 'success', summary: '¡Venta Exitosa!', detail: `El Ticket #${res.id} ha sido generado y cobrado.`, life: 4000 });
        this.lastTicket.set(res);
        this.showTicketModal.set(true);
        this.cart.set([]);
        this.selectedCliente.set(null);
        this.selectedMetodoPago.set('EFECTIVO');
        this.servicioSelloId.set(null);
        this.premiosAplicados.set([]);
        this.isProcessing.set(false);
        this.loadData(); // Recargar para actualizar stock
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error al Cobrar', detail: err.error?.message || err.message });
        this.isProcessing.set(false);
      }
    });
  }

  cerrarTicket() {
    this.showTicketModal.set(false);
    this.lastTicket.set(null);
  }

  onClienteSelected(cliente: ClienteResponse | null) {
    this.selectedCliente.set(cliente);
    this.premiosDisponibles.set([]);
    this.premiosAplicados.set([]);
    
    if (cliente) {
      this.fidelizacionService.listarPremiosDisponibles(cliente.id).subscribe(res => {
        if (res && res.length > 0) {
          this.premiosDisponibles.set(res);
          this.messageService.add({ severity: 'success', summary: 'Premio Disponible', detail: `El cliente tiene ${res.length} premio(s) por canjear.`, life: 5000 });
        }
      });
    }
  }

  abrirNuevoClienteDialog() {
    this.nuevoCliente = { nombres: '', apellidos: '', telefono: '', email: '' };
    this.showNuevoClienteDialog.set(true);
  }

  guardarNuevoCliente() {
    if (!this.nuevoCliente.nombres || !this.nuevoCliente.apellidos) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Nombres y apellidos son obligatorios' });
      return;
    }

    this.isProcessing.set(true);
    this.agendaService.crearCliente(this.nuevoCliente).subscribe({
      next: (res) => {
        // Añadir a la lista local y seleccionarlo
        this.clientes.update(current => [...current, res]);
        this.onClienteSelected(res);
        
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Cliente registrado correctamente' });
        this.showNuevoClienteDialog.set(false);
        this.isProcessing.set(false);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo registrar el cliente' });
        this.isProcessing.set(false);
      }
    });
  }

  aplicarPremio(premio: ClienteCartillaDTO) {
    // Buscar el servicio en catalogo
    const servicioItem = this.allItems().find(i => i.tipo === 'SERVICIO' && i.nombre === premio.servicioRequerido);
    
    if (!servicioItem) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Servicio del premio no encontrado en el catálogo.' });
      return;
    }

    // Agregar al carrito con el descuento
    let empleadoId: number | undefined = undefined;
    let empleadoNombre: string | undefined = undefined;
    
    if (this.empleados().length > 0) {
      empleadoId = this.empleados()[0].id;
      empleadoNombre = this.empleados()[0].nombres;
    }

    const cartId = `PREMIO-${premio.id}`;
    
    // Calculamos precio con descuento
    const precioConDescuento = servicioItem.precio - (servicioItem.precio * (premio.descuentoPremio / 100));

    this.cart.update(current => {
      return [...current, {
        id: cartId,
        item: { ...servicioItem, precio: precioConDescuento, nombre: `${servicioItem.nombre} (PREMIO)` },
        cantidad: 1,
        empleadoId,
        empleadoNombre,
        subtotal: precioConDescuento
      }];
    });

    // Guardar el cartillaId para enviarlo en la venta
    this.premiosAplicados.update(current => [...current, premio.cartillaId]);
    
    this.messageService.add({ severity: 'success', summary: 'Premio Aplicado en Carrito', detail: `Se aplicó ${premio.descuentoPremio}% Dscto. Se guardará al cobrar.` });
    this.premiosDisponibles.update(current => current.filter(p => p.id !== premio.id));
  }

  imprimirTicket() {
    window.print();
  }
}
