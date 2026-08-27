import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card card">
        <div class="auth-header">
          <div class="brand-badge">SU</div>
          <h2>Join ShowUp</h2>
          <p>Discover real events & communities</p>
        </div>

        <form (ngSubmit)="onSubmit()" #registerForm="ngForm">
          <div class="form-group">
            <label class="form-label" for="displayName">Your Name <span class="required-star">*</span></label>
            <input
              type="text"
              id="displayName"
              name="displayName"
              class="form-control"
              [class.is-invalid]="fieldErrors()['displayName']"
              [(ngModel)]="displayName"
              (input)="clearFieldError('displayName')"
              required
              placeholder="e.g. Alex Morgan"
            />
            @if (fieldErrors()['displayName']) {
              <span class="form-error">{{ fieldErrors()['displayName'] }}</span>
            }
          </div>

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

          <div class="form-group">
            <label class="form-label" for="password">Password <span class="required-star">*</span></label>
            <input
              type="password"
              id="password"
              name="password"
              class="form-control"
              [class.is-invalid]="fieldErrors()['password']"
              [(ngModel)]="password"
              (input)="clearFieldError('password')"
              required
              minlength="8"
              placeholder="At least 8 characters"
            />
            @if (fieldErrors()['password']) {
              <span class="form-error">{{ fieldErrors()['password'] }}</span>
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
              <span>Creating account...</span>
            } @else {
              <span>Create Account</span>
            }
          </button>
        </form>

        <div class="auth-footer">
          Already have an account? <a routerLink="/login">Log in</a>
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
        margin-bottom: 2rem;

        .brand-badge {
          display: inline-block;
          background: var(--color-primary);
          color: white;
          font-weight: 800;
          font-size: 1rem;
          padding: 6px 12px;
          border-radius: 8px;
          margin-bottom: 1rem;
        }

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

      .auth-footer {
        text-align: center;
        margin-top: 1.75rem;
        font-size: 0.875rem;
        color: var(--color-text-muted);
      }
    `,
  ],
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  displayName = '';
  email = '';
  password = '';
  readonly loading = signal(false);
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

    const errors: Record<string, string> = {};
    if (!this.displayName.trim()) {
      errors['displayName'] = 'Your name is required';
    }

    if (!this.email.trim()) {
      errors['email'] = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) {
      errors['email'] = 'Please enter a valid email address';
    }

    if (!this.password) {
      errors['password'] = 'Password is required';
    } else if (this.password.length < 8) {
      errors['password'] = 'Password must be at least 8 characters';
    }

    if (Object.keys(errors).length > 0) {
      this.fieldErrors.set(errors);
      return;
    }

    this.loading.set(true);

    this.auth
      .register({
        displayName: this.displayName,
        email: this.email,
        password: this.password,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/dashboard']);
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
          this.toast.error(err?.error?.message || 'Registration failed');
        },
      });
  }
}
