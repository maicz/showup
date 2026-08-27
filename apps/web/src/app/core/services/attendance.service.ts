import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AssignStaffRequest,
  CheckInRequest,
  CheckInResponse,
  StaffAssignmentSummary,
  TicketResponse,
} from '../models/attendance.model';

@Injectable({
  providedIn: 'root',
})
export class AttendanceService {
  private readonly http = inject(HttpClient);

  getTicket(eventId: string): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`/api/events/${eventId}/ticket`);
  }

  issueTicket(eventId: string): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(`/api/events/${eventId}/ticket`, {});
  }

  checkIn(eventId: string, payload: CheckInRequest): Observable<CheckInResponse> {
    return this.http.post<CheckInResponse>(`/api/events/${eventId}/check-ins`, payload);
  }

  checkInByMember(eventId: string, memberId: string): Observable<CheckInResponse> {
    return this.http.post<CheckInResponse>(`/api/events/${eventId}/check-ins/by-member/${memberId}`, {});
  }

  revokeTicket(eventId: string, ticketId: string): Observable<void> {
    return this.http.post<void>(`/api/events/${eventId}/tickets/${ticketId}/revoke`, {});
  }

  getStaff(eventId: string): Observable<StaffAssignmentSummary[]> {
    return this.http.get<StaffAssignmentSummary[]>(`/api/events/${eventId}/staff`);
  }

  assignStaff(eventId: string, payload: AssignStaffRequest): Observable<StaffAssignmentSummary> {
    return this.http.post<StaffAssignmentSummary>(`/api/events/${eventId}/staff`, payload);
  }

  removeStaff(eventId: string, assignmentId: string): Observable<void> {
    return this.http.delete<void>(`/api/events/${eventId}/staff/${assignmentId}`);
  }
}
