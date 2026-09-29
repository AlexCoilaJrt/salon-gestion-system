import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Permission {
  id: number;
  name: string;
  description: string;
  module: string;
  active: boolean;
}

export interface Role {
  id?: number;
  name: string;
  description: string;
  active: boolean;
  permissions?: Permission[];
  permissionIds?: number[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class SecurityService {
  private readonly ROLES_API_URL = 'http://localhost:8080/api/roles';
  private readonly PERMISSIONS_API_URL = 'http://localhost:8080/api/permissions';

  constructor(private http: HttpClient) {}

  // --- ROLES ---
  getRoles(page: number = 0, size: number = 10): Observable<ApiResponse<PageResponse<Role>>> {
    return this.http.get<ApiResponse<PageResponse<Role>>>(`${this.ROLES_API_URL}?page=${page}&size=${size}`);
  }

  createRole(role: Partial<Role>): Observable<ApiResponse<Role>> {
    return this.http.post<ApiResponse<Role>>(this.ROLES_API_URL, role);
  }

  updateRole(id: number, role: Partial<Role>): Observable<ApiResponse<Role>> {
    return this.http.put<ApiResponse<Role>>(`${this.ROLES_API_URL}/${id}`, role);
  }

  deleteRole(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.ROLES_API_URL}/${id}`);
  }

  assignPermissions(roleId: number, permissionIds: number[]): Observable<ApiResponse<Role>> {
    return this.http.post<ApiResponse<Role>>(`${this.ROLES_API_URL}/${roleId}/permissions`, permissionIds);
  }

  // --- PERMISSIONS ---
  getAllPermissionsList(): Observable<ApiResponse<Permission[]>> {
    return this.http.get<ApiResponse<Permission[]>>(`${this.PERMISSIONS_API_URL}/list`);
  }
}
