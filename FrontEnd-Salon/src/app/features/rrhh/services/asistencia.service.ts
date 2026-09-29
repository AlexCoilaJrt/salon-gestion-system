import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AsistenciaResponse {
  id: number;
  empleadoId: number;
  empleadoNombreCompleto: string;
  fecha: string;
  horaEntrada: string;
  horaSalida?: string;
  tardanzaMinutos: number;
  tipo: string;
  observaciones?: string;
  horasTrabajadas?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AsistenciaService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/rrhh/asistencias';

  getReporte(inicio: string, fin: string): Observable<AsistenciaResponse[]> {
    let params = new HttpParams().set('inicio', inicio).set('fin', fin);
    return this.http.get<AsistenciaResponse[]>(this.apiUrl, { params });
  }

  marcarKiosko(dni: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/kiosko`, { dni });
  }
}
