import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ProjectService } from '../../core/project.service';
import { TaskService } from '../../core/task.service';
import { Project, TASK_STATUSES, Task, TaskStatus } from '../../core/models';
import { TaskDialogComponent } from './task-dialog.component';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TaskDialogComponent],
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.css',
})
export class ProjectDetailComponent implements OnInit {
  auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private projects = inject(ProjectService);
  private tasks = inject(TaskService);

  project: Project | null = null;
  allTasks: Task[] = [];
  statuses = TASK_STATUSES;
  loading = true;
  error = '';

  dialogOpen = false;
  editingTask: Task | null = null;

  private projectId = 0;

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.loading = true;
    this.projects.get(this.projectId).subscribe({
      next: (p) => {
        this.project = p;
        this.loadTasks();
      },
      error: () => {
        this.error = 'Project not found.';
        this.loading = false;
      },
    });
  }

  loadTasks(): void {
    this.tasks.listByProject(this.projectId).subscribe({
      next: (page) => {
        this.allTasks = page.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load tasks.';
        this.loading = false;
      },
    });
  }

  tasksIn(status: TaskStatus): Task[] {
    return this.allTasks.filter((t) => t.status === status);
  }

  canEditTask(t: Task): boolean {
    return (
      this.auth.canManage() || t.assigneeUsername === this.auth.user()?.username
    );
  }

  openCreate(): void {
    this.editingTask = null;
    this.dialogOpen = true;
  }

  openEdit(t: Task): void {
    this.editingTask = t;
    this.dialogOpen = true;
  }

  move(t: Task, dir: -1 | 1): void {
    const idx = this.statuses.indexOf(t.status);
    const next = this.statuses[idx + dir];
    if (!next) {
      return;
    }
    this.tasks.updateStatus(t.id, next).subscribe({
      next: (updated) => {
        t.status = updated.status;
      },
      error: () => (this.error = 'Could not update status.'),
    });
  }

  remove(t: Task): void {
    if (!confirm(`Delete task "${t.title}"?`)) {
      return;
    }
    this.tasks.delete(t.id).subscribe({
      next: () => this.loadTasks(),
      error: () => (this.error = 'Could not delete task.'),
    });
  }

  statusLabel(s: TaskStatus): string {
    return s === 'TODO' ? 'To Do' : s === 'IN_PROGRESS' ? 'In Progress' : 'Done';
  }
}
