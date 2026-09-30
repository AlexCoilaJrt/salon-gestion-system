import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';
import { DropdownModule } from 'primeng/dropdown';
import { MessageService } from 'primeng/api';
import { AgendaService, CitaResponse, CitaRequest, ClienteResponse } from './agenda.service';
import { CatalogoService, Servicio, Producto } from '../../../catalogo/services/catalogo.service';
import { RrhhService, Empleado } from '../../../rrhh/services/rrhh.service';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [CommonModule, FormsModule, DialogModule, ToastModule, ButtonModule, MultiSelectModule, DropdownModule],
  providers: [MessageService, DatePipe],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.scss'
})
export class AgendaComponent implements OnInit {
  private agendaService = inject(AgendaService);
  private catalogoService = inject(CatalogoService);
  private rrhhService = inject(RrhhService);
  private messageService = inject(MessageService);
  private datePipe = inject(DatePipe);

  citas = signal<CitaResponse[]>([]);
  clientes = signal<ClienteResponse[]>([]);
  servicios = signal<Servicio[]>([]);
  empleados = signal<Empleado[]>([]);
  productos = signal<Producto[]>([]);

  showModal = signal(false);
  isProcessing = signal(false);
  isNuevoCliente = signal(false);
  
  viewMode = signal<'day'|'week'|'month'>('week');
  editMode = signal(false);
  citaIdToEdit = signal<number | null>(null);

  // Form signals para reactividad
  selectedServicioId = signal<number | null>(null);

  empleadosFiltrados = computed(() => {
    const sId = this.selectedServicioId();
    if (!sId) return [];

    const numSId = Number(sId);
    const servicio = this.servicios().find(s => Number(s.id) === numSId);
    
    if (!servicio || !servicio.especialidadRequeridaId) {
      return this.empleados(); 
    }

    const espReqId = Number(servicio.especialidadRequeridaId);
    const espReqNombre = servicio.especialidadRequeridaNombre?.toUpperCase().trim();

    const filtrados = this.empleados().filter(e => {
      // 1. Intento por ID
      if (e.especialidadIds && Array.isArray(e.especialidadIds)) {
        if (e.especialidadIds.some(id => Number(id) === espReqId)) return true;
      }
      // 2. Intento por Nombre
      if (espReqNombre && e.especialidadesNombres && Array.isArray(e.especialidadesNombres)) {
        if (e.especialidadesNombres.some(n => n.toUpperCase().trim() === espReqNombre)) return true;
      }
      return false;
    });

    return filtrados;
  });

  // Form
  formCita = {
    clienteId: null as number | null,
    servicioId: null as number | null,
    empleadoId: null as number | null,
    fecha: '',
    hora: '',
    notas: '',
    adelanto: 0,
    metodoPago: '',
    productosIds: [] as number[]
  };

  formNuevoCliente = {
    nombres: '',
    apellidos: '',
    telefono: '',
    email: ''
  };

  horas = [9, 10, 11, 12, 13, 14, 15, 16, 17, 18];

  ngOnInit() {
    this.cargarDatos();
  }

  cargarDatos() {
    this.agendaService.obtenerTodas().subscribe(data => this.citas.set(data));
    this.agendaService.obtenerClientes().subscribe(data => this.clientes.set(data));
    this.catalogoService.getServicios().subscribe(data => this.servicios.set(data.filter(s => s.estado !== false)));
    this.rrhhService.getEmpleados().subscribe(data => this.empleados.set(data.filter(e => e.estado !== false)));
    this.catalogoService.getProductos().subscribe(data => this.productos.set(data.filter(p => p.ventaDirecta && p.estado !== false)));
  }

  abrirModal() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];

    this.formCita = {
      clienteId: null,
      servicioId: null,
      empleadoId: null,
      fecha: tomorrowStr,
      hora: '10:00',
      notas: '',
      adelanto: 0,
      metodoPago: '',
      productosIds: []
    };
    this.selectedServicioId.set(null);
    this.formNuevoCliente = { nombres: '', apellidos: '', telefono: '', email: '' };
    this.isNuevoCliente.set(false);
    this.editMode.set(false);
    this.citaIdToEdit.set(null);
    this.showModal.set(true);
  }

  abrirModalEdicion(cita: CitaResponse) {
    const dateParts = cita.fechaHora.split('T');
    const timeParts = dateParts[1].substring(0, 5); // HH:MM
    
    this.formCita = {
      clienteId: cita.clienteId,
      servicioId: cita.servicioId,
      empleadoId: cita.empleadoId,
      fecha: dateParts[0],
      hora: timeParts,
      notas: cita.notas || '',
      adelanto: cita.adelanto || 0,
      metodoPago: cita.metodoPago || '',
      productosIds: cita.productosIds || []
    };
    this.selectedServicioId.set(cita.servicioId);
    this.isNuevoCliente.set(false);
    this.editMode.set(true);
    this.citaIdToEdit.set(cita.id);
    this.showModal.set(true);
  }

  guardarCita() {
    if (this.isNuevoCliente()) {
      if (!this.formNuevoCliente.nombres || !this.formNuevoCliente.apellidos) {
        this.messageService.add({ severity: 'warn', summary: 'Campos incompletos', detail: 'Por favor, ingrese nombres y apellidos del cliente.' });
        return;
      }
    } else {
      if (!this.formCita.clienteId) {
        this.messageService.add({ severity: 'warn', summary: 'Campos incompletos', detail: 'Por favor, seleccione un cliente.' });
        return;
      }
    }

    if (!this.formCita.servicioId || !this.formCita.empleadoId || !this.formCita.fecha || !this.formCita.hora) {
      this.messageService.add({ severity: 'warn', summary: 'Campos incompletos', detail: 'Por favor, complete los campos de servicio, empleado, fecha y hora.' });
      return;
    }

    this.isProcessing.set(true);

    if (this.isNuevoCliente()) {
      this.agendaService.crearCliente(this.formNuevoCliente).subscribe({
        next: (cliente) => {
          this.formCita.clienteId = cliente.id;
          this.clientes.update(c => [...c, cliente]);
          this.enviarCitaBackend();
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error al crear cliente', detail: err.error?.message || 'Hubo un problema.' });
          this.isProcessing.set(false);
        }
      });
    } else {
      this.enviarCitaBackend();
    }
  }

  private enviarCitaBackend() {
    // Convert to LocalDateTime string: YYYY-MM-DDTHH:MM:SS
    const fechaHoraStr = `${this.formCita.fecha}T${this.formCita.hora}:00`;

    const req: CitaRequest = {
      clienteId: this.formCita.clienteId!,
      servicioId: this.formCita.servicioId!,
      empleadoId: this.formCita.empleadoId!,
      fechaHora: fechaHoraStr,
      notas: this.formCita.notas,
      adelanto: this.formCita.adelanto,
      metodoPago: this.formCita.metodoPago || undefined,
      productosIds: this.formCita.productosIds
    };

    if (this.editMode() && this.citaIdToEdit()) {
      this.agendaService.actualizarCita(this.citaIdToEdit()!, req).subscribe({
        next: (res) => {
          this.messageService.add({ severity: 'success', summary: '¡Cita Actualizada!', detail: 'La cita ha sido reprogramada exitosamente.' });
          this.showModal.set(false);
          this.isProcessing.set(false);
          this.cargarDatos();
        },
        error: (err) => {
          console.error(err);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || err.message });
          this.isProcessing.set(false);
        }
      });
    } else {
      this.agendaService.crearCita(req).subscribe({
        next: (res) => {
          this.messageService.add({ severity: 'success', summary: '¡Cita Guardada!', detail: 'La cita ha sido programada exitosamente.' });
          this.showModal.set(false);
          this.isProcessing.set(false);
          this.cargarDatos(); // Recargar citas
        },
        error: (err) => {
          console.error(err);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || err.message });
          this.isProcessing.set(false);
        }
      });
    }
  }

  getCitaStyle(cita: CitaResponse) {
    // Parse fechaHora: "2026-10-28T10:30:00"
    const date = new Date(cita.fechaHora);
    const hour = date.getHours();
    const min = date.getMinutes();
    
    // Asumimos 60 min de duracion por defecto
    const durationMin = 60; 
    
    const top = ((hour - 9) + (min / 60)) * 80;
    const height = (durationMin / 60) * 80;
    
    return {
      top: `${top}px`,
      height: `${height}px`,
      left: '4px',
      right: '4px'
    };
  }

  // --- Helpers UI ---
  get servicioSeleccionado(): Servicio | undefined {
    if (!this.formCita.servicioId) return undefined;
    return this.servicios().find(s => s.id === this.formCita.servicioId);
  }

  get productosSeleccionados(): Producto[] {
    if (!this.formCita.productosIds || this.formCita.productosIds.length === 0) return [];
    return this.productos().filter(p => p.id && this.formCita.productosIds.includes(p.id));
  }

  get costoTotalCita(): number {
    let total = 0;
    const s = this.servicioSeleccionado;
    if (s && s.precioBase) total += s.precioBase;
    
    for (const p of this.productosSeleccionados) {
      if (p.precioVenta) total += p.precioVenta;
    }
    
    return total;
  }

  get saldoRestante(): number {
    return Math.max(0, this.costoTotalCita - (this.formCita.adelanto || 0));
  }
}
