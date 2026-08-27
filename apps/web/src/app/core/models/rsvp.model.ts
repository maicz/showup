import { MemberSummary } from './member.model';

export type RsvpStatus = 'YES' | 'NO' | 'WAITLISTED';

export interface RsvpSummary {
  id: string;
  status: RsvpStatus;
  guestCount: number;
  waitlistPosition?: number;
  respondedAt: string;
}

export interface SubmitRsvpRequest {
  status: 'YES' | 'NO';
  guestCount: number;
}

export interface AttendeeSummary {
  member: MemberSummary;
  status: RsvpStatus;
  guestCount: number;
  waitlistPosition?: number;
  respondedAt: string;
  checkedIn: boolean;
}
