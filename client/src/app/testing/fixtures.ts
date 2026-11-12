import { Livraison, SolutionStatistiques, Tournee } from '../core/models/api';

export function livraison(id: number, overrides: Partial<Livraison> = {}): Livraison {
  return {
    id,
    reference: `LIV-${String(id).padStart(8, '0')}`,
    poids: 5,
    livree: false,
    dateCommande: '2026-11-01',
    tempsMontage: 'PT0S',
    dateLivraison: null,
    adresse: { coordonnees: { lat: 45.19, lng: 5.72 }, numero: String(id), rue: 'Rue des Alpes', codePostal: '38000', ville: 'Grenoble' },
    ...overrides,
  };
}

export function tournee(id: number, livraisons: Livraison[]): Tournee {
  return { id, livraisons, statistiques: null, finances: null, poidsTotal: 0, chauffeur: null, vehicule: null };
}

export function solutionStats(id: number, overrides: Partial<SolutionStatistiques> = {}): SolutionStatistiques {
  return {
    id,
    date: '2026-11-12',
    strategie: { separation: 'NONE', optimisation: 'GREEDY', nbVehicules: 1, maxTempsParVehicule: 'PT8H' },
    nbVehicules: 1,
    nbLivraisonsTotal: 10,
    nbLivraisonsMin: 10,
    nbLivraisonsMax: 10,
    carburantTotal: 1_000,
    distanceTotal: 10_000,
    poidsTotal: 50,
    tempsRouteTotal: 'PT1H',
    tempsMontageTotal: 'PT10M',
    tempsTotal: 'PT1H10M',
    tempsTotalMin: 'PT1H10M',
    tempsTotalMax: 'PT1H10M',
    finances: { coutCarburant: 2, coutChauffeur: 20, coutTotal: 22, chiffreAffaires: 90, marge: 68 },
    ...overrides,
  };
}
