import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environment';
import { LoginRequest, LoginResponse, RegisterRequest, UserResponse } from '../models/user.model';
import { Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;
  private currentUser: UserResponse | null = null;

  register(body: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/auth/register`, body);
  }

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/auth/login`, body).pipe(
      tap((res) => {
        localStorage.setItem('auth_token', res.token);
        // Convert the login user response to UserResponse format
        const user: UserResponse = {
          id: res.user.id,
          name: res.user.name,
          email: res.user.email,
          role: res.user.role,
          avatarId: res.user.avatarId
        };
        this.currentUser = user;
        localStorage.setItem('current_user', JSON.stringify(user));

        // Fetch full user profile to get avatarId if available
        this.fetchUserProfile();
      })
    );
  }

  logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('current_user');
    this.currentUser = null;
  }

  getProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/profile/me`);
  }

  get token(): string | null {
    return localStorage.getItem('auth_token');
  }

  isAuthenticated(): boolean {
    return !!this.token;
  }

  // Fetch current user details from the backend to get avatar
  fetchUserProfile(): void {
    if (!this.token) return;

    this.http.get<UserResponse>(`${this.base}/profile/me`).subscribe({
      next: (user) => {
        this.currentUser = user;
        localStorage.setItem('current_user', JSON.stringify(user));
      },
      error: () => {
        // Keep existing user data on error
      }
    });
  }

  // Get cached user or from localStorage
  getCurrentUser(): UserResponse | null {
    if (this.currentUser) return this.currentUser;

    const stored = localStorage.getItem('current_user');
    if (stored) {
      this.currentUser = JSON.parse(stored);
      return this.currentUser;
    }

    return null;
  }

  isSeller(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'SELLER';
  }
}

