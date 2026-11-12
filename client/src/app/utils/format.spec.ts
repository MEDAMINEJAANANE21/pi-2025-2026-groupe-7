import { formatNumber } from './format';

describe('formatNumber', () => {
  it('prints integers as is', () => {
    expect(formatNumber(42)).toBe('42');
  });

  it('rounds decimals to one digit', () => {
    expect(formatNumber(3.14159)).toBe('3.1');
  });

  it('prints a dash for missing or non-finite values', () => {
    expect(formatNumber(null)).toBe('—');
    expect(formatNumber(undefined)).toBe('—');
    expect(formatNumber(Number.NaN)).toBe('—');
  });
});
