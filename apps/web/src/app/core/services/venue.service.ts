import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateVenueRequest, VenueSummary } from '../models/venue.model';

@Injectable({
  providedIn: 'root',
})
export class VenueService {
  private readonly http = inject(HttpClient);

  getVenuesForGroup(groupId: string): Observable<VenueSummary[]> {
    return this.http.get<VenueSummary[]>(`/api/groups/${groupId}/venues`);
  }

  createVenue(groupId: string, payload: CreateVenueRequest): Observable<VenueSummary> {
    return this.http.post<VenueSummary>(`/api/groups/${groupId}/venues`, payload);
  }

  getVenue(venueId: string): Observable<VenueSummary> {
    return this.http.get<VenueSummary>(`/api/venues/${venueId}`);
  }
}
