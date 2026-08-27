import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MemberProfile } from '../../core/models/member.model';
import { CategorySummary, TopicSummary } from '../../core/models/topic.model';
import { AuthService } from '../../core/services/auth.service';
import { MemberService } from '../../core/services/member.service';
import { ToastService } from '../../core/services/toast.service';
import { TopicService } from '../../core/services/topic.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="container container-narrow profile-page">
      <div class="page-header">
        <div>
          <h1>Member Profile</h1>
          <p class="header-sub">Manage your public persona and followed interests</p>
        </div>
      </div>

      <!-- Profile Form Card -->
      <div class="card profile-card">
        <form (ngSubmit)="onSaveProfile()">
          <div class="avatar-section">
            <div class="avatar-preview">
              @if (profile()?.photoUrl) {
                <img [src]="profile()?.photoUrl" alt="Avatar" />
              } @else {
                <div class="avatar-placeholder">
                  {{ profile()?.displayName?.charAt(0) || 'U' }}
                </div>
              }
            </div>
            <div class="avatar-inputs">
              <label class="form-label" for="photoUrl">Avatar Image URL</label>
              <input
                type="url"
                id="photoUrl"
                name="photoUrl"
                class="form-control"
                [(ngModel)]="photoUrl"
                placeholder="https://example.com/avatar.jpg"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="displayName">Display Name</label>
              <input
                type="text"
                id="displayName"
                name="displayName"
                class="form-control"
                [(ngModel)]="displayName"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="email">Email</label>
              <input
                type="email"
                id="email"
                name="email"
                class="form-control"
                [value]="profile()?.email"
                disabled
              />
            </div>
          </div>

          <div class="email-status-box">
            @if (profile()?.emailVerified) {
              <span class="badge badge-success">✓ Email Verified</span>
            } @else {
              <div class="unverified-row">
                <span class="badge badge-warning">Email Not Verified</span>
                <button type="button" class="btn btn-sm btn-outline" (click)="resendVerification()">
                  Resend Verification Email
                </button>
                <a routerLink="/verify-email" class="verify-link">Enter Token</a>
              </div>
            }
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="homeCity">Home City</label>
              <input
                type="text"
                id="homeCity"
                name="homeCity"
                class="form-control"
                [(ngModel)]="homeCity"
                placeholder="e.g. Cluj-Napoca"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="homeCountry">Home Country</label>
              <input
                type="text"
                id="homeCountry"
                name="homeCountry"
                class="form-control"
                [(ngModel)]="homeCountry"
                placeholder="e.g. Romania"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label" for="bio">Bio</label>
            <textarea
              id="bio"
              name="bio"
              class="form-control"
              [(ngModel)]="bio"
              rows="4"
              placeholder="Tell other members what you're passionate about..."
            ></textarea>
          </div>

          <button type="submit" class="btn btn-primary" [disabled]="saving()">
            @if (saving()) {
              <span>Saving changes...</span>
            } @else {
              <span>Save Profile</span>
            }
          </button>
        </form>
      </div>

      <!-- Interests & Topics Selector -->
      <div class="card interests-card">
        <div class="interests-header">
          <h3>Your Followed Interests</h3>
          <p>We curate event recommendations and notifications based on topics you follow.</p>
        </div>

        @if (loadingTopics()) {
          <div class="loading-topics">Loading topic catalog...</div>
        } @else {
          <div class="categories-container">
            @for (cat of categories(); track cat.id) {
              <div class="category-block">
                <h4>{{ cat.name }}</h4>
                <div class="topic-chips">
                  @for (topic of topicsForCategory(cat.slug); track topic.id) {
                    <button
                      type="button"
                      class="chip"
                      [class.active]="isTopicSelected(topic.slug)"
                      (click)="toggleTopic(topic.slug)"
                    >
                      <span>{{ topic.name }}</span>
                      @if (isTopicSelected(topic.slug)) {
                        <span class="chip-check">✓</span>
                      }
                    </button>
                  }
                </div>
              </div>
            }
          </div>

          <div class="interests-save-row">
            <button
              type="button"
              class="btn btn-primary"
              (click)="saveInterests()"
              [disabled]="savingInterests()"
            >
              @if (savingInterests()) {
                <span>Updating interests...</span>
              } @else {
                <span>Save Interests ({{ selectedTopicSlugs().length }} selected)</span>
              }
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .profile-page {
        padding: 2.5rem 1.5rem;
      }
      .header-sub {
        color: var(--color-text-muted);
        font-size: 1.05rem;
      }
      .profile-card, .interests-card {
        margin-bottom: 2rem;
      }
      .avatar-section {
        display: flex;
        align-items: center;
        gap: 1.5rem;
        margin-bottom: 1.5rem;
        padding-bottom: 1.5rem;
        border-bottom: 1px solid var(--color-border);
      }
      .avatar-preview {
        img, .avatar-placeholder {
          width: 80px;
          height: 80px;
          border-radius: 9999px;
        }
        img { object-fit: cover; }
        .avatar-placeholder {
          background: var(--color-primary-subtle);
          color: var(--color-primary-text);
          font-size: 2rem;
          font-weight: 800;
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
      .avatar-inputs {
        flex: 1;
      }
      .email-status-box {
        margin-bottom: 1.25rem;
        .unverified-row {
          display: flex;
          align-items: center;
          gap: 1rem;
          flex-wrap: wrap;
        }
        .verify-link {
          font-size: 0.85rem;
          color: var(--color-primary);
        }
      }
      .interests-header {
        margin-bottom: 1.5rem;
        h3 { margin-bottom: 0.25rem; }
        p { color: var(--color-text-subtle); font-size: 0.9rem; margin-bottom: 0; }
      }
      .categories-container {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }
      .category-block {
        h4 {
          font-size: 1rem;
          color: var(--color-text-main);
          margin-bottom: 0.6rem;
          border-bottom: 1px dashed var(--color-border);
          padding-bottom: 0.4rem;
        }
      }
      .topic-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }
      .chip-check {
        font-weight: 800;
        font-size: 0.85rem;
      }
      .interests-save-row {
        margin-top: 2rem;
        padding-top: 1.5rem;
        border-top: 1px solid var(--color-border);
        display: flex;
        justify-content: flex-end;
      }
      .loading-topics {
        text-align: center;
        padding: 2rem;
        color: var(--color-text-subtle);
      }
    `,
  ],
})
export class ProfileComponent implements OnInit {
  private readonly memberService = inject(MemberService);
  private readonly topicService = inject(TopicService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly profile = signal<MemberProfile | null>(null);
  readonly categories = signal<CategorySummary[]>([]);
  readonly allTopics = signal<TopicSummary[]>([]);
  readonly selectedTopicSlugs = signal<string[]>([]);

  readonly saving = signal(false);
  readonly savingInterests = signal(false);
  readonly loadingTopics = signal(true);

  displayName = '';
  bio = '';
  homeCity = '';
  homeCountry = '';
  photoUrl = '';

  ngOnInit() {
    this.loadProfile();
    this.loadTopicsCatalog();
  }

  loadProfile() {
    this.memberService.getProfile().subscribe({
      next: profile => {
        this.profile.set(profile);
        this.displayName = profile.displayName;
        this.bio = profile.bio || '';
        this.homeCity = profile.homeCity || '';
        this.homeCountry = profile.homeCountry || '';
        this.photoUrl = profile.photoUrl || '';
      },
    });

    this.memberService.getMyInterests().subscribe({
      next: interests => {
        this.selectedTopicSlugs.set(interests.map(t => t.slug));
      },
    });
  }

  loadTopicsCatalog() {
    this.loadingTopics.set(true);
    this.topicService.getCategories().subscribe({
      next: cats => {
        this.categories.set(cats);
        this.topicService.getTopics().subscribe({
          next: topics => {
            this.allTopics.set(topics);
            this.loadingTopics.set(false);
          },
          error: () => this.loadingTopics.set(false),
        });
      },
      error: () => this.loadingTopics.set(false),
    });
  }

  topicsForCategory(categorySlug: string): TopicSummary[] {
    return this.allTopics().filter(t => t.categorySlug === categorySlug);
  }

  isTopicSelected(slug: string): boolean {
    return this.selectedTopicSlugs().includes(slug);
  }

  toggleTopic(slug: string) {
    this.selectedTopicSlugs.update(current =>
      current.includes(slug) ? current.filter(s => s !== slug) : [...current, slug]
    );
  }

  onSaveProfile() {
    this.saving.set(true);
    this.memberService
      .updateProfile({
        displayName: this.displayName,
        bio: this.bio,
        homeCity: this.homeCity,
        homeCountry: this.homeCountry,
        photoUrl: this.photoUrl,
      })
      .subscribe({
        next: updated => {
          this.saving.set(false);
          this.profile.set(updated);
          this.auth.setCurrentUser(updated);
          this.toast.success('Profile updated successfully.');
        },
        error: err => {
          this.saving.set(false);
          this.toast.error(err?.error?.message || 'Failed to update profile');
        },
      });
  }

  saveInterests() {
    this.savingInterests.set(true);
    const topicIds = this.allTopics()
      .filter(t => this.selectedTopicSlugs().includes(t.slug))
      .map(t => t.id);
    this.memberService.updateMyInterests({ topicIds }).subscribe({
      next: updated => {
        this.savingInterests.set(false);
        this.selectedTopicSlugs.set(updated.map(t => t.slug));
        this.toast.success('Interests updated!');
      },
      error: err => {
        this.savingInterests.set(false);
        this.toast.error(err?.error?.message || 'Failed to update interests');
      },
    });
  }

  resendVerification() {
    this.auth.resendVerification().subscribe({
      error: err => this.toast.error(err?.error?.message || 'Could not resend email'),
    });
  }
}
