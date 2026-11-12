import { livraison, tournee } from '../../testing/fixtures';
import { HORS_SOLUTION, TourneesDraft } from './tournees-draft';

describe('TourneesDraft', () => {
  const [a, b, c, d] = [1, 2, 3, 4].map((id) => livraison(id));
  const initial = () => TourneesDraft.from([tournee(10, [a, b]), tournee(20, [c])], [d]);

  const ids = (draft: TourneesDraft, list: number | typeof HORS_SOLUTION) => draft.list(list).map((l) => l.id);

  it('n’est pas modifié au départ', () => {
    expect(initial().dirty).toBeFalse();
  });

  it('réordonne une tournée', () => {
    const draft = initial().move(10, 0, 10, 1);
    expect(ids(draft, 10)).toEqual([2, 1]);
    expect(draft.dirty).toBeTrue();
  });

  it('déplace une livraison vers une autre tournée', () => {
    const draft = initial().move(10, 1, 20, 0);
    expect(ids(draft, 10)).toEqual([1]);
    expect(ids(draft, 20)).toEqual([2, 3]);
  });

  it('ajoute une livraison hors solution à une tournée', () => {
    const draft = initial().move(HORS_SOLUTION, 0, 20, 5);
    expect(ids(draft, 20)).toEqual([3, 4]);
    expect(draft.horsSolution).toEqual([]);
  });

  it('redevient propre quand on annule son geste', () => {
    expect(initial().move(10, 0, 20, 0).move(20, 0, 10, 0).dirty).toBeFalse();
  });

  it('ne change rien sur un déplacement impossible et garde l’original intact', () => {
    const original = initial();
    expect(original.move(10, 9, 20, 0)).toBe(original);
    original.move(10, 0, 20, 0);
    expect(ids(original, 10)).toEqual([1, 2]);
  });

  it('produit la requête d’enregistrement', () => {
    expect(initial().move(20, 0, 10, 2).toRequest()).toEqual([
      { tourneeId: 10, livraisonIds: [1, 2, 3] },
      { tourneeId: 20, livraisonIds: [] },
    ]);
  });
});
