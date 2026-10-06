import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ProjectService } from '../../core/project.service';
import { Project } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  private projects = inject(ProjectService);

  list: Project[] = [];
  loading = true;
  error = '';

  showCreate = false;
  newName = '';
  newDescription = '';
  creating = false;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.projects.list(0, 50).subscribe({
      next: (page) => {
        this.list = page.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load projects.';
        this.loading = false;
      },
    });
  }

  create(): void {
    if (!this.newName.trim() || this.creating) {
      return;
    }
    this.creating = true;
    this.projects.create(this.newName.trim(), this.newDescription.trim()).subscribe({
      next: () => {
        this.creating = false;
        this.showCreate = false;
        this.newName = '';
        this.newDescription = '';
        this.load();
      },
      error: (err) => {
        this.creating = false;
        this.error = err.error?.message || 'Could not create project.';
      },
    });
  }

  remove(p: Project): void {
    if (!confirm(`Delete project "${p.name}" and all its tasks?`)) {
      return;
    }
    this.projects.delete(p.id).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Could not delete project.'),
    });
  }
}
