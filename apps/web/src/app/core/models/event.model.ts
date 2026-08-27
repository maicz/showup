import { GroupSummary } from './group.model';
import { MemberSummary } from './member.model';
import { VenueSummary } from './venue.model';
import { Money } from './common.model';
import { RsvpSummary } from './rsvp.model';

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED';
export type EventFormat = 'IN_PERSON' | 'ONLINE' | 'HYBRID';
export type AvailabilityState = 'SEATS_AVAILABLE' | 'WAITLIST' | 'FULL';
export type EventHostRole = 'HOST' | 'CO_HOST';

export interface EventSummary {
  id: string;
  title: string;
  startsAt: string;
  timeZone: string;
  format: EventFormat;
  venueCity?: string;
  fee?: Money;
  yesRsvpCount: number;
  capacity?: number;
  availability: AvailabilityState;
  group: GroupSummary;
}

export interface MemberEventSummary extends EventSummary {
  rsvpStatus: 'YES' | 'WAITLISTED';
  guestCount: number;
  waitlistPosition?: number;
}

export interface EventDetail {
  id: string;
  title: string;
  description: string;
  status: EventStatus;
  format: EventFormat;
  venue?: VenueSummary;
  onlineUrl?: string;
  startsAt: string;
  endsAt?: string;
  timeZone: string;
  capacity?: number;
  waitlistEnabled: boolean;
  guestsPerRsvpLimit: number;
  fee?: Money;
  rsvpOpensAt?: string;
  rsvpClosesAt?: string;
  yesRsvpCount: number;
  waitlistCount: number;
  availability: AvailabilityState;
  group: GroupSummary;
  hosts: MemberSummary[];
  viewerRsvp?: RsvpSummary;
  cancelledAt?: string;
  cancellationReason?: string;
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  format: EventFormat;
  venueId?: string;
  onlineUrl?: string;
  startsAt: string;
  endsAt?: string;
  timeZone: string;
  capacity?: number;
  waitlistEnabled: boolean;
  guestsPerRsvpLimit: number;
  feeAmountMinor?: number;
  feeCurrency?: string;
  rsvpOpensAt?: string;
  rsvpClosesAt?: string;
}

export interface UpdateEventRequest {
  title: string;
  description?: string;
  format: EventFormat;
  venueId?: string;
  onlineUrl?: string;
  startsAt: string;
  endsAt?: string;
  timeZone: string;
  capacity?: number;
  waitlistEnabled: boolean;
  guestsPerRsvpLimit: number;
  feeAmountMinor?: number;
  feeCurrency?: string;
  rsvpOpensAt?: string;
  rsvpClosesAt?: string;
}

export interface CancelEventRequest {
  reason: string;
}

export interface EventSearchQuery {
  lat?: number;
  lon?: number;
  radiusKm?: number;
  categorySlug?: string;
  topicSlugs?: string[];
  format?: EventFormat;
  dateFrom?: string;
  dateTo?: string;
  maxFee?: number;
  availability?: AvailabilityState;
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface EventSeriesDetail {
  id: string;
  recurrenceRule: string;
  until?: string;
  templateTitle: string;
  templateDescription?: string;
  templateVenue?: VenueSummary;
  templateDurationMinutes?: number;
}

export interface CreateEventSeriesRequest {
  groupId: string;
  recurrenceRule: string;
  until?: string;
  templateTitle: string;
  templateDescription?: string;
  templateVenueId?: string;
  templateDurationMinutes?: number;
}
