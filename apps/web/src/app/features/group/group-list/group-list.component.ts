import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GroupSummary } from '../../../core/models/group.model';
import { CategorySummary } from '../../../core/models/topic.model';
import { AuthService } from '../../../core/services/auth.service';
import { GroupService } from '../../../core/services/group.service';
import { TopicService } from '../../../core/services/topic.service';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [RouterLink, FormsModule, DecimalPipe],
  template: `
    <section class="community-hero">
      <div class="container hero-inner">
        <div>
          <span class="eyebrow">Communities make the calendar matter</span>
          <h1>Find people who are into what <span>you are into.</span></h1>
          <p>Join a local circle, learn alongside peers, or build the community you wish already existed.</p>
        </div>
        <div class="hero-actions">
          <a [routerLink]="auth.isAuthenticated() ? '/groups/create' : '/register'" class="btn btn-primary btn-lg">
            Start a community <span aria-hidden="true">→</span>
          </a>
          <a routerLink="/events" class="browse-link">Browse upcoming events</a>
        </div>
      </div>
    </section>

    <main class="container groups-page">
      <section class="search-panel" aria-labelledby="groups-heading">
        <div class="search-heading">
          <div>
            <span class="section-kicker">Explore together</span>
            <h2 id="groups-heading">Discover communities</h2>
          </div>
          @if (hasActiveFilters()) {
            <button type="button" class="clear-button" (click)="clearFilters()">Clear filters</button>
          }
        </div>

        <form class="search-grid" (ngSubmit)="applyFilters()">
          <label class="field search-field">
            <span>Community name</span>
            <input
              type="search"
              class="form-control"
              placeholder="Try Angular, hiking, photography…"
              [(ngModel)]="searchKeyword"
              name="searchKeyword"
            />
          </label>
          <label class="field">
            <span>City</span>
            <input
              type="search"
              class="form-control"
              placeholder="e.g. Bucharest"
              [(ngModel)]="city"
              name="city"
            />
          </label>
          <button class="btn btn-primary search-button" type="submit">Find communities</button>
        </form>

        <div class="category-strip" aria-label="Filter by category">
          <button
            type="button"
            class="category-chip"
            [class.active]="!selectedCategorySlug"
            [attr.aria-pressed]="!selectedCategorySlug"
            (click)="selectCategory('')"
          >
            All interests
          </button>
          @for (category of categories(); track category.id) {
            <button
              type="button"
              class="category-chip"
              [class.active]="selectedCategorySlug === category.slug"
              [attr.aria-pressed]="selectedCategorySlug === category.slug"
              (click)="selectCategory(category.slug)"
            >
              {{ category.name }}
            </button>
          }
        </div>
      </section>

      <div class="results-toolbar">
        <div aria-live="polite">
          @if (!loading() && !errorMessage()) {
            <strong>{{ totalElements() }}</strong>
            {{ totalElements() === 1 ? 'community' : 'communities' }} found
          } @else if (loading()) {
            <span>Finding communities…</span>
          }
        </div>
        <label class="sort-control">
          <span>Sort by</span>
          <select class="form-select" [(ngModel)]="selectedSort" (change)="applyFilters()">
            <option value="popular">Most members</option>
            <option value="rating">Highest rated</option>
            <option value="newest">Newest</option>
            <option value="name">Name</option>
          </select>
        </label>
      </div>

      @if (loading()) {
        <div class="groups-grid" aria-label="Loading communities" aria-busy="true">
          @for (placeholder of [1, 2, 3, 4, 5, 6]; track placeholder) {
            <div class="card group-card skeleton-card" aria-hidden="true">
              <div class="skeleton avatar"></div>
              <div class="skeleton short"></div>
              <div class="skeleton title"></div>
              <div class="skeleton medium"></div>
            </div>
          }
        </div>
      } @else if (errorMessage()) {
        <section class="state-panel error-state" role="alert">
          <span class="state-icon" aria-hidden="true">!</span>
          <div>
            <h3>We could not load communities</h3>
            <p>{{ errorMessage() }}</p>
          </div>
          <button class="btn btn-secondary" type="button" (click)="loadGroups(currentPage())">Try again</button>
        </section>
      } @else if (groups().length === 0) {
        <section class="state-panel">
          <span class="state-icon" aria-hidden="true">⌕</span>
          <div>
            <h3>{{ hasActiveFilters() ? 'No exact matches—yet' : 'A community could start here' }}</h3>
            <p>
              {{ hasActiveFilters()
                ? 'Try another city, interest, or a broader community name.'
                : 'Create the first community and invite people who share your interests.' }}
            </p>
          </div>
          @if (hasActiveFilters()) {
            <button class="btn btn-secondary" type="button" (click)="clearFilters()">Clear filters</button>
          } @else {
            <a [routerLink]="auth.isAuthenticated() ? '/groups/create' : '/register'" class="btn btn-primary">Start a community</a>
          }
        </section>
      } @else {
        <div class="groups-grid">
          @for (group of groups(); track group.id) {
            <a class="card group-card card-interactive" [routerLink]="['/groups', group.id]" [attr.aria-label]="'View ' + group.name">
              <div class="group-card-top">
                <span class="group-avatar" aria-hidden="true">{{ initials(group.name) }}</span>
                <span class="badge badge-neutral">{{ group.category.name }}</span>
              </div>
              <div class="group-card-body">
                <h3>{{ group.name }}</h3>
                <p class="location"><span aria-hidden="true">⌖</span> {{ locationLabel(group) }}</p>
              </div>
              <div class="group-card-footer">
                <span><strong>{{ group.memberCount }}</strong> {{ group.memberCount === 1 ? 'member' : 'members' }}</span>
                @if (group.ratingAverage !== undefined && group.ratingAverage !== null) {
                  <span class="rating" [attr.aria-label]="group.ratingAverage + ' out of 5 from ' + group.ratingCount + ' ratings'">
                    <span aria-hidden="true">★</span> {{ group.ratingAverage | number: '1.1-1' }}
                    <small>({{ group.ratingCount }})</small>
                  </span>
                } @else {
                  <span class="new-label">New community</span>
                }
              </div>
              <span class="view-community">View community <span aria-hidden="true">→</span></span>
            </a>
          }
        </div>

        @if (totalPages() > 1) {
          <nav class="pagination" aria-label="Community pages">
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
    </main>
  `,
  styles: [
    `
      .community-hero {
        position: relative;
        overflow: hidden;
        color: #fff;
        background:
          radial-gradient(circle at 82% 20%, rgba(251, 113, 133, 0.26), transparent 30%),
          radial-gradient(circle at 12% 95%, rgba(52, 211, 153, 0.16), transparent 34%),
          linear-gradient(125deg, #111827 0%, #1e1b4b 56%, #4c1d95 100%);
      }
      .community-hero::after {
        content: '';
        position: absolute;
        width: 320px;
        height: 320px;
        right: 6%;
        bottom: -250px;
        border: 45px solid rgba(255, 255, 255, 0.06);
        border-radius: 50%;
      }
      .hero-inner {
        position: relative;
        z-index: 1;
        display: grid;
        gap: 2rem;
        padding-top: 4rem;
        padding-bottom: 4rem;
      }
      .hero-inner > div:first-child { max-width: 760px; }
      .eyebrow, .section-kicker {
        display: block;
        margin-bottom: 0.75rem;
        font-size: 0.75rem;
        font-weight: 800;
        letter-spacing: 0.12em;
        text-transform: uppercase;
      }
      .eyebrow { color: #fda4af; }
      .hero-inner h1 {
        margin-bottom: 1rem;
        color: #fff;
        font-size: clamp(2.4rem, 6vw, 4.5rem);
        line-height: 1.02;
        letter-spacing: -0.055em;
      }
      .hero-inner h1 span { color: #a7f3d0; }
      .hero-inner p { max-width: 680px; margin: 0; color: #cbd5e1; font-size: 1.05rem; }
      .hero-actions { display: flex; align-items: flex-start; flex-direction: column; gap: 0.8rem; }
      .hero-actions .btn { box-shadow: 0 16px 40px rgba(234, 67, 53, 0.28); }
      .browse-link { color: #cbd5e1; font-size: 0.85rem; font-weight: 700; }
      .browse-link:hover { color: #fff; }
      .groups-page { padding-top: 2.5rem; padding-bottom: 4rem; }
      .search-panel {
        padding: 1.5rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-xl);
        background: var(--color-bg-surface);
        box-shadow: var(--shadow-md);
      }
      .search-heading {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 1rem;
        margin-bottom: 1.25rem;
      }
      .section-kicker { margin-bottom: 0.25rem; color: var(--color-primary); }
      .search-heading h2 { margin: 0; font-size: 1.65rem; }
      .clear-button {
        border: 0;
        background: transparent;
        color: var(--color-primary);
        font: 700 0.85rem var(--font-family);
        cursor: pointer;
      }
      .search-grid {
        display: grid;
        grid-template-columns: minmax(0, 1.5fr) minmax(190px, 0.7fr) auto;
        gap: 0.8rem;
        align-items: end;
      }
      .field { display: grid; gap: 0.4rem; color: var(--color-text-muted); font-size: 0.8rem; font-weight: 700; }
      .field input { min-height: 48px; }
      .search-button { min-height: 48px; }
      .category-strip {
        display: flex;
        gap: 0.5rem;
        margin-top: 1rem;
        padding-bottom: 0.15rem;
        overflow-x: auto;
        scrollbar-width: thin;
      }
      .category-chip {
        flex: 0 0 auto;
        padding: 0.45rem 0.8rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-full);
        background: #fff;
        color: var(--color-text-muted);
        font: 650 0.8rem var(--font-family);
        cursor: pointer;
        transition: all var(--transition-fast);
      }
      .category-chip:hover, .category-chip.active {
        border-color: var(--color-primary);
        background: var(--color-primary-subtle);
        color: var(--color-primary);
      }
      .results-toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        min-height: 72px;
        color: var(--color-text-muted);
        font-size: 0.9rem;
      }
      .sort-control { display: flex; align-items: center; gap: 0.65rem; white-space: nowrap; }
      .sort-control select { min-width: 165px; }
      .groups-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
        gap: 1.25rem;
      }
      .group-card {
        position: relative;
        display: flex;
        min-height: 280px;
        flex-direction: column;
        padding: 1.45rem;
        color: inherit;
        text-decoration: none;
      }
      .group-card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
      .group-avatar {
        display: grid;
        width: 52px;
        height: 52px;
        place-items: center;
        border-radius: 16px;
        color: #312e81;
        background: linear-gradient(135deg, #ddd6fe, #a7f3d0);
        font-size: 1rem;
        font-weight: 850;
        letter-spacing: 0.03em;
      }
      .group-card-body { flex: 1; padding: 1.15rem 0 1rem; }
      .group-card-body h3 { margin-bottom: 0.55rem; font-size: 1.3rem; line-height: 1.25; }
      .location { margin: 0; color: var(--color-text-muted); font-size: 0.88rem; }
      .group-card-footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
        padding-top: 0.9rem;
        border-top: 1px solid var(--color-border);
        color: var(--color-text-muted);
        font-size: 0.82rem;
      }
      .rating { color: #a16207; font-weight: 750; }
      .rating small { color: var(--color-text-subtle); }
      .new-label { color: var(--color-primary); font-weight: 700; }
      .view-community {
        margin-top: 0.9rem;
        color: var(--color-primary);
        font-size: 0.84rem;
        font-weight: 750;
      }
      .state-panel {
        display: flex;
        align-items: center;
        gap: 1.2rem;
        padding: 2rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-xl);
        background: var(--color-bg-surface);
      }
      .state-panel > div { flex: 1; }
      .state-panel h3 { margin-bottom: 0.35rem; }
      .state-panel p { margin: 0; color: var(--color-text-muted); }
      .state-icon {
        display: grid;
        width: 48px;
        height: 48px;
        flex: 0 0 auto;
        place-items: center;
        border-radius: 50%;
        background: var(--color-primary-subtle);
        color: var(--color-primary);
        font-size: 1.3rem;
        font-weight: 800;
      }
      .error-state { border-color: var(--color-danger); }
      .error-state .state-icon { background: var(--color-danger-subtle); color: var(--color-danger); }
      .pagination {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 1rem;
        margin-top: 2rem;
        color: var(--color-text-muted);
        font-size: 0.9rem;
      }
      .skeleton-card { pointer-events: none; }
      .skeleton {
        border-radius: 0.5rem;
        background: linear-gradient(90deg, #eef2f7 25%, #f8fafc 50%, #eef2f7 75%);
        background-size: 200% 100%;
        animation: shimmer 1.4s infinite;
      }
      .skeleton.avatar { width: 52px; height: 52px; }
      .skeleton.short { width: 35%; height: 14px; margin-top: 1.2rem; }
      .skeleton.title { width: 78%; height: 25px; margin-top: 0.8rem; }
      .skeleton.medium { width: 58%; height: 15px; margin-top: 0.7rem; }
      @keyframes shimmer { to { background-position: -200% 0; } }
      @media (min-width: 900px) {
        .hero-inner { grid-template-columns: minmax(0, 1fr) auto; align-items: end; }
      }
      @media (max-width: 720px) {
        .hero-inner { padding-top: 3rem; padding-bottom: 3rem; }
        .groups-page { padding-top: 1.25rem; }
        .search-grid { grid-template-columns: 1fr; }
        .search-heading { align-items: flex-start; }
        .results-toolbar { align-items: flex-start; flex-direction: column; padding: 1rem 0; }
        .sort-control { width: 100%; justify-content: space-between; }
        .sort-control select { min-width: 0; max-width: 190px; }
        .groups-grid { grid-template-columns: 1fr; }
        .state-panel { align-items: flex-start; flex-wrap: wrap; }
        .state-panel .btn { width: 100%; }
        .pagination { justify-content: space-between; gap: 0.5rem; }
        .pagination span { font-size: 0.8rem; }
      }
    `,
  ],
})
export class GroupListComponent implements OnInit {
  private readonly groupService = inject(GroupService);
  private readonly topicService = inject(TopicService);
  readonly auth = inject(AuthService);

  readonly groups = signal<GroupSummary[]>([]);
  readonly categories = signal<CategorySummary[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  searchKeyword = '';
  city = '';
  selectedCategorySlug = '';
  selectedSort: 'popular' | 'rating' | 'newest' | 'name' = 'popular';

  ngOnInit(): void {
    this.topicService.getCategories().subscribe({
      next: categories => this.categories.set(categories),
    });
    this.loadGroups();
  }

  applyFilters(): void {
    this.loadGroups(0);
  }

  selectCategory(slug: string): void {
    this.selectedCategorySlug = slug;
    this.applyFilters();
  }

  clearFilters(): void {
    this.searchKeyword = '';
    this.city = '';
    this.selectedCategorySlug = '';
    this.selectedSort = 'popular';
    this.applyFilters();
  }

  hasActiveFilters(): boolean {
    return Boolean(
      this.searchKeyword.trim() ||
      this.city.trim() ||
      this.selectedCategorySlug ||
      this.selectedSort !== 'popular'
    );
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) {
      return;
    }
    this.loadGroups(page);
    window.scrollTo({ top: 360, behavior: 'smooth' });
  }

  loadGroups(page = 0): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.groupService.getGroups({
      query: this.searchKeyword,
      city: this.city,
      categorySlug: this.selectedCategorySlug,
      sort: this.selectedSort,
      page,
      size: 12,
    }).subscribe({
      next: result => {
        this.groups.set(result.content ?? []);
        this.currentPage.set(result.page ?? page);
        this.totalPages.set(result.totalPages ?? 0);
        this.totalElements.set(result.totalElements ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.groups.set([]);
        this.errorMessage.set('Please check your connection and try again.');
        this.loading.set(false);
      },
    });
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  }

  locationLabel(group: GroupSummary): string {
    if (group.city && group.country) {
      return group.city + ', ' + group.country;
    }
    return group.city || group.country || 'Online and everywhere';
  }
}
