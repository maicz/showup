import { DatePipe } from '@angular/common';
import { Component, HostListener, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EventSummary } from '../../../core/models/event.model';
import { GroupDetail, GroupMemberSummary } from '../../../core/models/group.model';
import { AuthService } from '../../../core/services/auth.service';
import { GroupService } from '../../../core/services/group.service';
import { ToastService } from '../../../core/services/toast.service';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';

@Component({
  selector: 'app-group-detail',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe, FocusTrapDirective],
  template: `
    @if (loading()) {
      <div class="container loading-container">
        <p>Loading community group...</p>
      </div>
    } @else if (group()) {
      <div class="container group-detail-page">
        <!-- Group Hero Card -->
        <div class="card group-hero">
          <div class="hero-top">
            <div>
              <span class="badge badge-neutral">{{ group()!.category.name }}</span>
              <h1 class="group-name">{{ group()!.name }}</h1>
              <p class="group-location">
                📍 {{ group()!.city || 'Global' }}
                @if (group()!.country) {
                  , {{ group()!.country }}
                }
                • 👥 {{ group()!.memberCount }} members
                @if (group()!.ratingAverage) {
                  • <span class="rating-highlight">★ {{ group()!.ratingAverage }} ({{ group()!.ratingCount }} reviews)</span>
                }
              </p>
            </div>

            <div class="hero-actions">
              @if (group()!.viewerStatus === 'ACTIVE') {
                <span class="badge badge-success role-badge">{{ group()!.viewerRole }}</span>
                @if (isGroupAdmin()) {
                  <a [routerLink]="['/groups', group()!.id, 'manage']" class="btn btn-sm btn-secondary">
                    ⚙️ Organizer Settings
                  </a>
                  <a [routerLink]="['/events/create']" class="btn btn-sm btn-primary">
                    + Host Event
                  </a>
                }
                <button
                  class="btn btn-sm btn-outline"
                  [disabled]="leavingGroup()"
                  (click)="leaveGroup()"
                >
                  {{ leavingGroup() ? 'Leaving...' : 'Leave Group' }}
                </button>
              } @else if (group()!.viewerStatus === 'PENDING_APPROVAL') {
                <span class="badge badge-warning">Membership Pending Approval</span>
              } @else {
                <button class="btn btn-primary btn-lg" (click)="showJoinModal.set(true)">
                  Join this Group
                </button>
              }
            </div>
          </div>

          <!-- Topics / Tags -->
          <div class="topics-tags-row">
            @for (t of group()!.topics; track t.id) {
              <span class="chip">{{ t.name }}</span>
            }
          </div>
        </div>

        <!-- Group Tabs -->
        <div class="tabs-nav">
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'about'"
            (click)="activeTab.set('about')"
          >
            About & Description
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'events'"
            (click)="activeTab.set('events')"
          >
            Upcoming Events ({{ events().length }})
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'members'"
            (click)="activeTab.set('members')"
          >
            Members ({{ group()?.memberCount ?? 0 }})
          </button>
        </div>

        <!-- Tab 1: About -->
        @if (activeTab() === 'about') {
          <div class="tab-pane">
            <div class="card content-card">
              <h3>What we're about</h3>
              <p class="description-text">{{ group()!.description }}</p>
            </div>

            @if (group()!.organizer; as organizer) {
              <div class="card organizer-card">
                <h3>Primary Organizer</h3>
                <div class="organizer-info">
                  <div class="organizer-avatar">{{ organizer.displayName.charAt(0) }}</div>
                  <div>
                    <strong>{{ organizer.displayName }}</strong>
                    <p class="sub">Group Founder & Organizer</p>
                  </div>
                </div>
              </div>
            }
          </div>
        }

        <!-- Tab 2: Events -->
        @if (activeTab() === 'events') {
          <div class="tab-pane">
            @if (events().length === 0) {
              <div class="empty-state">
                <span class="empty-icon">📅</span>
                <h3>No upcoming events scheduled</h3>
                <p>Check back later or propose an event to the organizers.</p>
                @if (isGroupAdmin()) {
                  <a routerLink="/events/create" class="btn btn-primary">+ Schedule an Event</a>
                }
              </div>
            } @else {
              <div class="events-grid">
                @for (ev of events(); track ev.id) {
                  <div class="card event-card card-interactive" [routerLink]="['/events', ev.id]">
                    <div class="event-card-header">
                      <span class="badge badge-info">{{ ev.format }}</span>
                      <span class="badge badge-success">{{ ev.availability }}</span>
                    </div>
                    <div class="event-time">{{ ev.startsAt | date: 'EEEE, MMM d • h:mm a' }}</div>
                    <h3 class="event-title">{{ ev.title }}</h3>
                    <p class="event-venue">{{ ev.venueCity || 'Online' }} • 👥 {{ ev.yesRsvpCount }} attendees</p>
                  </div>
                }
              </div>
            }
          </div>
        }

        <!-- Tab 3: Members -->
        @if (activeTab() === 'members') {
          <div class="tab-pane">
            @if (group()?.viewerStatus === 'ACTIVE') {
              <div class="members-grid">
                @for (m of members(); track m.memberId) {
                  <div class="card member-card">
                    <div class="member-avatar">{{ m.displayName.charAt(0) }}</div>
                    <div class="member-info">
                      <strong>{{ m.displayName }}</strong>
                      <span class="badge badge-sm badge-neutral">{{ m.role }}</span>
                    </div>
                  </div>
                }
              </div>
            } @else {
              <div class="card members-locked">
                <p>🔒 <strong>Member roster is private.</strong></p>
                <p class="text-muted">Join this group to connect with {{ group()?.memberCount ?? 0 }} fellow members.</p>
              </div>
            }
          </div>
        }

        <!-- Join Group Modal -->
        @if (showJoinModal()) {
          <div class="modal-backdrop" (click)="showJoinModal.set(false)">
            <div appFocusTrap tabindex="-1" class="modal-content" role="dialog" aria-modal="true" aria-labelledby="join-group-modal-title" (click)="$event.stopPropagation()">
              <h2 id="join-group-modal-title">Join {{ group()!.name }}</h2>
              <p>Introduce yourself to the organizers and fellow members.</p>

              <div class="form-group">
                <label class="form-label">Introduction / Note (optional)</label>
                <textarea
                  class="form-control"
                  [(ngModel)]="joinIntro"
                  placeholder="Tell the group why you want to join..."
                  rows="3"
                ></textarea>
              </div>

              <div class="modal-actions">
                <button class="btn btn-secondary" (click)="showJoinModal.set(false)">Cancel</button>
                <button class="btn btn-primary" (click)="joinGroup()">Join Group</button>
              </div>
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [
    `
      .group-detail-page {
        padding: 2.5rem 1.5rem;
      }
      .group-hero {
        padding: 2rem;
        margin-bottom: 2rem;
      }
      .hero-top {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        flex-wrap: wrap;
        gap: 1.5rem;
      }
      .group-name {
        font-size: 2.25rem;
        margin: 0.5rem 0 0.25rem 0;
      }
      .group-location {
        color: var(--color-text-muted);
        font-size: 1rem;
        margin: 0;
      }
      .rating-highlight {
        color: var(--color-warning-text);
        font-weight: 700;
      }
      .hero-actions {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-wrap: wrap;
      }
      .role-badge {
        font-size: 0.85rem;
        padding: 6px 12px;
      }
      .topics-tags-row {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        margin-top: 1.5rem;
        padding-top: 1.25rem;
        border-top: 1px solid var(--color-border);
      }
      .tab-pane {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }
      .content-card, .organizer-card {
        padding: 1.75rem;
        h3 { margin-bottom: 1rem; }
      }
      .description-text {
        white-space: pre-wrap;
        line-height: 1.7;
      }
      .organizer-info {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .organizer-avatar, .member-avatar {
        width: 44px;
        height: 44px;
        border-radius: 9999px;
        background: var(--color-primary-subtle);
        color: var(--color-primary);
        font-size: 1.25rem;
        font-weight: 800;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .events-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 1.5rem;
      }
      .event-card {
        padding: 1.5rem;
      }
      .event-card-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 0.75rem;
      }
      .event-time {
        font-size: 0.85rem;
        font-weight: 700;
        color: var(--color-primary);
      }
      .event-title {
        font-size: 1.2rem;
        margin: 0.35rem 0 0.5rem 0;
      }
      .event-venue {
        font-size: 0.875rem;
        color: var(--color-text-muted);
        margin: 0;
      }
      .members-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 1rem;
      }
      .member-card {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1rem;
      }
      .member-info {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .members-locked {
        padding: 2.5rem;
        text-align: center;
        background: #f8fafc;
        border: 1px dashed #cbd5e1;
        p { margin: 0.25rem 0; }
      }
      .modal-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
        margin-top: 1.5rem;
      }
    `,
  ],
})
export class GroupDetailComponent implements OnInit {
  private readonly groupService = inject(GroupService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  readonly group = signal<GroupDetail | null>(null);
  readonly events = signal<EventSummary[]>([]);
  readonly members = signal<GroupMemberSummary[]>([]);
  readonly loading = signal(true);
  readonly activeTab = signal<'about' | 'events' | 'members'>('about');
  readonly showJoinModal = signal(false);
  readonly leavingGroup = signal(false);

  joinIntro = '';

  @HostListener('document:keydown.escape')
  closeJoinModalOnEscape() {
    if (this.showJoinModal()) {
      this.showJoinModal.set(false);
    }
  }

  isGroupAdmin(): boolean {
    const role = this.group()?.viewerRole;
    return role === 'ORGANIZER' || role === 'CO_ORGANIZER';
  }

  ngOnInit() {
    this.loadGroup();
  }

  loadGroup() {
    this.loading.set(true);
    this.groupService.getGroup(this.id()).subscribe({
      next: res => {
        this.group.set(res);
        this.loading.set(false);
        this.loadEvents();
        if (res.viewerStatus === 'ACTIVE') {
          this.loadMembers();
        } else {
          this.members.set([]);
        }
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Failed to load group');
      },
    });
  }

  loadEvents() {
    this.groupService.getGroupEvents(this.id()).subscribe({
      next: evts => this.events.set(evts),
    });
  }

  loadMembers() {
    this.groupService.getMembers(this.id()).subscribe({
      next: m => this.members.set(m),
      error: () => this.members.set([]),
    });
  }

  joinGroup() {
    if (!this.auth.isAuthenticated()) {
      this.toast.info('Please sign in to join groups.');
      this.router.navigate(['/login']);
      return;
    }

    this.groupService
      .joinGroup(this.id(), { introduction: this.joinIntro.trim() || undefined })
      .subscribe({
        next: membership => {
          this.showJoinModal.set(false);
          if (membership.status === 'PENDING_APPROVAL') {
            this.toast.info('Join request sent. An organizer needs to approve you first.');
          } else {
            this.toast.success('Joined group!');
          }
          this.loadGroup();
        },
        error: err => this.toast.error(err?.error?.message || 'Failed to join group'),
      });
  }

  leaveGroup() {
    if (!confirm('Are you sure you want to leave this group?')) {
      return;
    }
    this.leavingGroup.set(true);
    this.groupService.leaveGroup(this.id()).subscribe({
      next: () => {
        this.leavingGroup.set(false);
        this.toast.info('You have left the group.');
        this.members.set([]);
        this.loadGroup();
      },
      error: err => {
        this.leavingGroup.set(false);
        this.toast.error(err?.error?.message || 'Could not leave group');
      },
    });
  }
}
