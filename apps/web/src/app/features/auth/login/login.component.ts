import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card card">
        <div class="auth-header">
          <div class="brand-badge">SU</div>
          <h2>Welcome back</h2>
          <p>Sign in to your ShowUp account</p>
        </div>

        <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
          <div class="form-group">
            <label class="form-label" for="email">Email address</label>
            <input
              type="email"
              id="email"
              name="email"
              class="form-control"
              [(ngModel)]="email"
              required
              placeholder="you@example.com"
            />
          </div>

          <div class="form-group">
            <div class="password-label-row">
              <label class="form-label" for="password">Password</label>
              <a routerLink="/forgot-password" class="forgot-link">Forgot password?</a>
            </div>
            <input
              type="password"
              id="password"
              name="password"
              class="form-control"
              [(ngModel)]="password"
              required
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            class="btn btn-primary btn-block"
            [disabled]="loading() || !email || !password"
          >
            @if (loading()) {
              <span>Signing in...</span>
            } @else {
              <span>Sign In</span>
            }
          </button>
        </form>

        <div class="auth-footer">
          Don't have an account? <a routerLink="/register">Sign up</a>
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
          margin-bottom: 0;
        }
      }

      .password-label-row {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .forgot-link {
          font-size: 0.8rem;
          color: var(--color-primary);
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
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  email = '';
  password = '';
  readonly loading = signal(false);

  onSubmit() {
    if (!this.email || !this.password) return;
    this.loading.set(true);

    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl(this.safeReturnUrl());
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Invalid email or password');
      },
    });
  }

  private safeReturnUrl(): string {
    const requested = this.route.snapshot.queryParamMap.get('returnUrl');
    return requested?.startsWith('/') && !requested.startsWith('//') ? requested : '/dashboard';
  }
}
