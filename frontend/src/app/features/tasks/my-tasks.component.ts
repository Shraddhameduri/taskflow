import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TaskService } from '../../core/task.service';
import { TASK_STATUSES, Task, TaskStatus } from '../../core/models';

@Component({
  selector: 'app-my-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './my-tasks.component.html',
  styleUrl: './my-tasks.component.css',
})
export class MyTasksComponent implements OnInit {
  private tasks = inject(TaskService);

  list: Task[] = [];
  statuses = TASK_STATUSES;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.tasks.myTasks().subscribe({
      next: (page) => {
        this.list = page.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load your tasks.';
        this.loading = false;
      },
    });
  }

  changeStatus(t: Task, status: TaskStatus): void {
    if (status === t.status) {
      return;
    }
    this.tasks.updateStatus(t.id, status).subscribe({
      next: (updated) => (t.status = updated.status),
      error: () => (this.error = 'Could not update status.'),
    });
  }

  statusLabel(s: TaskStatus): string {
    return s === 'TODO' ? 'To Do' : s === 'IN_PROGRESS' ? 'In Progress' : 'Done';
  }
}
