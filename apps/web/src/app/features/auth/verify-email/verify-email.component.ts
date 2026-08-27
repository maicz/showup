import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card card">
        <div class="auth-header">
          <h2>Email Verification</h2>
          <p>Confirm your email address to enable all organizer & RSVP features.</p>
        </div>

        @if (verified()) {
          <div class="verified-box">
            <div class="check-icon">✓</div>
            <h3>Email Verified!</h3>
            <p>Your email address has been confirmed.</p>
            <a routerLink="/dashboard" class="btn btn-primary btn-block">Go to Dashboard</a>
          </div>
        } @else {
          <form (ngSubmit)="verify()">
            <div class="form-group">
              <label class="form-label" for="token">Verification Token</label>
              <input
                type="text"
                id="token"
                name="token"
                class="form-control"
                [(ngModel)]="token"
                required
                placeholder="Paste token from email"
              />
            </div>

            <button
              type="submit"
              class="btn btn-primary btn-block"
              [disabled]="loading() || !token"
            >
              @if (loading()) {
                <span>Verifying...</span>
              } @else {
                <span>Verify Email</span>
              }
            </button>
          </form>

          <div class="resend-section">
            <p>Didn't receive a token?</p>
            <button type="button" class="btn btn-secondary btn-sm" (click)="resend()">Resend Token</button>
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
        h2 { margin-bottom: 0.25rem; }
        p { color: var(--color-text-subtle); font-size: 0.9rem; }
      }
      .btn-block { width: 100%; margin-top: 1rem; }
      .verified-box {
        text-align: center;
        padding: 1.5rem;
        background: var(--color-success-subtle);
        border-radius: 12px;
        .check-icon {
          font-size: 2.5rem;
          color: var(--color-success);
          font-weight: 800;
        }
        h3 { margin: 0.5rem 0; color: var(--color-success-text); }
        p { color: var(--color-success-text); margin-bottom: 1rem; }
      }
      .resend-section {
        margin-top: 2rem;
        padding-top: 1.5rem;
        border-top: 1px solid var(--color-border);
        text-align: center;
        p { font-size: 0.85rem; color: var(--color-text-subtle); margin-bottom: 0.5rem; }
      }
    `,
  ],
})
export class VerifyEmailComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly tokenQuery = input<string>('', { alias: 'token' });

  token = '';
  readonly loading = signal(false);
  readonly verified = signal(false);

  ngOnInit() {
    if (this.tokenQuery()) {
      this.token = this.tokenQuery();
      this.verify();
    }
  }

  verify() {
    if (!this.token) return;
    this.loading.set(true);

    this.auth.verifyEmail({ token: this.token }).subscribe({
      next: () => {
        this.loading.set(false);
        this.verified.set(true);
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Invalid or expired verification token');
      },
    });
  }

  resend() {
    if (!this.auth.isAuthenticated()) {
      this.toast.info('Please sign in first to request a new verification token.');
      return;
    }
    this.auth.resendVerification().subscribe({
      error: err => this.toast.error(err?.error?.message || 'Unable to resend token'),
    });
  }
}
