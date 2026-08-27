import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PhotoSummary, UploadPhotoRequest } from '../models/photo.model';

@Injectable({
  providedIn: 'root',
})
export class PhotoService {
  private readonly http = inject(HttpClient);

  getPhotos(eventId: string): Observable<PhotoSummary[]> {
    return this.http.get<PhotoSummary[]>(`/api/events/${eventId}/photos`);
  }

  uploadPhoto(eventId: string, payload: UploadPhotoRequest): Observable<PhotoSummary> {
    return this.http.post<PhotoSummary>(`/api/events/${eventId}/photos`, payload);
  }

  deletePhoto(eventId: string, photoId: string): Observable<void> {
    return this.http.delete<void>(`/api/events/${eventId}/photos/${photoId}`);
  }
}
