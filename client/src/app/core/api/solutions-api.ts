import { inject, Injectable } from '@angular/core';
import { Solution, SolutionLivraisons, SolutionLivraisonsSchema, SolutionSchema, TourneeUpdate } from '../models/api';
import { ApiClient } from './api-client';

@Injectable({ providedIn: 'root' })
export class SolutionsApi {
  private readonly api = inject(ApiClient);

  get(id: number): Promise<Solution> {
    return this.api.get(`/solutions/${id}`, SolutionSchema);
  }

  livraisons(id: number): Promise<SolutionLivraisons> {
    return this.api.get(`/solutions/${id}/livraisons`, SolutionLivraisonsSchema);
  }

  choisir(id: number): Promise<Solution> {
    return this.api.put(`/solutions/${id}/choisie`, SolutionSchema);
  }

  enregistrerTournees(id: number, tournees: TourneeUpdate[]): Promise<Solution> {
    return this.api.put(`/solutions/${id}/tournees`, SolutionSchema, { tournees });
  }
}
