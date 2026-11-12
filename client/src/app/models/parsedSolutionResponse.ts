import { z } from 'zod'
import { LivraisonResponseSchema, StrategieResponseSchema } from './backend';

export const ParsedSolutionResponseSchema = z.object({
  id: z.number().readonly(),
  date: z.string().readonly(),
  strategie: StrategieResponseSchema.nullable().optional(),
  tournees: LivraisonResponseSchema.array().array().readonly(),
  statistiques: z.any().nullable().optional(),
  tourneeStatistics: z.array(z.object({
    id: z.number(),
    livraisonIds: z.array(z.number()),
    statistiques: z.any().nullable().optional()
  })).nullable().optional(),
})

export type ParsedSolutionResponse = z.infer<typeof ParsedSolutionResponseSchema>
