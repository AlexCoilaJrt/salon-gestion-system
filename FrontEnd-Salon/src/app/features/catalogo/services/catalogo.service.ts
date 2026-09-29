import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Categoria {
  id?: number;
  nombre: string;
  descripcion?: string;
  estado?: boolean;
}

export interface Servicio {
  id?: number;
  nombre: string;
  descripcion?: string;
  precioBase: number;
  duracionMinutos: number;
  comisionPorcentaje?: number;
  costoMaterial?: number;
  estado?: boolean;
  especialidadRequeridaId: number;
  especialidadRequeridaNombre?: string;
  categoriaId: number;
  categoriaNombre?: string;
  insumosIds?: number[];
  insumos?: Producto[];
}

export interface Producto {
  id?: number;
  nombre: string;
  marca?: string;
  sku?: string;
  proveedor?: string;
  imageUrl?: string;
  costo: number;
  precioVenta: number;
  stockActual: number;
  stockMinimo: number;
  usoInterno: boolean;
  ventaDirecta: boolean;
  estado?: boolean;
  categoriaId: number;
  categoriaNombre?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CatalogoService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/v1';

  // --- Categorías ---
  getCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.apiUrl}/categorias`);
  }

  getCategoriasActivas(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.apiUrl}/categorias/activas`);
  }

  createCategoria(categoria: Categoria): Observable<Categoria> {
    return this.http.post<Categoria>(`${this.apiUrl}/categorias`, categoria);
  }

  updateCategoria(id: number, categoria: Categoria): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.apiUrl}/categorias/${id}`, categoria);
  }

  deleteCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/categorias/${id}`);
  }

  // --- Servicios ---
  getServicios(): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(`${this.apiUrl}/servicios`);
  }

  createServicio(servicio: Servicio): Observable<Servicio> {
    return this.http.post<Servicio>(`${this.apiUrl}/servicios`, servicio);
  }

  updateServicio(id: number, servicio: Servicio): Observable<Servicio> {
    return this.http.put<Servicio>(`${this.apiUrl}/servicios/${id}`, servicio);
  }

  deleteServicio(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/servicios/${id}`);
  }

  // --- Productos ---
  getProductos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.apiUrl}/productos`);
  }

  createProducto(producto: Producto): Observable<Producto> {
    return this.http.post<Producto>(`${this.apiUrl}/productos`, producto);
  }

  updateProducto(id: number, producto: Producto): Observable<Producto> {
    return this.http.put<Producto>(`${this.apiUrl}/productos/${id}`, producto);
  }

  deleteProducto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/productos/${id}`);
  }
}
