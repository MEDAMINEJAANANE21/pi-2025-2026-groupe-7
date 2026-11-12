const ISO_DURATION = /^P(?:(\d+(?:\.\d+)?)D)?(?:T(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?)?$/;

/** Convertit une durée ISO-8601 (PT1H30M, P1DT2H…) en secondes ; null si illisible. */
export function isoToSeconds(iso: string | null | undefined): number | null {
  if (!iso) return null;
  const match = ISO_DURATION.exec(iso);
  if (!match) return null;
  const [, days, hours, minutes, seconds] = match.map((part) => Number(part ?? 0));
  return Math.round(days * 86_400 + hours * 3_600 + minutes * 60 + seconds);
}

export function secondsToIso(totalSeconds: number): string {
  const seconds = Math.max(0, Math.round(totalSeconds));
  const h = Math.floor(seconds / 3_600);
  const m = Math.floor((seconds % 3_600) / 60);
  const s = seconds % 60;
  if (seconds === 0) return 'PT0S';
  return `PT${h ? `${h}H` : ''}${m ? `${m}M` : ''}${s ? `${s}S` : ''}`;
}

export function minutesToIso(minutes: number): string {
  return secondsToIso(minutes * 60);
}

/** Durée lisible au format « 2 h 05 », « 45 min » ou « 30 s ». */
export function formatDuration(iso: string | null | undefined): string {
  const seconds = isoToSeconds(iso);
  if (seconds === null) return '—';
  return formatSeconds(seconds);
}

export function formatSeconds(seconds: number): string {
  if (seconds < 60) return `${seconds} s`;
  const totalMinutes = Math.round(seconds / 60);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  if (h === 0) return `${m} min`;
  return `${h} h ${String(m).padStart(2, '0')}`;
}
