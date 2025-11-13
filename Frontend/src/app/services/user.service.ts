import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environment';
import { LoginRequest, LoginResponse, RegisterRequest, UserResponse } from '../models/user.model';
import { Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  register(body: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/auth/register`, body);
  }

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/auth/login`, body).pipe(
      tap((res) => localStorage.setItem('auth_token', res.token))
    );
  }

  logout() {
    localStorage.removeItem('auth_token');
  }

  get token(): string | null {
    return localStorage.getItem('auth_token');
  }
}

