import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConfiguracionSistema {
  clave: string;
  valor: string;
  descripcion?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfiguracionService {
  private apiUrl = 'http://localhost:8080/api/admin/configuracion';

  constructor(private http: HttpClient) {}

  obtenerTodas(): Observable<ConfiguracionSistema[]> {
    return this.http.get<ConfiguracionSistema[]>(this.apiUrl);
  }

  obtenerPorClave(clave: string): Observable<ConfiguracionSistema> {
    return this.http.get<ConfiguracionSistema>(`${this.apiUrl}/${clave}`);
  }

  actualizar(clave: string, valor: string): Observable<ConfiguracionSistema> {
    return this.http.put<ConfiguracionSistema>(`${this.apiUrl}/${clave}`, { valor });
  }
}
