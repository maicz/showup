import { CategorySummary, TopicSummary } from './topic.model';
import { GeoPoint } from './venue.model';
import { MemberSummary } from './member.model';

export type GroupVisibility = 'PUBLIC' | 'PRIVATE';
export type GroupJoinPolicy = 'OPEN' | 'APPROVAL_REQUIRED' | 'INVITE_ONLY';
export type GroupStatus = 'ACTIVE' | 'ARCHIVED';
export type GroupMemberRole =
  | 'ORGANIZER'
  | 'CO_ORGANIZER'
  | 'ASSISTANT_ORGANIZER'
  | 'EVENT_ORGANIZER'
  | 'MEMBER';
export type GroupMembershipStatus =
  | 'ACTIVE'
  | 'PENDING_APPROVAL'
  | 'BANNED'
  | 'LEFT';

export interface GroupSummary {
  id: string;
  urlname: string;
  name: string;
  city?: string;
  country?: string;
  memberCount: number;
  ratingAverage?: number;
  ratingCount: number;
  category: CategorySummary;
}

export interface GroupSearchQuery {
  query?: string;
  categorySlug?: string;
  city?: string;
  sort?: 'popular' | 'rating' | 'newest' | 'name';
  page?: number;
  size?: number;
}

export interface GroupDetail {
  id: string;
  urlname: string;
  name: string;
  description: string;
  city?: string;
  country?: string;
  location?: GeoPoint;
  timeZone: string;
  visibility: GroupVisibility;
  joinPolicy: GroupJoinPolicy;
  memberCount: number;
  ratingAverage?: number;
  ratingCount: number;
  foundedAt: string;
  status: GroupStatus;
  category: CategorySummary;
  topics: TopicSummary[];
  organizer?: MemberSummary;
  viewerRole?: GroupMemberRole;
  viewerStatus?: GroupMembershipStatus;
}

export interface CreateGroupRequest {
  name: string;
  urlname: string;
  description: string;
  categoryId: string;
  city?: string;
  country?: string;
  location?: GeoPoint;
  timeZone: string;
  visibility: GroupVisibility;
  joinPolicy: GroupJoinPolicy;
  topicIds?: string[];
}

export interface UpdateGroupRequest {
  name: string;
  description: string;
  categoryId: string;
  city?: string;
  country?: string;
  location?: GeoPoint;
  timeZone: string;
  visibility: GroupVisibility;
  joinPolicy: GroupJoinPolicy;
  topicIds?: string[];
}

export interface JoinGroupRequest {
  introduction?: string;
}

export interface GroupMemberSummary {
  memberId: string;
  displayName: string;
  photoUrl?: string;
  role: GroupMemberRole;
  status: GroupMembershipStatus;
  joinedAt: string;
}

export interface UpdateMemberRoleRequest {
  role: GroupMemberRole;
}
