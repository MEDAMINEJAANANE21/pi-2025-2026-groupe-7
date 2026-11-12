import { z } from 'zod';

/**
 * Schémas des réponses de l'API. Chaque réponse est validée à l'arrivée :
 * un changement de contrat côté serveur casse tôt et à un seul endroit.
 */

export const CoordonneesSchema = z.object({ lat: z.number(), lng: z.number() });
export type Coordonnees = z.infer<typeof CoordonneesSchema>;

export const AdresseSchema = z.object({
  coordonnees: CoordonneesSchema.nullable(),
  rue: z.string().nullable(),
  numero: z.string().nullable(),
  codePostal: z.string().nullable(),
  ville: z.string().nullable(),
});
export type Adresse = z.infer<typeof AdresseSchema>;

export const LivraisonSchema = z.object({
  id: z.number(),
  reference: z.string().nullable(),
  poids: z.number(),
  adresse: AdresseSchema,
  livree: z.boolean(),
  dateCommande: z.string().nullable(),
  tempsMontage: z.string().nullable(),
  dateLivraison: z.string().nullable(),
});
export type Livraison = z.infer<typeof LivraisonSchema>;

export const StrategieSchema = z.object({
  separation: z.string().nullable(),
  optimisation: z.string().nullable(),
  nbVehicules: z.number().nullable(),
  maxTempsParVehicule: z.string().nullable(),
});
export type Strategie = z.infer<typeof StrategieSchema>;

export const StatutGenerationSchema = z.enum(['INITIALISATION', 'GENERATION_MATRICE', 'GENERATION_ROUTE', 'TERMINE']);
export type StatutGeneration = z.infer<typeof StatutGenerationSchema>;

export const JourneeSchema = z.object({
  date: z.string(),
  statutGeneration: StatutGenerationSchema.nullable(),
  strategie: StrategieSchema.nullable(),
});
export type Journee = z.infer<typeof JourneeSchema>;

export const JourneeResumeSchema = z.object({
  date: z.string(),
  statutGeneration: StatutGenerationSchema.nullable(),
  nbSolutions: z.number(),
  solutionChoisieId: z.number().nullable(),
});
export type JourneeResume = z.infer<typeof JourneeResumeSchema>;

export const JourneeStatusUpdateSchema = z.object({
  date: z.string(),
  statutGeneration: StatutGenerationSchema,
  timestamp: z.string(),
});

export const FinancesSchema = z.object({
  coutCarburant: z.number(),
  coutChauffeur: z.number(),
  coutTotal: z.number(),
  chiffreAffaires: z.number(),
  marge: z.number(),
});
export type Finances = z.infer<typeof FinancesSchema>;

export const ChauffeurSchema = z.object({
  id: z.number(),
  prenom: z.string(),
  nom: z.string(),
  telephone: z.string().nullable(),
  tauxHoraire: z.number().nullable(),
  actif: z.boolean(),
  photoUrl: z.string().nullable(),
});
export type Chauffeur = z.infer<typeof ChauffeurSchema>;

export const VehiculeSchema = z.object({
  id: z.number(),
  immatriculation: z.string(),
  modele: z.string().nullable(),
  capaciteKg: z.number().nullable(),
  consommationLitresAux100Km: z.number().nullable(),
  actif: z.boolean(),
  photoUrl: z.string().nullable(),
});
export type Vehicule = z.infer<typeof VehiculeSchema>;

export const TourneeStatistiquesSchema = z.object({
  carburant: z.number().nullable(),
  distance: z.number().nullable(),
  tempsTrajet: z.string().nullable(),
  tempsMontage: z.string().nullable(),
});
export type TourneeStatistiques = z.infer<typeof TourneeStatistiquesSchema>;

export const TourneeSchema = z.object({
  id: z.number(),
  livraisons: z.array(LivraisonSchema),
  statistiques: TourneeStatistiquesSchema.nullable(),
  finances: FinancesSchema.nullable(),
  poidsTotal: z.number(),
  chauffeur: ChauffeurSchema.nullable(),
  vehicule: VehiculeSchema.nullable(),
});
export type Tournee = z.infer<typeof TourneeSchema>;

export const SolutionStatistiquesSchema = z.object({
  id: z.number(),
  date: z.string().nullable(),
  strategie: StrategieSchema.nullable(),
  nbVehicules: z.number(),
  nbLivraisonsTotal: z.number(),
  nbLivraisonsMin: z.number().nullable(),
  nbLivraisonsMax: z.number().nullable(),
  carburantTotal: z.number().nullable(),
  distanceTotal: z.number().nullable(),
  poidsTotal: z.number().nullable(),
  tempsRouteTotal: z.string().nullable(),
  tempsMontageTotal: z.string().nullable(),
  tempsTotal: z.string().nullable(),
  tempsTotalMin: z.string().nullable(),
  tempsTotalMax: z.string().nullable(),
  finances: FinancesSchema.nullable(),
});
export type SolutionStatistiques = z.infer<typeof SolutionStatistiquesSchema>;

export const SolutionSchema = z.object({
  id: z.number(),
  date: z.string(),
  strategie: StrategieSchema.nullable(),
  tournees: z.array(TourneeSchema),
  statistiques: SolutionStatistiquesSchema.nullable(),
});
export type Solution = z.infer<typeof SolutionSchema>;

export const SolutionLivraisonsSchema = z.object({
  dansSolution: z.array(LivraisonSchema),
  horsSolution: z.array(LivraisonSchema),
});
export type SolutionLivraisons = z.infer<typeof SolutionLivraisonsSchema>;

export const ParkingSchema = z.object({ adresse: AdresseSchema });

export const RouteSchema = z.object({ coordinates: z.array(CoordonneesSchema) });

export const DatasetImportSchema = z.object({
  dataset: z.string(),
  addressesImported: z.number(),
  cheminsImported: z.number(),
});

export const RandomGenerationSchema = z.object({
  resource: z.string(),
  generated: z.number(),
  addressesAvailable: z.number(),
  livraisonsAvailable: z.number(),
});

export const ErrorSchema = z.object({ uri: z.string(), message: z.string() });

/** Corps envoyés au serveur. */
export interface AdresseRequest {
  numero: string;
  rue: string;
  codePostal: string;
  ville: string;
  lat: number;
  lng: number;
}

export interface LivraisonRequest {
  adresse: AdresseRequest;
  poids: number;
  tempsMontage: string | null;
  dateCommande: string | null;
}

export interface ChauffeurRequest {
  prenom: string;
  nom: string;
  telephone: string | null;
  tauxHoraire: number | null;
  actif: boolean;
}

export interface VehiculeRequest {
  immatriculation: string;
  modele: string | null;
  capaciteKg: number | null;
  consommationLitresAux100Km: number | null;
  actif: boolean;
}

export interface OptimisationRequest {
  nbVehicules: number;
  maxTempsParVehicule: string;
  livraisonIds: number[];
}

export interface TourneeUpdate {
  tourneeId: number;
  livraisonIds: number[];
}
