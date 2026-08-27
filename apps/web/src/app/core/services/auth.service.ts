import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import {
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  ResetPasswordRequest,
  SsoLoginRequest,
  TokenResponse,
  VerifyEmailRequest,
} from '../models/auth.model';
import { MemberProfile } from '../models/member.model';
import { ToastService } from './toast.service';

const TOKEN_KEY = 'showup_token';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly _token = signal<string | null>(this.getInitialToken());
  private readonly _currentUser = signal<MemberProfile | null>(null);

  readonly token = this._token.asReadonly();
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());

  constructor() {
    if (this._token()) {
      this.fetchProfile().subscribe({
        error: () => this.logout(false),
      });
    }
  }

  private getInitialToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  register(payload: RegisterRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/register', payload).pipe(
      tap(res => {
        this.saveToken(res.accessToken);
        this.toast.success('Welcome to ShowUp! Check your inbox to verify your email.');
        this.fetchProfile().subscribe();
      })
    );
  }

  login(payload: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', payload).pipe(
      tap(res => {
        this.saveToken(res.accessToken);
        this.toast.success('Signed in successfully.');
        this.fetchProfile().subscribe();
      })
    );
  }

  ssoLogin(payload: SsoLoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/sso', payload).pipe(
      tap(res => {
        this.saveToken(res.accessToken);
        this.toast.success(`Signed in with ${payload.provider}.`);
        this.fetchProfile().subscribe();
      })
    );
  }

  forgotPassword(payload: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/forgot-password', payload).pipe(
      tap(res => this.toast.info(res.message))
    );
  }

  resetPassword(payload: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/reset-password', payload).pipe(
      tap(res => this.toast.success(res.message))
    );
  }

  verifyEmail(payload: VerifyEmailRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/verify-email', payload).pipe(
      tap(res => {
        this.toast.success(res.message);
        if (this.isAuthenticated()) {
          this.fetchProfile().subscribe();
        }
      })
    );
  }

  resendVerification(): Observable<void> {
    return this.http.post<void>('/api/auth/resend-verification', {}).pipe(
      tap(() => this.toast.info('Verification email resent.'))
    );
  }

  fetchProfile(): Observable<MemberProfile> {
    return this.http.get<MemberProfile>('/api/members/me').pipe(
      tap(profile => this._currentUser.set(profile))
    );
  }

  setCurrentUser(profile: MemberProfile) {
    this._currentUser.set(profile);
  }

  logout(showToast = true) {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      // ignore
    }
    this._token.set(null);
    this._currentUser.set(null);
    if (showToast) {
      this.toast.info('Signed out.');
    }
    this.router.navigate(['/events']);
  }

  private saveToken(token: string) {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {
      // ignore
    }
    this._token.set(token);
  }
}
