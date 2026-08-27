import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EventService } from './event.service';

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EventService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getEvents should make a GET request to /api/events', () => {
    service.getEvents().subscribe(events => {
      expect(events.length).toBe(1);
      expect(events[0].title).toBe('Spring Boot Meetup');
    });

    const req = httpMock.expectOne('/api/events');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: '1',
        title: 'Spring Boot Meetup',
        startsAt: '2026-09-01T18:00:00Z',
        timeZone: 'Europe/Bucharest',
        format: 'IN_PERSON',
        yesRsvpCount: 15,
        availability: 'SEATS_AVAILABLE',
        group: { id: 'g1', name: 'Java Group', urlname: 'java-group' },
      },
    ]);
  });

  it('searchEvents should pass filter query parameters', () => {
    service
      .searchEvents({
        query: 'angular',
        categorySlug: 'tech',
        format: 'ONLINE',
        availability: 'SEATS_AVAILABLE',
        page: 2,
        size: 9,
        sort: 'popularity',
      })
      .subscribe();

    const req = httpMock.expectOne(
      req =>
        req.url === '/api/events/search' &&
        req.params.get('categorySlug') === 'tech' &&
        req.params.get('query') === 'angular' &&
        req.params.get('format') === 'ONLINE' &&
        req.params.get('availability') === 'SEATS_AVAILABLE' &&
        req.params.get('page') === '2' &&
        req.params.get('size') === '9' &&
        req.params.get('sort') === 'popularity'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });
});
