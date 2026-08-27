import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { EventService } from '../../../core/services/event.service';
import { TopicService } from '../../../core/services/topic.service';
import { EventListComponent } from './event-list.component';

describe('EventListComponent', () => {
  let component: EventListComponent;
  let fixture: ComponentFixture<EventListComponent>;
  let eventServiceSpy: any;
  let topicServiceSpy: any;

  beforeEach(async () => {
    eventServiceSpy = {
      searchEvents: () =>
        of({
          content: [
            {
              id: 'e1',
              title: 'Java 25 Features Meetup',
              startsAt: '2026-09-10T17:00:00Z',
              timeZone: 'Europe/Bucharest',
              format: 'IN_PERSON',
              yesRsvpCount: 20,
              availability: 'SEATS_AVAILABLE',
              group: { id: 'g1', name: 'Java Community', urlname: 'java-community', category: { name: 'Tech' } },
            },
          ],
          page: 0,
          size: 9,
          totalElements: 1,
          totalPages: 1,
          last: true,
        }),
      getEvents: () => of([]),
    };

    topicServiceSpy = {
      getCategories: () => of([{ id: 'c1', slug: 'tech', name: 'Technology', displayOrder: 1 }]),
      getTopics: () => of([]),
    };

    await TestBed.configureTestingModule({
      imports: [EventListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: EventService, useValue: eventServiceSpy },
        { provide: TopicService, useValue: topicServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EventListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial events', () => {
    expect(component).toBeTruthy();
    expect(component.events().length).toBe(1);
    expect(component.events()[0].title).toBe('Java 25 Features Meetup');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.event-title')?.textContent).toContain('Java 25 Features Meetup');
  });

  it('should format availability correctly', () => {
    expect(component.formatAvailability('SEATS_AVAILABLE')).toBe('Seats available');
    expect(component.formatAvailability('WAITLIST')).toBe('Waitlist open');
    expect(component.formatAvailability('FULL')).toBe('Fully booked');
  });

  it('should expose an accessible event link and result count', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const eventLink = compiled.querySelector<HTMLAnchorElement>('.event-card');

    expect(eventLink?.getAttribute('aria-label')).toBe('View Java 25 Features Meetup');
    expect(compiled.querySelector('.results-toolbar')?.textContent).toContain('1 event found');
  });
});
