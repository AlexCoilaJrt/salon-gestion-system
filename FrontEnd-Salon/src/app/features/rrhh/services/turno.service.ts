import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Turno {
  id?: number;
  nombre: string;
  horaEntrada: string;
  horaSalida: string;
  toleranciaMinutos: number;
  estado: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TurnoService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/rrhh/turnos';

  getAll(): Observable<Turno[]> {
    return this.http.get<Turno[]>(this.apiUrl);
  }

  getActivos(): Observable<Turno[]> {
    return this.http.get<Turno[]>(`${this.apiUrl}/activos`);
  }

  create(turno: Turno): Observable<Turno> {
    return this.http.post<Turno>(this.apiUrl, turno);
  }

  update(id: number, turno: Turno): Observable<Turno> {
    return this.http.put<Turno>(`${this.apiUrl}/${id}`, turno);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
