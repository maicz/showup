import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="app-header">
      <div class="container header-container">
        <div class="header-left">
          <a routerLink="/events" class="brand-logo">
            <span class="brand-badge">SU</span>
            <span class="brand-name">ShowUp</span>
          </a>

          <nav class="nav-links">
            <a routerLink="/events" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Explore Events</a>
            <a routerLink="/groups" routerLinkActive="active">Find Groups</a>
            @if (auth.isAuthenticated()) {
              <a routerLink="/dashboard" routerLinkActive="active">My Dashboard</a>
            }
          </nav>
        </div>

        <div class="header-right">
          @if (auth.isAuthenticated()) {
            <a routerLink="/groups/create" class="btn btn-sm btn-outline create-btn">+ Start Group</a>
            
            <div class="user-menu-wrapper">
              <button
                class="user-profile-btn"
                type="button"
                aria-label="Open account menu"
                [attr.aria-expanded]="isMenuOpen()"
                (click)="toggleMenu()"
              >
                @if (auth.currentUser()?.photoUrl) {
                  <img [src]="auth.currentUser()?.photoUrl" alt="Avatar" class="avatar-img" />
                } @else {
                  <div class="avatar-placeholder">
                    {{ auth.currentUser()?.displayName?.charAt(0) || 'U' }}
                  </div>
                }
                <span class="user-name">{{ auth.currentUser()?.displayName || 'Account' }}</span>
                <span class="dropdown-chevron">▾</span>
              </button>

              @if (isMenuOpen()) {
                <div class="dropdown-panel" (click)="closeMenu()">
                  <div class="dropdown-header">
                    <strong>{{ auth.currentUser()?.displayName }}</strong>
                    <div class="dropdown-email">{{ auth.currentUser()?.email }}</div>
                  </div>
                  <hr class="dropdown-divider" />
                  <a routerLink="/dashboard" class="dropdown-item">Dashboard & RSVPs</a>
                  <a routerLink="/profile" class="dropdown-item">Profile & Interests</a>
                  <hr class="dropdown-divider" />
                  <button class="dropdown-item dropdown-logout" (click)="logout()">Sign Out</button>
                </div>
              }
            </div>
          } @else {
            <div class="auth-buttons">
              <a routerLink="/login" class="btn btn-subtle btn-sm">Log In</a>
              <a routerLink="/register" class="btn btn-primary btn-sm">Sign Up</a>
            </div>
          }
          <button
            class="mobile-menu-toggle"
            type="button"
            aria-label="Toggle navigation"
            [attr.aria-expanded]="isMobileMenuOpen()"
            aria-controls="mobile-navigation"
            (click)="toggleMobileMenu()"
          >
            <span aria-hidden="true">{{ isMobileMenuOpen() ? '×' : '☰' }}</span>
          </button>
        </div>
      </div>

      @if (isMobileMenuOpen()) {
        <nav id="mobile-navigation" class="mobile-navigation" aria-label="Mobile navigation">
          <div class="container mobile-nav-inner">
            <a routerLink="/events" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }" (click)="closeNavigation()">Explore Events</a>
            <a routerLink="/groups" routerLinkActive="active" (click)="closeNavigation()">Find Groups</a>
            @if (auth.isAuthenticated()) {
              <a routerLink="/dashboard" routerLinkActive="active" (click)="closeNavigation()">My Dashboard</a>
              <a routerLink="/groups/create" routerLinkActive="active" (click)="closeNavigation()">Start a Group</a>
            }
          </div>
        </nav>
      }
    </header>
  `,
  styles: [
    `
      .app-header {
        background: #ffffff;
        border-bottom: 1px solid var(--color-border);
        position: sticky;
        top: 0;
        z-index: 100;
        box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03);
      }

      .header-container {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 68px;
      }

      .header-left {
        display: flex;
        align-items: center;
        gap: 2rem;
      }

      .brand-logo {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        text-decoration: none;
      }

      .brand-badge {
        background: var(--color-primary);
        color: #ffffff;
        font-weight: 800;
        font-size: 0.95rem;
        padding: 4px 8px;
        border-radius: 8px;
        letter-spacing: -0.05em;
      }

      .brand-name {
        font-size: 1.35rem;
        font-weight: 800;
        color: var(--color-text-main);
        letter-spacing: -0.03em;
      }

      .nav-links {
        display: none;
        align-items: center;
        gap: 1.25rem;

        @media (min-width: 768px) {
          display: flex;
        }

        a {
          font-weight: 600;
          font-size: 0.95rem;
          color: var(--color-text-muted);
          text-decoration: none;
          padding: 6px 10px;
          border-radius: 6px;
          transition: all var(--transition-fast);

          &:hover {
            color: var(--color-primary);
            background: var(--color-bg-subtle);
          }

          &.active {
            color: var(--color-primary);
            font-weight: 700;
          }
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .mobile-menu-toggle {
        display: grid;
        width: 38px;
        height: 38px;
        place-items: center;
        border: 1px solid var(--color-border);
        border-radius: 9px;
        background: #fff;
        color: var(--color-text-main);
        font-size: 1.25rem;
        cursor: pointer;

        @media (min-width: 768px) {
          display: none;
        }
      }

      .mobile-navigation {
        border-top: 1px solid var(--color-border);
        background: #fff;

        @media (min-width: 768px) {
          display: none;
        }
      }

      .mobile-nav-inner {
        display: grid;
        gap: 0.25rem;
        padding-top: 0.75rem;
        padding-bottom: 0.75rem;

        a {
          padding: 0.7rem 0.8rem;
          border-radius: 8px;
          color: var(--color-text-muted);
          font-size: 0.92rem;
          font-weight: 650;
          text-decoration: none;

          &:hover, &.active {
            background: var(--color-primary-subtle);
            color: var(--color-primary-text);
          }
        }
      }

      .auth-buttons {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .create-btn {
        display: none;
        @media (min-width: 640px) {
          display: inline-flex;
        }
      }

      .user-menu-wrapper {
        position: relative;
      }

      .user-profile-btn {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        background: transparent;
        border: 1px solid var(--color-border);
        padding: 4px 10px 4px 4px;
        border-radius: 9999px;
        cursor: pointer;
        transition: all var(--transition-fast);

        &:hover {
          border-color: var(--color-border-hover);
          background: var(--color-bg-subtle);
        }
      }

      .avatar-img, .avatar-placeholder {
        width: 32px;
        height: 32px;
        border-radius: 9999px;
      }

      .avatar-img {
        object-fit: cover;
      }

      .avatar-placeholder {
        background: var(--color-primary-subtle);
        color: var(--color-primary-text);
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 0.85rem;
      }

      .user-name {
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--color-text-main);
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        @media (max-width: 480px) {
          display: none;
        }
      }

      @media (max-width: 480px) {
        .header-container { padding-inline: 1rem; }
        .brand-name { font-size: 1.15rem; }
        .auth-buttons { gap: 0.15rem; }
        .auth-buttons .btn { padding-inline: 0.65rem; }
      }

      .dropdown-chevron {
        font-size: 0.75rem;
        color: var(--color-text-subtle);
      }

      .dropdown-panel {
        position: absolute;
        top: calc(100% + 8px);
        right: 0;
        width: 220px;
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 12px;
        box-shadow: var(--shadow-lg);
        padding: 6px;
        z-index: 1000;
      }

      .dropdown-header {
        padding: 8px 12px;
      }

      .dropdown-email {
        font-size: 0.75rem;
        color: var(--color-text-subtle);
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .dropdown-divider {
        margin: 4px 0;
        border: none;
        border-top: 1px solid var(--color-border);
      }

      .dropdown-item {
        display: block;
        width: 100%;
        text-align: left;
        padding: 8px 12px;
        border-radius: 6px;
        font-size: 0.875rem;
        font-weight: 500;
        color: var(--color-text-main);
        text-decoration: none;
        background: transparent;
        border: none;
        cursor: pointer;

        &:hover {
          background: var(--color-bg-subtle);
          color: var(--color-primary);
        }

        &.dropdown-logout {
          color: var(--color-danger);
          &:hover {
            background: var(--color-danger-subtle);
          }
        }
      }
    `,
  ],
})
export class HeaderComponent {
  readonly auth = inject(AuthService);
  readonly isMenuOpen = signal(false);
  readonly isMobileMenuOpen = signal(false);

  toggleMenu() {
    this.isMobileMenuOpen.set(false);
    this.isMenuOpen.update(v => !v);
  }

  toggleMobileMenu() {
    this.isMenuOpen.set(false);
    this.isMobileMenuOpen.update(v => !v);
  }

  closeNavigation() {
    this.isMobileMenuOpen.set(false);
  }

  closeMenu() {
    this.isMenuOpen.set(false);
  }

  logout() {
    this.closeMenu();
    this.closeNavigation();
    this.auth.logout();
  }
}
