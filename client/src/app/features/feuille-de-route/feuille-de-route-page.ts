import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SolutionsApi } from '../../core/api/solutions-api';
import { TourneesApi } from '../../core/api/tournees-api';
import { Solution } from '../../core/models/api';
import { formatLongDay } from '../../core/util/dates';
import { formatDuration, isoToSeconds, formatSeconds } from '../../core/util/duration';
import { formatDistance, formatWeight } from '../../core/util/format';
import { cityLine, streetLine } from '../../core/util/labels';
import { routeColor } from '../../core/util/route-colors';
import { Icon } from '../../shared/icon';
import { RouteRibbon } from '../../shared/route-ribbon';

/** Document remis au chauffeur : une page A4, un arrêt par ligne, des cases à cocher sur le terrain. */
@Component({
  selector: 'app-feuille-de-route-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, RouteRibbon],
  templateUrl: './feuille-de-route-page.html',
  styleUrl: './feuille-de-route-page.scss',
})
export class FeuilleDeRoutePage {
  readonly solutionId = input.required<string>();
  readonly tourneeId = input.required<string>();

  private readonly solutionsApi = inject(SolutionsApi);
  private readonly tourneesApi = inject(TourneesApi);

  protected readonly solution = signal<Solution | null>(null);
  protected readonly failed = signal(false);

  protected readonly index = computed(() => this.solution()?.tournees.findIndex((t) => t.id === Number(this.tourneeId())) ?? -1);
  protected readonly tournee = computed(() => this.solution()?.tournees[this.index()] ?? null);
  protected readonly color = computed(() => routeColor(this.index()));
  protected readonly day = computed(() => (this.solution() ? formatLongDay(this.solution()!.date) : ''));
  protected readonly csvUrl = computed(() => this.tourneesApi.feuilleDeRouteCsvUrl(Number(this.tourneeId())));
  protected readonly totalTime = computed(() => {
    const stats = this.tournee()?.statistiques;
    return stats ? formatSeconds((isoToSeconds(stats.tempsTrajet) ?? 0) + (isoToSeconds(stats.tempsMontage) ?? 0)) : '—';
  });

  protected readonly streetLine = streetLine;
  protected readonly cityLine = cityLine;
  protected readonly distance = formatDistance;
  protected readonly duration = formatDuration;
  protected readonly weight = formatWeight;

  constructor() {
    effect(() => void this.load(Number(this.solutionId())));
  }

  protected print(): void {
    window.print();
  }

  private async load(id: number): Promise<void> {
    try {
      this.solution.set(await this.solutionsApi.get(id));
    } catch {
      this.failed.set(true);
    }
  }
}
