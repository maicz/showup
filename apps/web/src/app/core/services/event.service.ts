import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/common.model';
import {
  CancelEventRequest,
  CreateEventRequest,
  CreateEventSeriesRequest,
  EventDetail,
  EventSearchQuery,
  EventSeriesDetail,
  EventSummary,
  UpdateEventRequest,
} from '../models/event.model';
import { MemberSummary } from '../models/member.model';

@Injectable({
  providedIn: 'root',
})
export class EventService {
  private readonly http = inject(HttpClient);

  getEvents(): Observable<EventSummary[]> {
    return this.http.get<EventSummary[]>('/api/events');
  }

  searchEvents(query: EventSearchQuery): Observable<PageResponse<EventSummary>> {
    let params = new HttpParams();
    if (query.lat !== undefined) params = params.set('lat', query.lat.toString());
    if (query.lon !== undefined) params = params.set('lon', query.lon.toString());
    if (query.radiusKm !== undefined) params = params.set('radiusKm', query.radiusKm.toString());
    if (query.categorySlug) params = params.set('categorySlug', query.categorySlug);
    if (query.topicSlugs && query.topicSlugs.length > 0) {
      params = params.set('topicSlugs', query.topicSlugs.join(','));
    }
    if (query.format) params = params.set('format', query.format);
    if (query.dateFrom) params = params.set('dateFrom', query.dateFrom);
    if (query.dateTo) params = params.set('dateTo', query.dateTo);
    if (query.maxFee !== undefined) params = params.set('maxFee', query.maxFee.toString());
    if (query.availability) params = params.set('availability', query.availability);
    if (query.query) params = params.set('query', query.query);
    if (query.page !== undefined) params = params.set('page', query.page.toString());
    if (query.size !== undefined) params = params.set('size', query.size.toString());
    if (query.sort) params = params.set('sort', query.sort);

    return this.http.get<PageResponse<EventSummary>>('/api/events/search', { params });
  }

  getEvent(id: string): Observable<EventDetail> {
    return this.http.get<EventDetail>(`/api/events/${id}`);
  }

  createEvent(groupId: string, payload: CreateEventRequest): Observable<EventDetail> {
    return this.http.post<EventDetail>(`/api/groups/${groupId}/events`, payload);
  }

  updateEvent(id: string, payload: UpdateEventRequest): Observable<EventDetail> {
    return this.http.put<EventDetail>(`/api/events/${id}`, payload);
  }

  publishEvent(id: string): Observable<EventDetail> {
    return this.http.post<EventDetail>(`/api/events/${id}/publish`, {});
  }

  cancelEvent(id: string, payload: CancelEventRequest): Observable<void> {
    return this.http.post<void>(`/api/events/${id}/cancel`, payload);
  }

  addHost(id: string, memberId: string): Observable<void> {
    return this.http.post<void>(`/api/events/${id}/hosts`, { memberId });
  }

  removeHost(id: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`/api/events/${id}/hosts/${memberId}`);
  }

  createEventSeries(payload: CreateEventSeriesRequest): Observable<EventSeriesDetail> {
    return this.http.post<EventSeriesDetail>('/api/event-series', payload);
  }

  getEventSeries(id: string): Observable<EventSeriesDetail> {
    return this.http.get<EventSeriesDetail>(`/api/event-series/${id}`);
  }

  getGroupSeries(groupId: string): Observable<EventSeriesDetail[]> {
    return this.http.get<EventSeriesDetail[]>(`/api/groups/${groupId}/event-series`);
  }

  generateSeriesOccurrences(seriesId: string, firstStartsAt: string, count = 5): Observable<EventSummary[]> {
    const params = new HttpParams().set('firstStartsAt', firstStartsAt).set('count', count.toString());
    return this.http.post<EventSummary[]>(`/api/event-series/${seriesId}/occurrences`, {}, { params });
  }
}
