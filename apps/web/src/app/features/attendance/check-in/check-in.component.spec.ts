import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AttendanceService } from '../../../core/services/attendance.service';
import { EventService } from '../../../core/services/event.service';
import { RsvpService } from '../../../core/services/rsvp.service';
import { CheckInComponent } from './check-in.component';

describe('CheckInComponent', () => {
  let component: CheckInComponent;
  let fixture: ComponentFixture<CheckInComponent>;
  let attendanceServiceSpy: any;
  let eventServiceSpy: any;
  let rsvpServiceSpy: any;

  beforeEach(async () => {
    attendanceServiceSpy = {
      checkIn: () =>
        of({
          ticketId: 't1',
          checkedInAt: '2026-09-10T18:05:00Z',
          admittedCount: 2,
          method: 'QR_SCAN',
          alreadyCheckedIn: false,
          attendeeName: 'Alex Morgan',
        }),
    };

    eventServiceSpy = {
      getEvent: () =>
        of({
          id: 'e1',
          title: 'Cloud Summit',
          capacity: 100,
          group: { id: 'g1', name: 'Cloud Group' },
        }),
    };

    rsvpServiceSpy = {
      getAttendees: () =>
        of([
          {
            member: { id: 'm1', displayName: 'Alex Morgan' },
            status: 'YES',
            guestCount: 1,
            checkedIn: false,
            respondedAt: '2026-09-01T12:00:00Z',
          },
          {
            member: { id: 'm2', displayName: 'Sarah Connor' },
            status: 'YES',
            guestCount: 0,
            checkedIn: true,
            respondedAt: '2026-09-01T13:00:00Z',
          },
        ]),
    };

    await TestBed.configureTestingModule({
      imports: [CheckInComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AttendanceService, useValue: attendanceServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: RsvpService, useValue: rsvpServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CheckInComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('id', 'e1');
    fixture.detectChanges();
  });

  it('should calculate checked-in count and attendance percentage', () => {
    expect(component).toBeTruthy();
    expect(component.attendees().length).toBe(2);
    expect(component.checkedInCount()).toBe(1);
    expect(component.checkInPercentage()).toBe(50);
  });

  it('submitTicketCode should submit code and record check-in', () => {
    component.manualTicketCode = 'valid_ticket_code_123';
    component.submitTicketCode();

    expect(component.lastCheckInResult()).toBeTruthy();
    expect(component.lastCheckInResult()!.attendeeName).toBe('Alex Morgan');
    expect(component.lastCheckInResult()!.alreadyCheckedIn).toBe(false);
  });
});
