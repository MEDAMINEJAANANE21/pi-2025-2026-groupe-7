import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../../core/api/api-client';
import { LivraisonsApi } from '../../core/api/livraisons-api';
import { Livraison } from '../../core/models/api';
import { formatShortDay } from '../../core/util/dates';
import { formatDuration } from '../../core/util/duration';
import { formatInteger, formatWeight } from '../../core/util/format';
import { cityLine, streetLine } from '../../core/util/labels';
import { Confirm } from '../../shared/confirm';
import { Icon } from '../../shared/icon';
import { Notifications } from '../../shared/notifications';
import { DonneesDialog } from './donnees-dialog';
import { LivraisonForm } from './livraison-form';

type StatutFilter = 'a-livrer' | 'livrees' | 'toutes';

const LIVREE_PARAM: Record<StatutFilter, boolean | null> = { 'a-livrer': false, livrees: true, toutes: null };

@Component({
  selector: 'app-livraisons-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon, LivraisonForm, DonneesDialog],
  templateUrl: './livraisons-page.html',
  styleUrl: './livraisons-page.scss',
})
export class LivraisonsPage {
  private readonly api = inject(LivraisonsApi);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);

  protected readonly form = viewChild.required(LivraisonForm);
  protected readonly donnees = viewChild.required(DonneesDialog);

  protected readonly statut = signal<StatutFilter>('a-livrer');
  protected readonly search = signal('');
  protected readonly livraisons = signal<Livraison[]>([]);
  protected readonly loading = signal(true);

  protected readonly visible = computed(() => {
    const query = this.search().trim().toLowerCase();
    return query
      ? this.livraisons().filter((l) => `${l.reference ?? ''} ${streetLine(l.adresse)} ${cityLine(l.adresse)}`.toLowerCase().includes(query))
      : this.livraisons();
  });
  protected readonly poidsTotal = computed(() => this.visible().reduce((sum, l) => sum + l.poids, 0));

  protected readonly filters: { value: StatutFilter; label: string }[] = [
    { value: 'a-livrer', label: 'À livrer' },
    { value: 'livrees', label: 'Livrées' },
    { value: 'toutes', label: 'Toutes' },
  ];

  protected readonly streetLine = streetLine;
  protected readonly cityLine = cityLine;
  protected readonly shortDay = formatShortDay;
  protected readonly duration = formatDuration;
  protected readonly weight = formatWeight;
  protected readonly integer = formatInteger;

  constructor() {
    effect(() => void this.load(this.statut()));
  }

  protected reload(): void {
    void this.load(this.statut());
  }

  protected async toggleLivree(livraison: Livraison): Promise<void> {
    try {
      const updated = await this.api.setLivree(livraison.id, !livraison.livree);
      this.notifications.success(updated.livree ? 'Livraison marquée comme livrée' : 'Livraison remise à livrer');
      this.reload();
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Le statut n’a pas pu être modifié'));
    }
  }

  protected async remove(livraison: Livraison): Promise<void> {
    const ok = await this.confirm.ask(
      'Supprimer la livraison',
      `${streetLine(livraison.adresse)}, ${cityLine(livraison.adresse)} sera retirée des tournées qui la contiennent.`,
    );
    if (!ok) return;
    try {
      await this.api.delete(livraison.id);
      this.livraisons.update((list) => list.filter((l) => l.id !== livraison.id));
      this.notifications.success('Livraison supprimée');
    } catch (error) {
      this.notifications.error(errorMessage(error, 'La livraison n’a pas pu être supprimée'));
    }
  }

  private async load(statut: StatutFilter): Promise<void> {
    this.loading.set(true);
    try {
      // Sans filtre, l'API ne renvoie que les livraisons à faire : « Toutes » réunit donc les deux listes.
      const livree = LIVREE_PARAM[statut];
      const lists = livree === null
        ? await Promise.all([this.api.list({ livree: false }), this.api.list({ livree: true })])
        : [await this.api.list({ livree })];
      this.livraisons.set(lists.flat().sort((a, b) => a.id - b.id));
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Impossible de charger les livraisons'));
    } finally {
      this.loading.set(false);
    }
  }
}
