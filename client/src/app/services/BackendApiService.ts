import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PromiseHttpClient } from './PromiseHttpClient';
import { LatLngTuple } from '../models/LatLngTuple';
import {
  JourneeResponse,
  JourneeResponseSchema,
  LivraisonResponse,
  LivraisonResponseSchema,
  ParkingResponseSchema,
  SolutionResponse,
  SolutionResponseSchema,
  SolutionStatisticsResponseSchema,
  SolutionStatisticsResponse,
  Configuration,
  StrategieRequest,
  SolutionTourneesUpdateRequest,
  SolutionLivraisonsResponseSchema,
  SolutionLivraisonsResponse,
  RouteResponseSchema,
} from '../models/backend';
import { Adresse } from '../models/adresse';
import { ParsedSolutionResponse } from '../models/parsedSolutionResponse';
import { toAdresse } from '../utils/adresse';

@Injectable({
  providedIn: 'root',
})
export class BackendApiService {
  private readonly _http = inject(PromiseHttpClient);

  // Base URL for backend routes proxied in development and served by Spring Boot in production.
  private readonly _apiBaseUrl = '/api';

  private _url(path: string): string {
    return `${this._apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`;
  }

  public async getDirections(lngLatCoordinates: ReadonlyArray<LatLngTuple>): Promise<ReadonlyArray<LatLngTuple>> {
    const response = await this._http.post<unknown>(this._url('/routes'), {
      adresses: lngLatCoordinates.map(([lng, lat]) => ({ lat, lng })),
    });
    const { coordinates } = RouteResponseSchema.parse(response);
    return coordinates.map(c => [c.lng, c.lat]);
  }

  public async getParking(): Promise<Adresse | null> {
    const response = await this._http.get<unknown>(this._url('/parkings'));
    const parsed = ParkingResponseSchema.safeParse(response);
    if (!parsed.success) {
      console.warn('Invalid parking response', parsed.error);
      return null;
    }
    return toAdresse(parsed.data.adresse);
  }

  public async getLivraisons(filters?: {
    livree?: boolean;
    dateDebut?: string;
    dateFin?: string;
  }): Promise<LivraisonResponse[]> {
    const params = new URLSearchParams();
    if (filters?.livree !== undefined) {
      params.set('livree', String(filters.livree));
    }
    if (filters?.dateDebut) {
      params.set('dateDebut', filters.dateDebut);
    }
    if (filters?.dateFin) {
      params.set('dateFin', filters.dateFin);
    }
    const query = params.toString();
    const url = query ? `${this._url('/livraisons')}?${query}` : this._url('/livraisons');
    const response = await this._http.get<unknown>(url);
    const parsed = LivraisonResponseSchema.array().safeParse(response);
    if (!parsed.success) {
      console.warn('Invalid livraisons response', parsed.error);
      return [];
    }
    return parsed.data;
  }

  public async getNonDeliveredLivraisons(): Promise<readonly Adresse[]> {
    const livraisons = await this.getLivraisons({ livree: false });
    return livraisons.map(l => toAdresse(l.adresse));
  }

  public async getJournee(date: string): Promise<JourneeResponse> {
    const response = await this._http.get<unknown>(this._url(`/journees/${date}`));
    const parsed = JourneeResponseSchema.parse(response);
    return parsed;
  }

  public async getSolution(solutionId: number): Promise<ParsedSolutionResponse> {
    const response = await this._http.get<unknown>(this._url('/solutions/' + solutionId));
    return this._parseSolutionResponse(response);
  }

  public async updateSolutionTournees(
    solutionId: number,
    request: SolutionTourneesUpdateRequest
  ): Promise<ParsedSolutionResponse> {
    const response = await this._http.put<unknown>(
      this._url(`/solutions/${solutionId}/tournees`),
      request
    );
    return this._parseSolutionResponse(response);
  }

  private _parseSolutionResponse(raw: unknown): ParsedSolutionResponse {
    const semiparsed = SolutionResponseSchema.parse(raw);
    return {
      id: semiparsed.id,
      date: semiparsed.date,
      tournees: semiparsed.tournees.map(t => t.livraisons ?? []),
      statistiques: semiparsed.statistiques ?? null,
      strategie: semiparsed.strategie,
      tourneeStatistics: semiparsed.tournees.map(t => ({
        id: t.id,
        livraisonIds: (t.livraisons ?? []).map(l => l.id),
        statistiques: t.statistiques ?? null,
      })),
    };
  }

  public async optimizeJournee(
    date: string,
    config: Configuration,
    livraisonIds: number[]
  ): Promise<JourneeResponse> {
    const body: StrategieRequest = {
      nbVehicules: config.nbVehicules,
      maxTempsParVehicule: config.maxTempsParVehicule,
      livraisonIds,
    };
    const response = await this._http.post<unknown>(
      this._url(`/journees/${date}/optimization`),
      body
    );
    const parsed = JourneeResponseSchema.parse(response);
    return parsed;
  }

  public async getJourneeSolutions(date: string): Promise<SolutionStatisticsResponse[]> {
    const response = await this._http.get<unknown>(this._url(`/journees/${date}/solutions`));
    return SolutionStatisticsResponseSchema.array().parse(response);
  }

  public async choisirSolution(solutionId: number): Promise<SolutionResponse> {
    const response = await this._http.put<unknown>(this._url(`/solutions/${solutionId}/choisie`), {});
    return SolutionResponseSchema.parse(response);
  }

  public async getSolutionChoisie(date: string): Promise<SolutionResponse | null> {
    try {
      const response = await this._http.get<unknown>(this._url(`/journees/${date}/solutions/choisie`));
      return SolutionResponseSchema.parse(response);
    } catch (err) {
      // 404 is the normal "nothing chosen yet" answer; anything else is a real failure.
      if (err instanceof HttpErrorResponse && err.status === HttpStatusCode.NotFound) {
        return null;
      }
      throw err;
    }
  }

  public async getSolutionLivraisons(solutionId: number): Promise<SolutionLivraisonsResponse | null> {
    const response = await this._http.get<unknown>(this._url(`/solutions/${solutionId}/livraisons`));
    const parsed = SolutionLivraisonsResponseSchema.safeParse(response);
    if (!parsed.success) {
      console.warn('Invalid SolutionLivraisonsResponse', parsed.error);
      return null;
    }
    return parsed.data;
  }

  public async recalculateSolutionStats(solutionId: number): Promise<ParsedSolutionResponse> {
    const response = await this._http.post<unknown>(
      this._url(`/solutions/${solutionId}/recalculate-stats`),
      {}
    );
    return this._parseSolutionResponse(response);
  }
}
