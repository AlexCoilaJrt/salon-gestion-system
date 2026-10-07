import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LiquidacionRequest {
  empleadoId: number;
  fechaInicio: string;
  fechaFin: string;
  totalVentas?: number;
  totalComision?: number;
  sueldoFijoProporcional?: number;
  descuentos?: number;
  totalAPagar: number;
  estado: string;
  diasAsistidos?: number;
}

export interface LiquidacionResponse {
  id: number;
  empleadoId: number;
  empleadoNombreCompleto: string;
  fechaInicio: string;
  fechaFin: string;
  diasAsistidos: number;
  sueldoFijoProporcional: number;
  totalVentas: number;
  totalComision: number;
  descuentosAdelantos: number;
  totalPagar: number;
  estado: string;
  fechaRegistro: string;
}

@Injectable({
  providedIn: 'root'
})
export class LiquidacionesService {

  private apiUrl = 'http://localhost:8080/api/rrhh/liquidaciones';

  constructor(private http: HttpClient) { }

  crearLiquidacion(req: LiquidacionRequest): Observable<LiquidacionResponse> {
    return this.http.post<LiquidacionResponse>(this.apiUrl, req);
  }

  obtenerTodas(): Observable<LiquidacionResponse[]> {
    return this.http.get<LiquidacionResponse[]>(this.apiUrl);
  }

  obtenerPorEmpleado(empleadoId: number): Observable<LiquidacionResponse[]> {
    return this.http.get<LiquidacionResponse[]>(`${this.apiUrl}/empleado/${empleadoId}`);
  }

  cambiarEstado(id: number, estado: string): Observable<LiquidacionResponse> {
    return this.http.patch<LiquidacionResponse>(`${this.apiUrl}/${id}/estado`, { estado });
  }
}
