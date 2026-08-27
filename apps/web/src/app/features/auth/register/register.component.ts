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
            <label class="form-label" for="displayName">Your Name</label>
            <input
              type="text"
              id="displayName"
              name="displayName"
              class="form-control"
              [(ngModel)]="displayName"
              required
              placeholder="e.g. Alex Morgan"
            />
          </div>

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
            <label class="form-label" for="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              class="form-control"
              [(ngModel)]="password"
              required
              minlength="8"
              placeholder="At least 8 characters"
            />
          </div>

          <button
            type="submit"
            class="btn btn-primary btn-block"
            [disabled]="loading() || !displayName || !email || !password || password.length < 8"
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

  onSubmit() {
    if (!this.displayName || !this.email || !this.password) return;
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
          this.toast.error(err?.error?.message || 'Registration failed');
        },
      });
  }
}
