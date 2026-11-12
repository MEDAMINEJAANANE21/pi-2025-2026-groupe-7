import { inject, Injectable } from '@angular/core';
import { z } from 'zod';
import { ApiClient } from './api-client';

const DemoSchema = z.object({ livraisons: z.number(), chauffeurs: z.number(), vehicules: z.number() });
export type DemoSummary = z.infer<typeof DemoSchema>;

/** Jeu d'essai : dépôt, livraisons autour de Grenoble, chauffeurs et véhicules. */
@Injectable({ providedIn: 'root' })
export class DonneesApi {
  private readonly api = inject(ApiClient);

  chargerDemo(): Promise<DemoSummary> {
    return this.api.post('/data/demo', DemoSchema);
  }
}
