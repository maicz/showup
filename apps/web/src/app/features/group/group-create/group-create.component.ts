import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { GroupJoinPolicy, GroupVisibility } from '../../../core/models/group.model';
import { CategorySummary, TopicSummary } from '../../../core/models/topic.model';
import { GroupService } from '../../../core/services/group.service';
import { ToastService } from '../../../core/services/toast.service';
import { TopicService } from '../../../core/services/topic.service';

@Component({
  selector: 'app-group-create',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="container container-narrow group-create-page">
      <div class="page-header">
        <div>
          <h1>Start a New Community Group</h1>
          <p class="header-sub">Create a group, organize meetups, and cultivate your local or global audience.</p>
        </div>
      </div>

      <div class="card form-card">
        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label" for="name">Group Name</label>
            <input
              type="text"
              id="name"
              name="name"
              class="form-control"
              [(ngModel)]="name"
              (input)="generateSlug()"
              required
              placeholder="e.g. Cluj Java & Cloud Native Community"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="urlname">URL Slug</label>
            <div class="input-prefix-row">
              <span class="prefix">showup.app/groups/</span>
              <input
                type="text"
                id="urlname"
                name="urlname"
                class="form-control"
                [(ngModel)]="urlname"
                required
                placeholder="cluj-java-community"
              />
            </div>
            <span class="form-hint">Unique identifier for your group URL.</span>
          </div>

          <div class="form-group">
            <label class="form-label" for="category">Primary Category</label>
            <select
              id="category"
              name="category"
              class="form-select"
              [(ngModel)]="selectedCategoryId"
              required
              (change)="onCategoryChange()"
            >
              <option value="" disabled>Select category</option>
              @for (cat of categories(); track cat.id) {
                <option [value]="cat.id">{{ cat.name }}</option>
              }
            </select>
          </div>

          @if (topics().length > 0) {
            <div class="form-group">
              <label class="form-label">Select Topics / Tags</label>
              <div class="topics-picker">
                @for (t of topics(); track t.id) {
                  <button
                    type="button"
                    class="chip"
                    [class.active]="selectedTopicIds().includes(t.id)"
                    (click)="toggleTopic(t.id)"
                  >
                    {{ t.name }}
                  </button>
                }
              </div>
            </div>
          }

          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="city">City</label>
              <input
                type="text"
                id="city"
                name="city"
                class="form-control"
                [(ngModel)]="city"
                placeholder="e.g. Cluj-Napoca"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="country">Country</label>
              <input
                type="text"
                id="country"
                name="country"
                class="form-control"
                [(ngModel)]="country"
                placeholder="e.g. Romania"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="joinPolicy">Join Policy</label>
              <select id="joinPolicy" name="joinPolicy" class="form-select" [(ngModel)]="joinPolicy">
                <option value="OPEN">Open (Anyone can join directly)</option>
                <option value="APPROVAL_REQUIRED">Approval Required (Organizer approves)</option>
                <option value="INVITE_ONLY">Invite Only</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label" for="timeZone">Default Time Zone</label>
              <input
                type="text"
                id="timeZone"
                name="timeZone"
                class="form-control"
                [(ngModel)]="timeZone"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label" for="description">Group Description</label>
            <textarea
              id="description"
              name="description"
              class="form-control"
              [(ngModel)]="description"
              rows="5"
              required
              placeholder="Describe your community's purpose, what members can expect, and guidelines..."
            ></textarea>
          </div>

          <div class="form-actions">
            <a routerLink="/groups" class="btn btn-secondary">Cancel</a>
            <button
              type="submit"
              class="btn btn-primary btn-lg"
              [disabled]="saving() || !name || !urlname || !selectedCategoryId || !description"
            >
              @if (saving()) {
                <span>Creating Group...</span>
              } @else {
                <span>Launch Group</span>
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .group-create-page {
        padding: 2.5rem 1.5rem;
      }
      .header-sub {
        color: var(--color-text-muted);
        font-size: 1.05rem;
      }
      .form-card {
        padding: 2rem;
      }
      .input-prefix-row {
        display: flex;
        align-items: center;
        .prefix {
          background: var(--color-bg-muted);
          padding: 0.65rem 0.9rem;
          border: 1px solid var(--color-border);
          border-right: none;
          border-radius: var(--radius-md) 0 0 var(--radius-md);
          font-size: 0.85rem;
          color: var(--color-text-subtle);
        }
        input {
          border-top-left-radius: 0;
          border-bottom-left-radius: 0;
        }
      }
      .topics-picker {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }
      .form-actions {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 1rem;
        margin-top: 2rem;
        padding-top: 1.5rem;
        border-top: 1px solid var(--color-border);
      }
    `,
  ],
})
export class GroupCreateComponent implements OnInit {
  private readonly groupService = inject(GroupService);
  private readonly topicService = inject(TopicService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly categories = signal<CategorySummary[]>([]);
  readonly topics = signal<TopicSummary[]>([]);
  readonly selectedTopicIds = signal<string[]>([]);
  readonly saving = signal(false);

  name = '';
  urlname = '';
  description = '';
  selectedCategoryId = '';
  city = '';
  country = '';
  joinPolicy: GroupJoinPolicy = 'OPEN';
  visibility: GroupVisibility = 'PUBLIC';
  timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

  ngOnInit() {
    this.topicService.getCategories().subscribe({
      next: cats => this.categories.set(cats),
    });
  }

  generateSlug() {
    this.urlname = this.name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

  onCategoryChange() {
    const category = this.categories().find(c => c.id === this.selectedCategoryId);
    if (category) {
      this.topicService.getTopics(category.slug).subscribe({
        next: t => this.topics.set(t),
      });
    } else {
      this.topics.set([]);
    }
    this.selectedTopicIds.set([]);
  }

  toggleTopic(topicId: string) {
    this.selectedTopicIds.update(current =>
      current.includes(topicId) ? current.filter(id => id !== topicId) : [...current, topicId]
    );
  }

  onSubmit() {
    if (!this.name || !this.urlname || !this.selectedCategoryId || !this.description) return;
    this.saving.set(true);

    this.groupService
      .createGroup({
        name: this.name,
        urlname: this.urlname,
        description: this.description,
        categoryId: this.selectedCategoryId,
        city: this.city || undefined,
        country: this.country || undefined,
        timeZone: this.timeZone,
        joinPolicy: this.joinPolicy,
        visibility: this.visibility,
        topicIds: this.selectedTopicIds(),
      })
      .subscribe({
        next: created => {
          this.saving.set(false);
          this.toast.success('Group created successfully!');
          this.router.navigate(['/groups', created.id]);
        },
        error: err => {
          this.saving.set(false);
          this.toast.error(err?.error?.message || 'Failed to create group');
        },
      });
  }
}
