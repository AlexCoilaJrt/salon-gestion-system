import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CartillaDTO {
  id?: number;
  nombre: string;
  servicioId: number;
  servicioNombre?: string;
  metaSellos: number;
  descuentoPremio: number;
  estado?: boolean;
}

export interface ClienteCartillaDTO {
  id: number;
  clienteId: number;
  clienteNombre: string;
  cartillaId: number;
  cartillaNombre: string;
  servicioRequerido: string;
  metaSellos: number;
  sellosActuales: number;
  descuentoPremio: number;
  completada: boolean;
  canjeada: boolean;
  fechaUltimaActualizacion: string;
}

@Injectable({
  providedIn: 'root'
})
export class FidelizacionService {
  private http = inject(HttpClient);
  private API_URL = 'http://localhost:8080/api/fidelizacion';

  // Cartillas
  crearCartilla(cartilla: CartillaDTO): Observable<CartillaDTO> {
    return this.http.post<CartillaDTO>(`${this.API_URL}/cartillas`, cartilla);
  }

  listarCartillas(): Observable<CartillaDTO[]> {
    return this.http.get<CartillaDTO[]>(`${this.API_URL}/cartillas`);
  }

  // Clientes
  listarCartillasCliente(clienteId: number): Observable<ClienteCartillaDTO[]> {
    return this.http.get<ClienteCartillaDTO[]>(`${this.API_URL}/clientes/${clienteId}/cartillas`);
  }

  listarPremiosDisponibles(clienteId: number): Observable<ClienteCartillaDTO[]> {
    return this.http.get<ClienteCartillaDTO[]>(`${this.API_URL}/clientes/${clienteId}/premios`);
  }

  canjearPremio(clienteId: number, cartillaId: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/clientes/${clienteId}/canjear/${cartillaId}`, {});
  }
}
