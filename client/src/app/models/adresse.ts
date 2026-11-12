import { z } from 'zod'
import { CoordsSchema } from './coords';
import type { LivraisonResponse } from './backend';

export const AdresseSchema = z.object({
  coords: CoordsSchema.readonly(),
  name: z.string().readonly(),
  postCode: z.string().readonly(),
  city: z.string().readonly(),
})

export type Adresse = z.infer<typeof AdresseSchema>

/** Adresse enriched with optional map-display metadata. */
export type AdresseWithMeta = Adresse & {
  color?: string;
  horsSolution?: boolean;
  livraison?: LivraisonResponse;
}
