import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card card">
        <div class="auth-header">
          <h2>Reset password</h2>
          <p>Enter your email and we'll send you recovery instructions.</p>
        </div>

        @if (submitted()) {
          <div class="alert-success">
            <p>If that email address is registered, instructions have been sent.</p>
            <a routerLink="/login" class="btn btn-secondary btn-block">Back to Sign In</a>
          </div>
        } @else {
          <form (ngSubmit)="onSubmit()">
            <div class="form-group">
              <label class="form-label" for="email">Email address <span class="required-star">*</span></label>
              <input
                type="email"
                id="email"
                name="email"
                class="form-control"
                [class.is-invalid]="fieldErrors()['email']"
                [(ngModel)]="email"
                (input)="clearFieldError('email')"
                required
                placeholder="you@example.com"
              />
              @if (fieldErrors()['email']) {
                <span class="form-error">{{ fieldErrors()['email'] }}</span>
              }
            </div>

            <button
              type="submit"
              class="btn btn-primary btn-block"
              [disabled]="loading()"
            >
              @if (loading()) {
                <span>Sending link...</span>
              } @else {
                <span>Send Reset Link</span>
              }
            </button>
          </form>

          <div class="auth-footer">
            Remembered your password? <a routerLink="/login">Sign in</a>
          </div>
        }
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

        h2 {
          margin-bottom: 0.25rem;
        }

        p {
          color: var(--color-text-subtle);
          font-size: 0.9rem;
        }
      }

      .btn-block {
        width: 100%;
        margin-top: 1rem;
      }

      .alert-success {
        background: var(--color-success-subtle);
        border: 1px solid var(--color-success);
        color: var(--color-success-text);
        padding: 1rem;
        border-radius: 8px;
        text-align: center;
      }

      .auth-footer {
        text-align: center;
        margin-top: 1.75rem;
        font-size: 0.875rem;
        color: var(--color-text-muted);
      }
    `,
  ],
})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  email = '';
  readonly loading = signal(false);
  readonly submitted = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});

  clearFieldError(field: string) {
    this.fieldErrors.update(errs => {
      const copy = { ...errs };
      delete copy[field];
      return copy;
    });
  }

  onSubmit() {
    this.fieldErrors.set({});

    if (!this.email.trim()) {
      this.fieldErrors.set({ email: 'Email address is required' });
      return;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) {
      this.fieldErrors.set({ email: 'Please enter a valid email address' });
      return;
    }

    this.loading.set(true);

    this.auth.forgotPassword({ email: this.email }).subscribe({
      next: () => {
        this.loading.set(false);
        this.submitted.set(true);
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
        this.toast.error(err?.error?.message || 'Unable to process request');
      },
    });
  }
}
