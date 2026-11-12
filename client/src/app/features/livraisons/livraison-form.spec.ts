import { toRequest } from './livraison-form';

describe('toRequest (formulaire de livraison)', () => {
  const valid = {
    numero: ' 12 ', rue: 'Rue Félix Viallet', codePostal: '38000', ville: 'Grenoble',
    lat: 45.19, lng: 5.72, poids: 3.5, montageMinutes: 15, dateCommande: '2026-11-10',
  };

  it('nettoie les champs et convertit le montage en durée ISO', () => {
    expect(toRequest(valid)).toEqual({
      adresse: { numero: '12', rue: 'Rue Félix Viallet', codePostal: '38000', ville: 'Grenoble', lat: 45.19, lng: 5.72 },
      poids: 3.5,
      tempsMontage: 'PT15M',
      dateCommande: '2026-11-10',
    });
  });

  it('n’envoie pas de montage quand il est nul', () => {
    expect(toRequest({ ...valid, montageMinutes: 0 })?.tempsMontage).toBeNull();
  });

  it('refuse un formulaire incomplet', () => {
    expect(toRequest({ ...valid, rue: '  ' })).toBeNull();
    expect(toRequest({ ...valid, lat: null })).toBeNull();
    expect(toRequest({ ...valid, poids: -1 })).toBeNull();
  });
});
