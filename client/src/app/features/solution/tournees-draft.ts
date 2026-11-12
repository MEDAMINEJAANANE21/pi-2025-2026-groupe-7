import { Livraison, Tournee, TourneeUpdate } from '../../core/models/api';

export const HORS_SOLUTION = 'hors';
export type ListId = number | typeof HORS_SOLUTION;

export interface DraftTournee {
  id: number;
  livraisons: Livraison[];
}

/**
 * Brouillon des modifications d'une solution, indépendant de l'affichage :
 * on déplace des livraisons entre tournées, puis on compare avec l'état enregistré.
 */
export class TourneesDraft {
  private constructor(
    readonly tournees: readonly DraftTournee[],
    readonly horsSolution: readonly Livraison[],
    private readonly saved: string,
  ) {}

  static from(tournees: readonly Tournee[], horsSolution: readonly Livraison[]): TourneesDraft {
    const draft = tournees.map((t) => ({ id: t.id, livraisons: [...t.livraisons] }));
    return new TourneesDraft(draft, [...horsSolution], signature(draft));
  }

  get dirty(): boolean {
    return signature(this.tournees) !== this.saved;
  }

  list(id: ListId): readonly Livraison[] {
    return id === HORS_SOLUTION ? this.horsSolution : (this.tournees.find((t) => t.id === id)?.livraisons ?? []);
  }

  /** Renvoie un nouveau brouillon : l'état précédent reste intact, ce qui simplifie les signaux. */
  move(from: ListId, fromIndex: number, to: ListId, toIndex: number): TourneesDraft {
    const lists = new Map<ListId, Livraison[]>([
      [HORS_SOLUTION, [...this.horsSolution]],
      ...this.tournees.map((t) => [t.id, [...t.livraisons]] as [ListId, Livraison[]]),
    ]);
    const source = lists.get(from);
    const target = lists.get(to);
    if (!source || !target || fromIndex < 0 || fromIndex >= source.length) return this;

    const [moved] = source.splice(fromIndex, 1);
    target.splice(Math.max(0, Math.min(toIndex, target.length)), 0, moved);

    return new TourneesDraft(
      this.tournees.map((t) => ({ id: t.id, livraisons: lists.get(t.id) ?? [] })),
      lists.get(HORS_SOLUTION) ?? [],
      this.saved,
    );
  }

  toRequest(): TourneeUpdate[] {
    return this.tournees.map((t) => ({ tourneeId: t.id, livraisonIds: t.livraisons.map((l) => l.id) }));
  }
}

function signature(tournees: readonly DraftTournee[]): string {
  return tournees.map((t) => `${t.id}:${t.livraisons.map((l) => l.id).join(',')}`).join('|');
}
