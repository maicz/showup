import { GeoPoint } from './venue.model';

export interface MemberSummary {
  id: string;
  displayName: string;
  photoUrl?: string;
  bio?: string;
  homeCity?: string;
  memberSince?: string;
}

export interface MemberProfile {
  id: string;
  email: string;
  displayName: string;
  bio?: string;
  photoUrl?: string;
  homeCity?: string;
  homeCountry?: string;
  homeLocation?: GeoPoint;
  emailVerified: boolean;
  memberSince: string;
}

export interface UpdateProfileRequest {
  displayName: string;
  bio?: string;
  photoUrl?: string;
  homeCity?: string;
  homeCountry?: string;
  homeLocation?: GeoPoint;
}

export interface UpdateInterestsRequest {
  topicIds: string[];
}
