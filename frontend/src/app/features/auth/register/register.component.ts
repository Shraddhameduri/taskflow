import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrl: '../login/login.component.css',
})
export class RegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  username = '';
  email = '';
  password = '';
  error = '';
  loading = false;

  submit(): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth.register(this.username.trim(), this.email.trim(), this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.loading = false;
        this.error = err.status === 429
          ? 'Too many attempts — please wait a minute and try again.'
          : err.error?.message || 'Registration failed. Try a different username.';
      },
    });
  }
}
