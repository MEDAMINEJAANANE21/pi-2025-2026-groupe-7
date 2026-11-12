import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { switchMap, tap, throttleTime } from 'rxjs';
import { errorMessage } from '../../core/api/api-client';
import { CartoApi } from '../../core/api/carto-api';
import { JourneesApi } from '../../core/api/journees-api';
import { LivraisonsApi } from '../../core/api/livraisons-api';
import { SolutionsApi } from '../../core/api/solutions-api';
import { Adresse, Journee, JourneeStatusUpdateSchema, Livraison, Solution, SolutionStatistiques } from '../../core/models/api';
import { addDays, formatLongDay, formatShortDay, today } from '../../core/util/dates';
import { formatDistance, formatWeight } from '../../core/util/format';
import { cityLine, isRunning, STATUT_LABELS, strategieLabel, streetLine } from '../../core/util/labels';
import { formatDuration, minutesToIso } from '../../core/util/duration';
import { routeColor } from '../../core/util/route-colors';
import { stompTopic, websocketUrl } from '../../core/util/websocket';
import { Icon } from '../../shared/icon';
import { Notifications } from '../../shared/notifications';
import { RouteRibbon } from '../../shared/route-ribbon';
import { MapStop, RoutesMap } from '../../shared/routes-map';
import { OptimisationForm, OptimisationParams } from './optimisation-form';
import { SolutionsTable } from './solutions-table';

const RELOAD_THROTTLE_MS = 2_000;

@Component({
  selector: 'app-journee-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, RoutesMap, RouteRibbon, OptimisationForm, SolutionsTable],
  templateUrl: './journee-page.html',
  styleUrl: './journee-page.scss',
})
export class JourneePage {
  /** Lié au paramètre d'URL grâce à withComponentInputBinding. */
  readonly date = input.required<string>();

  private readonly journeesApi = inject(JourneesApi);
  private readonly livraisonsApi = inject(LivraisonsApi);
  private readonly solutionsApi = inject(SolutionsApi);
  private readonly cartoApi = inject(CartoApi);
  private readonly notifications = inject(Notifications);

  protected readonly journee = signal<Journee | null>(null);
  protected readonly livraisons = signal<Livraison[]>([]);
  protected readonly excluded = signal<ReadonlySet<number>>(new Set());
  protected readonly solutions = signal<SolutionStatistiques[]>([]);
  protected readonly choisie = signal<Solution | null>(null);
  protected readonly depot = signal<Adresse | null>(null);
  protected readonly loading = signal(true);
  protected readonly launching = signal(false);
  protected readonly highlighted = signal<number | null>(null);
  protected readonly tab = signal<'optimiser' | 'livraisons'>('optimiser');
  protected readonly search = signal('');

  protected readonly todayDate = today();
  protected readonly title = computed(() => formatLongDay(this.date()));
  protected readonly isToday = computed(() => this.date() === today());
  protected readonly statut = computed(() => this.journee()?.statutGeneration ?? null);
  protected readonly running = computed(() => isRunning(this.statut()));
  protected readonly statutLabel = computed(() => {
    const statut = this.statut();
    return statut ? `Optimisation ${statut === 'TERMINE' ? 'terminée' : 'en cours · ' + STATUT_LABELS[statut].toLowerCase()}` : 'Pas encore optimisée';
  });

  protected readonly selected = computed(() => this.livraisons().filter((l) => !this.excluded().has(l.id)));
  protected readonly filtered = computed(() => {
    const query = this.search().trim().toLowerCase();
    if (!query) return this.livraisons();
    return this.livraisons().filter((l) =>
      `${streetLine(l.adresse)} ${cityLine(l.adresse)} ${l.reference ?? ''}`.toLowerCase().includes(query));
  });

  protected readonly stops = computed<MapStop[]>(() => {
    const solution = this.choisie();
    if (solution) {
      const planned = solution.tournees.flatMap((tournee, index) =>
        tournee.livraisons.map((livraison, order) => ({ livraison, color: routeColor(index), order: order + 1 })));
      const plannedIds = new Set(planned.map((stop) => stop.livraison.id));
      const others = this.selected().filter((l) => !plannedIds.has(l.id)).map((livraison) => ({ livraison, color: null, order: null }));
      return [...planned, ...others];
    }
    return this.selected().map((livraison) => ({ livraison, color: null, order: null }));
  });

  protected readonly strategieLabel = strategieLabel;
  protected readonly formatShortDay = formatShortDay;
  protected readonly streetLine = streetLine;
  protected readonly cityLine = cityLine;
  protected readonly distance = formatDistance;
  protected readonly weight = formatWeight;
  protected readonly duration = formatDuration;
  protected readonly routeColor = routeColor;

  constructor() {
    void this.cartoApi.depot().then((depot) => this.depot.set(depot));
    effect(() => void this.load(this.date()));
    this.listenToStatus();
  }

  protected previousDay(): string {
    return addDays(this.date(), -1);
  }

  protected nextDay(): string {
    return addDays(this.date(), 1);
  }

  protected toggle(id: number): void {
    this.excluded.update((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  protected selectAll(include: boolean): void {
    this.excluded.set(include ? new Set() : new Set(this.livraisons().map((l) => l.id)));
  }

  protected async launch(params: OptimisationParams): Promise<void> {
    this.launching.set(true);
    try {
      this.journee.set(await this.journeesApi.optimiser(this.date(), {
        nbVehicules: params.nbVehicules,
        maxTempsParVehicule: minutesToIso(params.maxMinutes),
        livraisonIds: this.selected().map((l) => l.id),
      }));
      this.notifications.success('Optimisation lancée, les solutions arrivent au fil du calcul');
    } catch (error) {
      this.notifications.error(errorMessage(error, "L'optimisation n'a pas pu démarrer"));
    } finally {
      this.launching.set(false);
    }
  }

  protected async choose(solutionId: number): Promise<void> {
    try {
      this.choisie.set(await this.solutionsApi.choisir(solutionId));
      this.notifications.success(`Solution n° ${solutionId} retenue pour la journée`);
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Impossible de retenir cette solution'));
    }
  }

  private async load(date: string): Promise<void> {
    this.loading.set(true);
    this.excluded.set(new Set());
    try {
      const [journee, livraisons, solutions, choisie] = await Promise.all([
        this.journeesApi.get(date),
        this.livraisonsApi.list({ livree: false }),
        this.journeesApi.solutions(date),
        this.journeesApi.solutionChoisie(date),
      ]);
      this.journee.set(journee);
      this.livraisons.set(livraisons);
      this.solutions.set(solutions);
      this.choisie.set(choisie);
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Impossible de charger la journée'));
    } finally {
      this.loading.set(false);
    }
  }

  private listenToStatus(): void {
    const destroyRef = inject(DestroyRef);
    toObservable(this.date)
      .pipe(
        switchMap((date) => stompTopic(websocketUrl('/ws'), `/topic/journee/${date}`, JourneeStatusUpdateSchema)),
        // Le statut s'affiche tout de suite ; seul le rechargement des solutions est espacé.
        tap((update) => this.journee.update((j) => ({ date: update.date, strategie: j?.strategie ?? null, statutGeneration: update.statutGeneration }))),
        throttleTime(RELOAD_THROTTLE_MS, undefined, { leading: true, trailing: true }),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe(() => void this.journeesApi.solutions(this.date()).then((s) => this.solutions.set(s)));
  }
}
