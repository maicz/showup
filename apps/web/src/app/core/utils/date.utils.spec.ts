import { describe, expect, it } from 'vitest';
import { localDateTimeToUtcIso } from './date.utils';

describe('date.utils', () => {
  it('handles empty input gracefully', () => {
    expect(localDateTimeToUtcIso('', 'UTC')).toBe('');
  });

  it('converts datetime-local correctly in UTC timezone', () => {
    const iso = localDateTimeToUtcIso('2026-09-01T14:30', 'UTC');
    expect(iso).toBe('2026-09-01T14:30:00.000Z');
  });

  it('correctly offsets UTC when event is in Europe/Bucharest (UTC+3 in summer)', () => {
    const iso = localDateTimeToUtcIso('2026-08-15T18:00', 'Europe/Bucharest');
    expect(iso).toBe('2026-08-15T15:00:00.000Z');
  });

  it('correctly offsets UTC when event is in America/New_York (UTC-4 in summer)', () => {
    const iso = localDateTimeToUtcIso('2026-08-15T18:00', 'America/New_York');
    expect(iso).toBe('2026-08-15T22:00:00.000Z');
  });
});
