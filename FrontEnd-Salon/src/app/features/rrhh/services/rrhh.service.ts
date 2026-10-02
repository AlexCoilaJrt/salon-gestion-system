import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// En Angular no existe proxy environment de momento, hardcodeado por ahora
const API_URL = 'http://localhost:8080/api/rrhh';

export interface Especialidad {
  id: number;
  nombre: string;
  descripcion: string;
  estado: boolean;
}

export interface Empleado {
  id?: number;
  nombres: string;
  apellidos: string;
  dni: string;
  email?: string;
  telefono?: string;
  fechaNacimiento?: string; // yyyy-MM-dd
  disponibilidad?: string;
  estado?: boolean;
  edad?: number;
  especialidad?: Especialidad;
  especialidadesNombres?: string[];
  especialidadIds?: number[]; // Para el Request
  turnoId?: number;
  turnoNombre?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RrhhService {
  private http = inject(HttpClient);

  // === EMPLEADOS ===
  getEmpleados(): Observable<Empleado[]> {
    return this.http.get<Empleado[]>(`${API_URL}/empleados`);
  }

  getEmpleadosPaginado(page: number, size: number, search?: string): Observable<PageResponse<Empleado>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<PageResponse<Empleado>>(`${API_URL}/empleados/paginado`, { params });
  }

  getEmpleado(id: number): Observable<Empleado> {
    return this.http.get<Empleado>(`${API_URL}/empleados/${id}`);
  }

  createEmpleado(empleado: Empleado): Observable<Empleado> {
    return this.http.post<Empleado>(`${API_URL}/empleados`, empleado);
  }

  updateEmpleado(id: number, empleado: Empleado): Observable<Empleado> {
    return this.http.put<Empleado>(`${API_URL}/empleados/${id}`, empleado);
  }

  deleteEmpleado(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/empleados/${id}`);
  }

  // === ESPECIALIDADES ===
  getEspecialidades(): Observable<Especialidad[]> {
    return this.http.get<Especialidad[]>(`${API_URL}/especialidades`);
  }

  createEspecialidad(especialidad: Partial<Especialidad>): Observable<Especialidad> {
    return this.http.post<Especialidad>(`${API_URL}/especialidades`, especialidad);
  }

  updateEspecialidad(id: number, especialidad: Partial<Especialidad>): Observable<Especialidad> {
    return this.http.put<Especialidad>(`${API_URL}/especialidades/${id}`, especialidad);
  }

  deleteEspecialidad(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/especialidades/${id}`);
  }
}
