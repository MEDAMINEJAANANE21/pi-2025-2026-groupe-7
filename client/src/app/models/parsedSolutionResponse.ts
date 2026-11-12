import { z } from 'zod';
import {
  LivraisonResponseSchema,
  SolutionStatisticsResponseSchema,
  StrategieResponseSchema,
  TourneeStatisticsResponseSchema,
} from './backend';

export const ParsedSolutionResponseSchema = z.object({
  id: z.number().readonly(),
  date: z.string().readonly(),
  strategie: StrategieResponseSchema.nullable().optional(),
  tournees: LivraisonResponseSchema.array().array().readonly(),
  statistiques: SolutionStatisticsResponseSchema.nullable().optional(),
  tourneeStatistics: z
    .array(
      z.object({
        id: z.number(),
        livraisonIds: z.array(z.number()),
        statistiques: TourneeStatisticsResponseSchema.nullable().optional(),
      }),
    )
    .nullable()
    .optional(),
});

export type ParsedSolutionResponse = z.infer<typeof ParsedSolutionResponseSchema>;
