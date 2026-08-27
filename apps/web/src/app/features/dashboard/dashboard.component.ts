import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AiRecommendationsResponse } from '../../core/models/ai.model';
import { EventSummary, MemberEventSummary } from '../../core/models/event.model';
import { GroupSummary } from '../../core/models/group.model';
import { TicketResponse } from '../../core/models/attendance.model';
import { AiService } from '../../core/services/ai.service';
import { AttendanceService } from '../../core/services/attendance.service';
import { AuthService } from '../../core/services/auth.service';
import { MemberService } from '../../core/services/member.service';
import { ToastService } from '../../core/services/toast.service';
import { QrCodeComponent } from '../../shared/components/qr-code/qr-code.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, QrCodeComponent],
  template: `
    <div class="container dashboard-page">
      <div class="page-header">
        <div>
          <h1>My Dashboard</h1>
          <p class="header-sub">Welcome back, {{ auth.currentUser()?.displayName || 'Member' }}</p>
        </div>
        <div class="header-actions">
          <a routerLink="/events" class="btn btn-outline">Explore Events</a>
          <a routerLink="/groups/create" class="btn btn-primary">+ Create Group</a>
        </div>
      </div>

      <div class="tabs-nav">
        <button
          class="tab-btn"
          [class.active]="activeTab() === 'events'"
          (click)="activeTab.set('events')"
        >
          My Events ({{ myEvents().length }})
        </button>
        <button
          class="tab-btn"
          [class.active]="activeTab() === 'groups'"
          (click)="activeTab.set('groups')"
        >
          My Groups ({{ myGroups().length }})
        </button>
        <button
          class="tab-btn"
          [class.active]="activeTab() === 'recommendations'"
          (click)="activeTab.set('recommendations')"
        >
          ✨ Recommended for You
        </button>
      </div>

      <!-- Events Tab -->
      @if (activeTab() === 'events') {
        @if (loadingEvents()) {
          <div class="loading-spinner">Loading your events...</div>
        } @else if (myEvents().length === 0) {
          <div class="empty-state">
            <span class="empty-icon">📅</span>
            <h3>No upcoming RSVPs yet</h3>
            <p>Discover interesting meetups happening in your city or join online sessions.</p>
            <a routerLink="/events" class="btn btn-primary">Find Events</a>
          </div>
        } @else {
          <div class="events-grid">
            @for (event of myEvents(); track event.id) {
              <div class="card event-card">
                <div class="event-header-row">
                  <span class="badge badge-info">{{ event.format }}</span>
                  <span class="badge" [class.badge-success]="event.rsvpStatus === 'YES'" [class.badge-warning]="event.rsvpStatus === 'WAITLISTED'">
                    {{ event.rsvpStatus === 'YES' ? 'Going' : 'Waitlist #' + event.waitlistPosition }}
                  </span>
                </div>

                <h3 class="event-title">
                  <a [routerLink]="['/events', event.id]">{{ event.title }}</a>
                </h3>

                <div class="event-meta">
                  <div class="meta-item">
                    <span class="meta-icon">🕒</span>
                    <span>{{ event.startsAt | date: 'EEEE, MMM d, y • h:mm a' }}</span>
                  </div>
                  @if (event.venueCity) {
                    <div class="meta-item">
                      <span class="meta-icon">📍</span>
                      <span>{{ event.venueCity }}</span>
                    </div>
                  }
                  <div class="meta-item">
                    <span class="meta-icon">👥</span>
                    <span>Hosted by <a [routerLink]="['/groups', event.group.id]">{{ event.group.name }}</a></span>
                  </div>
                </div>

                <div class="card-footer-actions">
                  @if (event.rsvpStatus === 'YES') {
                    <button class="btn btn-sm btn-secondary" (click)="viewTicket(event.id, event.title)">
                      🎟️ View Ticket & QR
                    </button>
                  } @else {
                    <span class="waitlist-note">We’ll notify you if a place opens.</span>
                  }
                  <a [routerLink]="['/events', event.id]" class="btn btn-sm btn-outline">Event Details</a>
                </div>
              </div>
            }
          </div>
        }
      }

      <!-- Groups Tab -->
      @if (activeTab() === 'groups') {
        @if (loadingGroups()) {
          <div class="loading-spinner">Loading your groups...</div>
        } @else if (myGroups().length === 0) {
          <div class="empty-state">
            <span class="empty-icon">👥</span>
            <h3>You haven't joined any groups yet</h3>
            <p>Connect with communities around your interests or start your own group.</p>
            <div class="empty-actions">
              <a routerLink="/groups" class="btn btn-primary">Discover Groups</a>
              <a routerLink="/groups/create" class="btn btn-secondary">Start a Group</a>
            </div>
          </div>
        } @else {
          <div class="groups-grid">
            @for (group of myGroups(); track group.id) {
              <div class="card group-card">
                <div class="group-card-top">
                  <span class="badge badge-neutral">{{ group.category.name }}</span>
                  @if (group.ratingAverage) {
                    <span class="rating-badge">★ {{ group.ratingAverage }} ({{ group.ratingCount }})</span>
                  }
                </div>

                <h3 class="group-title">
                  <a [routerLink]="['/groups', group.id]">{{ group.name }}</a>
                </h3>

                <p class="group-location">{{ group.city || 'Global Community' }} • {{ group.memberCount }} members</p>

                <div class="group-actions">
                  <a [routerLink]="['/groups', group.id]" class="btn btn-sm btn-primary">View Group</a>
                </div>
              </div>
            }
          </div>
        }
      }

      <!-- AI Recommendations Tab -->
      @if (activeTab() === 'recommendations') {
        @if (loadingAi()) {
          <div class="loading-spinner">✨ Curating recommendations with AI...</div>
        } @else {
          <div class="ai-banner">
            <div class="ai-icon">✨</div>
            <div>
              <h4>Smart AI Recommendations</h4>
              <p>{{ aiRationale() }}</p>
            </div>
          </div>

          <div class="events-grid">
            @for (event of recommendations(); track event.id) {
              <div class="card event-card">
                <div class="event-header-row">
                  <span class="badge badge-info">{{ event.format }}</span>
                  <span class="badge badge-success">{{ event.availability }}</span>
                </div>

                <h3 class="event-title">
                  <a [routerLink]="['/events', event.id]">{{ event.title }}</a>
                </h3>

                <div class="event-meta">
                  <div class="meta-item">
                    <span class="meta-icon">🕒</span>
                    <span>{{ event.startsAt | date: 'MMM d, y • h:mm a' }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-icon">👥</span>
                    <span>{{ event.group.name }}</span>
                  </div>
                </div>

                <div class="card-footer-actions">
                  <a [routerLink]="['/events', event.id]" class="btn btn-sm btn-primary">RSVP & Details</a>
                </div>
              </div>
            }
          </div>
        }
      }

      <!-- Ticket Modal -->
      @if (selectedTicket()) {
        <div class="modal-backdrop" (click)="selectedTicket.set(null)">
          <div class="modal-content ticket-modal" (click)="$event.stopPropagation()">
            <div class="ticket-header">
              <h2>Event Admission Ticket</h2>
              <p class="ticket-event-title">{{ activeTicketEventTitle() }}</p>
            </div>

            <div class="ticket-body">
              <div class="qr-box">
                <app-qr-code [value]="selectedTicket()!.code" [size]="220"></app-qr-code>
                <div class="ticket-code-text">{{ selectedTicket()!.code }}</div>
              </div>

              <div class="ticket-details">
                <div class="detail-row">
                  <span>Admit Count:</span>
                  <strong>{{ selectedTicket()!.admitCount }} Person(s)</strong>
                </div>
                <div class="detail-row">
                  <span>Status:</span>
                  @if (selectedTicket()!.checkedIn) {
                    <span class="badge badge-success">Checked In</span>
                  } @else {
                    <span class="badge badge-info">Valid for Admission</span>
                  }
                </div>
                <div class="detail-row">
                  <span>Issued At:</span>
                  <span>{{ selectedTicket()!.issuedAt | date: 'short' }}</span>
                </div>
              </div>
            </div>

            <div class="ticket-footer">
              <button class="btn btn-secondary" (click)="selectedTicket.set(null)">Close</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .dashboard-page {
        padding: 2.5rem 1.5rem;
      }
      .header-sub {
        color: var(--color-text-muted);
        font-size: 1.05rem;
      }
      .events-grid, .groups-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 1.5rem;
      }
      .event-card, .group-card {
        display: flex;
        flex-direction: column;
        justify-content: space-between;
      }
      .event-header-row, .group-card-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }
      .event-title, .group-title {
        font-size: 1.25rem;
        margin-bottom: 0.75rem;
      }
      .event-meta {
        display: flex;
        flex-direction: column;
        gap: 0.4rem;
        font-size: 0.875rem;
        color: var(--color-text-muted);
        margin-bottom: 1.25rem;
      }
      .meta-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }
      .card-footer-actions, .group-actions {
        display: flex;
        gap: 0.75rem;
        padding-top: 1rem;
        border-top: 1px solid var(--color-border);
      }
      .waitlist-note {
        align-self: center;
        color: var(--color-warning-text);
        font-size: 0.78rem;
        font-weight: 650;
      }
      .rating-badge {
        font-weight: 700;
        color: var(--color-warning-text);
        background: var(--color-warning-subtle);
        padding: 2px 8px;
        border-radius: 9999px;
        font-size: 0.8rem;
      }
      .group-location {
        font-size: 0.875rem;
        color: var(--color-text-subtle);
        margin-bottom: 1.25rem;
      }
      .loading-spinner {
        text-align: center;
        padding: 3rem;
        color: var(--color-text-subtle);
      }
      .empty-actions {
        display: flex;
        justify-content: center;
        gap: 1rem;
      }
      .ai-banner {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1.25rem 1.5rem;
        background: linear-gradient(135deg, #f0fdf4 0%, #e0f2fe 100%);
        border: 1px solid #bae6fd;
        border-radius: 16px;
        margin-bottom: 2rem;
        .ai-icon {
          font-size: 2rem;
        }
        h4 { margin: 0 0 0.25rem 0; color: #0369a1; }
        p { margin: 0; font-size: 0.9rem; color: #334155; }
      }
      .ticket-modal {
        text-align: center;
        max-width: 420px;
      }
      .ticket-header {
        margin-bottom: 1.5rem;
        h2 { margin-bottom: 0.25rem; }
        .ticket-event-title { color: var(--color-primary); font-weight: 600; }
      }
      .qr-box {
        margin: 1.5rem 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
      }
      .ticket-code-text {
        font-family: var(--font-mono);
        font-size: 0.8rem;
        color: var(--color-text-subtle);
        background: var(--color-bg-subtle);
        padding: 4px 8px;
        border-radius: 4px;
      }
      .ticket-details {
        background: var(--color-bg-subtle);
        border-radius: 10px;
        padding: 1rem;
        text-align: left;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        font-size: 0.875rem;
        margin-bottom: 1.5rem;
        .detail-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
      }
      .ticket-footer {
        display: flex;
        justify-content: center;
      }
    `,
  ],
})
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly memberService = inject(MemberService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly aiService = inject(AiService);
  private readonly toast = inject(ToastService);

  readonly activeTab = signal<'events' | 'groups' | 'recommendations'>('events');
  readonly myEvents = signal<MemberEventSummary[]>([]);
  readonly myGroups = signal<GroupSummary[]>([]);
  readonly recommendations = signal<EventSummary[]>([]);
  readonly aiRationale = signal<string>('');

  readonly loadingEvents = signal(true);
  readonly loadingGroups = signal(true);
  readonly loadingAi = signal(true);

  readonly selectedTicket = signal<TicketResponse | null>(null);
  readonly activeTicketEventTitle = signal<string>('');

  ngOnInit() {
    this.loadEvents();
    this.loadGroups();
    this.loadRecommendations();
  }

  loadEvents() {
    this.loadingEvents.set(true);
    this.memberService.getMyEvents().subscribe({
      next: events => {
        this.myEvents.set(events);
        this.loadingEvents.set(false);
      },
      error: () => this.loadingEvents.set(false),
    });
  }

  loadGroups() {
    this.loadingGroups.set(true);
    this.memberService.getMyGroups().subscribe({
      next: groups => {
        this.myGroups.set(groups);
        this.loadingGroups.set(false);
      },
      error: () => this.loadingGroups.set(false),
    });
  }

  loadRecommendations() {
    this.loadingAi.set(true);
    this.aiService.getRecommendations().subscribe({
      next: res => {
        this.recommendations.set(res.recommendedEvents);
        this.aiRationale.set(res.rationale);
        this.loadingAi.set(false);
      },
      error: () => this.loadingAi.set(false),
    });
  }

  viewTicket(eventId: string, eventTitle: string) {
    this.activeTicketEventTitle.set(eventTitle);
    this.attendanceService.getTicket(eventId).subscribe({
      next: ticket => this.selectedTicket.set(ticket),
      error: () => {
        // If not yet issued, issue ticket
        this.attendanceService.issueTicket(eventId).subscribe({
          next: ticket => this.selectedTicket.set(ticket),
          error: err => this.toast.error(err?.error?.message || 'Ticket not available for this event'),
        });
      },
    });
  }
}
