import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8080/api/analitica/dashboard';

export interface RankingEspecialistaDTO {
    empleadoId: number;
    nombreCompleto: string;
    cantidadServicios: number;
    totalVendido: number;
}

export interface MargenNetoResponse {
    ingresosTotales: number;
    ingresosServicios: number;
    ingresosProductos: number;
    gastosOperativos: number;
    pagoComisiones: number;
    gananciaNeta: number;
    periodo: string;
}

export interface ServicioDemandaDTO {
    servicioId: number;
    servicioNombre: string;
    cantidadVendida: number;
    totalIngresado: number;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);

  getRankingEspecialistas(mes?: number, anio?: number): Observable<RankingEspecialistaDTO[]> {
    let params = new HttpParams();
    if (mes) params = params.set('mes', mes);
    if (anio) params = params.set('anio', anio);
    return this.http.get<RankingEspecialistaDTO[]>(`${API_URL}/ranking-especialistas`, { params });
  }

  getMargenNeto(mes?: number, anio?: number): Observable<MargenNetoResponse> {
    let params = new HttpParams();
    if (mes) params = params.set('mes', mes);
    if (anio) params = params.set('anio', anio);
    return this.http.get<MargenNetoResponse>(`${API_URL}/margen-neto`, { params });
  }

  getServiciosDemandados(mes?: number, anio?: number): Observable<ServicioDemandaDTO[]> {
    let params = new HttpParams();
    if (mes) params = params.set('mes', mes);
    if (anio) params = params.set('anio', anio);
    return this.http.get<ServicioDemandaDTO[]>(`${API_URL}/servicios-demandados`, { params });
  }
}
