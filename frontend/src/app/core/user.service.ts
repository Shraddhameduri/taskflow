import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, Role, User } from './models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private base = '/api/users';

  list(page = 0, size = 20): Observable<Page<User>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'username,asc');
    return this.http.get<Page<User>>(this.base, { params });
  }

  updateRoles(id: number, roles: Role[]): Observable<User> {
    return this.http.put<User>(`${this.base}/${id}/roles`, { roles });
  }

  setEnabled(id: number, enabled: boolean): Observable<User> {
    return this.http.patch<User>(`${this.base}/${id}/enabled`, { enabled });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
