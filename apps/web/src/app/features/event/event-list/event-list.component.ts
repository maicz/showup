import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AvailabilityState, EventFormat, EventSummary } from '../../../core/models/event.model';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { TopicService } from '../../../core/services/topic.service';
import { CategorySummary, TopicSummary } from '../../../core/models/topic.model';

type DateWindow = '' | 'week' | 'month';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  template: `
    <section class="discover-hero">
      <div class="container hero-inner">
        <div class="hero-copy">
          <span class="eyebrow">Meet people. Learn something. Belong somewhere.</span>
          <h1>Find your next reason to <span>show up.</span></h1>
          <p>Discover hands-on workshops, local communities, and memorable gatherings—online or around the corner.</p>
          <div class="hero-proof" aria-label="ShowUp highlights">
            <span><b>✓</b> Smart waitlists</span>
            <span><b>✓</b> QR check-in</span>
            <span><b>✓</b> Community-led</span>
          </div>
        </div>
        <div class="hero-action">
          @if (auth.isAuthenticated()) {
            <a routerLink="/events/create" class="btn btn-primary btn-lg">Create an event <span aria-hidden="true">→</span></a>
            <span>Bring your community together.</span>
          } @else {
            <a routerLink="/register" class="btn btn-primary btn-lg">Join ShowUp <span aria-hidden="true">→</span></a>
            <span>Free to join. Find your people.</span>
          }
        </div>
      </div>
    </section>

    <div class="container events-page">
      <section class="search-panel" aria-labelledby="discover-heading">
        <div class="search-heading-row">
          <div>
            <span class="section-kicker">Explore what is happening</span>
            <h2 id="discover-heading">Upcoming events</h2>
          </div>
          @if (auth.isAuthenticated()) {
            <a routerLink="/events/create" class="btn btn-outline create-event-btn">+ Create event</a>
          }
        </div>

        <div class="primary-search">
          <label class="sr-only" for="event-search">Search by event or group name</label>
          <span class="search-icon" aria-hidden="true">⌕</span>
          <input
            id="event-search"
            type="search"
            class="form-control"
            placeholder="Search events or groups"
            [(ngModel)]="searchKeyword"
            (keyup.enter)="applyFilters()"
          />
          <button class="btn btn-primary search-button" type="button" (click)="applyFilters()">Search</button>
        </div>

        <div class="quick-filters" aria-label="Quick filters">
          <button type="button" class="quick-filter" [class.active]="selectedDateWindow === 'week'" (click)="selectDateWindow('week')">
            This week
          </button>
          <button type="button" class="quick-filter" [class.active]="selectedDateWindow === 'month'" (click)="selectDateWindow('month')">
            Next 30 days
          </button>
          <button type="button" class="quick-filter" [class.active]="freeOnly" (click)="toggleFreeOnly()">
            Free events
          </button>
        </div>

        <div class="filters-grid">
          <div class="filter-item">
            <label class="filter-label" for="category-filter">Category</label>
            <select id="category-filter" class="form-select" [(ngModel)]="selectedCategorySlug" (change)="onCategoryChange()">
              <option value="">All categories</option>
              @for (cat of categories(); track cat.id) {
                <option [value]="cat.slug">{{ cat.name }}</option>
              }
            </select>
          </div>

          <div class="filter-item">
            <label class="filter-label" for="format-filter">Format</label>
            <select id="format-filter" class="form-select" [(ngModel)]="selectedFormat" (change)="applyFilters()">
              <option value="">Any format</option>
              <option value="IN_PERSON">In person</option>
              <option value="ONLINE">Online</option>
              <option value="HYBRID">Hybrid</option>
            </select>
          </div>

          <div class="filter-item">
            <label class="filter-label" for="availability-filter">Availability</label>
            <select id="availability-filter" class="form-select" [(ngModel)]="selectedAvailability" (change)="applyFilters()">
              <option value="">Any availability</option>
              <option value="SEATS_AVAILABLE">Seats available</option>
              <option value="WAITLIST">Waitlist open</option>
              <option value="FULL">Fully booked</option>
            </select>
          </div>
        </div>

        @if (topics().length > 0) {
          <div class="topics-row">
            <span>Topics</span>
            <div class="chips-scroll">
              @for (topic of topics(); track topic.id) {
                <button
                  type="button"
                  class="chip"
                  [class.active]="selectedTopicSlug === topic.slug"
                  [attr.aria-pressed]="selectedTopicSlug === topic.slug"
                  (click)="selectTopic(topic.slug)"
                >
                  {{ topic.name }}
                </button>
              }
            </div>
          </div>
        }

        @if (hasActiveFilters()) {
          <button class="clear-filters" type="button" (click)="resetFilters()">Clear all filters</button>
        }
      </section>

      <div class="results-toolbar">
        <div aria-live="polite">
          @if (!loading() && !errorMessage()) {
            <strong>{{ totalElements() }}</strong> {{ totalElements() === 1 ? 'event' : 'events' }} found
          } @else {
            <span>Finding events…</span>
          }
        </div>
        <label class="sort-control">
          <span>Sort by</span>
          <select class="form-select" [(ngModel)]="selectedSort" (change)="applyFilters()">
            <option value="startsAt">Soonest first</option>
            <option value="popularity">Most popular</option>
            <option value="newest">Recently added</option>
            <option value="fee">Lowest price</option>
          </select>
        </label>
      </div>

      @if (loading()) {
        <div class="events-grid" aria-busy="true" aria-label="Loading events">
          @for (placeholder of [1, 2, 3, 4, 5, 6]; track placeholder) {
            <div class="card event-card skeleton-card" aria-hidden="true">
              <div class="skeleton short"></div>
              <div class="skeleton title"></div>
              <div class="skeleton medium"></div>
              <div class="skeleton footer"></div>
            </div>
          }
        </div>
      } @else if (errorMessage()) {
        <div class="state-panel error-state" role="alert">
          <span class="state-icon" aria-hidden="true">!</span>
          <div>
            <h3>We could not load events</h3>
            <p>{{ errorMessage() }}</p>
          </div>
          <button class="btn btn-secondary" type="button" (click)="applyFilters(false)">Try again</button>
        </div>
      } @else if (events().length === 0) {
        <div class="state-panel empty-results">
          <span class="state-icon" aria-hidden="true">⌕</span>
          <div>
            <h3>{{ hasActiveFilters() ? 'No exact matches—yet' : 'The calendar is open' }}</h3>
            <p>
              {{ hasActiveFilters()
                ? 'Try a broader search or remove a filter to see more events.'
                : 'Be the first to bring people together with a new event.' }}
            </p>
          </div>
          @if (hasActiveFilters()) {
            <button class="btn btn-secondary" type="button" (click)="resetFilters()">Clear filters</button>
          } @else {
            <a [routerLink]="auth.isAuthenticated() ? '/events/create' : '/register'" class="btn btn-primary">Create the first event</a>
          }
        </div>
      } @else {
        <div class="events-grid">
          @for (event of events(); track event.id) {
            <a class="card event-card card-interactive" [routerLink]="['/events', event.id]" [attr.aria-label]="'View ' + event.title">
              <div class="event-card-header">
                <span class="format-badge">{{ formatEventFormat(event.format) }}</span>
                <span
                  class="availability-dot"
                  [class.waitlist]="event.availability === 'WAITLIST'"
                  [class.full]="event.availability === 'FULL'"
                >
                  {{ formatAvailability(event.availability) }}
                </span>
              </div>

              <div class="event-card-body">
                <time class="event-time" [attr.datetime]="event.startsAt">
                  {{ event.startsAt | date: 'EEE, MMM d' }} <span>•</span> {{ event.startsAt | date: 'h:mm a' }}
                </time>
                <h3 class="event-title">{{ event.title }}</h3>
                <p class="group-name">Hosted by {{ event.group.name }}</p>
                <p class="event-location">
                  <span aria-hidden="true">{{ event.format === 'ONLINE' ? '◉' : '⌖' }}</span>
                  {{ event.format === 'ONLINE' ? 'Online event' : (event.venueCity || 'Location shared after RSVP') }}
                </p>
              </div>

              <div class="event-card-footer">
                <span class="attendees-count">
                  {{ event.yesRsvpCount }} going
                  @if (event.capacity) {
                    <span class="capacity-total"> · {{ event.capacity - event.yesRsvpCount > 0 ? event.capacity - event.yesRsvpCount : 0 }} spots left</span>
                  }
                </span>
                <span class="fee-badge" [class.free]="!event.fee || event.fee.amountMinor === 0">{{ formatFee(event) }}</span>
              </div>
            </a>
          }
        </div>

        @if (totalPages() > 1) {
          <nav class="pagination" aria-label="Event pages">
            <button class="btn btn-secondary" type="button" [disabled]="currentPage() === 0" (click)="goToPage(currentPage() - 1)">
              ← Previous
            </button>
            <span>Page <strong>{{ currentPage() + 1 }}</strong> of {{ totalPages() }}</span>
            <button class="btn btn-secondary" type="button" [disabled]="currentPage() + 1 >= totalPages()" (click)="goToPage(currentPage() + 1)">
              Next →
            </button>
          </nav>
        }
      }
    </div>
  `,
  styles: [
    `
      .discover-hero {
        position: relative;
        overflow: hidden;
        color: #fff;
        background:
          radial-gradient(circle at 86% 20%, rgba(251, 191, 36, 0.26), transparent 30%),
          radial-gradient(circle at 8% 90%, rgba(56, 189, 248, 0.18), transparent 36%),
          linear-gradient(125deg, #111827 0%, #172554 58%, #312e81 100%);
      }
      .discover-hero::after {
        content: '';
        position: absolute;
        width: 300px;
        height: 300px;
        right: 7%;
        bottom: -235px;
        border: 42px solid rgba(255, 255, 255, 0.06);
        border-radius: 50%;
      }
      .hero-inner {
        position: relative;
        z-index: 1;
        display: grid;
        gap: 2rem;
        padding-top: 4.5rem;
        padding-bottom: 4.5rem;
      }
      .hero-copy { max-width: 750px; }
      .eyebrow, .section-kicker {
        display: block;
        margin-bottom: 0.8rem;
        font-size: 0.75rem;
        font-weight: 800;
        letter-spacing: 0.12em;
        text-transform: uppercase;
      }
      .eyebrow { color: #fdba74; }
      .hero-copy h1 {
        max-width: 700px;
        margin-bottom: 1rem;
        color: #fff;
        font-size: clamp(2.5rem, 6vw, 4.75rem);
        line-height: 1.02;
        letter-spacing: -0.055em;
      }
      .hero-copy h1 span { color: #fb7185; }
      .hero-copy p {
        max-width: 680px;
        margin-bottom: 1.4rem;
        color: #cbd5e1;
        font-size: 1.05rem;
      }
      .hero-proof {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem 1.25rem;
        color: #e2e8f0;
        font-size: 0.86rem;
        font-weight: 650;
      }
      .hero-proof b { color: #86efac; }
      .hero-action {
        display: flex;
        align-items: flex-start;
        flex-direction: column;
        gap: 0.65rem;
      }
      .hero-action > span { color: #94a3b8; font-size: 0.8rem; }
      .hero-action .btn { box-shadow: 0 16px 40px rgba(234, 67, 53, 0.3); }
      .events-page { padding-top: 2.5rem; padding-bottom: 4rem; }
      .search-panel {
        position: relative;
        padding: 1.5rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-xl);
        background: var(--color-bg-surface);
        box-shadow: var(--shadow-md);
      }
      .search-heading-row {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 1rem;
        margin-bottom: 1.25rem;
      }
      .section-kicker { margin-bottom: 0.25rem; color: var(--color-primary); }
      .search-heading-row h2 { margin: 0; font-size: 1.65rem; }
      .primary-search { position: relative; display: flex; gap: 0.65rem; }
      .primary-search input { min-height: 50px; padding-left: 2.7rem; font-size: 1rem; }
      .search-icon {
        position: absolute;
        z-index: 1;
        left: 1rem;
        top: 50%;
        transform: translateY(-50%);
        color: var(--color-text-subtle);
        font-size: 1.4rem;
      }
      .search-button { min-width: 105px; }
      .quick-filters { display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 1rem 0; }
      .quick-filter {
        padding: 0.42rem 0.8rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-full);
        background: #fff;
        color: var(--color-text-muted);
        font: 650 0.8rem var(--font-family);
        cursor: pointer;
        transition: all var(--transition-fast);
      }
      .quick-filter:hover, .quick-filter.active {
        border-color: var(--color-primary);
        background: var(--color-primary-subtle);
        color: var(--color-primary-text);
      }
      .filters-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 0.8rem;
        padding-top: 1rem;
        border-top: 1px solid var(--color-border);
      }
      .filter-label {
        display: block;
        margin-bottom: 0.3rem;
        color: var(--color-text-subtle);
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.07em;
        text-transform: uppercase;
      }
      .topics-row { display: flex; align-items: center; gap: 0.75rem; margin-top: 1rem; }
      .topics-row > span { flex: 0 0 auto; color: var(--color-text-subtle); font-size: 0.8rem; font-weight: 700; }
      .chips-scroll { display: flex; gap: 0.45rem; overflow-x: auto; padding: 2px 2px 5px; }
      .clear-filters {
        margin-top: 1rem;
        padding: 0;
        border: 0;
        background: none;
        color: var(--color-primary);
        font: 700 0.82rem var(--font-family);
        cursor: pointer;
      }
      .results-toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        min-height: 76px;
        color: var(--color-text-muted);
        font-size: 0.9rem;
      }
      .results-toolbar strong { color: var(--color-text-main); }
      .sort-control { display: flex; align-items: center; gap: 0.6rem; }
      .sort-control span { white-space: nowrap; font-size: 0.8rem; font-weight: 650; }
      .sort-control select { min-width: 160px; padding-block: 0.48rem; }
      .events-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1.25rem;
      }
      .event-card {
        display: flex;
        min-width: 0;
        min-height: 290px;
        flex-direction: column;
        padding: 1.35rem;
        color: var(--color-text-main);
        text-decoration: none;
      }
      .event-card:hover { color: var(--color-text-main); text-decoration: none; }
      .event-card:focus-visible { outline: 3px solid var(--color-primary-subtle); border-color: var(--color-primary); }
      .event-card-header, .event-card-footer { display: flex; align-items: center; justify-content: space-between; gap: 0.6rem; }
      .format-badge {
        padding: 0.28rem 0.58rem;
        border-radius: var(--radius-full);
        background: var(--color-accent-subtle);
        color: var(--color-info-text);
        font-size: 0.7rem;
        font-weight: 800;
        letter-spacing: 0.04em;
        text-transform: uppercase;
      }
      .availability-dot { color: var(--color-success-text); font-size: 0.73rem; font-weight: 750; }
      .availability-dot::before { content: ''; display: inline-block; width: 7px; height: 7px; margin-right: 0.38rem; border-radius: 50%; background: var(--color-success); }
      .availability-dot.waitlist { color: var(--color-warning-text); }
      .availability-dot.waitlist::before { background: var(--color-warning); }
      .availability-dot.full { color: var(--color-danger-text); }
      .availability-dot.full::before { background: var(--color-danger); }
      .event-card-body { flex: 1; padding: 1.2rem 0; }
      .event-time { display: block; margin-bottom: 0.5rem; color: var(--color-primary); font-size: 0.78rem; font-weight: 800; text-transform: uppercase; }
      .event-time span { color: var(--color-text-subtle); }
      .event-title { margin-bottom: 0.5rem; font-size: 1.2rem; line-height: 1.35; }
      .group-name { margin-bottom: 0.9rem; color: var(--color-text-muted); font-size: 0.84rem; }
      .event-location { display: flex; align-items: center; gap: 0.4rem; margin: 0; color: var(--color-text-subtle); font-size: 0.82rem; }
      .event-card-footer { padding-top: 1rem; border-top: 1px solid var(--color-border); font-size: 0.78rem; }
      .attendees-count { color: var(--color-text-muted); font-weight: 700; }
      .capacity-total { color: var(--color-text-subtle); font-weight: 500; }
      .fee-badge { color: var(--color-text-main); font-weight: 800; }
      .fee-badge.free { color: var(--color-success-text); }
      .state-panel {
        display: flex;
        align-items: center;
        gap: 1rem;
        min-height: 160px;
        padding: 2rem;
        border: 1px dashed var(--color-border-hover);
        border-radius: var(--radius-xl);
        background: var(--color-bg-surface);
      }
      .state-panel .state-icon { display: grid; width: 48px; height: 48px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: var(--color-bg-subtle); color: var(--color-text-muted); font-size: 1.5rem; font-weight: 800; }
      .state-panel div { flex: 1; }
      .state-panel h3 { margin-bottom: 0.3rem; }
      .state-panel p { margin: 0; color: var(--color-text-muted); }
      .error-state { border-style: solid; border-color: #fecaca; background: #fffafa; }
      .error-state .state-icon { background: var(--color-danger-subtle); color: var(--color-danger-text); }
      .skeleton-card { pointer-events: none; }
      .skeleton { border-radius: 6px; background: linear-gradient(90deg, #eef2f7 25%, #f8fafc 50%, #eef2f7 75%); background-size: 200% 100%; animation: shimmer 1.3s infinite; }
      .skeleton.short { width: 34%; height: 22px; }
      .skeleton.title { width: 90%; height: 28px; margin-top: 2.5rem; }
      .skeleton.medium { width: 65%; height: 16px; margin-top: 0.8rem; }
      .skeleton.footer { width: 100%; height: 18px; margin-top: auto; }
      @keyframes shimmer { to { background-position: -200% 0; } }
      .pagination { display: flex; align-items: center; justify-content: center; gap: 1.25rem; margin-top: 2rem; color: var(--color-text-muted); font-size: 0.86rem; }
      .sr-only { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }

      @media (min-width: 900px) {
        .hero-inner { grid-template-columns: minmax(0, 1fr) auto; align-items: end; }
        .hero-action { align-items: center; padding-bottom: 0.3rem; }
      }
      @media (max-width: 900px) {
        .events-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      }
      @media (max-width: 640px) {
        .hero-inner { padding-top: 3.25rem; padding-bottom: 3.25rem; }
        .hero-proof { gap: 0.5rem 0.9rem; }
        .events-page { padding-inline: 1rem; padding-top: 1.25rem; }
        .search-panel { padding: 1.1rem; border-radius: var(--radius-lg); }
        .search-heading-row { align-items: flex-start; }
        .create-event-btn { display: none; }
        .primary-search { flex-direction: column; }
        .search-button { min-height: 46px; }
        .filters-grid, .events-grid { grid-template-columns: 1fr; }
        .results-toolbar { align-items: flex-start; flex-direction: column; justify-content: center; padding-block: 0.8rem; }
        .sort-control, .sort-control select { width: 100%; }
        .state-panel { align-items: flex-start; flex-wrap: wrap; padding: 1.4rem; }
        .state-panel .btn { width: 100%; }
        .pagination { gap: 0.65rem; }
        .pagination .btn { padding-inline: 0.7rem; }
      }
      @media (prefers-reduced-motion: reduce) {
        .skeleton { animation: none; }
      }
    `,
  ],
})
export class EventListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly eventService = inject(EventService);
  private readonly topicService = inject(TopicService);

  readonly events = signal<EventSummary[]>([]);
  readonly categories = signal<CategorySummary[]>([]);
  readonly topics = signal<TopicSummary[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  searchKeyword = '';
  selectedCategorySlug = '';
  selectedTopicSlug = '';
  selectedFormat: '' | EventFormat = '';
  selectedAvailability: '' | AvailabilityState = '';
  selectedDateWindow: DateWindow = '';
  selectedSort = 'startsAt';
  freeOnly = false;

  ngOnInit() {
    this.loadCategories();
    this.applyFilters();
  }

  loadCategories() {
    this.topicService.getCategories().subscribe({
      next: categories => this.categories.set(categories),
    });
  }

  onCategoryChange() {
    this.selectedTopicSlug = '';
    if (this.selectedCategorySlug) {
      this.topicService.getTopics(this.selectedCategorySlug).subscribe({
        next: topics => this.topics.set(topics),
        error: () => this.topics.set([]),
      });
    } else {
      this.topics.set([]);
    }
    this.applyFilters();
  }

  selectTopic(topicSlug: string) {
    this.selectedTopicSlug = this.selectedTopicSlug === topicSlug ? '' : topicSlug;
    this.applyFilters();
  }

  selectDateWindow(window: Exclude<DateWindow, ''>) {
    this.selectedDateWindow = this.selectedDateWindow === window ? '' : window;
    this.applyFilters();
  }

  toggleFreeOnly() {
    this.freeOnly = !this.freeOnly;
    this.applyFilters();
  }

  applyFilters(resetPage = true) {
    if (resetPage) this.currentPage.set(0);
    this.loading.set(true);
    this.errorMessage.set('');

    const now = new Date();
    const dateTo = this.selectedDateWindow
      ? new Date(now.getTime() + (this.selectedDateWindow === 'week' ? 7 : 30) * 86_400_000).toISOString()
      : undefined;

    this.eventService
      .searchEvents({
        query: this.searchKeyword.trim() || undefined,
        categorySlug: this.selectedCategorySlug || undefined,
        topicSlugs: this.selectedTopicSlug ? [this.selectedTopicSlug] : undefined,
        format: this.selectedFormat || undefined,
        availability: this.selectedAvailability || undefined,
        dateFrom: this.selectedDateWindow ? now.toISOString() : undefined,
        dateTo,
        maxFee: this.freeOnly ? 0 : undefined,
        page: this.currentPage(),
        size: 9,
        sort: this.selectedSort,
      })
      .subscribe({
        next: page => {
          this.events.set(page.content || []);
          this.totalElements.set(page.totalElements ?? page.content?.length ?? 0);
          this.totalPages.set(page.totalPages ?? (page.content?.length ? 1 : 0));
          this.currentPage.set(page.page ?? this.currentPage());
          this.loading.set(false);
        },
        error: () => {
          this.events.set([]);
          this.totalElements.set(0);
          this.totalPages.set(0);
          this.errorMessage.set('Please check your connection and try again. Your filters are still here.');
          this.loading.set(false);
        },
      });
  }

  goToPage(page: number) {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.applyFilters(false);
    globalThis.scrollTo?.({ top: 500, behavior: 'smooth' });
  }

  resetFilters() {
    this.searchKeyword = '';
    this.selectedCategorySlug = '';
    this.selectedTopicSlug = '';
    this.selectedFormat = '';
    this.selectedAvailability = '';
    this.selectedDateWindow = '';
    this.selectedSort = 'startsAt';
    this.freeOnly = false;
    this.topics.set([]);
    this.applyFilters();
  }

  hasActiveFilters() {
    return !!(
      this.searchKeyword.trim() ||
      this.selectedCategorySlug ||
      this.selectedTopicSlug ||
      this.selectedFormat ||
      this.selectedAvailability ||
      this.selectedDateWindow ||
      this.freeOnly
    );
  }

  formatAvailability(state: AvailabilityState): string {
    return {
      SEATS_AVAILABLE: 'Seats available',
      WAITLIST: 'Waitlist open',
      FULL: 'Fully booked',
    }[state];
  }

  formatEventFormat(format: EventFormat): string {
    return { IN_PERSON: 'In person', ONLINE: 'Online', HYBRID: 'Hybrid' }[format];
  }

  formatFee(event: EventSummary): string {
    if (!event.fee || event.fee.amountMinor === 0) return 'Free';
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: event.fee.currency,
        maximumFractionDigits: 2,
      }).format(event.fee.amountMinor / 100);
    } catch {
      return `${event.fee.amountMinor / 100} ${event.fee.currency}`;
    }
  }
}
