import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card card">
        <div class="auth-header">
          <h2>Create new password</h2>
          <p>Choose a secure password with at least 8 characters.</p>
        </div>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label" for="token">Reset Token <span class="required-star">*</span></label>
            <input
              type="text"
              id="token"
              name="token"
              class="form-control font-mono"
              [class.is-invalid]="fieldErrors()['token']"
              [(ngModel)]="token"
              (input)="clearFieldError('token')"
              required
              placeholder="Paste token received"
            />
            @if (fieldErrors()['token']) {
              <span class="form-error">{{ fieldErrors()['token'] }}</span>
            }
          </div>

          <div class="form-group">
            <label class="form-label" for="newPassword">New Password <span class="required-star">*</span></label>
            <input
              type="password"
              id="newPassword"
              name="newPassword"
              class="form-control"
              [class.is-invalid]="fieldErrors()['newPassword']"
              [(ngModel)]="newPassword"
              (input)="clearFieldError('newPassword')"
              required
              minlength="8"
              placeholder="At least 8 characters"
            />
            @if (fieldErrors()['newPassword']) {
              <span class="form-error">{{ fieldErrors()['newPassword'] }}</span>
            } @else {
              <span class="form-hint">Must be at least 8 characters.</span>
            }
          </div>

          <button
            type="submit"
            class="btn btn-primary btn-block"
            [disabled]="loading()"
          >
            @if (loading()) {
              <span>Updating password...</span>
            } @else {
              <span>Update Password</span>
            }
          </button>
        </form>

        <div class="auth-footer">
          Back to <a routerLink="/login">Sign in</a>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .auth-page {
        min-height: calc(100vh - 200px);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 2rem 1rem;
      }
      .auth-card {
        max-width: 440px;
        width: 100%;
        padding: 2.5rem 2rem;
      }
      .auth-header {
        text-align: center;
        margin-bottom: 1.5rem;
        h2 { margin-bottom: 0.25rem; }
        p { color: var(--color-text-subtle); font-size: 0.9rem; }
      }
      .font-mono { font-family: var(--font-mono); }
      .btn-block { width: 100%; margin-top: 1rem; }
      .auth-footer { text-align: center; margin-top: 1.75rem; font-size: 0.875rem; color: var(--color-text-muted); }
    `,
  ],
})
export class ResetPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly tokenQuery = input<string>('', { alias: 'token' });

  token = '';
  newPassword = '';
  readonly loading = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});

  clearFieldError(field: string) {
    this.fieldErrors.update(errs => {
      const copy = { ...errs };
      delete copy[field];
      return copy;
    });
  }

  ngOnInit() {
    if (this.tokenQuery()) {
      this.token = this.tokenQuery();
    }
  }

  onSubmit() {
    this.fieldErrors.set({});

    const errors: Record<string, string> = {};
    if (!this.token.trim()) {
      errors['token'] = 'Reset token is required';
    }

    if (!this.newPassword) {
      errors['newPassword'] = 'New password is required';
    } else if (this.newPassword.length < 8) {
      errors['newPassword'] = 'Password must be at least 8 characters';
    }

    if (Object.keys(errors).length > 0) {
      this.fieldErrors.set(errors);
      return;
    }

    this.loading.set(true);

    this.auth.resetPassword({ token: this.token, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Password updated successfully! Please sign in.');
        this.router.navigate(['/login']);
      },
      error: err => {
        this.loading.set(false);
        if (err?.error?.fieldErrors && Array.isArray(err.error.fieldErrors)) {
          const backendErrors: Record<string, string> = {};
          for (const fe of err.error.fieldErrors) {
            backendErrors[fe.field] = fe.message;
          }
          this.fieldErrors.set(backendErrors);
        }
        this.toast.error(err?.error?.message || 'Invalid or expired reset token');
      },
    });
  }
}
