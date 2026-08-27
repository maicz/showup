import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AiDraftEventRequest,
  AiDraftEventResponse,
  AiFeedbackSummaryResponse,
  AiRecommendationsResponse,
} from '../models/ai.model';

@Injectable({
  providedIn: 'root',
})
export class AiService {
  private readonly http = inject(HttpClient);

  draftEvent(payload: AiDraftEventRequest): Observable<AiDraftEventResponse> {
    return this.http.post<AiDraftEventResponse>('/api/ai/copilot/draft-event', payload);
  }

  summarizeFeedback(eventId: string): Observable<AiFeedbackSummaryResponse> {
    return this.http.get<AiFeedbackSummaryResponse>(`/api/ai/feedback/${eventId}/summary`);
  }

  getRecommendations(): Observable<AiRecommendationsResponse> {
    return this.http.get<AiRecommendationsResponse>('/api/ai/recommendations');
  }
}
