import { toAdresse } from './adresse';

describe('toAdresse', () => {
  it('maps a backend address DTO to the map-friendly Adresse model', () => {
    const adresse = toAdresse({
      coordonnees: { lat: 45.19, lng: 5.72 },
      numero: '12',
      rue: 'rue des Alpes',
      codePostal: '38000',
      ville: 'Grenoble',
    });

    expect(adresse).toEqual({
      coords: { lat: 45.19, lng: 5.72 },
      name: '12 rue des Alpes',
      postCode: '38000',
      city: 'Grenoble',
    });
  });

  it('trims the name when the street number is empty', () => {
    const adresse = toAdresse({
      coordonnees: { lat: 0, lng: 0 },
      numero: '',
      rue: 'Place Grenette',
      codePostal: '38000',
      ville: 'Grenoble',
    });

    expect(adresse.name).toBe('Place Grenette');
  });
});
