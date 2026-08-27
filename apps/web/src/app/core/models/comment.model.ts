import { MemberSummary } from './member.model';

export interface CommentSummary {
  id: string;
  author: MemberSummary;
  body: string;
  parentCommentId?: string;
  createdAt: string;
  deletedAt?: string;
}

export interface CreateCommentRequest {
  body: string;
  parentCommentId?: string;
}
