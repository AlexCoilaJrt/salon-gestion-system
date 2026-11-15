import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BiometricoConfig {
  ip: string;
  puerto: number;
  commKey: string;
  nombreDispositivo: string;
  autoSincronizar: boolean;
  intervaloMinutos: number;
  activo: boolean;
}

export interface BiometricoTestResult {
  conectado: boolean;
  ip: string;
  puerto: number;
  latenciaMs?: number;
  mensaje: string;
  detallesTecnicos?: string;
}

export interface BiometricoSincronizacion {
  exito: boolean;
  totalMarcacionesLeidas: number;
  nuevasAsistenciasRegistradas: number;
  fechaSincronizacion: string;
  mensaje: string;
}

@Injectable({
  providedIn: 'root'
})
export class BiometricoService {
  private apiUrl = 'http://localhost:8080/api/rrhh/biometrico';

  constructor(private http: HttpClient) {}

  getConfig(): Observable<BiometricoConfig> {
    return this.http.get<BiometricoConfig>(`${this.apiUrl}/config`);
  }

  saveConfig(config: BiometricoConfig): Observable<BiometricoConfig> {
    return this.http.put<BiometricoConfig>(`${this.apiUrl}/config`, config);
  }

  testConexion(config?: Partial<BiometricoConfig>): Observable<BiometricoTestResult> {
    return this.http.post<BiometricoTestResult>(`${this.apiUrl}/test-conexion`, config || {});
  }

  sincronizar(): Observable<BiometricoSincronizacion> {
    return this.http.post<BiometricoSincronizacion>(`${this.apiUrl}/sincronizar`, {});
  }
}
