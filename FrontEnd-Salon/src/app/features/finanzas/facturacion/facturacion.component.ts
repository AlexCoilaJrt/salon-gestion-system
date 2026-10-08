import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TicketService, TicketResponse } from '../services/ticket.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { TooltipModule } from 'primeng/tooltip';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ToastModule, ButtonModule, DialogModule, 
    TableModule, InputTextModule, TagModule, ConfirmDialogModule, TooltipModule,
    IconFieldModule, InputIconModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './facturacion.component.html'
})
export class FacturacionComponent implements OnInit {
  private ticketService = inject(TicketService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  tickets: TicketResponse[] = [];
  loading = false;
  
  displayDetalle = false;
  ticketSeleccionado: TicketResponse | null = null;
  
  searchTerm: string = '';

  ngOnInit() {
    this.cargarTickets();
  }

  cargarTickets() {
    this.loading = true;
    this.ticketService.obtenerTodosTickets().subscribe({
      next: (data) => {
        this.tickets = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cargar el historial de ventas.' });
        this.loading = false;
      }
    });
  }

  verDetalle(ticket: TicketResponse) {
    this.ticketSeleccionado = ticket;
    this.displayDetalle = true;
  }

  confirmarAnulacion(ticket: TicketResponse) {
    this.confirmationService.confirm({
      message: '¿Estás seguro que deseas anular el Ticket #' + ticket.id + '? Esta acción descontará el dinero de la caja y revertirá el stock/comisiones.',
      header: 'Confirmar Anulación',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, Anular',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.anularTicket(ticket);
      }
    });
  }

  anularTicket(ticket: TicketResponse) {
    this.ticketService.anularTicket(ticket.id).subscribe({
      next: (res) => {
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Ticket anulado correctamente.' });
        const index = this.tickets.findIndex(t => t.id === ticket.id);
        if (index !== -1) {
          this.tickets[index] = res; // Update the ticket in the list (now it will have activo=false)
        }
        if (this.ticketSeleccionado?.id === ticket.id) {
            this.ticketSeleccionado.activo = false;
        }
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'No se pudo anular el ticket.' });
      }
    });
  }

  imprimirTicket() {
    if (!this.ticketSeleccionado) return;
    window.print();
  }

  getEspecialistas(ticket: TicketResponse): string[] {
    if (!ticket.detalles || ticket.detalles.length === 0) return [];
    
    // Create a map to group specialties by employee name to avoid duplicates
    const empMap = new Map<string, Set<string>>();
    
    ticket.detalles.forEach(d => {
      const nombre = d.empleadoNombreCompleto;
      if (nombre) {
        if (!empMap.has(nombre)) {
          empMap.set(nombre, new Set<string>());
        }
        if (d.empleadoEspecialidades) {
          d.empleadoEspecialidades.forEach(esp => empMap.get(nombre)?.add(esp));
        }
      }
    });
    
    const result: string[] = [];
    empMap.forEach((especialidades, nombre) => {
      if (especialidades.size > 0) {
        result.push(`${nombre} (${Array.from(especialidades).join(', ')})`);
      } else {
        result.push(nombre);
      }
    });
    
    return result;
  }
}
