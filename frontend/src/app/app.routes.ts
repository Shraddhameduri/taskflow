import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ShellComponent } from './layout/shell.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProjectDetailComponent } from './features/projects/project-detail.component';
import { MyTasksComponent } from './features/tasks/my-tasks.component';
import { AdminComponent } from './features/admin/admin.component';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'projects/:id', component: ProjectDetailComponent },
      { path: 'my-tasks', component: MyTasksComponent },
      { path: 'admin', component: AdminComponent, canActivate: [adminGuard] },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
