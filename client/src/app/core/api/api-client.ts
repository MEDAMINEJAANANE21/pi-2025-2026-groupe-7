import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { z } from 'zod';
import { ErrorSchema } from '../models/api';

type QueryValue = string | number | boolean | null | undefined;

/**
 * Pattern utilisé : Facade. Toutes les requêtes passent ici : préfixe /api,
 * conversion en Promise et validation Zod de la réponse.
 */
@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);

  async get<T>(path: string, schema: z.ZodType<T>, query?: Record<string, QueryValue>): Promise<T> {
    const body = await firstValueFrom(this.http.get<unknown>(url(path), { params: params(query) }));
    return schema.parse(body);
  }

  async post<T>(path: string, schema: z.ZodType<T>, body: unknown = {}): Promise<T> {
    return schema.parse(await firstValueFrom(this.http.post<unknown>(url(path), body)));
  }

  async put<T>(path: string, schema: z.ZodType<T>, body: unknown = {}): Promise<T> {
    return schema.parse(await firstValueFrom(this.http.put<unknown>(url(path), body)));
  }

  async patch<T>(path: string, schema: z.ZodType<T>, body: unknown): Promise<T> {
    return schema.parse(await firstValueFrom(this.http.patch<unknown>(url(path), body)));
  }

  async delete(path: string): Promise<void> {
    await firstValueFrom(this.http.delete(url(path)));
  }
}

export function apiUrl(path: string): string {
  return url(path);
}

/** Message lisible pour l'utilisateur, tiré de la réponse d'erreur du serveur quand elle existe. */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const parsed = ErrorSchema.safeParse(error.error);
    if (parsed.success) return parsed.data.message;
    if (error.status === 0) return 'Le serveur ne répond pas';
  }
  return fallback;
}

export function isNotFound(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 404;
}

function url(path: string): string {
  return `/api${path.startsWith('/') ? path : `/${path}`}`;
}

function params(query?: Record<string, QueryValue>): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== null && value !== undefined && value !== '') result = result.set(key, String(value));
  }
  return result;
}
