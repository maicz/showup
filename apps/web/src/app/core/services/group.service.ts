import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/common.model';
import { EventSummary } from '../models/event.model';
import {
  CreateGroupRequest,
  GroupDetail,
  GroupMemberSummary,
  GroupSearchQuery,
  GroupSummary,
  JoinGroupRequest,
  UpdateGroupRequest,
  UpdateMemberRoleRequest,
} from '../models/group.model';

@Injectable({
  providedIn: 'root',
})
export class GroupService {
  private readonly http = inject(HttpClient);

  getGroups(query: GroupSearchQuery = {}): Observable<PageResponse<GroupSummary>> {
    let params = new HttpParams()
      .set('page', (query.page ?? 0).toString())
      .set('size', (query.size ?? 12).toString());

    if (query.query?.trim()) {
      params = params.set('query', query.query.trim());
    }
    if (query.categorySlug) {
      params = params.set('categorySlug', query.categorySlug);
    }
    if (query.city?.trim()) {
      params = params.set('city', query.city.trim());
    }
    if (query.sort) {
      params = params.set('sort', query.sort);
    }

    return this.http.get<PageResponse<GroupSummary>>('/api/groups', {
      params,
    });
  }

  getGroup(id: string): Observable<GroupDetail> {
    return this.http.get<GroupDetail>(`/api/groups/${id}`);
  }

  getGroupByUrlname(urlname: string): Observable<GroupDetail> {
    return this.http.get<GroupDetail>(`/api/groups/by-urlname/${urlname}`);
  }

  createGroup(payload: CreateGroupRequest): Observable<GroupDetail> {
    return this.http.post<GroupDetail>('/api/groups', payload);
  }

  updateGroup(id: string, payload: UpdateGroupRequest): Observable<GroupDetail> {
    return this.http.put<GroupDetail>(`/api/groups/${id}`, payload);
  }

  getGroupEvents(groupId: string): Observable<EventSummary[]> {
    return this.http.get<EventSummary[]>(`/api/groups/${groupId}/events`);
  }

  joinGroup(groupId: string, payload: JoinGroupRequest = {}): Observable<GroupMemberSummary> {
    return this.http.post<GroupMemberSummary>(`/api/groups/${groupId}/members`, payload);
  }

  leaveGroup(groupId: string): Observable<void> {
    return this.http.delete<void>(`/api/groups/${groupId}/members/me`);
  }

  getMembers(groupId: string, status?: string): Observable<GroupMemberSummary[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<GroupMemberSummary[]>(`/api/groups/${groupId}/members`, { params });
  }

  updateMemberRole(groupId: string, memberId: string, payload: UpdateMemberRoleRequest): Observable<void> {
    return this.http.put<void>(`/api/groups/${groupId}/members/${memberId}/role`, payload);
  }

  approveMember(groupId: string, memberId: string): Observable<void> {
    return this.http.post<void>(`/api/groups/${groupId}/members/${memberId}/approve`, {});
  }
}
