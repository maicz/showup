import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CategorySummary, TopicSummary } from '../models/topic.model';

@Injectable({
  providedIn: 'root',
})
export class TopicService {
  private readonly http = inject(HttpClient);

  getCategories(): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>('/api/categories');
  }

  getTopics(categorySlug?: string): Observable<TopicSummary[]> {
    let params = new HttpParams();
    if (categorySlug) {
      params = params.set('categorySlug', categorySlug);
    }
    return this.http.get<TopicSummary[]>('/api/topics', { params });
  }
}
