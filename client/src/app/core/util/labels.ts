import { Adresse, StatutGeneration, Strategie } from '../models/api';
import { formatDuration } from './duration';

const SEPARATIONS: Record<string, string> = {
  NONE: 'Sans découpage',
  ANGULAR: 'Secteurs',
  KMEDOIDS: 'K-medoids',
};

const OPTIMISATIONS: Record<string, string> = {
  VROOM: 'VROOM',
  GENETIC: 'Génétique',
  GREEDY: 'Plus proche voisin',
};

export const STATUT_LABELS: Record<StatutGeneration, string> = {
  INITIALISATION: 'Préparation',
  GENERATION_MATRICE: 'Calcul des distances',
  GENERATION_ROUTE: 'Calcul des itinéraires',
  TERMINE: 'Terminée',
};

export function isRunning(statut: StatutGeneration | null | undefined): boolean {
  return !!statut && statut !== 'TERMINE';
}

export function separationLabel(code: string | null | undefined): string {
  return code ? (SEPARATIONS[code] ?? code) : '—';
}

export function optimisationLabel(code: string | null | undefined): string {
  return code ? (OPTIMISATIONS[code] ?? code) : '—';
}

/** « K-medoids · Génétique · 3 véhicules · 8 h 00 max » */
export function strategieLabel(strategie: Strategie | null | undefined): string {
  if (!strategie) return '—';
  const parts = [separationLabel(strategie.separation), optimisationLabel(strategie.optimisation)];
  if (strategie.nbVehicules) parts.push(`${strategie.nbVehicules} véhicule${strategie.nbVehicules > 1 ? 's' : ''}`);
  if (strategie.maxTempsParVehicule) parts.push(`${formatDuration(strategie.maxTempsParVehicule)} max`);
  return parts.join(' · ');
}

export function streetLine(adresse: Adresse): string {
  return [adresse.numero, adresse.rue].filter((part) => !!part && part.trim()).join(' ') || 'Adresse sans rue';
}

export function cityLine(adresse: Adresse): string {
  return [adresse.codePostal, adresse.ville].filter(Boolean).join(' ');
}
