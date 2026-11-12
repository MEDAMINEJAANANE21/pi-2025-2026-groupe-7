import { z } from 'zod'

// Represent either [number, number] or [number, number, number]
export const LatLngTupleSchema = z.union([
  z.tuple([z.number(), z.number()]),
  z.tuple([z.number(), z.number(), z.number()]),
])

export type LatLngTuple = z.infer<typeof LatLngTupleSchema>
