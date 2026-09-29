import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Comision {
  id?: number;
  empleadoId: number;
  empleadoNombre?: string;
  empleadoApellido?: string;
  tipoComision: string;
  valor: number;
  estado?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ComisionService {

  private apiUrl = 'http://localhost:8080/api/v1/comisiones'; // Ajusta la URL si es necesario

  constructor(private http: HttpClient) { }

  getComisiones(): Observable<Comision[]> {
    return this.http.get<Comision[]>(this.apiUrl);
  }

  getComision(id: number): Observable<Comision> {
    return this.http.get<Comision>(`${this.apiUrl}/${id}`);
  }

  createComision(comision: Comision): Observable<Comision> {
    return this.http.post<Comision>(this.apiUrl, comision);
  }

  updateComision(id: number, comision: Comision): Observable<Comision> {
    return this.http.put<Comision>(`${this.apiUrl}/${id}`, comision);
  }

  deleteComision(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
