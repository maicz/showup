import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CheckInResponse } from '../../../core/models/attendance.model';
import { EventDetail } from '../../../core/models/event.model';
import { AttendeeSummary } from '../../../core/models/rsvp.model';
import { AttendanceService } from '../../../core/services/attendance.service';
import { EventService } from '../../../core/services/event.service';
import { RsvpService } from '../../../core/services/rsvp.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-check-in',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  template: `
    <div class="container check-in-page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <a [routerLink]="['/events', id()]">← Back to {{ event()?.title || 'Event' }}</a>
          </div>
          <h1>Door Check-In & Scanner</h1>
          <p class="header-sub">Scan attendee QR codes or search attendee roster manually</p>
        </div>
      </div>

      <!-- Realtime Tally Stats -->
      <div class="stats-row">
        <div class="card stat-card">
          <span class="stat-number">{{ checkedInCount() }} / {{ attendees().length }}</span>
          <span class="stat-title">Attendees Checked In</span>
        </div>
        <div class="card stat-card">
          <span class="stat-number">{{ checkInPercentage() }}%</span>
          <span class="stat-title">Attendance Rate</span>
        </div>
        <div class="card stat-card">
          <span class="stat-number">{{ event()?.capacity || '∞' }}</span>
          <span class="stat-title">Total Capacity</span>
        </div>
      </div>

      <div class="scanner-grid">
        <!-- Scan / Input Box -->
        <div class="card scanner-box">
          <h3>QR Code & Ticket Scanner</h3>
          <p>Scan a QR code with your camera or enter the ticket code directly.</p>

          <form (ngSubmit)="submitTicketCode()" class="ticket-input-form">
            <div class="form-group">
              <label class="form-label" for="code">Ticket Code</label>
              <div class="input-with-btn">
                <input
                  type="text"
                  id="code"
                  class="form-control font-mono"
                  [(ngModel)]="manualTicketCode"
                  name="code"
                  placeholder="Paste or scan ticket code..."
                  autofocus
                />
                <button
                  type="submit"
                  class="btn btn-primary"
                  [disabled]="!manualTicketCode.trim() || scanning()"
                >
                  Admit
                </button>
              </div>
            </div>
          </form>

          <!-- Scanner Simulation / Fast Test Buttons -->
          <div class="quick-scan-section">
            <span class="quick-label">Or click to admit from roster below:</span>
          </div>

          <!-- Last Scan Result Card -->
          @if (lastCheckInResult()) {
            <div
              class="scan-result-card"
              [class.result-success]="!lastCheckInResult()!.alreadyCheckedIn"
              [class.result-warning]="lastCheckInResult()!.alreadyCheckedIn"
            >
              @if (!lastCheckInResult()!.alreadyCheckedIn) {
                <div class="result-icon">✓</div>
                <div class="result-info">
                  <h4>Admitted: {{ lastCheckInResult()!.attendeeName }}</h4>
                  <p>Admitted count: {{ lastCheckInResult()!.admittedCount }} • {{ lastCheckInResult()!.method }}</p>
                  <span class="result-time">{{ lastCheckInResult()!.checkedInAt | date: 'mediumTime' }}</span>
                </div>
              } @else {
                <div class="result-icon">⚠️</div>
                <div class="result-info">
                  <h4>Already Checked In!</h4>
                  <p>{{ lastCheckInResult()!.attendeeName }} was already admitted earlier.</p>
                  <span class="result-time">First scanned at: {{ lastCheckInResult()!.checkedInAt | date: 'mediumTime' }}</span>
                </div>
              }
            </div>
          }
        </div>

        <!-- Attendee Roster Box -->
        <div class="card roster-box">
          <div class="roster-header">
            <h3>Attendee Roster</h3>
            <input
              type="text"
              class="form-control roster-search"
              placeholder="Search attendee by name..."
              [(ngModel)]="rosterFilter"
            />
          </div>

          <div class="roster-list">
            @for (att of filteredAttendees(); track att.member.id) {
              <div class="roster-item" [class.item-checked]="att.checkedIn">
                <div class="roster-member">
                  <div class="roster-avatar">{{ att.member.displayName.charAt(0) }}</div>
                  <div>
                    <strong>{{ att.member.displayName }}</strong>
                    <div class="roster-sub">
                      <span>{{ att.guestCount > 0 ? '+ ' + att.guestCount + ' guest(s)' : '1 seat' }}</span>
                      • <span class="badge badge-sm" [class.badge-success]="att.status === 'YES'">{{ att.status }}</span>
                    </div>
                  </div>
                </div>

                <div class="roster-action">
                  @if (att.checkedIn) {
                    <span class="badge badge-success">✓ Checked In</span>
                  } @else {
                    <button class="btn btn-sm btn-outline" (click)="manualAdmitAttendee(att)">
                      Admit (Manual)
                    </button>
                  }
                </div>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .check-in-page {
        padding: 2.5rem 1.5rem;
      }
      .breadcrumb {
        margin-bottom: 0.5rem;
        a { color: var(--color-primary); font-weight: 600; font-size: 0.9rem; }
      }
      .header-sub {
        color: var(--color-text-muted);
        font-size: 1.05rem;
      }
      .stats-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1.25rem;
        margin-bottom: 2rem;
      }
      .stat-card {
        text-align: center;
        padding: 1.5rem;
        .stat-number {
          font-size: 2rem;
          font-weight: 800;
          color: var(--color-primary);
          display: block;
        }
        .stat-title {
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--color-text-subtle);
          text-transform: uppercase;
        }
      }
      .scanner-grid {
        display: grid;
        grid-template-columns: 1fr;
        gap: 2rem;
        @media (min-width: 900px) {
          grid-template-columns: 1fr 1fr;
        }
      }
      .font-mono {
        font-family: var(--font-mono);
      }
      .input-with-btn {
        display: flex;
        gap: 0.5rem;
      }
      .quick-scan-section {
        margin: 1.5rem 0 1rem 0;
        .quick-label {
          font-size: 0.85rem;
          color: var(--color-text-subtle);
        }
      }
      .scan-result-card {
        display: flex;
        gap: 1rem;
        align-items: center;
        padding: 1.25rem;
        border-radius: 12px;
        margin-top: 1.5rem;
        animation: pulseIn 200ms ease-out;

        &.result-success {
          background: var(--color-success-subtle);
          border: 1px solid var(--color-success);
          .result-icon { font-size: 2rem; color: var(--color-success); font-weight: 800; }
          h4 { margin: 0; color: var(--color-success-text); }
          p { margin: 0.2rem 0; color: var(--color-success-text); }
          .result-time { font-size: 0.75rem; color: var(--color-success-text); }
        }

        &.result-warning {
          background: var(--color-warning-subtle);
          border: 1px solid var(--color-warning);
          .result-icon { font-size: 2rem; color: var(--color-warning); font-weight: 800; }
          h4 { margin: 0; color: var(--color-warning-text); }
          p { margin: 0.2rem 0; color: var(--color-warning-text); }
          .result-time { font-size: 0.75rem; color: var(--color-warning-text); }
        }
      }
      .roster-box {
        display: flex;
        flex-direction: column;
        max-height: 600px;
      }
      .roster-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1.25rem;
        gap: 1rem;
        h3 { margin: 0; white-space: nowrap; }
        .roster-search { max-width: 200px; }
      }
      .roster-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        overflow-y: auto;
        padding-right: 4px;
      }
      .roster-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem 1rem;
        border: 1px solid var(--color-border);
        border-radius: 10px;
        background: var(--color-bg-surface);
        transition: all var(--transition-fast);

        &.item-checked {
          background: #f0fdf4;
          border-color: #bbf7d0;
        }
      }
      .roster-member {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }
      .roster-avatar {
        width: 32px;
        height: 32px;
        border-radius: 9999px;
        background: var(--color-bg-muted);
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 0.85rem;
      }
      .roster-sub {
        font-size: 0.8rem;
        color: var(--color-text-subtle);
        display: flex;
        align-items: center;
        gap: 0.4rem;
      }
      @keyframes pulseIn {
        from { transform: scale(0.97); opacity: 0; }
        to { transform: scale(1); opacity: 1; }
      }
    `,
  ],
})
export class CheckInComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly rsvpService = inject(RsvpService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);

  readonly id = input.required<string>();

  readonly event = signal<EventDetail | null>(null);
  readonly attendees = signal<AttendeeSummary[]>([]);
  readonly lastCheckInResult = signal<CheckInResponse | null>(null);
  readonly scanning = signal(false);

  manualTicketCode = '';
  rosterFilter = '';

  checkedInCount(): number {
    return this.attendees().filter(a => a.checkedIn).length;
  }

  checkInPercentage(): number {
    const total = this.attendees().length;
    if (total === 0) return 0;
    return Math.round((this.checkedInCount() / total) * 100);
  }

  filteredAttendees(): AttendeeSummary[] {
    if (!this.rosterFilter.trim()) return this.attendees();
    const q = this.rosterFilter.toLowerCase();
    return this.attendees().filter(a => a.member.displayName.toLowerCase().includes(q));
  }

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.eventService.getEvent(this.id()).subscribe({
      next: ev => this.event.set(ev),
    });

    this.rsvpService.getAttendees(this.id()).subscribe({
      next: atts => this.attendees.set(atts),
    });
  }

  submitTicketCode() {
    if (!this.manualTicketCode.trim()) return;
    this.scanning.set(true);

    this.attendanceService
      .checkIn(this.id(), {
        ticketCode: this.manualTicketCode.trim(),
        method: 'QR_SCAN',
        admittedCount: 1,
      })
      .subscribe({
        next: res => {
          this.scanning.set(false);
          this.lastCheckInResult.set(res);
          this.manualTicketCode = '';
          this.toast.success(`Check-in recorded for ${res.attendeeName}!`);
          this.loadData();
        },
        error: err => {
          this.scanning.set(false);
          this.toast.error(err?.error?.message || 'Invalid or revoked ticket code');
        },
      });
  }

  manualAdmitAttendee(att: AttendeeSummary) {
    this.attendanceService.checkInByMember(this.id(), att.member.id).subscribe({
      next: res => {
        this.lastCheckInResult.set(res);
        this.toast.success(`Manual check-in completed for ${att.member.displayName}`);
        this.loadData();
      },
      error: err => {
        this.toast.error(err?.error?.message || `Could not check in ${att.member.displayName}`);
      },
    });
  }
}
