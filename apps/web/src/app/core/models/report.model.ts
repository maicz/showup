export interface StaffScanTotal {
  memberId: string;
  displayName: string;
  scanCount: number;
}

export interface CheckInTime {
  minute: string;
  count: number;
}

export interface EventAttendanceReport {
  eventId: string;
  eventTitle: string;
  registeredCount: number;
  attendedCount: number;
  noShowCount: number;
  attendanceRate: number;
  checkInTimeline: CheckInTime[];
  staffScans: StaffScanTotal[];
}

export interface GroupActivityReport {
  groupId: string;
  groupName: string;
  eventsHeld: number;
  totalAttendees: number;
  averageAttendancePerEvent: number;
  newMembersCount: number;
  currentRatingAverage?: number;
}
