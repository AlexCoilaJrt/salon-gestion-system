import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UsuarioResponseDTO {
  id: number;
  username: string;
  email: string;
  nombre: string;
  apellido: string;
  rol: string;
  estado: boolean;
  ultimoAcceso: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private apiUrl = 'http://localhost:8080/api/usuarios';

  constructor(private http: HttpClient) { }

  getUsuarios(page: number = 0, size: number = 10, sort: string = 'username'): Observable<ApiResponse<PageResponse<UsuarioResponseDTO>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    
    return this.http.get<ApiResponse<PageResponse<UsuarioResponseDTO>>>(this.apiUrl, { params });
  }

  createUsuario(usuario: any): Observable<ApiResponse<UsuarioResponseDTO>> {
    return this.http.post<ApiResponse<UsuarioResponseDTO>>(this.apiUrl, usuario);
  }

  updateUsuario(id: number, usuario: any): Observable<ApiResponse<UsuarioResponseDTO>> {
    return this.http.put<ApiResponse<UsuarioResponseDTO>>(`${this.apiUrl}/${id}`, usuario);
  }

  toggleStatus(id: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.apiUrl}/${id}/toggle-status`, {});
  }
}
