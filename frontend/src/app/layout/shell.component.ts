import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
})
export class ShellComponent {
  auth = inject(AuthService);
  private router = inject(Router);

  get isAdmin(): boolean {
    return this.auth.hasRole('ADMIN');
  }

  logout(): void {
    this.auth.logout();
  }

  logoutEverywhere(): void {
    if (confirm('Sign out on all devices?')) {
      this.auth.logoutEverywhere();
    }
  }
}
