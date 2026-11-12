import { CdkDragDrop, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../core/api/api-client';
import { CartoApi } from '../../core/api/carto-api';
import { EquipeApi } from '../../core/api/equipe-api';
import { SolutionsApi } from '../../core/api/solutions-api';
import { TourneesApi } from '../../core/api/tournees-api';
import { Adresse, Chauffeur, Coordonnees, Livraison, Solution, Vehicule } from '../../core/models/api';
import { formatLongDay } from '../../core/util/dates';
import { formatDuration } from '../../core/util/duration';
import { formatDistance, formatEuros, formatFuel } from '../../core/util/format';
import { strategieLabel } from '../../core/util/labels';
import { routeColor } from '../../core/util/route-colors';
import { Confirm } from '../../shared/confirm';
import { Icon } from '../../shared/icon';
import { Notifications } from '../../shared/notifications';
import { MapRoute, MapStop, RoutesMap } from '../../shared/routes-map';
import { Affectation, TourneeCard } from './tournee-card';
import { HORS_SOLUTION, ListId, TourneesDraft } from './tournees-draft';

@Component({
  selector: 'app-solution-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, CdkDropListGroup, Icon, RoutesMap, TourneeCard],
  templateUrl: './solution-page.html',
  styleUrl: './solution-page.scss',
})
export class SolutionPage {
  readonly solutionId = input.required<string>();

  private readonly solutionsApi = inject(SolutionsApi);
  private readonly tourneesApi = inject(TourneesApi);
  private readonly equipeApi = inject(EquipeApi);
  private readonly cartoApi = inject(CartoApi);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);

  protected readonly solution = signal<Solution | null>(null);
  protected readonly draft = signal<TourneesDraft | null>(null);
  protected readonly chauffeurs = signal<Chauffeur[]>([]);
  protected readonly vehicules = signal<Vehicule[]>([]);
  protected readonly depot = signal<Adresse | null>(null);
  protected readonly routes = signal<MapRoute[]>([]);
  protected readonly highlighted = signal<number | null>(null);
  protected readonly notFound = signal(false);
  protected readonly saving = signal(false);

  private initialHors: Livraison[] = [];
  private tracesFailed = false;

  protected readonly id = computed(() => Number(this.solutionId()));
  protected readonly dirty = computed(() => this.draft()?.dirty ?? false);
  protected readonly stats = computed(() => this.solution()?.statistiques ?? null);
  protected readonly title = computed(() => {
    const solution = this.solution();
    return solution ? formatLongDay(solution.date) : '';
  });
  protected readonly strategie = computed(() => strategieLabel(this.solution()?.strategie));

  protected readonly stops = computed<MapStop[]>(() => {
    const draft = this.draft();
    if (!draft) return [];
    const planned = draft.tournees.flatMap((tournee, index) =>
      tournee.livraisons.map((livraison, order) => ({ livraison, color: routeColor(index), order: order + 1 })));
    const free = draft.horsSolution.map((livraison) => ({ livraison, color: null, order: null }));
    return [...planned, ...free];
  });

  protected readonly horsSolutionId: ListId = HORS_SOLUTION;
  protected readonly routeColor = routeColor;
  protected readonly distance = formatDistance;
  protected readonly duration = formatDuration;
  protected readonly fuel = formatFuel;
  protected readonly euros = formatEuros;

  constructor() {
    void this.cartoApi.depot().then((depot) => this.depot.set(depot));
    void Promise.all([this.equipeApi.chauffeurs(), this.equipeApi.vehicules()])
      .then(([chauffeurs, vehicules]) => {
        this.chauffeurs.set(chauffeurs);
        this.vehicules.set(vehicules);
      })
      .catch(() => this.notifications.error("L'équipe n'a pas pu être chargée"));
    effect(() => void this.load(this.id()));
  }

  protected tourneeOf(id: number) {
    return this.solution()?.tournees.find((t) => t.id === id) ?? null;
  }

  protected isModified(tourneeId: number): boolean {
    const saved = this.tourneeOf(tourneeId)?.livraisons.map((l) => l.id).join(',');
    const current = this.draft()?.list(tourneeId).map((l) => l.id).join(',');
    return saved !== current;
  }

  protected drop(event: CdkDragDrop<ListId>): void {
    this.draft.update((draft) =>
      draft?.move(event.previousContainer.data, event.previousIndex, event.container.data, event.currentIndex) ?? draft);
  }

  protected reset(): void {
    const solution = this.solution();
    if (solution) this.draft.set(TourneesDraft.from(solution.tournees, this.initialHors));
  }

  protected async save(): Promise<void> {
    const draft = this.draft();
    if (!draft) return;
    this.saving.set(true);
    try {
      await this.solutionsApi.enregistrerTournees(this.id(), draft.toRequest());
      await this.load(this.id());
      this.notifications.success('Tournées enregistrées, distances et coûts recalculés');
    } catch (error) {
      this.notifications.error(errorMessage(error, "Les modifications n'ont pas pu être enregistrées"));
    } finally {
      this.saving.set(false);
    }
  }

  protected async affecter(tourneeId: number, affectation: Affectation): Promise<void> {
    try {
      await this.tourneesApi.affecter(tourneeId, affectation.chauffeurId, affectation.vehiculeId);
      await this.refreshSolution();
    } catch (error) {
      this.notifications.error(errorMessage(error, "L'affectation a échoué"));
      // Remet les listes déroulantes sur la valeur réellement enregistrée.
      this.solution.update((solution) => (solution ? { ...solution } : solution));
    }
  }

  protected async terminer(tourneeId: number, index: number): Promise<void> {
    const ok = await this.confirm.ask(
      `Clôturer la tournée ${index + 1}`,
      'Toutes ses livraisons seront marquées comme livrées et disparaîtront des prochaines optimisations.',
      'Marquer livrée',
    );
    if (!ok) return;
    try {
      await this.tourneesApi.terminer(tourneeId);
      await this.refreshSolution();
      this.notifications.success(`Tournée ${index + 1} marquée comme livrée`);
    } catch (error) {
      this.notifications.error(errorMessage(error, 'La tournée n’a pas pu être clôturée'));
    }
  }

  protected focusStop(id: number): void {
    this.highlighted.set(id);
    document.querySelector(`[data-livraison="${id}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  protected async redraw(): Promise<void> {
    const draft = this.draft();
    const depot = this.depot()?.coordonnees ?? (await this.cartoApi.depot())?.coordonnees ?? null;
    if (!draft) return;
    const routes = await Promise.all(draft.tournees.map(async (tournee, index) => {
      const stops = tournee.livraisons.map((l) => l.adresse.coordonnees).filter((c): c is Coordonnees => !!c);
      const points = depot ? [depot, ...stops, depot] : stops;
      return { color: routeColor(index), points: await this.trace(points) };
    }));
    this.routes.set(routes);
  }

  private async load(id: number): Promise<void> {
    try {
      const [solution, livraisons] = await Promise.all([this.solutionsApi.get(id), this.solutionsApi.livraisons(id)]);
      this.initialHors = livraisons.horsSolution;
      this.solution.set(solution);
      this.draft.set(TourneesDraft.from(solution.tournees, livraisons.horsSolution));
      this.notFound.set(false);
      void this.redraw();
    } catch {
      this.notFound.set(true);
    }
  }

  /** Recharge chiffres et affectations sans écraser un brouillon en cours. */
  private async refreshSolution(): Promise<void> {
    const solution = await this.solutionsApi.get(this.id());
    this.solution.set(solution);
    if (!this.dirty()) this.draft.set(TourneesDraft.from(solution.tournees, this.initialHors));
  }

  private async trace(points: Coordonnees[]): Promise<Coordonnees[]> {
    if (points.length < 2) return points;
    try {
      return await this.cartoApi.trace(points);
    } catch {
      // Sans clé OpenRouteService, on relie les arrêts à vol d'oiseau pour garder une carte lisible.
      if (!this.tracesFailed) this.notifications.error('Tracés routiers indisponibles, affichage à vol d’oiseau');
      this.tracesFailed = true;
      return points;
    }
  }
}
