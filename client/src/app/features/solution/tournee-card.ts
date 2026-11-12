import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDragPlaceholder, CdkDropList } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Chauffeur, Livraison, Tournee, Vehicule } from '../../core/models/api';
import { formatDuration, formatSeconds, isoToSeconds } from '../../core/util/duration';
import { formatDistance, formatEuros, formatInteger, formatWeight } from '../../core/util/format';
import { cityLine, streetLine } from '../../core/util/labels';
import { Icon } from '../../shared/icon';
import { RouteRibbon } from '../../shared/route-ribbon';
import { ListId } from './tournees-draft';

export interface Affectation {
  chauffeurId: number | null;
  vehiculeId: number | null;
}

@Component({
  selector: 'app-tournee-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CdkDropList, CdkDrag, CdkDragHandle, CdkDragPlaceholder, RouterLink, Icon, RouteRibbon],
  templateUrl: './tournee-card.html',
  styleUrl: './tournee-card.scss',
})
export class TourneeCard {
  readonly listId = input.required<ListId>();
  readonly title = input.required<string>();
  readonly color = input<string | null>(null);
  readonly livraisons = input.required<readonly Livraison[]>();
  /** Absent pour la liste « hors solution ». */
  readonly tournee = input<Tournee | null>(null);
  readonly solutionId = input<number | null>(null);
  readonly chauffeurs = input<Chauffeur[]>([]);
  readonly vehicules = input<Vehicule[]>([]);
  readonly highlightedId = input<number | null>(null);
  readonly modified = input(false);

  readonly dropped = output<CdkDragDrop<ListId>>();
  readonly affecter = output<Affectation>();
  readonly terminer = output<void>();
  readonly hovered = output<number | null>();

  protected readonly poids = computed(() => this.livraisons().reduce((sum, l) => sum + l.poids, 0));
  protected readonly toutesLivrees = computed(() => this.livraisons().length > 0 && this.livraisons().every((l) => l.livree));
  protected readonly surcharge = computed(() => {
    const capacite = this.tournee()?.vehicule?.capaciteKg;
    return capacite !== null && capacite !== undefined && this.poids() > capacite;
  });
  protected readonly tempsTotal = computed(() => {
    const stats = this.tournee()?.statistiques;
    const seconds = (isoToSeconds(stats?.tempsTrajet) ?? 0) + (isoToSeconds(stats?.tempsMontage) ?? 0);
    return stats ? formatSeconds(seconds) : '—';
  });
  protected readonly chauffeursActifs = computed(() =>
    this.chauffeurs().filter((c) => c.actif || c.id === this.tournee()?.chauffeur?.id));
  protected readonly vehiculesActifs = computed(() =>
    this.vehicules().filter((v) => v.actif || v.id === this.tournee()?.vehicule?.id));

  protected readonly streetLine = streetLine;
  protected readonly cityLine = cityLine;
  protected readonly distance = formatDistance;
  protected readonly duration = formatDuration;
  protected readonly weight = formatWeight;
  protected readonly euros = formatEuros;
  protected readonly integer = formatInteger;

  protected changeChauffeur(value: string): void {
    this.affecter.emit({ chauffeurId: value ? Number(value) : null, vehiculeId: this.tournee()?.vehicule?.id ?? null });
  }

  protected changeVehicule(value: string): void {
    this.affecter.emit({ chauffeurId: this.tournee()?.chauffeur?.id ?? null, vehiculeId: value ? Number(value) : null });
  }
}
