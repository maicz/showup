import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AttendeeSummary, RsvpSummary, SubmitRsvpRequest } from '../models/rsvp.model';

@Injectable({
  providedIn: 'root',
})
export class RsvpService {
  private readonly http = inject(HttpClient);

  submitRsvp(eventId: string, payload: SubmitRsvpRequest): Observable<RsvpSummary> {
    return this.http.put<RsvpSummary>(`/api/events/${eventId}/rsvp`, payload);
  }

  getMyRsvp(eventId: string): Observable<RsvpSummary> {
    return this.http.get<RsvpSummary>(`/api/events/${eventId}/rsvp`);
  }

  cancelRsvp(eventId: string): Observable<void> {
    return this.http.delete<void>(`/api/events/${eventId}/rsvp`);
  }

  getAttendees(eventId: string, status?: string): Observable<AttendeeSummary[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<AttendeeSummary[]>(`/api/events/${eventId}/attendees`, { params });
  }
}
