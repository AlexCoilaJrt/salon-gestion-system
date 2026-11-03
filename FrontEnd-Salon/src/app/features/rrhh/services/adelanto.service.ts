import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdelantoRequest {
  empleadoId: number;
  monto: number;
  fecha: string;
  metodoPago: string;
  motivo: string;
  descontarDeCaja?: boolean;
}

export interface AdelantoResponse {
  id: number;
  empleadoId: number;
  empleadoNombreCompleto: string;
  monto: number;
  fecha: string;
  metodoPago: string;
  motivo: string;
  egresoId?: number;
  usuarioRegistro?: string;
  fechaRegistro?: string;
  estado: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdelantoService {

  private apiUrl = 'http://localhost:8080/api/rrhh/adelantos';

  constructor(private http: HttpClient) { }

  registrarAdelanto(req: AdelantoRequest): Observable<AdelantoResponse> {
    return this.http.post<AdelantoResponse>(this.apiUrl, req);
  }

  obtenerAdelantos(empleadoId: number, fechaInicio?: string, fechaFin?: string): Observable<AdelantoResponse[]> {
    let url = `${this.apiUrl}?empleadoId=${empleadoId}`;
    if (fechaInicio && fechaFin) {
      url += `&fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`;
    }
    return this.http.get<AdelantoResponse[]>(url);
  }

  anularAdelanto(id: number): Observable<AdelantoResponse> {
    return this.http.put<AdelantoResponse>(`${this.apiUrl}/${id}/anular`, {});
  }
}

