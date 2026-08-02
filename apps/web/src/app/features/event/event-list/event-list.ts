import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith } from 'rxjs';

import { EventService } from '../event.service';
import { Event } from '../event.model';

type EventListState =
  | { status: 'loading' }
  | { status: 'loaded'; events: Event[] }
  | { status: 'error' };

@Component({
  selector: 'app-event-list',
  imports: [DatePipe],
  templateUrl: './event-list.html',
  styleUrl: './event-list.scss',
})
export class EventList {
  private readonly eventService = inject(EventService);

  private readonly state = toSignal(
    this.eventService.list().pipe(
      map((events): EventListState => ({ status: 'loaded', events })),
      catchError(() => of<EventListState>({ status: 'error' })),
      startWith<EventListState>({ status: 'loading' }),
    ),
    { initialValue: { status: 'loading' } as EventListState },
  );

  protected readonly status = computed(() => this.state().status);

  protected readonly events = computed(() => {
    const state = this.state();
    return state.status === 'loaded' ? state.events : [];
  });
}
