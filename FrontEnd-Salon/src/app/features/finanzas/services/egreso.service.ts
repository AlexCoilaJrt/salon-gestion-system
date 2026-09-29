import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EgresoRequest {
  monto: number;
  categoria: string;
  motivo: string;
}

export interface EgresoDTO {
  id: number;
  monto: number;
  categoria: string;
  motivo: string;
  fechaHora: string;
  usuarioNombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class EgresoService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/operaciones/egresos';

  registrarEgreso(request: EgresoRequest): Observable<EgresoDTO> {
    return this.http.post<EgresoDTO>(this.apiUrl, request);
  }

  obtenerEgresosCajaActual(): Observable<EgresoDTO[]> {
    return this.http.get<EgresoDTO[]>(`${this.apiUrl}/caja-actual`);
  }
}
