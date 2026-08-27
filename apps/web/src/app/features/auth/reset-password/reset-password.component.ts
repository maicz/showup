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
            <label class="form-label" for="token">Reset Token</label>
            <input
              type="text"
              id="token"
              name="token"
              class="form-control font-mono"
              [(ngModel)]="token"
              required
              placeholder="Paste token received"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="newPassword">New Password</label>
            <input
              type="password"
              id="newPassword"
              name="newPassword"
              class="form-control"
              [(ngModel)]="newPassword"
              required
              minlength="8"
              placeholder="At least 8 characters"
            />
          </div>

          <button
            type="submit"
            class="btn btn-primary btn-block"
            [disabled]="loading() || !token || !newPassword || newPassword.length < 8"
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

  ngOnInit() {
    if (this.tokenQuery()) {
      this.token = this.tokenQuery();
    }
  }

  onSubmit() {
    if (!this.token || !this.newPassword) return;
    this.loading.set(true);

    this.auth.resetPassword({ token: this.token, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/login']);
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Invalid or expired reset token');
      },
    });
  }
}
