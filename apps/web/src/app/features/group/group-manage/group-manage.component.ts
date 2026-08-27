import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GroupDetail, GroupMemberRole, GroupMemberSummary } from '../../../core/models/group.model';
import { GroupService } from '../../../core/services/group.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-group-manage',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  template: `
    <div class="container container-narrow group-manage-page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <a [routerLink]="['/groups', id()]">← Back to {{ group()?.name || 'Group' }}</a>
          </div>
          <h1>Organizer Console</h1>
          <p class="header-sub">Manage membership roles, pending join approvals, and group activity</p>
        </div>
        <div class="header-actions">
          <a [routerLink]="['/groups', id(), 'report']" class="btn btn-secondary">
            📊 Group Activity Report
          </a>
        </div>
      </div>

      <!-- Pending Join Approvals Section -->
      @if (pendingMembers().length > 0) {
        <div class="card pending-card">
          <h3>Pending Join Requests ({{ pendingMembers().length }})</h3>
          <p class="sub-text">These members have requested to join and need organizer approval.</p>

          <div class="pending-list">
            @for (p of pendingMembers(); track p.memberId) {
              <div class="pending-item">
                <div class="member-meta">
                  <div class="member-avatar">{{ p.displayName.charAt(0) }}</div>
                  <div>
                    <strong>{{ p.displayName }}</strong>
                    <span class="request-time">Requested on {{ p.joinedAt | date: 'mediumDate' }}</span>
                  </div>
                </div>

                <div class="pending-actions">
                  <button class="btn btn-sm btn-primary" (click)="approveMember(p.memberId)">
                    Approve
                  </button>
                  <button class="btn btn-sm btn-outline-danger" (click)="declineMember(p.memberId, p.displayName)">
                    Decline
                  </button>
                </div>
              </div>
            }
          </div>
        </div>
      }

      <!-- Members & Role Management Section -->
      <div class="card members-card">
        <h3>Group Roster & Roles ({{ activeMembers().length }})</h3>
        <p class="sub-text">Delegate organizer and event creation capabilities to trusted members.</p>

        <div class="members-roster">
          @for (m of activeMembers(); track m.memberId) {
            <div class="roster-row">
              <div class="member-meta">
                <div class="member-avatar">{{ m.displayName.charAt(0) }}</div>
                <div>
                  <strong>{{ m.displayName }}</strong>
                  <span class="join-date">Joined {{ m.joinedAt | date: 'mediumDate' }}</span>
                </div>
              </div>

              <div class="role-selector-row">
                <select
                  class="form-select role-select"
                  [ngModel]="m.role"
                  (ngModelChange)="updateRole(m.memberId, $event)"
                >
                  <option value="ORGANIZER">Organizer (Full Control)</option>
                  <option value="CO_ORGANIZER">Co-Organizer</option>
                  <option value="ASSISTANT_ORGANIZER">Assistant Organizer</option>
                  <option value="EVENT_ORGANIZER">Event Organizer</option>
                  <option value="MEMBER">Member</option>
                </select>
              </div>
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .group-manage-page {
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
      .pending-card, .members-card {
        margin-bottom: 2rem;
        padding: 1.75rem;
        h3 { margin-bottom: 0.25rem; }
        .sub-text { color: var(--color-text-subtle); font-size: 0.9rem; margin-bottom: 1.5rem; }
      }
      .pending-card {
        border-color: #fde68a;
        background: #fffdf5;
      }
      .pending-list, .members-roster {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .pending-item, .roster-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 10px;
        flex-wrap: wrap;
        gap: 1rem;
      }
      .member-meta {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .member-avatar {
        width: 40px;
        height: 40px;
        border-radius: 9999px;
        background: var(--color-primary-subtle);
        color: var(--color-primary);
        font-weight: 800;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .intro-quote {
        font-size: 0.85rem;
        font-style: italic;
        color: var(--color-text-muted);
        margin: 0.2rem 0;
      }
      .request-time, .join-date {
        font-size: 0.75rem;
        color: var(--color-text-subtle);
      }
      .pending-actions {
        display: flex;
        gap: 0.5rem;
      }
      .btn-outline-danger {
        background: transparent;
        border: 1px solid var(--color-danger, #ef4444);
        color: var(--color-danger, #ef4444);
        &:hover {
          background: #fef2f2;
        }
      }
      .role-select {
        min-width: 180px;
        font-size: 0.875rem;
      }
    `,
  ],
})
export class GroupManageComponent implements OnInit {
  private readonly groupService = inject(GroupService);
  private readonly toast = inject(ToastService);

  readonly id = input.required<string>();

  readonly group = signal<GroupDetail | null>(null);
  readonly activeMembers = signal<GroupMemberSummary[]>([]);
  readonly pendingMembers = signal<GroupMemberSummary[]>([]);

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.groupService.getGroup(this.id()).subscribe({
      next: g => this.group.set(g),
    });

    this.groupService.getMembers(this.id(), 'ACTIVE').subscribe({
      next: m => this.activeMembers.set(m),
    });

    this.groupService.getMembers(this.id(), 'PENDING_APPROVAL').subscribe({
      next: p => this.pendingMembers.set(p),
    });
  }

  approveMember(memberId: string) {
    this.groupService.approveMember(this.id(), memberId).subscribe({
      next: () => {
        this.toast.success('Member approved!');
        this.loadData();
      },
      error: err => this.toast.error(err?.error?.message || 'Could not approve member'),
    });
  }

  declineMember(memberId: string, name?: string) {
    const label = name ? `Decline join request from ${name}?` : 'Decline this join request?';
    if (!confirm(label)) return;
    this.groupService.declineMember(this.id(), memberId).subscribe({
      next: () => {
        this.toast.info('Join request declined.');
        this.loadData();
      },
      error: err => this.toast.error(err?.error?.message || 'Could not decline member'),
    });
  }

  updateRole(memberId: string, role: GroupMemberRole) {
    this.groupService.updateMemberRole(this.id(), memberId, { role }).subscribe({
      next: () => {
        this.toast.success('Role updated.');
        this.loadData();
      },
      error: err => this.toast.error(err?.error?.message || 'Failed to update member role'),
    });
  }
}
