import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="app-footer">
      <div class="container footer-container">
        <div class="footer-brand-col">
          <div class="footer-brand">
            <span class="brand-badge">SU</span>
            <span class="brand-name">ShowUp</span>
          </div>
          <p class="footer-tagline">
            The community and event-planning platform built for organizers, staff, and attendees.
          </p>
        </div>

        <div class="footer-links-col">
          <h4>Explore</h4>
          <a routerLink="/events">All Events</a>
          <a routerLink="/groups">Discover Groups</a>
          <a routerLink="/groups/create">Start a Group</a>
        </div>

        <div class="footer-links-col">
          <h4>Features</h4>
          <span>Real-time QR Check-in</span>
          <span>Waitlisting & Capacity</span>
          <span>Staff Assignments</span>
          <span>Organizer Analytics</span>
        </div>

        <div class="footer-links-col">
          <h4>Stack</h4>
          <span>Java 25 & Spring Boot</span>
          <span>Angular 22 & Signals</span>
          <span>PostgreSQL 18 & PostGIS</span>
          <span>Spring AI Copilot</span>
        </div>
      </div>
      <div class="footer-bottom">
        <div class="container bottom-content">
          <p>&copy; 2026 ShowUp. Built with modern engineering & design best practices.</p>
        </div>
      </div>
    </footer>
  `,
  styles: [
    `
      .app-footer {
        background: #ffffff;
        border-top: 1px solid var(--color-border);
        margin-top: 4rem;
        padding-top: 3rem;
      }

      .footer-container {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 2.5rem;
        padding-bottom: 3rem;
      }

      .footer-brand {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        margin-bottom: 0.75rem;
      }

      .brand-badge {
        background: var(--color-primary);
        color: #ffffff;
        font-weight: 800;
        font-size: 0.85rem;
        padding: 3px 6px;
        border-radius: 6px;
      }

      .brand-name {
        font-size: 1.2rem;
        font-weight: 800;
        color: var(--color-text-main);
      }

      .footer-tagline {
        font-size: 0.875rem;
        color: var(--color-text-subtle);
        line-height: 1.5;
      }

      .footer-links-col {
        display: flex;
        flex-direction: column;
        gap: 0.6rem;

        h4 {
          font-size: 0.95rem;
          font-weight: 700;
          color: var(--color-text-main);
          margin-bottom: 0.5rem;
        }

        a, span {
          font-size: 0.875rem;
          color: var(--color-text-muted);
          text-decoration: none;
          transition: color var(--transition-fast);
        }

        a:hover {
          color: var(--color-primary);
        }
      }

      .footer-bottom {
        border-top: 1px solid var(--color-border);
        padding: 1.25rem 0;
        background: var(--color-bg-subtle);

        .bottom-content {
          display: flex;
          align-items: center;
          justify-content: space-between;

          p {
            margin: 0;
            font-size: 0.8rem;
            color: var(--color-text-subtle);
          }
        }
      }
    `,
  ],
})
export class FooterComponent {}
