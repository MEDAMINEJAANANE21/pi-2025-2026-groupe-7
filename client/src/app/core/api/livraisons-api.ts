import { inject, Injectable } from '@angular/core';
import { z } from 'zod';
import { Livraison, LivraisonRequest, LivraisonSchema } from '../models/api';
import { ApiClient } from './api-client';

export interface LivraisonFilters {
  livree?: boolean | null;
  dateDebut?: string | null;
  dateFin?: string | null;
}

@Injectable({ providedIn: 'root' })
export class LivraisonsApi {
  private readonly api = inject(ApiClient);

  list(filters: LivraisonFilters = {}): Promise<Livraison[]> {
    return this.api.get('/livraisons', z.array(LivraisonSchema), { ...filters });
  }

  create(request: LivraisonRequest): Promise<Livraison> {
    return this.api.post('/livraisons', LivraisonSchema, request);
  }

  update(id: number, request: LivraisonRequest): Promise<Livraison> {
    return this.api.put(`/livraisons/${id}`, LivraisonSchema, request);
  }

  delete(id: number): Promise<void> {
    return this.api.delete(`/livraisons/${id}`);
  }

  setLivree(id: number, livree: boolean): Promise<Livraison> {
    return this.api.patch(`/livraisons/${id}/statut`, LivraisonSchema, { livree });
  }
}
