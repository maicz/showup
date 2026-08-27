import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'events', component: class {} }]),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created and initial state not authenticated', () => {
    expect(service).toBeTruthy();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('login should save token and set authenticated to true', () => {
    service.login({ email: 'test@example.com', password: 'Password123!' }).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ accessToken: 'mock_jwt_token', tokenType: 'Bearer', expiresInSeconds: 3600 });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('mock_jwt_token');

    const profileReq = httpMock.expectOne('/api/members/me');
    profileReq.flush({ id: '1', email: 'test@example.com', displayName: 'Test User' });
  });

  it('logout should clear token and state', () => {
    service.logout(false);
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });
});
