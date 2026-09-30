import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CitaResponse {
  id: number;
  fechaHora: string;
  estado: string;
  adelanto: number;
  metodoPago?: string;
  notas: string;
  clienteId: number;
  clienteNombreCompleto: string;
  empleadoId: number;
  empleadoNombreCompleto: string;
  servicioId: number;
  servicioNombre: string;
  productosIds?: number[];
}

export interface CitaRequest {
  fechaHora: string;
  estado?: string;
  adelanto?: number;
  metodoPago?: string;
  notas?: string;
  clienteId: number;
  empleadoId: number;
  servicioId: number;
  productosIds?: number[];
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  apellido: string;
  nombreCompleto: string;
  email: string;
  telefono: string;
}

export interface ClienteRequest {
  nombres: string;
  apellidos: string;
  telefono?: string;
  email?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AgendaService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/operaciones/citas';
  private clientesUrl = 'http://localhost:8080/api/operaciones/clientes';

  obtenerTodas(): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(this.apiUrl);
  }

  crearCita(request: CitaRequest): Observable<CitaResponse> {
    return this.http.post<CitaResponse>(this.apiUrl, request);
  }

  actualizarCita(id: number, request: CitaRequest): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.apiUrl}/${id}`, request);
  }

  obtenerClientes(): Observable<ClienteResponse[]> {
    return this.http.get<ClienteResponse[]>(this.clientesUrl);
  }

  crearCliente(request: ClienteRequest): Observable<ClienteResponse> {
    return this.http.post<ClienteResponse>(this.clientesUrl, request);
  }
}
