import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class PromiseHttpClient {
  private readonly _httpClient = inject(HttpClient);

  public request<TResponse>(
    method: string,
    url: string,
    options?: Parameters<typeof HttpClient.prototype.request<TResponse>>[2]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.request<TResponse>(method, url, options));
  }

  public get<TResponse>(
    url: string,
    options?: Parameters<typeof HttpClient.prototype.get<TResponse>>[1]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.get<TResponse>(url, options));
  }

  public delete<TResponse>(
    url: string,
    options?: Parameters<typeof HttpClient.prototype.delete<TResponse>>[1]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.delete<TResponse>(url, options));
  }

  public head<TResponse>(
    url: string,
    options?: Parameters<typeof HttpClient.prototype.head<TResponse>>[1]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.head<TResponse>(url, options));
  }

  public options<TResponse>(
    url: string,
    options?: Parameters<typeof HttpClient.prototype.options<TResponse>>[1]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.options<TResponse>(url, options));
  }

  public post<TResponse, TRequest = unknown>(
    url: string,
    body: TRequest,
    options?: Parameters<typeof HttpClient.prototype.post<TResponse>>[2]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.post<TResponse>(url, body, options));
  }

  public put<TResponse, TRequest = unknown>(
    url: string,
    body: TRequest,
    options?: Parameters<typeof HttpClient.prototype.put<TResponse>>[2]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.put<TResponse>(url, body, options));
  }

  public patch<TResponse, TRequest = unknown>(
    url: string,
    body: TRequest,
    options?: Parameters<typeof HttpClient.prototype.patch<TResponse>>[2]
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.patch<TResponse>(url, body, options));
  }

  public jsonp<TResponse>(
    url: string,
    callbackParam: string
  ): Promise<TResponse> {
    return firstValueFrom(this._httpClient.jsonp<TResponse>(url, callbackParam));
  }
}