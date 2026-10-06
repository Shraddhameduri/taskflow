import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, Project } from './models';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private http = inject(HttpClient);
  private base = '/api/projects';

  list(page = 0, size = 12): Observable<Page<Project>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<Project>>(this.base, { params });
  }

  get(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.base}/${id}`);
  }

  create(name: string, description: string): Observable<Project> {
    return this.http.post<Project>(this.base, { name, description });
  }

  update(id: number, name: string, description: string): Observable<Project> {
    return this.http.put<Project>(`${this.base}/${id}`, { name, description });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
