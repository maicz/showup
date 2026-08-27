import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EventFormat } from '../../../core/models/event.model';
import { GroupSummary } from '../../../core/models/group.model';
import { VenueSummary } from '../../../core/models/venue.model';
import { AiService } from '../../../core/services/ai.service';
import { EventService } from '../../../core/services/event.service';
import { MemberService } from '../../../core/services/member.service';
import { ToastService } from '../../../core/services/toast.service';
import { VenueService } from '../../../core/services/venue.service';

@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="container container-narrow event-create-page">
      <div class="page-header">
        <div>
          <h1>Create New Event</h1>
          <p class="header-sub">Set up a one-off meetup or recurring community event series</p>
        </div>
      </div>

      <!-- AI Copilot Assistant Card -->
      <div class="card ai-copilot-card">
        <div class="copilot-header">
          <span class="ai-sparkle">✨</span>
          <div>
            <h3>Draft with AI Copilot</h3>
            <p>Paste notes, speaker ideas, or draft bullet points. AI will craft the formatted title, description, and settings.</p>
          </div>
        </div>

        <div class="copilot-body">
          <textarea
            class="form-control"
            [(ngModel)]="aiPrompt"
            placeholder="e.g. Host a hands-on workshop on Spring Boot 4 and Java 25 microservices, 2 hours long, hybrid format with live coding and Q&A..."
            rows="3"
          ></textarea>
          
          <button
            type="button"
            class="btn btn-outline btn-sm ai-btn"
            [disabled]="!aiPrompt.trim() || draftingAi()"
            (click)="draftWithAi()"
          >
            @if (draftingAi()) {
              <span>✨ Generating Draft...</span>
            } @else {
              <span>✨ Generate Event Draft</span>
            }
          </button>
        </div>
      </div>

      <!-- Main Event Creation Form -->
      <div class="card form-card">
        <form (ngSubmit)="onSubmit()">
          <!-- Group Selection -->
          <div class="form-group">
            <label class="form-label" for="group">Hosting Group</label>
            <select
              id="group"
              class="form-select"
              [(ngModel)]="selectedGroupId"
              name="group"
              required
              (change)="onGroupChange()"
            >
              <option value="" disabled>Select a group you organize</option>
              @for (g of myGroups(); track g.id) {
                <option [value]="g.id">{{ g.name }}</option>
              }
            </select>
          </div>

          <!-- Title -->
          <div class="form-group">
            <label class="form-label" for="title">Event Title</label>
            <input
              type="text"
              id="title"
              name="title"
              class="form-control"
              [(ngModel)]="title"
              required
              placeholder="e.g. Spring Boot & AI Meetup #12"
            />
          </div>

          <!-- Format -->
          <div class="form-group">
            <label class="form-label" for="format">Format</label>
            <select id="format" class="form-select" [(ngModel)]="format" name="format" required>
              <option value="IN_PERSON">In Person</option>
              <option value="ONLINE">Online</option>
              <option value="HYBRID">Hybrid (Both)</option>
            </select>
          </div>

          <!-- Venue Selector (for in-person or hybrid) -->
          @if (format !== 'ONLINE') {
            <div class="form-group">
              <div class="venue-label-row">
                <label class="form-label" for="venue">Venue</label>
                <button type="button" class="btn btn-sm btn-subtle" (click)="openNewVenueModal()">
                  + Add New Venue
                </button>
              </div>
              <select id="venue" class="form-select" [(ngModel)]="selectedVenueId" name="venue">
                <option value="">Select a saved venue</option>
                @for (v of venues(); track v.id) {
                  <option [value]="v.id">{{ v.name }} ({{ v.address.city || 'Local' }})</option>
                }
              </select>
            </div>
          }

          <!-- Online URL (for online or hybrid) -->
          @if (format !== 'IN_PERSON') {
            <div class="form-group">
              <label class="form-label" for="onlineUrl">Online Meeting URL</label>
              <input
                type="url"
                id="onlineUrl"
                name="onlineUrl"
                class="form-control"
                [(ngModel)]="onlineUrl"
                placeholder="https://meet.google.com/xyz or Zoom link"
              />
            </div>
          }

          <!-- Timing Row -->
          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="startsAt">Start Date & Time</label>
              <input
                type="datetime-local"
                id="startsAt"
                name="startsAt"
                class="form-control"
                [(ngModel)]="startsAtStr"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="endsAt">End Date & Time</label>
              <input
                type="datetime-local"
                id="endsAt"
                name="endsAt"
                class="form-control"
                [(ngModel)]="endsAtStr"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label" for="timeZone">Time Zone</label>
            <input
              type="text"
              id="timeZone"
              name="timeZone"
              class="form-control"
              [(ngModel)]="timeZone"
              required
            />
          </div>

          <!-- Description -->
          <div class="form-group">
            <label class="form-label" for="description">Event Description</label>
            <textarea
              id="description"
              name="description"
              class="form-control"
              [(ngModel)]="description"
              rows="6"
              placeholder="What will happen at this event? Agenda, requirements..."
            ></textarea>
          </div>

          <!-- Capacity & Guests & Waitlist -->
          <div class="form-row">
            <div class="form-group">
              <label class="form-label" for="capacity">Capacity Limit</label>
              <input
                type="number"
                id="capacity"
                name="capacity"
                class="form-control"
                [(ngModel)]="capacity"
                placeholder="Leave blank for unlimited"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="guestsLimit">Max Guests Per RSVP</label>
              <input
                type="number"
                id="guestsLimit"
                name="guestsLimit"
                class="form-control"
                [(ngModel)]="guestsPerRsvpLimit"
                min="0"
                max="10"
              />
            </div>
          </div>

          <div class="checkbox-row">
            <label class="checkbox-label">
              <input type="checkbox" [(ngModel)]="waitlistEnabled" name="waitlist" />
              <span>Enable Waitlist when capacity is reached</span>
            </label>
          </div>

          <!-- Recurrence Options -->
          <div class="recurrence-box">
            <label class="checkbox-label">
              <input type="checkbox" [(ngModel)]="isRecurring" name="isRecurring" />
              <span>Create as Recurring Series (e.g. Weekly / Monthly Meetups)</span>
            </label>

            @if (isRecurring) {
              <div class="rrule-config">
                <label class="form-label">Recurrence Frequency</label>
                <select class="form-select" [(ngModel)]="recurrenceFreq" name="recurrenceFreq">
                  <option value="FREQ=WEEKLY;INTERVAL=1">Weekly (Every week)</option>
                  <option value="FREQ=WEEKLY;INTERVAL=2">Bi-weekly (Every 2 weeks)</option>
                  <option value="FREQ=MONTHLY;INTERVAL=1">Monthly (Every month)</option>
                </select>
              </div>
            }
          </div>

          <div class="form-actions">
            <a routerLink="/events" class="btn btn-secondary">Cancel</a>
            <button
              type="submit"
              class="btn btn-primary btn-lg"
              [disabled]="saving() || !selectedGroupId || !title || !startsAtStr"
            >
              @if (saving()) {
                <span>Creating Event...</span>
              } @else {
                <span>Create & Publish Event</span>
              }
            </button>
          </div>
        </form>
      </div>

      <!-- Add New Venue Modal with Full Validation -->
      @if (showNewVenueModal()) {
        <div class="modal-backdrop" (click)="showNewVenueModal.set(false)">
          <div class="modal-content modal-venue" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2>Add New Venue</h2>
              <p class="modal-sub">Create a physical meeting location for your group meetups.</p>
            </div>

            @if (venueFormError()) {
              <div class="alert-danger-box">
                <span class="error-icon">⚠️</span>
                <div>
                  <strong>Could not save venue</strong>
                  <p>{{ venueFormError() }}</p>
                </div>
              </div>
            }

            <form (ngSubmit)="saveNewVenue()">
              <!-- Venue Name -->
              <div class="form-group">
                <label class="form-label" for="vName">
                  Venue Name <span class="required-star">*</span>
                </label>
                <input
                  type="text"
                  id="vName"
                  class="form-control"
                  [class.is-invalid]="venueFieldErrors()['name']"
                  [(ngModel)]="newVenueName"
                  (input)="clearVenueFieldError('name')"
                  name="vName"
                  placeholder="e.g. TechHub Cluj, Conference Room B"
                  required
                />
                @if (venueFieldErrors()['name']) {
                  <span class="form-error">{{ venueFieldErrors()['name'] }}</span>
                }
              </div>

              <!-- Street Address -->
              <div class="form-group">
                <label class="form-label" for="vAddress1">
                  Street Address <span class="required-star">*</span>
                </label>
                <input
                  type="text"
                  id="vAddress1"
                  class="form-control"
                  [class.is-invalid]="venueFieldErrors()['addressLine1']"
                  [(ngModel)]="newVenueAddressLine1"
                  (input)="clearVenueFieldError('addressLine1')"
                  name="vAddress1"
                  placeholder="e.g. Strada Memorandumului 21"
                  required
                />
                @if (venueFieldErrors()['addressLine1']) {
                  <span class="form-error">{{ venueFieldErrors()['addressLine1'] }}</span>
                }
              </div>

              <!-- Address Line 2 -->
              <div class="form-group">
                <label class="form-label" for="vAddress2">Address Line 2 (Optional)</label>
                <input
                  type="text"
                  id="vAddress2"
                  class="form-control"
                  [(ngModel)]="newVenueAddressLine2"
                  name="vAddress2"
                  placeholder="e.g. Floor 2, Suite 204"
                />
              </div>

              <!-- City & Region -->
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label" for="vCity">
                    City <span class="required-star">*</span>
                  </label>
                  <input
                    type="text"
                    id="vCity"
                    class="form-control"
                    [class.is-invalid]="venueFieldErrors()['city']"
                    [(ngModel)]="newVenueCity"
                    (input)="clearVenueFieldError('city')"
                    name="vCity"
                    placeholder="e.g. Cluj-Napoca"
                    required
                  />
                  @if (venueFieldErrors()['city']) {
                    <span class="form-error">{{ venueFieldErrors()['city'] }}</span>
                  }
                </div>

                <div class="form-group">
                  <label class="form-label" for="vRegion">State / Region (Optional)</label>
                  <input
                    type="text"
                    id="vRegion"
                    class="form-control"
                    [(ngModel)]="newVenueRegion"
                    name="vRegion"
                    placeholder="e.g. Cluj or CA"
                  />
                </div>
              </div>

              <!-- Postal Code & Country -->
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label" for="vPostal">Postal Code (Optional)</label>
                  <input
                    type="text"
                    id="vPostal"
                    class="form-control"
                    [(ngModel)]="newVenuePostalCode"
                    name="vPostal"
                    placeholder="e.g. 400114"
                  />
                </div>

                <div class="form-group">
                  <label class="form-label" for="vCountry">
                    Country <span class="required-star">*</span>
                  </label>
                  <input
                    type="text"
                    id="vCountry"
                    class="form-control"
                    [class.is-invalid]="venueFieldErrors()['country']"
                    [(ngModel)]="newVenueCountry"
                    (input)="clearVenueFieldError('country')"
                    name="vCountry"
                    placeholder="e.g. Romania or USA"
                    required
                  />
                  @if (venueFieldErrors()['country']) {
                    <span class="form-error">{{ venueFieldErrors()['country'] }}</span>
                  }
                </div>
              </div>

              <!-- Optional Coordinates -->
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label" for="vLat">Latitude (Optional)</label>
                  <input
                    type="number"
                    step="any"
                    id="vLat"
                    class="form-control font-mono"
                    [class.is-invalid]="venueFieldErrors()['latitude'] || venueFieldErrors()['location']"
                    [(ngModel)]="newVenueLatitude"
                    (input)="clearVenueFieldError('latitude')"
                    name="vLat"
                    placeholder="e.g. 46.7712"
                  />
                  @if (venueFieldErrors()['latitude']) {
                    <span class="form-error">{{ venueFieldErrors()['latitude'] }}</span>
                  }
                </div>

                <div class="form-group">
                  <label class="form-label" for="vLon">Longitude (Optional)</label>
                  <input
                    type="number"
                    step="any"
                    id="vLon"
                    class="form-control font-mono"
                    [class.is-invalid]="venueFieldErrors()['longitude'] || venueFieldErrors()['location']"
                    [(ngModel)]="newVenueLongitude"
                    (input)="clearVenueFieldError('longitude')"
                    name="vLon"
                    placeholder="e.g. 23.6236"
                  />
                  @if (venueFieldErrors()['longitude']) {
                    <span class="form-error">{{ venueFieldErrors()['longitude'] }}</span>
                  }
                </div>
              </div>

              <!-- Notes & Access info -->
              <div class="form-group">
                <label class="form-label" for="vNotes">Access Notes & Instructions (Optional)</label>
                <textarea
                  id="vNotes"
                  class="form-control"
                  [(ngModel)]="newVenueNotes"
                  name="vNotes"
                  rows="2"
                  placeholder="e.g. Buzzer #10, parking lot available behind building..."
                ></textarea>
              </div>

              <div class="modal-actions">
                <button type="button" class="btn btn-secondary" (click)="showNewVenueModal.set(false)">
                  Cancel
                </button>
                <button type="submit" class="btn btn-primary" [disabled]="savingVenue()">
                  @if (savingVenue()) {
                    <span>Saving Venue...</span>
                  } @else {
                    <span>Save Venue</span>
                  }
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .event-create-page {
        padding: 2.5rem 1.5rem;
      }
      .header-sub {
        color: var(--color-text-muted);
        font-size: 1.05rem;
      }
      .ai-copilot-card {
        background: linear-gradient(135deg, #f0fdf4 0%, #e0f2fe 100%);
        border: 1px solid #bae6fd;
        padding: 1.5rem;
        margin-bottom: 2rem;
        .copilot-header {
          display: flex;
          align-items: center;
          gap: 1rem;
          margin-bottom: 1rem;
          .ai-sparkle { font-size: 2rem; }
          h3 { margin: 0 0 0.25rem 0; color: #0369a1; }
          p { margin: 0; font-size: 0.875rem; color: #334155; }
        }
        .ai-btn {
          margin-top: 0.75rem;
          background: #ffffff;
          border-color: #0284c7;
          color: #0284c7;
          font-weight: 700;
          &:hover {
            background: #e0f2fe;
          }
        }
      }
      .venue-label-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      .checkbox-row {
        margin: 1.25rem 0;
      }
      .checkbox-label {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.95rem;
      }
      .recurrence-box {
        background: var(--color-bg-subtle);
        border: 1px solid var(--color-border);
        border-radius: 12px;
        padding: 1.25rem;
        margin-bottom: 2rem;
        .rrule-config {
          margin-top: 1rem;
          padding-top: 1rem;
          border-top: 1px solid var(--color-border);
        }
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
      .modal-venue {
        max-width: 600px;
        width: 100%;
        max-height: 90vh;
        overflow-y: auto;
      }
      .modal-header {
        margin-bottom: 1.25rem;
        h2 { margin-bottom: 0.25rem; }
        .modal-sub { color: var(--color-text-muted); font-size: 0.9rem; margin-bottom: 0; }
      }
      .required-star {
        color: var(--color-danger);
        font-weight: 700;
      }
      .alert-danger-box {
        background: var(--color-danger-subtle);
        border: 1px solid var(--color-danger);
        color: var(--color-danger-text);
        padding: 1rem;
        border-radius: 8px;
        display: flex;
        align-items: flex-start;
        gap: 0.75rem;
        margin-bottom: 1.25rem;
        .error-icon { font-size: 1.25rem; }
        strong { display: block; margin-bottom: 0.2rem; }
        p { margin: 0; font-size: 0.875rem; }
      }
      .modal-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
        margin-top: 1.5rem;
        padding-top: 1rem;
        border-top: 1px solid var(--color-border);
      }
      .font-mono {
        font-family: var(--font-mono);
      }
    `,
  ],
})
export class EventCreateComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly memberService = inject(MemberService);
  private readonly venueService = inject(VenueService);
  private readonly aiService = inject(AiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly myGroups = signal<GroupSummary[]>([]);
  readonly venues = signal<VenueSummary[]>([]);
  readonly draftingAi = signal(false);
  readonly saving = signal(false);
  readonly showNewVenueModal = signal(false);
  readonly savingVenue = signal(false);

  aiPrompt = '';
  selectedGroupId = '';
  title = '';
  description = '';
  format: EventFormat = 'IN_PERSON';
  selectedVenueId = '';
  onlineUrl = '';
  startsAtStr = '';
  endsAtStr = '';
  timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  capacity: number | null = 30;
  guestsPerRsvpLimit = 2;
  waitlistEnabled = true;

  isRecurring = false;
  recurrenceFreq = 'FREQ=WEEKLY;INTERVAL=1';

  // Venue Modal state & fields
  newVenueName = '';
  newVenueAddressLine1 = '';
  newVenueAddressLine2 = '';
  newVenueCity = '';
  newVenueRegion = '';
  newVenuePostalCode = '';
  newVenueCountry = '';
  newVenueLatitude: number | null = null;
  newVenueLongitude: number | null = null;
  newVenueNotes = '';

  readonly venueFieldErrors = signal<Record<string, string>>({});
  readonly venueFormError = signal<string | null>(null);

  ngOnInit() {
    this.memberService.getMyGroups().subscribe({
      next: groups => {
        this.myGroups.set(groups);
        if (groups.length > 0) {
          this.selectedGroupId = groups[0].id;
          this.onGroupChange();
        }
      },
    });

    // Default dates to next week Saturday 18:00
    const now = new Date();
    now.setDate(now.getDate() + 7);
    now.setHours(18, 0, 0, 0);
    this.startsAtStr = now.toISOString().slice(0, 16);
    now.setHours(20, 0, 0, 0);
    this.endsAtStr = now.toISOString().slice(0, 16);
  }

  onGroupChange() {
    if (this.selectedGroupId) {
      this.venueService.getVenuesForGroup(this.selectedGroupId).subscribe({
        next: vs => {
          this.venues.set(vs);
          if (vs.length > 0) {
            this.selectedVenueId = vs[0].id;
          }
        },
      });
    }
  }

  draftWithAi() {
    if (!this.aiPrompt.trim()) return;
    this.draftingAi.set(true);

    this.aiService
      .draftEvent({
        prompt: this.aiPrompt,
        groupContext: this.selectedGroupId,
      })
      .subscribe({
        next: draft => {
          this.draftingAi.set(false);
          this.title = draft.title;
          this.description = draft.description;
          this.format = draft.format;
          if (draft.estimatedCapacity) {
            this.capacity = draft.estimatedCapacity;
          }
          this.toast.success('AI draft generated! Review details below.');
        },
        error: () => {
          this.draftingAi.set(false);
          this.toast.error('AI draft generation failed');
        },
      });
  }

  openNewVenueModal() {
    this.venueFieldErrors.set({});
    this.venueFormError.set(null);
    this.newVenueName = '';
    this.newVenueAddressLine1 = '';
    this.newVenueAddressLine2 = '';
    this.newVenueCity = '';
    this.newVenueRegion = '';
    this.newVenuePostalCode = '';
    this.newVenueCountry = '';
    this.newVenueLatitude = null;
    this.newVenueLongitude = null;
    this.newVenueNotes = '';

    // Inherit city/country from group if available
    const activeGroup = this.myGroups().find(g => g.id === this.selectedGroupId);
    if (activeGroup?.city) {
      this.newVenueCity = activeGroup.city;
    }
    if (activeGroup?.country) {
      this.newVenueCountry = activeGroup.country;
    }

    this.showNewVenueModal.set(true);
  }

  clearVenueFieldError(fieldName: string) {
    this.venueFieldErrors.update(errs => {
      const copy = { ...errs };
      delete copy[fieldName];
      return copy;
    });
    if (Object.keys(this.venueFieldErrors()).length === 0) {
      this.venueFormError.set(null);
    }
  }

  saveNewVenue() {
    const errors: Record<string, string> = {};

    if (!this.newVenueName.trim()) {
      errors['name'] = 'Venue name is required';
    }
    if (!this.newVenueAddressLine1.trim()) {
      errors['addressLine1'] = 'Street address is required';
    }
    if (!this.newVenueCity.trim()) {
      errors['city'] = 'City is required';
    }
    if (!this.newVenueCountry.trim()) {
      errors['country'] = 'Country is required';
    }

    // Latitude & Longitude validation if provided
    if (this.newVenueLatitude !== null && this.newVenueLatitude !== undefined) {
      if (isNaN(this.newVenueLatitude) || this.newVenueLatitude < -90 || this.newVenueLatitude > 90) {
        errors['latitude'] = 'Latitude must be a valid number between -90 and 90';
      }
      if (this.newVenueLongitude === null || this.newVenueLongitude === undefined) {
        errors['longitude'] = 'Longitude is required when latitude is specified';
      }
    }

    if (this.newVenueLongitude !== null && this.newVenueLongitude !== undefined) {
      if (isNaN(this.newVenueLongitude) || this.newVenueLongitude < -180 || this.newVenueLongitude > 180) {
        errors['longitude'] = 'Longitude must be a valid number between -180 and 180';
      }
      if (this.newVenueLatitude === null || this.newVenueLatitude === undefined) {
        errors['latitude'] = 'Latitude is required when longitude is specified';
      }
    }

    if (Object.keys(errors).length > 0) {
      this.venueFieldErrors.set(errors);
      this.venueFormError.set('Please fill in all required fields marked with *');
      return;
    }

    if (!this.selectedGroupId) {
      this.toast.error('Please select a group first');
      return;
    }

    this.savingVenue.set(true);
    this.venueFormError.set(null);
    this.venueFieldErrors.set({});

    const location =
      this.newVenueLatitude !== null && this.newVenueLongitude !== null
        ? { latitude: Number(this.newVenueLatitude), longitude: Number(this.newVenueLongitude) }
        : undefined;

    this.venueService
      .createVenue(this.selectedGroupId, {
        name: this.newVenueName.trim(),
        addressLine1: this.newVenueAddressLine1.trim(),
        addressLine2: this.newVenueAddressLine2.trim() || undefined,
        city: this.newVenueCity.trim(),
        region: this.newVenueRegion.trim() || undefined,
        postalCode: this.newVenuePostalCode.trim() || undefined,
        country: this.newVenueCountry.trim(),
        location,
        notes: this.newVenueNotes.trim() || undefined,
      })
      .subscribe({
        next: created => {
          this.savingVenue.set(false);
          this.showNewVenueModal.set(false);
          this.venues.update(v => [...v, created]);
          this.selectedVenueId = created.id;
          this.toast.success(`Venue "${created.name}" created successfully!`);
        },
        error: err => {
          this.savingVenue.set(false);
          const backendFieldErrors: Record<string, string> = {};

          if (err?.error?.fieldErrors && Array.isArray(err.error.fieldErrors)) {
            for (const f of err.error.fieldErrors) {
              backendFieldErrors[f.field] = f.message;
            }
            this.venueFieldErrors.set(backendFieldErrors);
            this.venueFormError.set('Validation failed: Please check the highlighted fields below.');
          } else {
            this.venueFormError.set(err?.error?.message || 'Server error occurred while creating venue.');
          }
        },
      });
  }

  onSubmit() {
    if (!this.selectedGroupId || !this.title || !this.startsAtStr) return;
    this.saving.set(true);

    const startsAtIso = new Date(this.startsAtStr).toISOString();
    const endsAtIso = this.endsAtStr ? new Date(this.endsAtStr).toISOString() : undefined;

    if (this.isRecurring) {
      this.eventService
        .createEventSeries({
          groupId: this.selectedGroupId,
          recurrenceRule: this.recurrenceFreq,
          templateTitle: this.title,
          templateDescription: this.description,
          templateVenueId: this.format !== 'ONLINE' && this.selectedVenueId ? this.selectedVenueId : undefined,
          templateDurationMinutes: 120,
        })
        .subscribe({
          next: series => {
            this.eventService.generateSeriesOccurrences(series.id, startsAtIso, 4).subscribe({
              next: occurrences => {
                this.saving.set(false);
                this.toast.success('Recurring event series created!');
                if (occurrences.length > 0) {
                  this.router.navigate(['/events', occurrences[0].id]);
                } else {
                  this.router.navigate(['/dashboard']);
                }
              },
              error: err => {
                this.saving.set(false);
                this.toast.error(err?.error?.message || 'Series was created, but generating its occurrences failed');
                this.router.navigate(['/groups', this.selectedGroupId]);
              },
            });
          },
          error: err => {
            this.saving.set(false);
            this.toast.error(err?.error?.message || 'Failed to create recurring series');
          },
        });
      return;
    }

    this.eventService
      .createEvent(this.selectedGroupId, {
        title: this.title,
        description: this.description,
        format: this.format,
        venueId: this.format !== 'ONLINE' && this.selectedVenueId ? this.selectedVenueId : undefined,
        onlineUrl: this.format !== 'IN_PERSON' && this.onlineUrl ? this.onlineUrl : undefined,
        startsAt: startsAtIso,
        endsAt: endsAtIso,
        timeZone: this.timeZone,
        capacity: this.capacity || undefined,
        waitlistEnabled: this.waitlistEnabled,
        guestsPerRsvpLimit: this.guestsPerRsvpLimit,
        feeAmountMinor: 0,
        feeCurrency: 'USD',
      })
      .subscribe({
        next: created => {
          this.eventService.publishEvent(created.id).subscribe({
            next: () => {
              this.saving.set(false);
              this.toast.success('Event created and published!');
              this.router.navigate(['/events', created.id]);
            },
            error: () => {
              this.saving.set(false);
              this.router.navigate(['/events', created.id]);
            },
          });
        },
        error: err => {
          this.saving.set(false);
          this.toast.error(err?.error?.message || 'Failed to create event');
        },
      });
  }
}
