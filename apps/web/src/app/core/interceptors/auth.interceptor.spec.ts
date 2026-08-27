import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let router: Router;
  let toastService: ToastService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: {
            navigate: vi.fn(),
          },
        },
        {
          provide: ToastService,
          useValue: {
            warning: vi.fn(),
            error: vi.fn(),
            info: vi.fn(),
            success: vi.fn(),
          },
        },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('injects Bearer token when user is authenticated', () => {
    (authService as any)._token.set('valid_jwt_token');

    http.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid_jwt_token');
    req.flush({});
  });

  it('logs out and redirects to login when 401 occurs with an active token', () => {
    (authService as any)._token.set('expired_jwt_token');
    const logoutSpy = vi.spyOn(authService, 'logout');

    http.get('/api/protected').subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(401);
      },
    });

    const req = httpMock.expectOne('/api/protected');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(logoutSpy).toHaveBeenCalledWith(false);
    expect(toastService.warning).toHaveBeenCalledWith('Your session has expired. Please sign in again.');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('does NOT log out or redirect when 401 occurs without a token', () => {
    (authService as any)._token.set(null);
    const logoutSpy = vi.spyOn(authService, 'logout');

    let receivedError: HttpErrorResponse | null = null;
    http.get('/api/public-endpoint').subscribe({
      error: (err: HttpErrorResponse) => {
        receivedError = err;
      },
    });

    const req = httpMock.expectOne('/api/public-endpoint');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(receivedError).not.toBeNull();
    expect(logoutSpy).not.toHaveBeenCalled();
    expect(toastService.warning).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does NOT redirect on 401 for login endpoint even with token', () => {
    (authService as any)._token.set('some_token');
    const logoutSpy = vi.spyOn(authService, 'logout');

    http.post('/api/auth/login', {}).subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(401);
      },
    });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush('Bad credentials', { status: 401, statusText: 'Unauthorized' });

    expect(logoutSpy).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
