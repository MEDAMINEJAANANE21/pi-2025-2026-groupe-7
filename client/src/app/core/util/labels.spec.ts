import { Adresse } from '../models/api';
import { cityLine, isRunning, strategieLabel, streetLine } from './labels';
import { websocketUrl } from './websocket';

const adresse = (numero: string | null, rue: string | null): Adresse => ({
  coordonnees: null, numero, rue, codePostal: '38000', ville: 'Grenoble',
});

describe('libellés', () => {
  it('résume une stratégie en une ligne', () => {
    expect(strategieLabel({ separation: 'KMEDOIDS', optimisation: 'GENETIC', nbVehicules: 3, maxTempsParVehicule: 'PT8H' }))
      .toBe('K-medoids · Génétique · 3 véhicules · 8 h 00 max');
    expect(strategieLabel(null)).toBe('—');
  });

  it('compose les lignes d’adresse', () => {
    expect(streetLine(adresse('12', 'Rue Félix Viallet'))).toBe('12 Rue Félix Viallet');
    expect(streetLine(adresse('', 'Place Grenette'))).toBe('Place Grenette');
    expect(streetLine(adresse(null, null))).toBe('Adresse sans rue');
    expect(cityLine(adresse('1', 'Rue'))).toBe('38000 Grenoble');
  });

  it('sait si une optimisation est en cours', () => {
    expect(isRunning('GENERATION_ROUTE')).toBeTrue();
    expect(isRunning('TERMINE')).toBeFalse();
    expect(isRunning(null)).toBeFalse();
  });

  it('choisit ws ou wss selon la page', () => {
    expect(websocketUrl('/ws', { protocol: 'http:', host: 'localhost:4200' })).toBe('ws://localhost:4200/ws');
    expect(websocketUrl('/ws', { protocol: 'https:', host: 'exemple.fr' })).toBe('wss://exemple.fr/ws');
  });
});
