import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { StaffAssignmentSummary, StaffRole } from '../../../core/models/attendance.model';
import { EventDetail } from '../../../core/models/event.model';
import { GroupMemberSummary } from '../../../core/models/group.model';
import { AttendanceService } from '../../../core/services/attendance.service';
import { EventService } from '../../../core/services/event.service';
import { GroupService } from '../../../core/services/group.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-staff-management',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="container container-narrow staff-page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <a [routerLink]="['/events', id()]">← Back to {{ event()?.title || 'Event' }}</a>
          </div>
          <h1>Staff & Volunteer Assignments</h1>
          <p class="header-sub">Delegate door scanning, greeting, AV, and setup roles</p>
        </div>
      </div>

      <!-- Assign Staff Form Card -->
      <div class="card assign-card">
        <h3>Assign Member to Role</h3>
        <form (ngSubmit)="assignStaff()">
          <div class="form-group">
            <label class="form-label" for="member">Select Group Member</label>
            <select id="member" class="form-select" [(ngModel)]="selectedMemberId" name="member" required>
              <option value="" disabled>Choose an active group member</option>
              @for (m of groupMembers(); track m.memberId) {
                <option [value]="m.memberId">{{ m.displayName }} ({{ m.role }})</option>
              }
            </select>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="role">Staff Role</label>
              <select id="role" class="form-select" [(ngModel)]="selectedRole" name="role" required>
                <option value="SCANNER">Scanner (Door check-in & QR)</option>
                <option value="GREETER">Greeter (Welcome desk & badges)</option>
                <option value="SETUP">Setup (Room layout & chairs)</option>
                <option value="AV">AV (Audio, projector, streaming)</option>
                <option value="SPEAKER_LIAISON">Speaker Liaison</option>
                <option value="CLEANUP">Cleanup</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label" for="notes">Shift Notes (optional)</label>
              <input
                type="text"
                id="notes"
                name="notes"
                class="form-control"
                [(ngModel)]="notes"
                placeholder="e.g. Bring iPad, arrive 30m early"
              />
            </div>
          </div>

          <button
            type="submit"
            class="btn btn-primary"
            [disabled]="!selectedMemberId || assigning()"
          >
            @if (assigning()) {
              <span>Assigning...</span>
            } @else {
              <span>Assign Staff Member</span>
            }
          </button>
        </form>
      </div>

      <!-- Active Staff Assignments List -->
      <div class="card staff-list-card">
        <h3>Current Staff Roster ({{ staffList().length }})</h3>

        @if (staffList().length === 0) {
          <div class="empty-state">
            <span class="empty-icon">👥</span>
            <h4>No staff assigned yet</h4>
            <p>Assign group volunteers to work door check-in or setup.</p>
          </div>
        } @else {
          <div class="staff-grid">
            @for (s of staffList(); track s.id) {
              <div class="staff-item card">
                <div class="staff-header">
                  <div class="staff-avatar">{{ s.member.displayName.charAt(0) }}</div>
                  <div>
                    <strong>{{ s.member.displayName }}</strong>
                    <div><span class="badge badge-info">{{ s.role }}</span></div>
                  </div>
                  <button
                    class="btn btn-sm btn-subtle delete-btn"
                    [disabled]="removingStaffId() === s.id"
                    (click)="removeStaff(s.id, s.member.displayName)"
                  >
                    {{ removingStaffId() === s.id ? 'Removing...' : '✕ Remove' }}
                  </button>
                </div>
                @if (s.notes) {
                  <p class="staff-notes">📌 {{ s.notes }}</p>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .staff-page {
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
      .assign-card {
        margin-bottom: 2rem;
        padding: 1.75rem;
        h3 { margin-bottom: 1.25rem; }
      }
      .staff-list-card {
        padding: 1.75rem;
        h3 { margin-bottom: 1.25rem; }
      }
      .staff-grid {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .staff-item {
        padding: 1.25rem;
        border: 1px solid var(--color-border);
      }
      .staff-header {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .staff-avatar {
        width: 36px;
        height: 36px;
        border-radius: 9999px;
        background: var(--color-primary-subtle);
        color: var(--color-primary);
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
      }
      .delete-btn {
        margin-left: auto;
        color: var(--color-danger);
      }
      .staff-notes {
        font-size: 0.85rem;
        color: var(--color-text-subtle);
        margin: 0.5rem 0 0 0;
      }
    `,
  ],
})
export class StaffManagementComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly groupService = inject(GroupService);
  private readonly toast = inject(ToastService);

  readonly id = input.required<string>();

  readonly event = signal<EventDetail | null>(null);
  readonly staffList = signal<StaffAssignmentSummary[]>([]);
  readonly groupMembers = signal<GroupMemberSummary[]>([]);
  readonly assigning = signal(false);
  readonly removingStaffId = signal<string | null>(null);

  selectedMemberId = '';
  selectedRole: StaffRole = 'SCANNER';
  notes = '';

  ngOnInit() {
    this.loadEventAndStaff();
  }

  loadEventAndStaff() {
    this.eventService.getEvent(this.id()).subscribe({
      next: ev => {
        this.event.set(ev);
        this.groupService.getMembers(ev.group.id).subscribe({
          next: members => this.groupMembers.set(members),
        });
      },
    });

    this.attendanceService.getStaff(this.id()).subscribe({
      next: list => this.staffList.set(list),
    });
  }

  assignStaff() {
    if (!this.selectedMemberId) return;
    this.assigning.set(true);

    this.attendanceService
      .assignStaff(this.id(), {
        memberId: this.selectedMemberId,
        role: this.selectedRole,
        notes: this.notes.trim() || undefined,
      })
      .subscribe({
        next: created => {
          this.assigning.set(false);
          this.staffList.update(list => [...list, created]);
          this.notes = '';
          this.toast.success(`Assigned ${created.member.displayName} as ${created.role}`);
        },
        error: err => {
          this.assigning.set(false);
          this.toast.error(err?.error?.message || 'Failed to assign staff');
        },
      });
  }

  removeStaff(assignmentId: string, memberName?: string) {
    const label = memberName
      ? `Are you sure you want to remove ${memberName} from event staff?`
      : 'Are you sure you want to remove this staff assignment?';
    if (!confirm(label)) return;

    this.removingStaffId.set(assignmentId);
    this.attendanceService.removeStaff(this.id(), assignmentId).subscribe({
      next: () => {
        this.removingStaffId.set(null);
        this.staffList.update(list => list.filter(s => s.id !== assignmentId));
        this.toast.info('Staff role assignment removed');
      },
      error: err => {
        this.removingStaffId.set(null);
        this.toast.error(err?.error?.message || 'Could not remove staff assignment');
      },
    });
  }
}
