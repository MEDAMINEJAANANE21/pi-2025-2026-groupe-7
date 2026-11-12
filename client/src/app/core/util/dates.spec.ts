import { addDays, formatLongDay, monthGrid, parseIsoDay, toIsoDay } from './dates';

describe('dates de journée', () => {
  it('lit et écrit le format yyyy-MM-dd en heure locale', () => {
    const date = parseIsoDay('2026-11-12');
    expect(date?.getDate()).toBe(12);
    expect(toIsoDay(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('refuse une date impossible', () => {
    expect(parseIsoDay('2026-02-30')).toBeNull();
    expect(parseIsoDay('12/11/2026')).toBeNull();
  });

  it('passe au jour suivant même en fin de mois', () => {
    expect(addDays('2026-01-31', 1)).toBe('2026-02-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('écrit le jour en toutes lettres avec une majuscule', () => {
    expect(formatLongDay('2026-11-12')).toBe('Jeudi 12 novembre 2026');
  });

  it('construit des semaines complètes du lundi au dimanche', () => {
    const weeks = monthGrid(2026, 10); // novembre 2026 commence un dimanche
    expect(weeks.every((week) => week.length === 7)).toBeTrue();
    expect(weeks[0][0].iso).toBe('2026-10-26');
    expect(weeks[0][6]).toEqual({ iso: '2026-11-01', dayOfMonth: 1, inMonth: true });
    expect(weeks.at(-1)?.[0].iso).toBe('2026-11-30');
  });
});
