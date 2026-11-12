import { inject, Injectable } from '@angular/core';
import { Tournee, TourneeSchema } from '../models/api';
import { ApiClient, apiUrl } from './api-client';

@Injectable({ providedIn: 'root' })
export class TourneesApi {
  private readonly api = inject(ApiClient);

  get(id: number): Promise<Tournee> {
    return this.api.get(`/tournees/${id}`, TourneeSchema);
  }

  affecter(id: number, chauffeurId: number | null, vehiculeId: number | null): Promise<Tournee> {
    return this.api.put(`/tournees/${id}/affectation`, TourneeSchema, { chauffeurId, vehiculeId });
  }

  terminer(id: number): Promise<Tournee> {
    return this.api.post(`/tournees/${id}/terminer`, TourneeSchema);
  }

  /** Lien direct : le navigateur télécharge le fichier grâce à l'en-tête Content-Disposition. */
  feuilleDeRouteCsvUrl(id: number): string {
    return apiUrl(`/tournees/${id}/feuille-de-route`);
  }
}
