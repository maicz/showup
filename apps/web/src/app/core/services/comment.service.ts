import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CommentSummary, CreateCommentRequest } from '../models/comment.model';

@Injectable({
  providedIn: 'root',
})
export class CommentService {
  private readonly http = inject(HttpClient);

  getComments(eventId: string): Observable<CommentSummary[]> {
    return this.http.get<CommentSummary[]>(`/api/events/${eventId}/comments`);
  }

  addComment(eventId: string, payload: CreateCommentRequest): Observable<CommentSummary> {
    return this.http.post<CommentSummary>(`/api/events/${eventId}/comments`, payload);
  }

  deleteComment(eventId: string, commentId: string): Observable<void> {
    return this.http.delete<void>(`/api/events/${eventId}/comments/${commentId}`);
  }
}
