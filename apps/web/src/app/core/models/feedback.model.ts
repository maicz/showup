import { MemberSummary } from './member.model';

export interface EventFeedbackSummary {
  id: string;
  member: MemberSummary;
  rating: number;
  comment?: string;
  submittedAt: string;
}

export interface SubmitFeedbackRequest {
  rating: number;
  comment?: string;
}
