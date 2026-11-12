import { z } from 'zod'

export const CoordsSchema = z.object({
  lat: z.number().readonly(),
  lng: z.number().readonly(),
})

/**
 * Represents geographical coordinates.
 */
export type Coords = z.infer<typeof CoordsSchema>
