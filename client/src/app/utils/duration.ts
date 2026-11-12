/**
 * Converts milliseconds to ISO-8601 duration format
 * @param milliseconds Duration in milliseconds
 * @returns ISO-8601 duration string (e.g., "PT2H30M15S")
 */
export function millisecondsToIsoDuration(milliseconds: number): string {
  if (milliseconds <= 0) {
    return 'PT0S';
  }

  let remaining = Math.floor(milliseconds / 1000); // Convert to seconds
  const hours = Math.floor(remaining / 3600);
  remaining %= 3600;
  const minutes = Math.floor(remaining / 60);
  const seconds = remaining % 60;

  let duration = 'PT';
  if (hours > 0) duration += `${hours}H`;
  if (minutes > 0) duration += `${minutes}M`;
  if (seconds > 0) duration += `${seconds}S`;

  return duration || 'PT0S';
}

/**
 * Converts minutes to ISO-8601 duration format
 * @param minutes Duration in minutes
 * @returns ISO-8601 duration string (e.g., "PT2H30M")
 */
export function minutesToIsoDuration(minutes: number): string {
  if (minutes <= 0) {
    return 'PT0S';
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  let duration = 'PT';
  if (hours > 0) duration += `${hours}H`;
  if (remainingMinutes > 0) duration += `${remainingMinutes}M`;

  return duration || 'PT0S';
}

/**
 * Formats an ISO-8601 duration string (e.g., "PT2H30M") into a human-readable label.
 * Returns "—" for null/undefined/empty, and omits zero components.
 */
export function formatIsoDuration(iso: string | null | undefined): string {
  if (!iso) return '—';
  const m = iso.match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$/);
  if (!m) return iso;
  const parts = [m[1] && `${m[1]}h`, m[2] && `${m[2]}m`, m[3] && `${m[3]}s`].filter(Boolean);
  return parts.length > 0 ? parts.join(' ') : '0s';
}

/**
 * Parses an ISO-8601 duration string to total seconds.
 */
function isoDurationToSeconds(iso: string): number {
  const m = iso.match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$/);
  if (!m) return 0;
  return (parseInt(m[1] ?? '0') * 3600) + (parseInt(m[2] ?? '0') * 60) + parseInt(m[3] ?? '0');
}

/**
 * Adds two nullable ISO-8601 duration strings and returns the sum as ISO-8601.
 * Returns null if both inputs are null/undefined.
 */
export function addIsoDurations(a: string | null | undefined, b: string | null | undefined): string | null {
  if (!a && !b) return null;
  const total = isoDurationToSeconds(a ?? 'PT0S') + isoDurationToSeconds(b ?? 'PT0S');
  return secondsToIsoDuration(total);
}

/**
 * Converts seconds to ISO-8601 duration format
 * @param seconds Duration in seconds
 * @returns ISO-8601 duration string (e.g., "PT1H30M45S")
 */
export function secondsToIsoDuration(seconds: number): string {
  if (seconds <= 0) {
    return 'PT0S';
  }

  const hours = Math.floor(seconds / 3600);
  const remaining = seconds % 3600;
  const minutes = Math.floor(remaining / 60);
  const secs = remaining % 60;

  let duration = 'PT';
  if (hours > 0) duration += `${hours}H`;
  if (minutes > 0) duration += `${minutes}M`;
  if (secs > 0) duration += `${secs}S`;

  return duration || 'PT0S';
}

