import { inject, Injectable } from '@angular/core';
import { z } from 'zod';
import { DatasetImportSchema, RandomGenerationSchema } from '../models/api';
import { ApiClient } from './api-client';

/** Outils de démonstration : jeux d'essai et livraisons aléatoires. */
@Injectable({ providedIn: 'root' })
export class DonneesApi {
  private readonly api = inject(ApiClient);

  async jeuxDisponibles(): Promise<string[]> {
    return (await this.api.get('/data', z.array(z.string()))).sort();
  }

  importer(dataset: string) {
    return this.api.post('/data/import', DatasetImportSchema, { dataset, clearBeforeImport: true });
  }

  genererLivraisons(count: number) {
    return this.api.post('/data/generate/livraisons', RandomGenerationSchema, { count });
  }
}
