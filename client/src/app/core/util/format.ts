const EMPTY = '—';

const integer = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });
const oneDecimal = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });

function isMissing(value: number | null | undefined): value is null | undefined {
  return value === null || value === undefined || !Number.isFinite(value);
}

export function formatInteger(value: number | null | undefined): string {
  return isMissing(value) ? EMPTY : integer.format(value);
}

export function formatDecimal(value: number | null | undefined): string {
  return isMissing(value) ? EMPTY : oneDecimal.format(value);
}

/** Distance en mètres → « 850 m » ou « 12,4 km ». */
export function formatDistance(meters: number | null | undefined): string {
  if (isMissing(meters)) return EMPTY;
  return meters < 1000 ? `${integer.format(meters)} m` : `${oneDecimal.format(meters / 1000)} km`;
}

/** Carburant en millilitres → « 3,2 L ». */
export function formatFuel(milliliters: number | null | undefined): string {
  return isMissing(milliliters) ? EMPTY : `${oneDecimal.format(milliliters / 1000)} L`;
}

export function formatWeight(kilograms: number | null | undefined): string {
  return isMissing(kilograms) ? EMPTY : `${oneDecimal.format(kilograms)} kg`;
}

export function formatEuros(value: number | null | undefined): string {
  return isMissing(value) ? EMPTY : euros.format(value);
}
