import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthResponse, SessionUser } from './models';

const ACCESS_KEY = 'tf_access';
const REFRESH_KEY = 'tf_refresh';
const SESSION_KEY = 'tf_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private session = signal<SessionUser | null>(this.loadSession());
  readonly user = this.session.asReadonly();
  readonly isLoggedIn = computed(() => this.session() !== null);

  accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  hasRole(role: string): boolean {
    return this.session()?.roles.includes(role) ?? false;
  }

  /** ADMIN or MANAGER — allowed to manage projects and tasks. */
  canManage(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER');
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { username, password })
      .pipe(tap((res) => this.saveSession(res)));
  }

  register(username: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { username, email, password })
      .pipe(tap((res) => this.saveSession(res)));
  }

  refresh(): Observable<AuthResponse> {
    const rt = localStorage.getItem(REFRESH_KEY);
    if (!rt) {
      return throwError(() => new Error('No refresh token'));
    }
    return this.http
      .post<AuthResponse>('/api/auth/refresh', { refreshToken: rt })
      .pipe(tap((res) => this.saveSession(res)));
  }

  logout(): void {
    const rt = localStorage.getItem(REFRESH_KEY);
    if (rt) {
      // Best effort: tell the server to revoke the token, then clear locally regardless.
      this.http.post('/api/auth/logout', { refreshToken: rt }).subscribe({ error: () => undefined });
    }
    this.clearSession();
    this.router.navigate(['/login']);
  }

  logoutEverywhere(): void {
    this.http.post('/api/auth/logout-all', {}).subscribe({
      next: () => this.logout(),
      error: () => this.logout(),
    });
  }

  private saveSession(res: AuthResponse): void {
    localStorage.setItem(ACCESS_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    const session: SessionUser = {
      username: res.username,
      roles: res.roles.map((r) => r.replace(/^ROLE_/, '')),
    };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  private clearSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(SESSION_KEY);
    this.session.set(null);
  }

  private loadSession(): SessionUser | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      return raw ? (JSON.parse(raw) as SessionUser) : null;
    } catch {
      return null;
    }
  }
}
