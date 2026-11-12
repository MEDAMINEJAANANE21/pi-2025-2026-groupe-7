import { TuiButton, TuiLabel, TuiLoader } from '@taiga-ui/core';
import { AppBreadcrumbsComponent } from '../../components/app-breadcrumbs/app-breadcrumbs';
import { TuiCardMedium } from "@taiga-ui/layout";
import { TuiCheckbox } from '@taiga-ui/kit';
import { Component, computed, inject, signal, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RoutesMap } from "../../components/RoutesMap/RoutesMap";
import { Coords } from "../../models/coords";
import { SolutionEditor } from "../../components/solution-editor/solution-editor";
import { BackendApiService } from "../../services/BackendApiService";
import { ActivatedRoute } from '@angular/router';
import { Adresse, AdresseWithMeta } from '../../models/adresse';
import { Route } from '../../models/route';
import { ROUTE_COLORS } from '../../utils/routeColors';
import { LivraisonResponse, StrategieResponse, SolutionTourneesUpdateRequest, TourneeStatisticsResponse } from '../../models/backend';
import { ParsedSolutionResponse } from '../../models/parsedSolutionResponse';
import { ValueDurationComponent } from '../../components/value-duration/value-duration';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, merge, forkJoin, from, EMPTY, debounceTime, switchMap, finalize, catchError } from 'rxjs';
import { formatIsoDuration } from '../../utils/duration';

type Tournee = readonly LivraisonResponse[];

@Component({
  selector: 'app-solution',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RoutesMap,
    TuiLabel,
    TuiLoader,
    TuiButton,
    TuiCardMedium,
    TuiCheckbox,
    FormsModule,
    SolutionEditor,
    ValueDurationComponent,
    AppBreadcrumbsComponent,
  ],
  templateUrl: './Solution.html',
  styleUrl: './Solution.scss',
})
export class Solution {
  private readonly _backend = inject(BackendApiService);
  private readonly _route = inject(ActivatedRoute);
  private readonly _destroyRef = inject(DestroyRef);

  protected readonly solutionId;

  protected readonly breadcrumbs = computed(() => {
    const date = this.date();
    return [
      { caption: 'Accueil', routerLink: '/journee' },
      ...(date ? [{ caption: `Journée du ${date}`, routerLink: `/journee/${date}` }] : []),
      { caption: `Solution #${this.solutionId ?? '…'}` },
    ];
  });

  // Server state
  protected readonly parking = signal<Adresse | null>(null);
  protected readonly horseSolutionLivraisons = signal<readonly LivraisonResponse[]>([]);
  protected readonly date = signal<String | null>(null);
  protected readonly solution = signal<ParsedSolutionResponse | null>(null);
  protected readonly routes = signal<Route[]>([]);
  protected readonly tourneeIds = signal<number[]>([]);

  // Local editing state — diverges from server when user makes changes
  protected readonly localTournees = signal<readonly Tournee[]>([]);
  protected readonly isDirty = signal<boolean>(false);
  protected readonly routesStale = signal<boolean>(false);

  // Per-tournee visibility (indexes into localTournees / routes)
  protected readonly hiddenTournees = signal<ReadonlySet<number>>(new Set());
  protected readonly missingLivraisonsHidden = signal<boolean>(false);

  protected readonly visibleRoutes = computed<Route[]>(() => {
    const hidden = this.hiddenTournees();
    if (hidden.size === 0) return this.routes();
    return this.routes().map((r, i) => (hidden.has(i) ? [] : r));
  });

  protected readonly currentTourneeStats = computed<readonly (TourneeStatisticsResponse | null)[]>(() =>
    (this.solution()?.tourneeStatistics ?? []).map(t => (t.statistiques as TourneeStatisticsResponse) ?? null)
  );

  protected readonly visibleDestinations = computed<readonly AdresseWithMeta[]>(() => {
    const hidden = this.hiddenTournees();
    const inSolution = this.localTournees().flatMap((tournee, i) => {
      if (hidden.has(i)) return [];
      const color = ROUTE_COLORS[i % ROUTE_COLORS.length];
      return tournee.map(l => ({ ...this._livraisonToAdresse(l), color, livraison: l }));
    });
    const hors: AdresseWithMeta[] = this.missingLivraisonsHidden()
      ? []
      : this.horseSolutionLivraisons().map(l => ({
          ...this._livraisonToAdresse(l),
          horsSolution: true as const,
          livraison: l,
        }));
    return [...inSolution, ...hors];
  });


protected readonly bounds = signal<readonly [Coords, Coords]>([
    { lat: 45.1, lng: 5.6 },
    { lat: 45.3, lng: 5.9 },
  ]);
  protected readonly hidePins = signal<boolean>(false);
  protected readonly highlightedLivraisonId = signal<number | null>(null);
  protected readonly pinnedLivraisonId = signal<number | null>(null);
  protected readonly loading = signal<boolean>(false);
  private _pendingRequests = 0;

  // Route redraw triggers
  private readonly _routeEditSubject = new Subject<void>();
  private readonly _manualRedrawSubject = new Subject<void>();

  constructor() {
    this.solutionId = this._route.snapshot.paramMap.get('solutionId');

    // RxJS pipeline: redraw routes on manual trigger or 15s after the last edit
    merge(
      this._manualRedrawSubject,
      this._routeEditSubject.pipe(debounceTime(15000)),
    )
      .pipe(
        switchMap(() => {
          const parking = this.parking();
          const localTournees = this.localTournees();
          if (!parking) return EMPTY;
          if (localTournees.length === 0) {
            this.routes.set([]);
            this.routesStale.set(false);
            return EMPTY;
          }
          this._startLoading();
          return forkJoin(
            localTournees.map(route => {
              const rt = route.map(l => [l.adresse.coordonnees.lng, l.adresse.coordonnees.lat] as [number, number]);
              rt.unshift([parking.coords.lng, parking.coords.lat]);
              rt.push([parking.coords.lng, parking.coords.lat]);
              return from(this._backend.getDirections(rt));
            }),
          ).pipe(
            finalize(() => this._stopLoading()),
            catchError(err => {
              console.error('Route computation failed', err);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this._destroyRef),
      )
      .subscribe(routeCoords => {
        this.routes.set(routeCoords.map(r => r.map(([lng, lat]) => ({ lat, lng }))));
        this.routesStale.set(false);
      });

    if (this.solutionId) {
      this._startLoading();
      this._backend
        .getParking()
        .then(parking => this.parking.set(parking))
        .finally(() => this._stopLoading());

      this._startLoading();
      Promise.all([
        this._backend.getSolution(+this.solutionId),
        this._backend.getSolutionLivraisons(+this.solutionId),
      ])
        .then(([solution, livraisons]) => {
          this.date.set(solution.date);
          this.solution.set(solution);
          const stats = solution.tourneeStatistics ?? [];
          this.tourneeIds.set(stats.map(t => t.id));
          this.localTournees.set(solution.tournees);

          if (livraisons) {
            this.horseSolutionLivraisons.set(livraisons.horsSolution);
          }
          this._manualRedrawSubject.next();
        })
        .catch(error => console.error('Error fetching solution:', error))
        .finally(() => this._stopLoading());
    }
  }

  private _livraisonToAdresse(l: LivraisonResponse): Adresse {
    return {
      coords: l.adresse.coordonnees,
      name: `${l.adresse.numero} ${l.adresse.rue}`.trim(),
      postCode: l.adresse.codePostal,
      city: l.adresse.ville,
    };
  }

  private _startLoading(): void {
    this._pendingRequests++;
    this.loading.set(true);
  }

  private _stopLoading(): void {
    this._pendingRequests = Math.max(0, this._pendingRequests - 1);
    if (this._pendingRequests === 0) this.loading.set(false);
  }

  protected getTourneeCount(): number {
    return this.solution()?.tournees?.length ?? 0;
  }

  protected getStat(key: string): any {
    const s = this.solution()?.statistiques;
    return s ? (s[key] ?? null) : null;
  }

  protected getStrategieNbVehicules(): number | null {
    const s = this.solution()?.strategie as StrategieResponse | null | undefined;
    return s?.nbVehicules ?? null;
  }

  protected formatNumber(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return Number.isFinite(value) ? (value % 1 === 0 ? value.toString() : value.toFixed(1)) : '—';
  }

  protected readonly formatIsoDuration = formatIsoDuration;

  protected triggerRedraw(): void {
    this._manualRedrawSubject.next();
  }

  // Called by the editor on every drag/drop — updates local state only, no backend call
  protected onUpdateTournees(updatedTournees: readonly Tournee[]): void {
    this.localTournees.set(updatedTournees);
    this.isDirty.set(true);
    this.routesStale.set(true);
    this._routeEditSubject.next();
  }

  // Explicit save: syncs localTournees to the server, then refreshes stats + missing livraisons
  protected async saveSolution(): Promise<void> {
    const solutionId = this.solutionId;
    if (!solutionId) return;

    const tourneeIds = this.tourneeIds();
    const request: SolutionTourneesUpdateRequest = {
      tournees: this.localTournees().map((tournee, ti) => ({
        tourneeId: tourneeIds[ti],
        livraisonIds: tournee.map(l => l.id),
      })),
    };

    this._startLoading();
    try {
      const updated = await this._backend.updateSolutionTournees(+solutionId, request);
      const stats = updated.tourneeStatistics ?? [];
      this.tourneeIds.set(stats.map(t => t.id));
      this.localTournees.set(updated.tournees);
      this.solution.set(updated);
      this.isDirty.set(false);

      const livraisons = await this._backend.getSolutionLivraisons(+solutionId);
      if (livraisons) {
        this.horseSolutionLivraisons.set(livraisons.horsSolution);
      }
    } catch (error) {
      console.error('Error saving solution:', error);
    } finally {
      this._stopLoading();
    }
  }

  protected toggleTourneeVisibility(index: number): void {
    this.hiddenTournees.update(set => {
      const next = new Set(set);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  }

  protected toggleMissingVisibility(): void {
    this.missingLivraisonsHidden.update(v => !v);
  }

  protected onUpdateHorseSolution(updated: readonly LivraisonResponse[]): void {
    this.horseSolutionLivraisons.set(updated);
    this.isDirty.set(true);
  }
}
