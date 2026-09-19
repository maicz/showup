import { Routes } from '@angular/router';
import { authGuard, noAuthGuard } from './core/guards/auth.guard';
export const routes: Routes = [
  { path: '', redirectTo: 'events', pathMatch: 'full' },
  { path: 'events', loadComponent: () => import('./features/event/event-list/event-list.component').then(m => m.EventListComponent) },
  { path: 'events/create', loadComponent: () => import('./features/event/event-create/event-create.component').then(m => m.EventCreateComponent), canActivate: [authGuard] },
  { path: 'events/:id', loadComponent: () => import('./features/event/event-detail/event-detail.component').then(m => m.EventDetailComponent) },
  { path: 'events/:id/check-in', loadComponent: () => import('./features/attendance/check-in/check-in.component').then(m => m.CheckInComponent), canActivate: [authGuard] },
  { path: 'events/:id/staff', loadComponent: () => import('./features/attendance/staff/staff-management.component').then(m => m.StaffManagementComponent), canActivate: [authGuard] },
  { path: 'events/:id/report', loadComponent: () => import('./features/report/event-report/event-report.component').then(m => m.EventReportComponent), canActivate: [authGuard] },

  { path: 'groups', loadComponent: () => import('./features/group/group-list/group-list.component').then(m => m.GroupListComponent) },
  { path: 'groups/create', loadComponent: () => import('./features/group/group-create/group-create.component').then(m => m.GroupCreateComponent), canActivate: [authGuard] },
  { path: 'groups/:id', loadComponent: () => import('./features/group/group-detail/group-detail.component').then(m => m.GroupDetailComponent) },
  { path: 'groups/:id/manage', loadComponent: () => import('./features/group/group-manage/group-manage.component').then(m => m.GroupManageComponent), canActivate: [authGuard] },
  { path: 'groups/:id/report', loadComponent: () => import('./features/report/group-report/group-report.component').then(m => m.GroupReportComponent), canActivate: [authGuard] },

  { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: 'profile', loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent), canActivate: [authGuard] },

  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent), canActivate: [noAuthGuard] },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent), canActivate: [noAuthGuard] },
  { path: 'forgot-password', loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'verify-email', loadComponent: () => import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent) },

  { path: '**', redirectTo: 'events' },
];
