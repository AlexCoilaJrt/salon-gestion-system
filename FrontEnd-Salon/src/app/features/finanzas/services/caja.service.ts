import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SesionCaja {
  id?: number;
  fechaApertura?: string;
  fechaCierre?: string;
  montoInicial: number;
  montoEsperado?: number;
  montoDeclarado?: number;
  descuadre?: number;
  observaciones?: string;
  estado?: boolean;
  usuarioAperturaId?: number;
  usuarioAperturaNombre?: string;
}

export interface SesionCajaRequest {
  montoInicial: number;
  usuarioId: number;
}

export interface CierreCajaRequest {
  montoDeclarado: number;
  observaciones?: string;
}

export interface TicketDetalleRequest {
  cantidad: number;
  servicioId?: number;
  productoId?: number;
  empleadoId?: number;
}

export interface TicketRequest {
  metodoPago: string; // 'EFECTIVO', 'TRANSFERENCIA', 'TARJETA'
  clienteId?: number;
  nombreClienteNoRegistrado?: string;
  detalles: TicketDetalleRequest[];
}

@Injectable({
  providedIn: 'root'
})
export class CajaService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/operaciones/caja';

  obtenerCajaActual(): Observable<SesionCaja> {
    return this.http.get<SesionCaja>(`${this.apiUrl}/actual`);
  }

  abrirCaja(request: SesionCajaRequest): Observable<SesionCaja> {
    return this.http.post<SesionCaja>(`${this.apiUrl}/abrir`, request);
  }

  cerrarCaja(request: CierreCajaRequest): Observable<SesionCaja> {
    return this.http.post<SesionCaja>(`${this.apiUrl}/cerrar`, request);
  }

  emitirTicket(request: TicketRequest): Observable<any> {
    return this.http.post<any>('http://localhost:8080/api/operaciones/tickets', request);
  }
}
