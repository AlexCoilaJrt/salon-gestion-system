import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ImpresoraConfig {
  ip: string;
  puerto: number;
  autoImprimirAlCobrar: boolean;
  autoCorte: boolean;
  abrirGaveta: boolean;
  mensajePie: string;
  anchoPapelMm: number;
}

export interface ImpresoraTestResult {
  conectado: boolean;
  mensaje: string;
  latenciaMs?: number;
  ip: string;
  puerto: number;
}

@Injectable({
  providedIn: 'root'
})
export class ImpresoraService {
  private apiUrl = 'http://localhost:8080/api/operaciones/impresora';

  constructor(private http: HttpClient) {}

  getConfig(): Observable<ImpresoraConfig> {
    return this.http.get<ImpresoraConfig>(`${this.apiUrl}/config`);
  }

  saveConfig(config: ImpresoraConfig): Observable<ImpresoraConfig> {
    return this.http.put<ImpresoraConfig>(`${this.apiUrl}/config`, config);
  }

  testConexion(config?: Partial<ImpresoraConfig>): Observable<ImpresoraTestResult> {
    return this.http.post<ImpresoraTestResult>(`${this.apiUrl}/test-conexion`, config || {});
  }

  imprimirTicketPrueba(config?: Partial<ImpresoraConfig>): Observable<ImpresoraTestResult> {
    return this.http.post<ImpresoraTestResult>(`${this.apiUrl}/test-ticket`, config || {});
  }

  imprimirTicketVenta(ticketId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/tickets/${ticketId}/imprimir`, {});
  }

  imprimirOrdenEspecialista(ticketId: number, mesaSillon?: string, especialistaManual?: string): Observable<ImpresoraTestResult> {
    const params: any = {};
    if (mesaSillon && mesaSillon.trim()) params.mesaSillon = mesaSillon.trim();
    if (especialistaManual && especialistaManual.trim()) params.especialistaManual = especialistaManual.trim();
    return this.http.post<ImpresoraTestResult>(`${this.apiUrl}/ordenes/${ticketId}/imprimir-especialista`, {}, { params });
  }
}

