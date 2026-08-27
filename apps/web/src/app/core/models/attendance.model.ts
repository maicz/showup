import { MemberSummary } from './member.model';

export type StaffRole =
  | 'GREETER'
  | 'SCANNER'
  | 'SETUP'
  | 'AV'
  | 'SPEAKER_LIAISON'
  | 'CLEANUP';

export type CheckInMethod = 'QR_SCAN' | 'MANUAL';

export interface TicketResponse {
  id: string;
  code: string;
  issuedAt: string;
  admitCount: number;
  checkedIn: boolean;
}

export interface CheckInRequest {
  ticketCode: string;
  method: CheckInMethod;
  admittedCount: number;
}

export interface CheckInResponse {
  id: string;
  ticketCode: string;
  checkedInAt: string;
  admittedCount: number;
  method: CheckInMethod;
  alreadyCheckedIn: boolean;
  attendeeName: string;
}

export interface StaffAssignmentSummary {
  id: string;
  member: MemberSummary;
  role: StaffRole;
  shiftStartsAt?: string;
  shiftEndsAt?: string;
  notes?: string;
}

export interface AssignStaffRequest {
  memberId: string;
  role: StaffRole;
  shiftStartsAt?: string;
  shiftEndsAt?: string;
  notes?: string;
}
