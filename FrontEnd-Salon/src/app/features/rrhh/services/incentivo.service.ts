import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Incentivo {
  id?: number;
  nombre: string;
  tipoIncentivo: string;
  valor: number;
  fechaInicio: string;
  fechaFin: string;
  estado?: boolean;
}

export interface LiquidacionResponse {
  empleadoId: number;
  empleadoNombreCompleto: string;
  sueldoFijo: number;
  totalVentas: number;
  totalComision: number;
  descuentosAdelantos: number;
  totalPagar: number;
}

export interface MonitorComision {
  empleadoId: number;
  empleadoNombreCompleto: string;
  enTurno: boolean;
  asistenciaActiva: boolean;
  basePorcentaje: number;
  baseMontoFijo: number;
  incentivosAplicados: any[];
  totalComisionPorcentaje: number;
  totalComisionMontoFijo: number;
  ventasHoy?: number;
  comisionesGanadasHoy?: number;
}

@Injectable({
  providedIn: 'root'
})
export class IncentivoService {

  private apiUrl = 'http://localhost:8080/api/v1/incentivos';

  constructor(private http: HttpClient) { }

  getIncentivos(): Observable<Incentivo[]> {
    return this.http.get<Incentivo[]>(this.apiUrl);
  }

  getMonitorComisiones(): Observable<MonitorComision[]> {
    return this.http.get<MonitorComision[]>(`${this.apiUrl}/monitor`);
  }

  getLiquidaciones(fechaInicio: string, fechaFin: string): Observable<LiquidacionResponse[]> {
    return this.http.get<LiquidacionResponse[]>(`${this.apiUrl}/liquidaciones?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`);
  }

  createIncentivo(incentivo: Incentivo): Observable<Incentivo> {
    return this.http.post<Incentivo>(this.apiUrl, incentivo);
  }

  updateIncentivo(id: number, incentivo: Incentivo): Observable<Incentivo> {
    return this.http.put<Incentivo>(`${this.apiUrl}/${id}`, incentivo);
  }

  deleteIncentivo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
