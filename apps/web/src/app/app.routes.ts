import { Routes } from '@angular/router';
import { authGuard, noAuthGuard } from './core/guards/auth.guard';
import { CheckInComponent } from './features/attendance/check-in/check-in.component';
import { StaffManagementComponent } from './features/attendance/staff/staff-management.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { VerifyEmailComponent } from './features/auth/verify-email/verify-email.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { EventCreateComponent } from './features/event/event-create/event-create.component';
import { EventDetailComponent } from './features/event/event-detail/event-detail.component';
import { EventListComponent } from './features/event/event-list/event-list.component';
import { GroupCreateComponent } from './features/group/group-create/group-create.component';
import { GroupDetailComponent } from './features/group/group-detail/group-detail.component';
import { GroupListComponent } from './features/group/group-list/group-list.component';
import { GroupManageComponent } from './features/group/group-manage/group-manage.component';
import { ProfileComponent } from './features/profile/profile.component';
import { EventReportComponent } from './features/report/event-report/event-report.component';
import { GroupReportComponent } from './features/report/group-report/group-report.component';

export const routes: Routes = [
  { path: '', redirectTo: 'events', pathMatch: 'full' },
  { path: 'events', component: EventListComponent },
  { path: 'events/create', component: EventCreateComponent, canActivate: [authGuard] },
  { path: 'events/:id', component: EventDetailComponent },
  { path: 'events/:id/check-in', component: CheckInComponent, canActivate: [authGuard] },
  { path: 'events/:id/staff', component: StaffManagementComponent, canActivate: [authGuard] },
  { path: 'events/:id/report', component: EventReportComponent, canActivate: [authGuard] },

  { path: 'groups', component: GroupListComponent },
  { path: 'groups/create', component: GroupCreateComponent, canActivate: [authGuard] },
  { path: 'groups/:id', component: GroupDetailComponent },
  { path: 'groups/:id/manage', component: GroupManageComponent, canActivate: [authGuard] },
  { path: 'groups/:id/report', component: GroupReportComponent, canActivate: [authGuard] },

  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },

  { path: 'login', component: LoginComponent, canActivate: [noAuthGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [noAuthGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'verify-email', component: VerifyEmailComponent },

  { path: '**', redirectTo: 'events' },
];
