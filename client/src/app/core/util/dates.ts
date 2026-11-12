/**
 * Les dates métier sont des jours civils (yyyy-MM-dd) sans fuseau : on les manipule
 * en heure locale pour qu'un « jeudi » reste un jeudi quel que soit le navigateur.
 */

export const DAY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export function toIsoDay(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function parseIsoDay(day: string): Date | null {
  if (!DAY_PATTERN.test(day)) return null;
  const [y, m, d] = day.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return date.getFullYear() === y && date.getMonth() === m - 1 && date.getDate() === d ? date : null;
}

export function today(): string {
  return toIsoDay(new Date());
}

export function addDays(day: string, amount: number): string {
  const date = parseIsoDay(day) ?? new Date();
  date.setDate(date.getDate() + amount);
  return toIsoDay(date);
}

export function formatLongDay(day: string): string {
  const date = parseIsoDay(day);
  if (!date) return day;
  const label = date.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export function formatShortDay(day: string | null | undefined): string {
  const date = day ? parseIsoDay(day) : null;
  return date ? date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) : '—';
}

export function formatMonth(year: number, monthIndex: number): string {
  const label = new Date(year, monthIndex, 1).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export interface CalendarDay {
  iso: string;
  dayOfMonth: number;
  inMonth: boolean;
}

/** Grille du mois en semaines complètes, du lundi au dimanche. */
export function monthGrid(year: number, monthIndex: number): CalendarDay[][] {
  const first = new Date(year, monthIndex, 1);
  const offsetToMonday = (first.getDay() + 6) % 7;
  const cursor = new Date(year, monthIndex, 1 - offsetToMonday);
  const weeks: CalendarDay[][] = [];
  do {
    const week: CalendarDay[] = [];
    for (let i = 0; i < 7; i++) {
      week.push({ iso: toIsoDay(cursor), dayOfMonth: cursor.getDate(), inMonth: cursor.getMonth() === monthIndex });
      cursor.setDate(cursor.getDate() + 1);
    }
    weeks.push(week);
  } while (cursor.getMonth() === monthIndex);
  return weeks;
}
