import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { GroupActivityReport } from '../../../core/models/report.model';
import { ReportService } from '../../../core/services/report.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-group-report',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container container-narrow group-report-page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <a [routerLink]="['/groups', id()]">← Back to Group</a>
          </div>
          <h1>Group Activity Analytics</h1>
          <p class="header-sub">{{ report()?.groupName || 'Community Analytics' }}</p>
        </div>
      </div>

      @if (loading()) {
        <div class="loading-state">Loading group analytics...</div>
      } @else if (report()) {
        <div class="kpi-grid">
          <div class="card kpi-card">
            <span class="kpi-num">{{ report()!.eventsHeld }}</span>
            <span class="kpi-label">Events Held</span>
          </div>

          <div class="card kpi-card">
            <span class="kpi-num color-primary">{{ report()!.totalAttendees }}</span>
            <span class="kpi-label">Total Turnout</span>
          </div>

          <div class="card kpi-card">
            <span class="kpi-num color-success">{{ report()!.averageAttendancePerEvent }}</span>
            <span class="kpi-label">Avg Attendance</span>
          </div>

          <div class="card kpi-card">
            <span class="kpi-num color-accent">{{ report()!.newMembersCount }}</span>
            <span class="kpi-label">New Members</span>
          </div>
        </div>

        <div class="card summary-card">
          <h3>Community Health & Engagement</h3>
          <p>
            {{ report()!.groupName }} has organized <strong>{{ report()!.eventsHeld }}</strong> events with an average of
            <strong>{{ report()!.averageAttendancePerEvent }}</strong> attendees per meetup.
          </p>
          @if (report()!.currentRatingAverage) {
            <p>
              The current community satisfaction score stands at
              <strong class="color-warning">★ {{ report()!.currentRatingAverage }} / 5.0</strong>.
            </p>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .group-report-page {
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
        .color-primary { color: var(--color-primary); }
        .color-success { color: var(--color-success); }
        .color-accent { color: var(--color-accent); }
      }
      .summary-card {
        padding: 1.75rem;
        h3 { margin-bottom: 1rem; }
        p { font-size: 1rem; line-height: 1.6; }
      }
      .color-warning { color: var(--color-warning-text); }
      .loading-state {
        text-align: center;
        padding: 4rem;
        color: var(--color-text-subtle);
      }
    `,
  ],
})
export class GroupReportComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly toast = inject(ToastService);

  readonly id = input.required<string>();

  readonly report = signal<GroupActivityReport | null>(null);
  readonly loading = signal(true);

  ngOnInit() {
    this.reportService.getGroupActivityReport(this.id()).subscribe({
      next: res => {
        this.report.set(res);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Failed to load group report');
      },
    });
  }
}
