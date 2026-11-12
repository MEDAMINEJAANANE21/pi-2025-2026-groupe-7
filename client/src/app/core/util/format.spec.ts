import { formatDecimal, formatDistance, formatEuros, formatFuel, formatInteger, formatWeight } from './format';

// Intl insère des espaces insécables : on les normalise pour garder des attentes lisibles.
const plain = (text: string) => text.replace(/[  ]/g, ' ');

describe('formats français', () => {
  it('affiche les distances en mètres puis en kilomètres', () => {
    expect(formatDistance(850)).toBe('850 m');
    expect(formatDistance(12_430)).toBe('12,4 km');
  });

  it('affiche carburant, poids et montants', () => {
    expect(formatFuel(3_250)).toBe('3,3 L');
    expect(formatWeight(2)).toBe('2,0 kg');
    expect(plain(formatEuros(1_234.5))).toBe('1 234,50 €');
    expect(plain(formatInteger(1_200))).toBe('1 200');
    expect(formatDecimal(6.25)).toBe('6,3');
  });

  it('affiche un tiret pour une valeur manquante', () => {
    for (const format of [formatDistance, formatFuel, formatWeight, formatEuros, formatInteger]) {
      expect(format(null)).toBe('—');
      expect(format(Number.NaN)).toBe('—');
    }
  });
});
