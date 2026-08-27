import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MemberEventSummary } from '../models/event.model';
import { GroupSummary } from '../models/group.model';
import {
  MemberProfile,
  MemberSummary,
  UpdateInterestsRequest,
  UpdateProfileRequest,
} from '../models/member.model';
import { TopicSummary } from '../models/topic.model';

@Injectable({
  providedIn: 'root',
})
export class MemberService {
  private readonly http = inject(HttpClient);

  getProfile(): Observable<MemberProfile> {
    return this.http.get<MemberProfile>('/api/members/me');
  }

  updateProfile(payload: UpdateProfileRequest): Observable<MemberProfile> {
    return this.http.put<MemberProfile>('/api/members/me', payload);
  }

  getMember(id: string): Observable<MemberSummary> {
    return this.http.get<MemberSummary>(`/api/members/${id}`);
  }

  getMyInterests(): Observable<TopicSummary[]> {
    return this.http.get<TopicSummary[]>('/api/members/me/interests');
  }

  updateMyInterests(payload: UpdateInterestsRequest): Observable<TopicSummary[]> {
    return this.http.put<TopicSummary[]>('/api/members/me/interests', payload);
  }

  getMyGroups(): Observable<GroupSummary[]> {
    return this.http.get<GroupSummary[]>('/api/members/me/groups');
  }

  getMyEvents(): Observable<MemberEventSummary[]> {
    return this.http.get<MemberEventSummary[]>('/api/members/me/events');
  }
}
