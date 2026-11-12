import { TuiButton, TuiLabel, TuiLoader, TuiSurface } from '@taiga-ui/core';
import { TuiCardMedium } from '@taiga-ui/layout';
import { TuiCheckbox, TuiTabs } from '@taiga-ui/kit';
import { Component, inject, signal, computed, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { formatIsoDuration } from '../../utils/duration';
import { AppBreadcrumbsComponent } from '../../components/app-breadcrumbs/app-breadcrumbs';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RoutesMap } from '../../components/RoutesMap/RoutesMap';
import { OptimizationControls, type OptimizationConfig } from '../../components/optimization-controls/optimization-controls';
import { LivraisonsFilterComponent } from '../../components/livraisons-filter/livraisons-filter';
import { BackendApiService } from '../../services/BackendApiService';
import { Adresse } from '../../models/adresse';
import {
  JourneeResponse,
  JourneeStatusUpdateSchema,
  SolutionResponse,
  LivraisonResponse,
  SolutionStatisticsResponse,
} from '../../models/backend';
import { debounceTime, filter, switchMap } from 'rxjs';
import { millisecondsToIsoDuration } from '../../utils/duration';
import { SolutionsTableComponent } from '../../components/solutions-table/solutions-table';
import { stompTopic } from '../../utils/stompTopic';

@Component({
  selector: 'app-journee-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RoutesMap,
    TuiLabel,
    TuiButton,
    TuiSurface,
    TuiCardMedium,
    TuiCheckbox,
    FormsModule,
    OptimizationControls,
    TuiLoader,
    SolutionsTableComponent,
    TuiTabs,
    LivraisonsFilterComponent,
    AppBreadcrumbsComponent,
    RouterLink,
  ],
  templateUrl: './Journee.html',
  styleUrl: './Journee.scss',
})
export class JourneePage {
  private readonly _backend = inject(BackendApiService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _destroyRef = inject(DestroyRef);

  protected readonly hidePins = signal<boolean>(false);
  protected readonly activeTab = signal<number>(0);
  protected readonly highlightedLivraisonId = signal<number | null>(null);

  protected readonly parking = signal<Adresse | null>(null);
  protected readonly livraisonsData = signal<LivraisonResponse[]>([]);
  protected readonly hiddenLivraisonIds = signal<ReadonlySet<number>>(new Set());
  protected readonly journeeState = signal<JourneeResponse | null>(null);
  protected readonly solutions = signal<SolutionStatisticsResponse[]>([]);
  protected readonly solutionChoisie = signal<SolutionResponse | null>(null);
  protected readonly loading = signal<boolean>(false);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedDate = signal<string | null>(null);

  protected readonly statutLabel = computed(() => {
    switch (this.journeeState()?.statutGeneration ?? null) {
      case null: return '⚪ Aucune optimisation demandée';
      case 'INITIALISATION': return '⏳ Initialisation';
      case 'GENERATION_MATRICE': return '⚙️ Génération de la matrice';
      case 'GENERATION_ROUTE': return '🗺️ Génération des routes';
      case 'TERMINE': return '✅ Terminé';
      default: return this.journeeState()?.statutGeneration ?? '';
    }
  });

  protected readonly formattedDate = computed(() => {
    const date = this.selectedDate();
    if (!date) return '';
    const [year, month, day] = date.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  });

  protected readonly formatIsoDuration = formatIsoDuration;

  protected formatNumber(value: number | null | undefined): string {
    if (value == null) return '—';
    return Number.isFinite(value) ? (value % 1 === 0 ? value.toString() : value.toFixed(1)) : '—';
  }

  protected solutionStats(sol: { statistiques?: SolutionStatisticsResponse | null }): SolutionStatisticsResponse | null {
    return sol.statistiques ?? null;
  }

  protected readonly breadcrumbs = computed(() => [
    { caption: 'Accueil', routerLink: '/journee' },
    { caption: `Journée du ${this.selectedDate() ?? '…'}` },
  ]);

  protected readonly visibleLivraisons = computed(() =>
    this.livraisonsData().filter(l => !this.hiddenLivraisonIds().has(l.id)),
  );

  protected readonly destinations = computed(() =>
    this.visibleLivraisons().map(l => ({ ...this._mapAdresse(l.adresse), livraison: l })),
  );

  constructor() {
    this._route.paramMap
      .pipe(takeUntilDestroyed(this._destroyRef))
      .subscribe(params => {
        const date = params.get('date');
        if (!date) return;
        this.selectedDate.set(date);
        void this._loadJournee(date);
      });

    toObservable(this.selectedDate)
      .pipe(
        takeUntilDestroyed(this._destroyRef),
        filter((v): v is string => !!v),
        switchMap((date) =>
          stompTopic(
            `ws://${window.location.host}/ws`,
            `/topic/journee/${date}`,
            JourneeStatusUpdateSchema,
          ).pipe(debounceTime(3000)),
        ),
      )
      .subscribe((update) => {
        const current = this.journeeState();
        this.journeeState.set({ ...(current ?? { date: update.date, strategie: null }), ...update });
        const date = this.selectedDate();
        if (date) void this._loadSolutions(date);
      });
  }

  protected async onOptimize(config: OptimizationConfig): Promise<void> {
    const date = this.selectedDate();
    if (!date) {
      this.error.set('Aucune date sélectionnée');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      const backendConfig = {
        nbVehicules: config.nbVehicules,
        maxTempsParVehicule: millisecondsToIsoDuration(config.maxTimePerVehicule),
      };

      const livraisonIds = config.livraisonIds ?? [];
      const updatedJournee = await this._backend.optimizeJournee(date, backendConfig, livraisonIds);
      this.journeeState.set(updatedJournee);
    } catch (err) {
      console.error('Failed to optimize journee', err);
      this.error.set("Échec de l'optimisation de la journée");
    } finally {
      this.loading.set(false);
    }
  }

  protected async onFilterChange(
    params: { livree?: boolean; dateDebut?: string; dateFin?: string },
  ): Promise<void> {
    this.hiddenLivraisonIds.set(new Set());
    await this._loadLivraisons(params);
  }

  protected onToggleLivraison(id: number): void {
    this.hiddenLivraisonIds.update(current => {
      const next = new Set(current);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  protected async onChoisirSolution(solutionId: number): Promise<void> {
    try {
      const chosen = await this._backend.choisirSolution(solutionId);
      this.solutionChoisie.set(chosen);
    } catch (err) {
      console.error('Failed to choose solution', err);
      this.error.set('Impossible de choisir la solution');
    }
  }

  private async _loadJournee(date: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [parking, livraisons, journee, solutions, solutionChoisie] = await Promise.all([
        this._backend.getParking(),
        this._backend.getLivraisons({ livree: false }),
        this._backend.getJournee(date),
        this._backend.getJourneeSolutions(date),
        this._backend.getSolutionChoisie(date),
      ]);
      this.parking.set(parking);
      this.livraisonsData.set(livraisons);
      this.journeeState.set(journee);
      this.solutions.set(solutions);
      this.solutionChoisie.set(solutionChoisie);
    } catch (err) {
      console.error('Failed to load journee data', err);
      this.error.set('Échec du chargement des données de la journée');
    } finally {
      this.loading.set(false);
    }
  }

  private _mapAdresse(dto: {
    coordonnees: { lat: number; lng: number };
    rue: string;
    numero: string;
    codePostal: string;
    ville: string;
  }): Adresse {
    return {
      coords: dto.coordonnees,
      name: `${dto.numero} ${dto.rue}`.trim(),
      postCode: dto.codePostal,
      city: dto.ville,
    };
  }

  private async _loadLivraisons(filterParams: {
    livree?: boolean;
    dateDebut?: string;
    dateFin?: string;
  }): Promise<void> {
    try {
      const livraisons = await this._backend.getLivraisons(filterParams);
      this.livraisonsData.set(livraisons);
    } catch (err) {
      console.error('Failed to load livraisons', err);
    }
  }

  private async _loadSolutions(date: string): Promise<void> {
    try {
      const solutions = await this._backend.getJourneeSolutions(date);
      this.solutions.set(solutions);
    } catch (err) {
      console.error('Failed to load solutions', err);
    }
  }
}
