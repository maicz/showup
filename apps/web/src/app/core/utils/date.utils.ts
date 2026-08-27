/**
 * Parses a datetime-local input string ("YYYY-MM-DDTHH:mm") in the context of the given
 * IANA timeZone (e.g. "Europe/London", "America/New_York", "UTC") and converts it to a UTC ISO string.
 *
 * This prevents local browser timezone drift when an organizer creates an event
 * in a timeZone different from their current device/browser setting.
 */
export function localDateTimeToUtcIso(dateTimeLocalStr: string, timeZone: string): string {
  if (!dateTimeLocalStr) return '';

  const [datePart, timePart = '00:00'] = dateTimeLocalStr.split('T');
  if (!datePart) return '';

  const dateSegments = datePart.split('-').map(Number);
  if (dateSegments.length < 3) return '';
  const [year, month, day] = dateSegments;

  const timeSegments = timePart.split(':').map(Number);
  const hours = timeSegments[0] ?? 0;
  const minutes = timeSegments[1] ?? 0;
  const seconds = timeSegments[2] ?? 0;

  // 1. Construct naive timestamp in UTC
  const naiveUtc = Date.UTC(year, month - 1, day, hours, minutes, seconds);

  try {
    // 2. Format naive timestamp in the target timeZone
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: timeZone || 'UTC',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    });

    const parts = formatter.formatToParts(new Date(naiveUtc));
    const findPart = (type: string) => Number(parts.find(p => p.type === type)?.value ?? 0);

    const tzYear = findPart('year');
    const tzMonth = findPart('month');
    const tzDay = findPart('day');
    let tzHour = findPart('hour');
    if (tzHour === 24) tzHour = 0;
    const tzMinute = findPart('minute');
    const tzSecond = findPart('second');

    const tzUtc = Date.UTC(tzYear, tzMonth - 1, tzDay, tzHour, tzMinute, tzSecond);
    const offsetDiff = tzUtc - naiveUtc;

    const correctedUtc = naiveUtc - offsetDiff;
    return new Date(correctedUtc).toISOString();
  } catch {
    // Fallback if invalid timezone passed
    return new Date(dateTimeLocalStr).toISOString();
  }
}
