import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, Task, TaskPriority, TaskStatus } from './models';

export interface TaskForm {
  title: string;
  description: string;
  priority: TaskPriority;
  assigneeUsername: string;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);
  private base = '/api/tasks';

  listByProject(projectId: number, page = 0, size = 100): Observable<Page<Task>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<Task>>(`${this.base}/project/${projectId}`, { params });
  }

  myTasks(page = 0, size = 50): Observable<Page<Task>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<Task>>(`${this.base}/me`, { params });
  }

  create(projectId: number, form: TaskForm): Observable<Task> {
    return this.http.post<Task>(this.base, {
      title: form.title,
      description: form.description || null,
      projectId,
      priority: form.priority,
      assigneeUsername: form.assigneeUsername || null,
    });
  }

  update(id: number, projectId: number, form: TaskForm): Observable<Task> {
    return this.http.put<Task>(`${this.base}/${id}`, {
      title: form.title,
      description: form.description || null,
      projectId,
      priority: form.priority,
      assigneeUsername: form.assigneeUsername || null,
    });
  }

  updateStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${this.base}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
