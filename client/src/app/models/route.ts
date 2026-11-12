import { z } from 'zod'
import { CoordsSchema } from './coords'

export const RouteSchema = CoordsSchema.array()
export type Route = z.infer<typeof RouteSchema>
