import { inject, Injectable } from '@angular/core';
import { z } from 'zod';
import {
  Journee,
  JourneeResume,
  JourneeResumeSchema,
  JourneeSchema,
  OptimisationRequest,
  Solution,
  SolutionSchema,
  SolutionStatistiques,
  SolutionStatistiquesSchema,
} from '../models/api';
import { ApiClient, isNotFound } from './api-client';

@Injectable({ providedIn: 'root' })
export class JourneesApi {
  private readonly api = inject(ApiClient);

  calendrier(debut: string, fin: string): Promise<JourneeResume[]> {
    return this.api.get('/journees', z.array(JourneeResumeSchema), { debut, fin });
  }

  get(date: string): Promise<Journee> {
    return this.api.get(`/journees/${date}`, JourneeSchema);
  }

  solutions(date: string): Promise<SolutionStatistiques[]> {
    return this.api.get(`/journees/${date}/solutions`, z.array(SolutionStatistiquesSchema));
  }

  async solutionChoisie(date: string): Promise<Solution | null> {
    try {
      return await this.api.get(`/journees/${date}/solutions/choisie`, SolutionSchema);
    } catch (error) {
      // 404 signifie « rien de choisi » : ce n'est pas une panne.
      if (isNotFound(error)) return null;
      throw error;
    }
  }

  optimiser(date: string, request: OptimisationRequest): Promise<Journee> {
    return this.api.post(`/journees/${date}/optimization`, JourneeSchema, request);
  }
}
