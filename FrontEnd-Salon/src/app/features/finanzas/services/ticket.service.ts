import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TicketDetalleResponse {
  id: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  servicioId?: number;
  servicioNombre?: string;
  productoId?: number;
  productoNombre?: string;
  empleadoId?: number;
  empleadoNombreCompleto?: string;
  empleadoEspecialidades?: string[];
}

export interface TicketResponse {
  id: number;
  fechaEmision: string;
  metodoPago: string;
  total: number;
  clienteId?: number;
  clienteNombreCompleto?: string;
  sesionCajaId: number;
  activo: boolean;
  detalles: TicketDetalleResponse[];
}

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/operaciones/tickets';

  obtenerTodosTickets(): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(this.apiUrl);
  }

  emitirTicket(request: any): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(this.apiUrl, request);
  }

  anularTicket(id: number): Observable<TicketResponse> {
    return this.http.put<TicketResponse>(`${this.apiUrl}/${id}/anular`, {});
  }
}
