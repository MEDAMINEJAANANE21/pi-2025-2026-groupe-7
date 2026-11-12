import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { z } from 'zod';
import { Adresse, Coordonnees, ParkingSchema, RouteSchema } from '../models/api';
import { ApiClient } from './api-client';

export interface AdresseTrouvee {
  label: string;
  numero: string;
  rue: string;
  codePostal: string;
  ville: string;
  lat: number;
  lng: number;
}

const BanResponseSchema = z.object({
  features: z.array(z.object({
    geometry: z.object({ coordinates: z.tuple([z.number(), z.number()]) }),
    properties: z.object({
      label: z.string(),
      housenumber: z.string().optional(),
      street: z.string().optional(),
      name: z.string(),
      postcode: z.string(),
      city: z.string(),
    }),
  })),
});

const BAN_URL = 'https://api-adresse.data.gouv.fr/search/';
// Biais géographique vers Grenoble, où se trouve le dépôt des jeux de données.
const GRENOBLE = { lat: 45.188, lon: 5.724 };

@Injectable({ providedIn: 'root' })
export class CartoApi {
  private readonly api = inject(ApiClient);
  private readonly http = inject(HttpClient);

  async depot(): Promise<Adresse | null> {
    try {
      return (await this.api.get('/parkings', ParkingSchema)).adresse;
    } catch {
      return null;
    }
  }

  /** Tracé routier réel passant par les points, dans l'ordre. */
  async trace(points: Coordonnees[]): Promise<Coordonnees[]> {
    const route = await this.api.post('/routes', RouteSchema, { adresses: points });
    return route.coordinates;
  }

  /** Recherche d'adresse dans la Base Adresse Nationale (service public, sans clé). */
  async chercherAdresse(query: string): Promise<AdresseTrouvee[]> {
    if (query.trim().length < 3) return [];
    const response = await firstValueFrom(this.http.get<unknown>(BAN_URL, {
      params: { q: query, limit: 5, lat: GRENOBLE.lat, lon: GRENOBLE.lon },
    }));
    return BanResponseSchema.parse(response).features.map(({ geometry, properties }) => ({
      label: properties.label,
      numero: properties.housenumber ?? '',
      rue: properties.street ?? properties.name,
      codePostal: properties.postcode,
      ville: properties.city,
      lng: geometry.coordinates[0],
      lat: geometry.coordinates[1],
    }));
  }
}
