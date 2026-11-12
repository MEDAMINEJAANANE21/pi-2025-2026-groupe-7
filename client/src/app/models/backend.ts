import { z } from 'zod';

// DTOs returned by backend REST API
export const AdresseDtoSchema = z.object({
  coordonnees: z.object({
    lat: z.number(),
    lng: z.number()
  }),
  rue: z.string(),
  numero: z.string(),
  codePostal: z.string(),
  ville: z.string(),
});
export type AdresseDto = z.infer<typeof AdresseDtoSchema>;

export const ParkingResponseSchema = z.object({
  adresse: AdresseDtoSchema,
});
export type ParkingResponse = z.infer<typeof ParkingResponseSchema>;

export const LivraisonResponseSchema = z.object({
  id: z.number(),
  poids: z.number(),
  adresse: AdresseDtoSchema,
  livree: z.boolean(),
  dateCommande: z.string().nullable().optional(),
  tempsMontage: z.string().nullable().optional(),
});
export type LivraisonResponse = z.infer<typeof LivraisonResponseSchema>;

export const StrategieResponseSchema = z.object({
  separation: z.string().nullable().optional(),
  optimisation: z.string().nullable().optional(),
  nbVehicules: z.number().nullable().optional(),
  maxTempsParVehicule: z.string().nullable().optional(),
});
export type StrategieResponse = z.infer<typeof StrategieResponseSchema>;

export const JourneeResponseSchema = z.object({
  date: z.string(),
  statutGeneration: z.string().nullable(),
  strategie: StrategieResponseSchema.nullable().optional(),
});
export type JourneeResponse = z.infer<typeof JourneeResponseSchema>;

export const OptimizationConfigurationSchema = z.object({
  nbVehicules: z.number(),
  maxTempsParVehicule: z.string()
})

export type Configuration = z.infer<typeof OptimizationConfigurationSchema>;

export const SolutionStatisticsResponseSchema = z.object({
  id: z.number(),
  date: z.string(),
  strategie: StrategieResponseSchema.nullable(),
  nbVehicules: z.number(),
  nbLivraisonsTotal: z.number(),
  nbLivraisonsMin: z.number().nullable(),
  nbLivraisonsMax: z.number().nullable(),
  nbLivraisonsMoyenne: z.number().nullable(),
  carburantTotal: z.number().nullable(),
  carburantMin: z.number().nullable(),
  carburantMax: z.number().nullable(),
  carburantMoyenne: z.number().nullable(),
  distanceTotal: z.number().nullable(),
  distanceMin: z.number().nullable(),
  distanceMax: z.number().nullable(),
  distanceMoyenne: z.number().nullable(),
  poidsTotal: z.number().nullable(),
  poidsMin: z.number().nullable(),
  poidsMax: z.number().nullable(),
  poidsMoyenne: z.number().nullable(),
  tempsRouteTotal: z.string().nullable(),
  tempsRouteMin: z.string().nullable(),
  tempsRouteMax: z.string().nullable(),
  tempsRouteMoyenne: z.string().nullable(),
  tempsMontageTotal: z.string().nullable(),
  tempsMontageMin: z.string().nullable(),
  tempsMontageMax: z.string().nullable(),
  tempsMontageMoyenne: z.string().nullable(),
  tempsTotal: z.string().nullable(),
  tempsTotalMin: z.string().nullable(),
  tempsTotalMax: z.string().nullable(),
  tempsTotalMoyenne: z.string().nullable(),
});
export type SolutionStatisticsResponse = z.infer<typeof SolutionStatisticsResponseSchema>;

export const TourneeStatisticsResponseSchema = z.object({
  carburant: z.number().nullable().optional(),
  distance: z.number().nullable().optional(),
  tempsTrajet: z.string().nullable().optional(),
  tempsMontage: z.string().nullable().optional(),
});
export type TourneeStatisticsResponse = z.infer<typeof TourneeStatisticsResponseSchema>;

export const SolutionResponseSchema = z.object({
  id: z.number().readonly(),
  date: z.string().readonly(),
  strategie: StrategieResponseSchema.nullable().optional(),
  tournees: z.array(z.object({
    id: z.number(),
    livraisons: z.array(LivraisonResponseSchema),
    statistiques: TourneeStatisticsResponseSchema.nullable().optional(),
  })).readonly(),
  statistiques: SolutionStatisticsResponseSchema.nullable().optional(),
})

export type SolutionResponse = z.infer<typeof SolutionResponseSchema>

export const StrategieRequestSchema = z.object({
  nbVehicules: z.number(),
  maxTempsParVehicule: z.string(),
  livraisonIds: z.array(z.number()),
});
export type StrategieRequest = z.infer<typeof StrategieRequestSchema>;

export const SolutionTourneesUpdateRequestSchema = z.object({
  tournees: z.array(z.object({
    tourneeId: z.number(),
    livraisonIds: z.array(z.number()),
  })),
});
export type SolutionTourneesUpdateRequest = z.infer<typeof SolutionTourneesUpdateRequestSchema>;

export const SolutionLivraisonsResponseSchema = z.object({
  dansSolution: z.array(LivraisonResponseSchema),
  horsSolution: z.array(LivraisonResponseSchema),
});
export type SolutionLivraisonsResponse = z.infer<typeof SolutionLivraisonsResponseSchema>;

export const JourneeStatusUpdateSchema = z.object({
  date: z.string(),
  statutGeneration: z.string(),
  timestamp: z.string(),
});
export type JourneeStatusUpdate = z.infer<typeof JourneeStatusUpdateSchema>;

export const CoordonneeSchema = z.object({
  lat: z.number(),
  lng: z.number(),
});
export type Coordonnee = z.infer<typeof CoordonneeSchema>;

export const RouteResponseSchema = z.object({
  coordinates: z.array(CoordonneeSchema),
});
export type RouteResponse = z.infer<typeof RouteResponseSchema>;
