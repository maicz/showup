import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventAttendanceReport } from '../../../core/models/report.model';
import { ReportService } from '../../../core/services/report.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-event-report',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
    <div class="container container-narrow report-page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <a [routerLink]="['/events', id()]">← Back to Event</a>
          </div>
          <h1>Attendance & Scanner Report</h1>
          <p class="header-sub">{{ report()?.eventTitle || 'Event Attendance Metrics' }}</p>
        </div>
      </div>

      @if (loading()) {
        <div class="loading-state">Loading attendance analytics...</div>
      } @else if (report()) {
        <!-- KPI Row -->
        <div class="kpi-grid">
          <div class="card kpi-card">
            <span class="kpi-num">{{ report()!.registeredCount }}</span>
            <span class="kpi-label">Registered Seats</span>
          </div>

          <div class="card kpi-card">
            <span class="kpi-num color-success">{{ report()!.attendedCount }}</span>
            <span class="kpi-label">Attended</span>
          </div>

          <div class="card kpi-card">
            <span class="kpi-num color-warning">{{ report()!.noShowCount }}</span>
            <span class="kpi-label">No-Shows</span>
          </div>

          <div class="card kpi-card">
            <span class="kpi-num color-primary">{{ report()!.attendanceRate }}%</span>
            <span class="kpi-label">Show-up Rate</span>
          </div>
        </div>

        <!-- Attendance Rate Progress Bar -->
        <div class="card progress-card">
          <h3>Turnout Ratio</h3>
          <div class="progress-bar-bg">
            <div class="progress-bar-fill" [style.width.%]="report()!.attendanceRate"></div>
          </div>
          <div class="progress-labels">
            <span>{{ report()!.attendedCount }} checked in</span>
            <span>{{ report()!.noShowCount }} absent</span>
          </div>
        </div>

        <!-- Staff Door Scan Totals -->
        <div class="card staff-scan-card">
          <h3>Door Staff Throughput</h3>
          <p class="sub-text">Breakdown of check-ins scanned by each door volunteer.</p>

          @if (report()!.staffScans.length === 0) {
            <p class="no-scans">No staff scanner activity recorded.</p>
          } @else {
            <div class="staff-scans-list">
              @for (staff of report()!.staffScans; track staff.memberId) {
                <div class="staff-scan-row">
                  <div class="staff-name">
                    <strong>{{ staff.displayName }}</strong>
                  </div>
                  <div class="scan-count-badge">
                    {{ staff.scanCount }} scan(s)
                  </div>
                </div>
              }
            </div>
          }
        </div>

        <!-- Check-in Timeline -->
        @if (report()!.checkInTimeline.length > 0) {
          <div class="card timeline-card">
            <h3>Check-In Velocity Timeline</h3>
            <div class="timeline-table-wrapper">
              <table class="timeline-table">
                <thead>
                  <tr>
                    <th>Time Bucket</th>
                    <th>Admitted Count</th>
                  </tr>
                </thead>
                <tbody>
                  @for (t of report()!.checkInTimeline; track t.minute) {
                    <tr>
                      <td>{{ t.minute | date: 'shortTime' }}</td>
                      <td><strong>{{ t.count }}</strong></td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: [
    `
      .report-page {
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
      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 1.25rem;
        margin-bottom: 2rem;
      }
      .kpi-card {
        text-align: center;
        padding: 1.5rem;
        .kpi-num {
          font-size: 2.25rem;
          font-weight: 800;
          display: block;
        }
        .kpi-label {
          font-size: 0.8rem;
          font-weight: 700;
          color: var(--color-text-subtle);
          text-transform: uppercase;
        }
        .color-success { color: var(--color-success); }
        .color-warning { color: var(--color-warning); }
        .color-primary { color: var(--color-primary); }
      }
      .progress-card, .staff-scan-card, .timeline-card {
        padding: 1.75rem;
        margin-bottom: 2rem;
        h3 { margin-bottom: 1rem; }
        .sub-text { color: var(--color-text-subtle); font-size: 0.9rem; margin-bottom: 1.25rem; }
      }
      .progress-bar-bg {
        height: 16px;
        background: var(--color-bg-muted);
        border-radius: 9999px;
        overflow: hidden;
        margin-bottom: 0.75rem;
      }
      .progress-bar-fill {
        height: 100%;
        background: linear-gradient(90deg, #10b981, #059669);
        border-radius: 9999px;
        transition: width 500ms ease-out;
      }
      .progress-labels {
        display: flex;
        justify-content: space-between;
        font-size: 0.85rem;
        color: var(--color-text-muted);
        font-weight: 600;
      }
      .staff-scans-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }
      .staff-scan-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem 1rem;
        background: var(--color-bg-subtle);
        border-radius: 8px;
      }
      .scan-count-badge {
        font-weight: 700;
        color: var(--color-primary);
      }
      .timeline-table {
        width: 100%;
        border-collapse: collapse;
        th, td {
          padding: 0.75rem 1rem;
          text-align: left;
          border-bottom: 1px solid var(--color-border);
        }
        th {
          font-size: 0.85rem;
          color: var(--color-text-subtle);
          text-transform: uppercase;
        }
      }
      .loading-state {
        text-align: center;
        padding: 4rem;
        color: var(--color-text-subtle);
      }
    `,
  ],
})
export class EventReportComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly toast = inject(ToastService);

  readonly id = input.required<string>();

  readonly report = signal<EventAttendanceReport | null>(null);
  readonly loading = signal(true);

  ngOnInit() {
    this.reportService.getEventAttendanceReport(this.id()).subscribe({
      next: res => {
        this.report.set(res);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Failed to load event report');
      },
    });
  }
}
