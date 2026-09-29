import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  contrasena: string;
}

export interface LoginResponse {
  token: string;
  usuario: {
    id: number;
    email: string;
    avatarUrl?: string;
    telefono?: string;
    dni?: string;
    roles: string[];
    permissions: string[];
  };
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(credentials: LoginRequest): Observable<any> {
    const payload = {
      username: credentials.email,
      password: credentials.contrasena
    };
    return this.http.post<any>(`${this.API_URL}/login`, payload).pipe(
      tap(response => {
        if (response.data && response.data.token) {
          this.setToken(response.data.token);
          this.setCurrentUser(response.data);
        }
      })
    );
  }

  setToken(token: string): void {
    localStorage.setItem('salon_token', token);
  }

  setCurrentUser(user: any): void {
    if (user) {
      localStorage.setItem('salon_user', JSON.stringify(user));
    }
  }

  getCurrentUser(): any {
    const userStr = localStorage.getItem('salon_user');
    if (!userStr || userStr === 'undefined') return null;
    try {
      return JSON.parse(userStr);
    } catch (e) {
      console.error('Error parsing user from localStorage:', e);
      return null;
    }
  }

  updateAvatar(avatarUrl: string): Observable<any> {
    return this.http.put<any>(`${this.API_URL}/profile/avatar`, { avatarUrl }).pipe(
      tap(() => {
        const user = this.getCurrentUser();
        if (user) {
          user.avatarUrl = avatarUrl;
          this.setCurrentUser(user);
        }
      })
    );
  }

  getToken(): string | null {
    return localStorage.getItem('salon_token');
  }

  logout(): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/logout`, {}).pipe(
      tap({
        next: () => this.clearLocalSession(),
        error: () => this.clearLocalSession()
      })
    );
  }

  clearLocalSession(): void {
    localStorage.removeItem('salon_token');
    localStorage.removeItem('salon_user');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
