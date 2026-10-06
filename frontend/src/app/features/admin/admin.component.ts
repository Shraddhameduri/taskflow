import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { UserService } from '../../core/user.service';
import { ALL_ROLES, Role, User } from '../../core/models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  auth = inject(AuthService);
  private users = inject(UserService);

  allRoles = ALL_ROLES;
  list: User[] = [];
  loading = true;
  error = '';
  editingRolesFor: number | null = null;
  draftRoles: Role[] = [];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.users.list().subscribe({
      next: (page) => {
        this.list = page.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load users.';
        this.loading = false;
      },
    });
  }

  startRoleEdit(u: User): void {
    this.editingRolesFor = u.id;
    this.draftRoles = [...u.roles] as Role[];
  }

  toggleDraftRole(role: Role): void {
    this.draftRoles = this.draftRoles.includes(role)
      ? this.draftRoles.filter((r) => r !== role)
      : [...this.draftRoles, role];
  }

  saveRoles(u: User): void {
    if (this.draftRoles.length === 0) {
      this.error = 'A user must have at least one role.';
      return;
    }
    this.users.updateRoles(u.id, this.draftRoles).subscribe({
      next: (updated) => {
        u.roles = updated.roles;
        this.editingRolesFor = null;
      },
      error: () => (this.error = 'Could not update roles.'),
    });
  }

  toggleEnabled(u: User): void {
    this.users.setEnabled(u.id, !u.enabled).subscribe({
      next: (updated) => (u.enabled = updated.enabled),
      error: () => (this.error = 'Could not update user.'),
    });
  }

  remove(u: User): void {
    if (u.username === this.auth.user()?.username) {
      this.error = 'You cannot delete your own account.';
      return;
    }
    if (!confirm(`Delete user "${u.username}"?`)) {
      return;
    }
    this.users.delete(u.id).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Could not delete user.'),
    });
  }
}
