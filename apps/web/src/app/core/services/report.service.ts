import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EventAttendanceReport, GroupActivityReport } from '../models/report.model';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly http = inject(HttpClient);

  getEventAttendanceReport(eventId: string): Observable<EventAttendanceReport> {
    return this.http.get<EventAttendanceReport>(`/api/events/${eventId}/reports/attendance`);
  }

  getGroupActivityReport(groupId: string, from?: string, to?: string): Observable<GroupActivityReport> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<GroupActivityReport>(`/api/groups/${groupId}/reports/activity`, { params });
  }
}
