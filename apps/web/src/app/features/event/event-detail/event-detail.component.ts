import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AiFeedbackSummaryResponse } from '../../../core/models/ai.model';
import { StaffAssignmentSummary, TicketResponse } from '../../../core/models/attendance.model';
import { CommentSummary } from '../../../core/models/comment.model';
import { EventDetail } from '../../../core/models/event.model';
import { EventFeedbackSummary } from '../../../core/models/feedback.model';
import { PhotoSummary } from '../../../core/models/photo.model';
import { AiService } from '../../../core/services/ai.service';
import { AttendanceService } from '../../../core/services/attendance.service';
import { AuthService } from '../../../core/services/auth.service';
import { CommentService } from '../../../core/services/comment.service';
import { EventService } from '../../../core/services/event.service';
import { FeedbackService } from '../../../core/services/feedback.service';
import { PhotoService } from '../../../core/services/photo.service';
import { RsvpService } from '../../../core/services/rsvp.service';
import { ToastService } from '../../../core/services/toast.service';
import { QrCodeComponent } from '../../../shared/components/qr-code/qr-code.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe, QrCodeComponent],
  template: `
    @if (loading()) {
      <div class="container loading-container">
        <p>Loading event details...</p>
      </div>
    } @else if (event()) {
      <div class="container event-detail-page">
        <!-- Organizer Action Bar -->
        @if (isOrganizer()) {
          <div class="organizer-toolbar card">
            <div class="toolbar-left">
              <span class="badge badge-warning">Organizer Tools</span>
              <span class="event-status-text">Status: <strong>{{ event()!.status }}</strong></span>
            </div>
            <div class="toolbar-actions">
              @if (event()!.status === 'DRAFT') {
                <button class="btn btn-sm btn-primary" (click)="publishEvent()">🚀 Publish Event</button>
              }
              <a [routerLink]="['/events', event()!.id, 'check-in']" class="btn btn-sm btn-secondary">
                📷 Door Scanner & Check-in
              </a>
              <a [routerLink]="['/events', event()!.id, 'staff']" class="btn btn-sm btn-secondary">
                👥 Staff Roles
              </a>
              <a [routerLink]="['/events', event()!.id, 'report']" class="btn btn-sm btn-secondary">
                📊 Attendance Report
              </a>
              @if (event()!.status !== 'CANCELLED') {
                <button class="btn btn-sm btn-danger" (click)="showCancelModal.set(true)">Cancel Event</button>
              }
            </div>
          </div>
        }

        <!-- Cancellation Notice -->
        @if (event()!.status === 'CANCELLED') {
          <div class="cancellation-banner card">
            <div class="banner-icon">⚠️</div>
            <div>
              <h3>This event has been cancelled</h3>
              @if (event()!.cancellationReason) {
                <p>Reason: {{ event()!.cancellationReason }}</p>
              }
            </div>
          </div>
        }

        <!-- Main Hero & RSVP Sidebar Grid -->
        <div class="event-layout-grid">
          <!-- Left Main Column -->
          <div class="main-content">
            <div class="event-hero">
              <div class="hero-badges">
                <span class="badge badge-info">{{ event()!.format }}</span>
                <span
                  class="badge"
                  [class.badge-success]="event()!.availability === 'SEATS_AVAILABLE'"
                  [class.badge-warning]="event()!.availability === 'WAITLIST'"
                  [class.badge-danger]="event()!.availability === 'FULL'"
                >
                  {{ event()!.availability }}
                </span>
                <a [routerLink]="['/groups', event()!.group.id]" class="group-link">
                  {{ event()!.group.name }}
                </a>
              </div>

              <h1 class="hero-title">{{ event()!.title }}</h1>

              <div class="host-info-row">
                <div class="hosts-avatars">
                  @for (host of event()!.hosts; track host.id) {
                    <div class="host-item">
                      <div class="host-avatar">
                        {{ host.displayName.charAt(0) }}
                      </div>
                      <span class="host-name">Hosted by <strong>{{ host.displayName }}</strong></span>
                    </div>
                  }
                </div>
              </div>
            </div>

            <!-- Tab Navigation -->
            <div class="tabs-nav">
              <button
                class="tab-btn"
                [class.active]="activeTab() === 'details'"
                (click)="activeTab.set('details')"
              >
                Details
              </button>
              <button
                class="tab-btn"
                [class.active]="activeTab() === 'comments'"
                (click)="activeTab.set('comments')"
              >
                Discussions ({{ comments().length }})
              </button>
              <button
                class="tab-btn"
                [class.active]="activeTab() === 'photos'"
                (click)="activeTab.set('photos')"
              >
                Photos ({{ photos().length }})
              </button>
              <button
                class="tab-btn"
                [class.active]="activeTab() === 'feedback'"
                (click)="activeTab.set('feedback')"
              >
                Reviews & Ratings ({{ feedbacks().length }})
              </button>
            </div>

            <!-- Tab 1: Details -->
            @if (activeTab() === 'details') {
              <div class="tab-pane">
                <div class="card description-card">
                  <h3>About the event</h3>
                  <div class="description-text">
                    {{ event()!.description || 'No detailed description provided.' }}
                  </div>
                </div>

                @if (event()!.venue) {
                  <div class="card venue-card">
                    <h3>Location</h3>
                    <h4>{{ event()!.venue?.name }}</h4>
                    <p class="venue-address">
                      {{ event()!.venue?.address?.addressLine1 }}
                      @if (event()!.venue?.address?.city) {
                        , {{ event()!.venue?.address?.city }}
                      }
                    </p>
                    @if (event()!.venue?.notes) {
                      <p class="venue-notes">📌 {{ event()!.venue?.notes }}</p>
                    }
                  </div>
                }

                @if (event()!.onlineUrl) {
                  <div class="card online-card">
                    <h3>Online Session Link</h3>
                    <p>Join link: <a [href]="event()!.onlineUrl" target="_blank">{{ event()!.onlineUrl }}</a></p>
                  </div>
                } @else if (event()!.format === 'ONLINE' || event()!.format === 'HYBRID') {
                  <div class="card online-card online-locked">
                    <h3>Online Session</h3>
                    <p class="lock-banner">🔒 Meeting link visible once RSVP is confirmed</p>
                  </div>
                }
              </div>
            }

            <!-- Tab 2: Comments / Discussions -->
            @if (activeTab() === 'comments') {
              <div class="tab-pane">
                <div class="card add-comment-card">
                  <h4>Join the Discussion</h4>
                  <div class="form-group">
                    <textarea
                      class="form-control"
                      [(ngModel)]="newCommentText"
                      placeholder="Ask a question or share thoughts..."
                      rows="3"
                    ></textarea>
                  </div>
                  <button
                    class="btn btn-primary btn-sm"
                    [disabled]="!newCommentText.trim() || submittingComment()"
                    (click)="submitComment()"
                  >
                    Post Comment
                  </button>
                </div>

                <div class="comments-list">
                  @for (comment of comments(); track comment.id) {
                    <div class="card comment-card">
                      <div class="comment-header">
                        <div class="comment-author">
                          <div class="author-avatar">{{ comment.author.displayName.charAt(0) }}</div>
                          <div>
                            <strong>{{ comment.author.displayName }}</strong>
                            <span class="comment-date">{{ comment.createdAt | date: 'short' }}</span>
                          </div>
                        </div>
                        @if (canDeleteComment(comment)) {
                          <button class="btn btn-sm btn-subtle delete-btn" (click)="deleteComment(comment.id)">
                            🗑️ Delete
                          </button>
                        }
                      </div>
                      <p class="comment-body" [class.deleted-body]="comment.body === '[comment deleted]'">
                        {{ comment.body }}
                      </p>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- Tab 3: Photos -->
            @if (activeTab() === 'photos') {
              <div class="tab-pane">
                <div class="photos-header">
                  <h3>Event Media & Photos</h3>
                  <button class="btn btn-sm btn-primary" (click)="showUploadPhotoModal.set(true)">
                    + Upload Photo
                  </button>
                </div>

                @if (photos().length === 0) {
                  <div class="empty-state">
                    <span class="empty-icon">📷</span>
                    <h3>No photos yet</h3>
                    <p>Be the first to share a photo from this event!</p>
                  </div>
                } @else {
                  <div class="photos-grid">
                    @for (photo of photos(); track photo.id) {
                      <div class="photo-item card">
                        <img [src]="photo.url" [alt]="photo.caption || 'Event photo'" />
                        @if (photo.caption) {
                          <p class="photo-caption">{{ photo.caption }}</p>
                        }
                        <div class="photo-meta">
                          <span>Uploaded by {{ photo.uploadedBy.displayName }}</span>
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            }

            <!-- Tab 4: Reviews & Ratings -->
            @if (activeTab() === 'feedback') {
              <div class="tab-pane">
                <div class="feedback-top-bar">
                  <div>
                    <h3>Attendee Reviews</h3>
                    <p>Post-event ratings and feedback from verified attendees.</p>
                  </div>
                  <div class="feedback-actions">
                    <button class="btn btn-sm btn-secondary" (click)="loadAiFeedbackSummary()">
                      ✨ AI Feedback Synthesis
                    </button>
                    <button class="btn btn-sm btn-primary" (click)="showFeedbackModal.set(true)">
                      ★ Leave Review
                    </button>
                  </div>
                </div>

                @if (aiFeedback()) {
                  <div class="card ai-synthesis-card">
                    <div class="synthesis-header">
                      <span class="ai-badge">✨ AI Synthesis</span>
                      <span class="sentiment-badge">{{ aiFeedback()!.overallSentiment }}</span>
                    </div>
                    <p class="narrative">{{ aiFeedback()!.narrativeSummary }}</p>
                    
                    <div class="themes-row">
                      <strong>Top Themes:</strong>
                      @for (theme of aiFeedback()!.topThemes; track theme) {
                        <span class="chip">{{ theme }}</span>
                      }
                    </div>
                  </div>
                }

                <div class="feedback-list">
                  @for (fb of feedbacks(); track fb.id) {
                    <div class="card feedback-card">
                      <div class="feedback-header">
                        <div class="author-avatar">{{ fb.member.displayName.charAt(0) }}</div>
                        <div>
                          <strong>{{ fb.member.displayName }}</strong>
                          <div class="star-rating">
                            @for (star of [1,2,3,4,5]; track star) {
                              <span [class.filled]="star <= fb.rating">★</span>
                            }
                          </div>
                        </div>
                        <span class="feedback-date">{{ fb.submittedAt | date: 'mediumDate' }}</span>
                      </div>
                      @if (fb.comment) {
                        <p class="feedback-comment">{{ fb.comment }}</p>
                      }
                    </div>
                  }
                </div>
              </div>
            }
          </div>

          <!-- Right RSVP / Sidebar Column -->
          <div class="sidebar-col">
            <div class="card rsvp-card">
              <div class="event-timing">
                <div class="timing-icon">📅</div>
                <div>
                  <div class="timing-date">{{ event()!.startsAt | date: 'EEEE, MMMM d, y' }}</div>
                  <div class="timing-hours">
                    {{ event()!.startsAt | date: 'shortTime' }}
                    @if (event()!.endsAt) {
                      – {{ event()!.endsAt | date: 'shortTime' }}
                    }
                    ({{ event()!.timeZone }})
                  </div>
                </div>
              </div>

              <div class="rsvp-divider"></div>

              <div class="capacity-stats">
                <div class="stat-box">
                  <span class="stat-num">{{ event()!.yesRsvpCount }}</span>
                  <span class="stat-label">Attending</span>
                </div>
                <div class="stat-box">
                  <span class="stat-num">{{ event()!.capacity || '∞' }}</span>
                  <span class="stat-label">Capacity</span>
                </div>
                @if (event()!.waitlistEnabled) {
                  <div class="stat-box">
                    <span class="stat-num">{{ event()!.waitlistCount }}</span>
                    <span class="stat-label">Waitlisted</span>
                  </div>
                }
              </div>

              <!-- Viewer RSVP Box -->
              <div class="viewer-rsvp-section">
                @if (event()!.viewerRsvp?.status === 'YES') {
                  <div class="rsvp-status-badge seated">
                    <span class="status-icon">✓</span>
                    <div>
                      <strong>You are attending!</strong>
                      <div class="sub">Seats reserved: {{ 1 + event()!.viewerRsvp!.guestCount }}</div>
                    </div>
                  </div>

                  <button class="btn btn-primary btn-block" (click)="viewTicket()">
                    🎟️ View Admission Ticket & QR
                  </button>

                  <button class="btn btn-outline btn-block btn-sm" (click)="cancelRsvp()">
                    Change / Cancel RSVP
                  </button>
                } @else if (event()!.viewerRsvp?.status === 'WAITLISTED') {
                  <div class="rsvp-status-badge waitlisted">
                    <span class="status-icon">⏳</span>
                    <div>
                      <strong>You are on the waitlist</strong>
                      <div class="sub">Position #{{ event()!.viewerRsvp!.waitlistPosition }}</div>
                    </div>
                  </div>

                  <button class="btn btn-outline btn-block btn-sm" (click)="cancelRsvp()">
                    Leave Waitlist
                  </button>
                } @else {
                  @if (event()!.guestsPerRsvpLimit > 0) {
                    <div class="form-group guest-selector">
                      <label class="form-label" for="guests">Additional Guests</label>
                      <select id="guests" class="form-select" [(ngModel)]="selectedGuests">
                        @for (g of guestOptions(); track g) {
                          <option [value]="g">{{ g === 0 ? 'No guests (+0)' : '+' + g + ' guest(s)' }}</option>
                        }
                      </select>
                    </div>
                  }

                  <button
                    class="btn btn-primary btn-block btn-lg"
                    (click)="submitRsvp('YES')"
                    [disabled]="event()!.status === 'CANCELLED' || submittingRsvp()"
                  >
                    @if (event()!.availability === 'WAITLIST') {
                      <span>Join Waitlist</span>
                    } @else {
                      <span>RSVP for Event</span>
                    }
                  </button>
                }
              </div>
            </div>
          </div>
        </div>

        <!-- Ticket Modal -->
        @if (ticket()) {
          <div class="modal-backdrop" (click)="ticket.set(null)">
            <div class="modal-content ticket-modal" (click)="$event.stopPropagation()">
              <div class="ticket-modal-header">
                <h2>Your Admission Pass</h2>
                <p>{{ event()!.title }}</p>
              </div>

              <div class="qr-container-box">
                <app-qr-code [value]="ticket()!.code" [size]="220"></app-qr-code>
                <div class="ticket-code-badge">{{ ticket()!.code }}</div>
              </div>

              <div class="ticket-info-grid">
                <div>
                  <span>Admit count:</span>
                  <strong>{{ ticket()!.admitCount }} Person(s)</strong>
                </div>
                <div>
                  <span>Status:</span>
                  <span class="badge badge-success">Valid</span>
                </div>
              </div>

              <button class="btn btn-secondary btn-block" (click)="ticket.set(null)">Close Pass</button>
            </div>
          </div>
        }

        <!-- Feedback Modal -->
        @if (showFeedbackModal()) {
          <div class="modal-backdrop" (click)="showFeedbackModal.set(false)">
            <div class="modal-content" (click)="$event.stopPropagation()">
              <h2>Leave Event Review</h2>
              <p>How was your experience at this event?</p>

              <div class="rating-input">
                @for (star of [1,2,3,4,5]; track star) {
                  <button type="button" class="star-btn" [class.active]="rating >= star" (click)="rating = star">
                    ★
                  </button>
                }
              </div>

              <div class="form-group">
                <label class="form-label">Review comment</label>
                <textarea
                  class="form-control"
                  [(ngModel)]="feedbackComment"
                  placeholder="What went well? Any suggestions for next time?"
                  rows="4"
                ></textarea>
              </div>

              <div class="modal-actions">
                <button class="btn btn-secondary" (click)="showFeedbackModal.set(false)">Cancel</button>
                <button class="btn btn-primary" (click)="submitFeedback()">Submit Review</button>
              </div>
            </div>
          </div>
        }

        <!-- Upload Photo Modal -->
        @if (showUploadPhotoModal()) {
          <div class="modal-backdrop" (click)="showUploadPhotoModal.set(false)">
            <div class="modal-content" (click)="$event.stopPropagation()">
              <h2>Upload Event Photo</h2>
              
              <div class="form-group">
                <label class="form-label">Photo URL</label>
                <input
                  type="url"
                  class="form-control"
                  [(ngModel)]="newPhotoUrl"
                  placeholder="https://example.com/photo.jpg"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Caption (optional)</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="newPhotoCaption"
                  placeholder="e.g. Group discussion during breakout"
                />
              </div>

              <div class="modal-actions">
                <button class="btn btn-secondary" (click)="showUploadPhotoModal.set(false)">Cancel</button>
                <button class="btn btn-primary" [disabled]="!newPhotoUrl" (click)="uploadPhoto()">Upload Photo</button>
              </div>
            </div>
          </div>
        }

        <!-- Cancel Event Modal -->
        @if (showCancelModal()) {
          <div class="modal-backdrop" (click)="showCancelModal.set(false)">
            <div class="modal-content" (click)="$event.stopPropagation()">
              <h2>Cancel Event</h2>
              <p>Please provide a reason for cancelling. Attendees will be notified.</p>

              <div class="form-group">
                <label class="form-label">Cancellation Reason</label>
                <textarea
                  class="form-control"
                  [(ngModel)]="cancellationReason"
                  placeholder="e.g. Severe weather conditions, speaker rescheduled..."
                  rows="3"
                ></textarea>
              </div>

              <div class="modal-actions">
                <button class="btn btn-secondary" (click)="showCancelModal.set(false)">Back</button>
                <button class="btn btn-danger" [disabled]="!cancellationReason.trim()" (click)="cancelEvent()">
                  Confirm Cancellation
                </button>
              </div>
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [
    `
      .event-detail-page {
        padding: 2.5rem 1.5rem;
      }
      .organizer-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
        background: #fffbeb;
        border-color: #fde68a;
        flex-wrap: wrap;
        gap: 1rem;
        .toolbar-left {
          display: flex;
          align-items: center;
          gap: 1rem;
        }
        .toolbar-actions {
          display: flex;
          gap: 0.5rem;
          flex-wrap: wrap;
        }
      }
      .cancellation-banner {
        display: flex;
        align-items: center;
        gap: 1rem;
        background: var(--color-danger-subtle);
        border-color: var(--color-danger);
        margin-bottom: 2rem;
        .banner-icon { font-size: 2rem; }
        h3 { margin: 0; color: var(--color-danger-text); }
        p { margin: 0.25rem 0 0 0; color: var(--color-danger-text); }
      }
      .event-layout-grid {
        display: grid;
        grid-template-columns: 1fr;
        gap: 2.5rem;
        @media (min-width: 900px) {
          grid-template-columns: 2fr 1fr;
        }
      }
      .hero-badges {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 0.75rem;
        .group-link {
          font-weight: 700;
          color: var(--color-primary);
        }
      }
      .hero-title {
        font-size: 2.25rem;
        line-height: 1.25;
        margin-bottom: 1.25rem;
      }
      .hosts-avatars {
        display: flex;
        gap: 1rem;
        margin-bottom: 2rem;
        .host-item {
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }
        .host-avatar {
          width: 32px;
          height: 32px;
          border-radius: 9999px;
          background: var(--color-primary-subtle);
          color: var(--color-primary);
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
        }
      }
      .tab-pane {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }
      .description-card, .venue-card, .online-card {
        padding: 1.75rem;
        h3 { margin-bottom: 1rem; }
        &.online-locked {
          background: #f8fafc;
          border: 1px dashed #cbd5e1;
          .lock-banner {
            color: #64748b;
            font-weight: 500;
            margin: 0;
          }
        }
      }
      .description-text {
        white-space: pre-wrap;
        line-height: 1.7;
        color: var(--color-text-main);
      }
      .comments-list {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .comment-card {
        padding: 1.25rem;
        .comment-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 0.75rem;
        }
        .comment-author {
          display: flex;
          align-items: center;
          gap: 0.75rem;
        }
        .author-avatar {
          width: 36px;
          height: 36px;
          border-radius: 9999px;
          background: var(--color-bg-muted);
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
        }
        .comment-date {
          display: block;
          font-size: 0.75rem;
          color: var(--color-text-subtle);
        }
        .deleted-body {
          color: var(--color-text-subtle);
          font-style: italic;
        }
      }
      .photos-header, .feedback-top-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1.5rem;
        h3 { margin-bottom: 0.25rem; }
      }
      .photos-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 1.25rem;
        .photo-item {
          padding: 0.75rem;
          img {
            width: 100%;
            height: 180px;
            object-fit: cover;
            border-radius: 8px;
          }
          .photo-caption {
            font-size: 0.85rem;
            margin: 0.5rem 0 0.25rem 0;
          }
          .photo-meta {
            font-size: 0.75rem;
            color: var(--color-text-subtle);
          }
        }
      }
      .ai-synthesis-card {
        background: linear-gradient(135deg, #f0fdf4 0%, #e0f2fe 100%);
        border: 1px solid #bae6fd;
        padding: 1.5rem;
        margin-bottom: 1.5rem;
        .synthesis-header {
          display: flex;
          justify-content: space-between;
          margin-bottom: 0.75rem;
        }
        .ai-badge {
          font-weight: 800;
          color: #0369a1;
        }
        .sentiment-badge {
          background: #0284c7;
          color: white;
          padding: 2px 8px;
          border-radius: 9999px;
          font-size: 0.75rem;
          font-weight: 700;
        }
        .narrative {
          font-size: 0.95rem;
          line-height: 1.6;
          color: #1e293b;
        }
        .themes-row {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          flex-wrap: wrap;
          margin-top: 0.75rem;
        }
      }
      .feedback-list {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .feedback-card {
        padding: 1.25rem;
        .feedback-header {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          margin-bottom: 0.75rem;
        }
        .star-rating span {
          color: #cbd5e1;
          font-size: 1.1rem;
          &.filled { color: #f59e0b; }
        }
        .feedback-date {
          margin-left: auto;
          font-size: 0.8rem;
          color: var(--color-text-subtle);
        }
      }
      .rsvp-card {
        position: sticky;
        top: 90px;
        padding: 1.75rem;
      }
      .event-timing {
        display: flex;
        gap: 1rem;
        align-items: flex-start;
        .timing-icon { font-size: 1.75rem; }
        .timing-date { font-weight: 700; font-size: 1.1rem; }
        .timing-hours { font-size: 0.875rem; color: var(--color-text-muted); }
      }
      .rsvp-divider {
        height: 1px;
        background: var(--color-border);
        margin: 1.25rem 0;
      }
      .capacity-stats {
        display: flex;
        justify-content: space-around;
        text-align: center;
        margin-bottom: 1.5rem;
        .stat-box {
          display: flex;
          flex-direction: column;
        }
        .stat-num {
          font-size: 1.4rem;
          font-weight: 800;
          color: var(--color-text-main);
        }
        .stat-label {
          font-size: 0.75rem;
          color: var(--color-text-subtle);
          text-transform: uppercase;
          font-weight: 600;
        }
      }
      .rsvp-status-badge {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.85rem 1rem;
        border-radius: 10px;
        margin-bottom: 1rem;
        &.seated {
          background: var(--color-success-subtle);
          color: var(--color-success-text);
          .status-icon { font-size: 1.25rem; font-weight: 800; }
        }
        &.waitlisted {
          background: var(--color-warning-subtle);
          color: var(--color-warning-text);
          .status-icon { font-size: 1.25rem; }
        }
      }
      .btn-block {
        width: 100%;
        margin-bottom: 0.75rem;
      }
      .rating-input {
        display: flex;
        gap: 0.5rem;
        margin: 1rem 0;
        .star-btn {
          background: transparent;
          border: none;
          font-size: 2rem;
          color: #cbd5e1;
          cursor: pointer;
          &.active { color: #f59e0b; }
        }
      }
      .modal-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
        margin-top: 1.5rem;
      }
      .ticket-modal {
        text-align: center;
        max-width: 400px;
      }
      .qr-container-box {
        margin: 1.5rem 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
      }
      .ticket-code-badge {
        font-family: var(--font-mono);
        font-size: 0.8rem;
        background: var(--color-bg-subtle);
        padding: 4px 8px;
        border-radius: 4px;
      }
      .ticket-info-grid {
        display: flex;
        justify-content: space-between;
        margin-bottom: 1.5rem;
        font-size: 0.875rem;
      }
    `,
  ],
})
export class EventDetailComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly rsvpService = inject(RsvpService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly commentService = inject(CommentService);
  private readonly photoService = inject(PhotoService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly aiService = inject(AiService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  readonly event = signal<EventDetail | null>(null);
  readonly comments = signal<CommentSummary[]>([]);
  readonly photos = signal<PhotoSummary[]>([]);
  readonly feedbacks = signal<EventFeedbackSummary[]>([]);
  readonly ticket = signal<TicketResponse | null>(null);
  readonly aiFeedback = signal<AiFeedbackSummaryResponse | null>(null);

  readonly loading = signal(true);
  readonly submittingRsvp = signal(false);
  readonly submittingComment = signal(false);

  readonly activeTab = signal<'details' | 'comments' | 'photos' | 'feedback'>('details');
  readonly showCancelModal = signal(false);
  readonly showFeedbackModal = signal(false);
  readonly showUploadPhotoModal = signal(false);

  selectedGuests = 0;
  newCommentText = '';
  newPhotoUrl = '';
  newPhotoCaption = '';
  rating = 5;
  feedbackComment = '';
  cancellationReason = '';

  guestOptions(): number[] {
    const limit = this.event()?.guestsPerRsvpLimit || 0;
    return Array.from({ length: limit + 1 }, (_, i) => i);
  }

  isOrganizer(): boolean {
    const cur = this.auth.currentUser();
    const ev = this.event();
    if (!cur || !ev) return false;
    return ev.hosts.some(h => h.id === cur.id);
  }

  ngOnInit() {
    this.loadEvent();
    this.loadComments();
    this.loadPhotos();
    this.loadFeedback();
  }

  loadEvent() {
    this.loading.set(true);
    this.eventService.getEvent(this.id()).subscribe({
      next: res => {
        this.event.set(res);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Failed to load event');
      },
    });
  }

  loadComments() {
    this.commentService.getComments(this.id()).subscribe({
      next: res => this.comments.set(res),
    });
  }

  loadPhotos() {
    this.photoService.getPhotos(this.id()).subscribe({
      next: res => this.photos.set(res),
    });
  }

  loadFeedback() {
    this.feedbackService.getFeedback(this.id()).subscribe({
      next: res => this.feedbacks.set(res),
    });
  }

  submitRsvp(status: 'YES' | 'NO') {
    if (!this.auth.isAuthenticated()) {
      this.toast.info('Please sign in to RSVP for events.');
      this.router.navigate(['/login']);
      return;
    }

    this.submittingRsvp.set(true);
    this.rsvpService
      .submitRsvp(this.id(), { status, guestCount: this.selectedGuests })
      .subscribe({
        next: () => {
          this.submittingRsvp.set(false);
          this.toast.success(status === 'YES' ? 'RSVP submitted successfully!' : 'RSVP updated.');
          this.loadEvent();
        },
        error: err => {
          this.submittingRsvp.set(false);
          this.toast.error(err?.error?.message || 'Unable to submit RSVP');
        },
      });
  }

  cancelRsvp() {
    this.rsvpService.cancelRsvp(this.id()).subscribe({
      next: () => {
        this.toast.info('RSVP cancelled.');
        this.loadEvent();
      },
      error: err => this.toast.error(err?.error?.message || 'Could not cancel RSVP'),
    });
  }

  viewTicket() {
    this.attendanceService.getTicket(this.id()).subscribe({
      next: t => this.ticket.set(t),
      error: () => {
        this.attendanceService.issueTicket(this.id()).subscribe({
          next: t => this.ticket.set(t),
          error: err => this.toast.error(err?.error?.message || 'Ticket unavailable'),
        });
      },
    });
  }

  submitComment() {
    if (!this.newCommentText.trim()) return;
    this.submittingComment.set(true);

    this.commentService
      .addComment(this.id(), { body: this.newCommentText })
      .subscribe({
        next: () => {
          this.submittingComment.set(false);
          this.newCommentText = '';
          this.loadComments();
        },
        error: err => {
          this.submittingComment.set(false);
          this.toast.error(err?.error?.message || 'Failed to post comment');
        },
      });
  }

  canDeleteComment(comment: CommentSummary): boolean {
    const cur = this.auth.currentUser();
    if (!cur || comment.deletedAt) return false;
    return comment.author.id === cur.id || this.isOrganizer();
  }

  deleteComment(commentId: string) {
    this.commentService.deleteComment(this.id(), commentId).subscribe({
      next: () => this.loadComments(),
      error: err => this.toast.error(err?.error?.message || 'Could not delete comment'),
    });
  }

  uploadPhoto() {
    if (!this.newPhotoUrl) return;
    this.photoService
      .uploadPhoto(this.id(), {
        url: this.newPhotoUrl,
        caption: this.newPhotoCaption,
        width: 1200,
        height: 800,
      })
      .subscribe({
        next: () => {
          this.showUploadPhotoModal.set(false);
          this.newPhotoUrl = '';
          this.newPhotoCaption = '';
          this.toast.success('Photo uploaded!');
          this.loadPhotos();
        },
        error: err => this.toast.error(err?.error?.message || 'Upload failed'),
      });
  }

  submitFeedback() {
    this.feedbackService
      .submitFeedback(this.id(), {
        rating: this.rating,
        comment: this.feedbackComment,
      })
      .subscribe({
        next: () => {
          this.showFeedbackModal.set(false);
          this.feedbackComment = '';
          this.toast.success('Thank you for your review!');
          this.loadFeedback();
        },
        error: err => this.toast.error(err?.error?.message || 'Could not submit review'),
      });
  }

  loadAiFeedbackSummary() {
    this.aiService.summarizeFeedback(this.id()).subscribe({
      next: summary => this.aiFeedback.set(summary),
      error: err => this.toast.error(err?.error?.message || 'Failed to generate AI synthesis'),
    });
  }

  publishEvent() {
    this.eventService.publishEvent(this.id()).subscribe({
      next: () => {
        this.toast.success('Event published! It is now visible in search.');
        this.loadEvent();
      },
      error: err => this.toast.error(err?.error?.message || 'Failed to publish event'),
    });
  }

  cancelEvent() {
    if (!this.cancellationReason.trim()) return;
    this.eventService
      .cancelEvent(this.id(), { reason: this.cancellationReason })
      .subscribe({
        next: () => {
          this.showCancelModal.set(false);
          this.toast.warning('Event cancelled.');
          this.loadEvent();
        },
        error: err => this.toast.error(err?.error?.message || 'Could not cancel event'),
      });
  }
}
