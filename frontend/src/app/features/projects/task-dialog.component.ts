import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TASK_PRIORITIES, Task, TaskPriority } from '../../core/models';
import { TaskForm, TaskService } from '../../core/task.service';

@Component({
  selector: 'app-task-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './task-dialog.component.html',
  styleUrl: './task-dialog.component.css',
})
export class TaskDialogComponent {
  private tasks = inject(TaskService);

  @Input({ required: true }) projectId!: number;
  @Input() task: Task | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  priorities = TASK_PRIORITIES;
  title = '';
  description = '';
  priority: TaskPriority = 'MEDIUM';
  assigneeUsername = '';
  saving = false;
  error = '';

  ngOnInit(): void {
    if (this.task) {
      this.title = this.task.title;
      this.description = this.task.description || '';
      this.priority = this.task.priority;
      this.assigneeUsername = this.task.assigneeUsername || '';
    }
  }

  get isEdit(): boolean {
    return this.task !== null;
  }

  save(): void {
    if (!this.title.trim() || this.saving) {
      return;
    }
    this.saving = true;
    this.error = '';
    const form: TaskForm = {
      title: this.title.trim(),
      description: this.description.trim(),
      priority: this.priority,
      assigneeUsername: this.assigneeUsername.trim(),
    };
    const req = this.isEdit
      ? this.tasks.update(this.task!.id, this.projectId, form)
      : this.tasks.create(this.projectId, form);
    req.subscribe({
      next: () => this.saved.emit(),
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message || 'Could not save task.';
      },
    });
  }
}
