import { formatDuration, formatSeconds, isoToSeconds, minutesToIso, secondsToIso } from './duration';

describe('durées ISO-8601', () => {
  it('convertit une durée en secondes', () => {
    expect(isoToSeconds('PT1H30M')).toBe(5_400);
    expect(isoToSeconds('PT45.5S')).toBe(46);
    expect(isoToSeconds('P1DT2H')).toBe(93_600);
    expect(isoToSeconds('PT0S')).toBe(0);
  });

  it('renvoie null pour une valeur absente ou illisible', () => {
    expect(isoToSeconds(null)).toBeNull();
    expect(isoToSeconds('deux heures')).toBeNull();
  });

  it('produit une durée ISO depuis des secondes ou des minutes', () => {
    expect(secondsToIso(3_725)).toBe('PT1H2M5S');
    expect(secondsToIso(0)).toBe('PT0S');
    expect(minutesToIso(450)).toBe('PT7H30M');
  });

  it('affiche une durée lisible', () => {
    expect(formatDuration('PT2H5M')).toBe('2 h 05');
    expect(formatDuration('PT45M')).toBe('45 min');
    expect(formatDuration('PT30S')).toBe('30 s');
    expect(formatDuration(undefined)).toBe('—');
    expect(formatSeconds(3_600)).toBe('1 h 00');
  });
});
