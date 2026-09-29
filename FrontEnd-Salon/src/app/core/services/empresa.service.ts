import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';

export interface EmpresaResponse {
  id: number;
  nombreComercial: string;
  razonSocial: string;
  ruc: string;
  direccion: string;
  telefono: string;
  email: string;
  logoUrl: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class EmpresaService {
  private apiUrl = 'http://localhost:8080/api/v1/empresa';
  private empresaSubject = new BehaviorSubject<EmpresaResponse | null>(null);
  
  public empresa$ = this.empresaSubject.asObservable();

  constructor(private http: HttpClient) {}

  getEmpresaActiva(): Observable<ApiResponse<EmpresaResponse>> {
    return this.http.get<ApiResponse<EmpresaResponse>>(`${this.apiUrl}/activa`).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.empresaSubject.next(res.data);
        }
      })
    );
  }

  actualizarEmpresa(id: number, data: any): Observable<ApiResponse<EmpresaResponse>> {
    return this.http.put<ApiResponse<EmpresaResponse>>(`${this.apiUrl}/${id}`, data).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.empresaSubject.next(res.data);
        }
      })
    );
  }
}
