/**
 * Une couleur par véhicule, reprise sur la carte, le ruban et la feuille de route.
 * Teintes assez sombres pour rester lisibles sur le fond clair de la carte.
 */
export const ROUTE_COLORS = [
  '#2563eb', '#0f9d6b', '#d97706', '#7c3aed', '#db2777', '#0891b2',
  '#65a30d', '#b91c1c', '#4338ca', '#0d9488', '#a16207', '#9333ea',
] as const;

export function routeColor(index: number): string {
  return ROUTE_COLORS[((index % ROUTE_COLORS.length) + ROUTE_COLORS.length) % ROUTE_COLORS.length];
}
