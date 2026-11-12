import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { errorMessage } from './api-client';
import { JourneesApi } from './journees-api';

describe('JourneesApi', () => {
  let api: JourneesApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(JourneesApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('demande le calendrier avec la période en paramètres', async () => {
    const pending = api.calendrier('2026-11-01', '2026-11-30');
    const request = http.expectOne((r) => r.url === '/api/journees');
    expect(request.request.params.get('debut')).toBe('2026-11-01');
    expect(request.request.params.get('fin')).toBe('2026-11-30');
    request.flush([{ date: '2026-11-12', statutGeneration: 'TERMINE', nbSolutions: 4, solutionChoisieId: 7 }]);

    expect(await pending).toEqual([{ date: '2026-11-12', statutGeneration: 'TERMINE', nbSolutions: 4, solutionChoisieId: 7 }]);
  });

  it('traite « aucune solution choisie » (404) comme une absence', async () => {
    const pending = api.solutionChoisie('2026-11-12');
    http.expectOne('/api/journees/2026-11-12/solutions/choisie')
      .flush({ uri: '/api', message: 'Aucune solution choisie' }, { status: 404, statusText: 'Not Found' });

    expect(await pending).toBeNull();
  });

  it('remonte le message d’erreur du serveur', async () => {
    const pending = api.optimiser('2026-11-12', { nbVehicules: 2, maxTempsParVehicule: 'PT8H', livraisonIds: [] });
    http.expectOne('/api/journees/2026-11-12/optimization')
      .flush({ uri: '/api', message: 'Une optimisation est déjà en cours' }, { status: 409, statusText: 'Conflict' });

    const error = await pending.catch((e: unknown) => e);
    expect(errorMessage(error, 'Erreur')).toBe('Une optimisation est déjà en cours');
  });

  it('refuse une réponse qui ne respecte pas le contrat', async () => {
    const pending = api.get('2026-11-12');
    http.expectOne('/api/journees/2026-11-12').flush({ date: 12 });

    await expectAsync(pending).toBeRejected();
  });
});
