import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EventFeedbackSummary, SubmitFeedbackRequest } from '../models/feedback.model';

@Injectable({
  providedIn: 'root',
})
export class FeedbackService {
  private readonly http = inject(HttpClient);

  submitFeedback(eventId: string, payload: SubmitFeedbackRequest): Observable<EventFeedbackSummary> {
    return this.http.post<EventFeedbackSummary>(`/api/events/${eventId}/feedback`, payload);
  }

  getFeedback(eventId: string): Observable<EventFeedbackSummary[]> {
    return this.http.get<EventFeedbackSummary[]>(`/api/events/${eventId}/feedback`);
  }
}
