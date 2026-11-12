import { Component, computed, effect, ElementRef, inject, signal, ChangeDetectionStrategy, input, output, untracked } from '@angular/core';
import { TuiAppearance, TuiButton, TuiDialog } from '@taiga-ui/core';
import { TuiAccordion } from '@taiga-ui/kit';
import { Adresse } from '../../models/adresse';
import { LivraisonResponse, TourneeStatisticsResponse } from '../../models/backend';
import { formatIsoDuration, addIsoDurations } from '../../utils/duration';
import { ROUTE_COLORS } from '../../utils/routeColors';


interface EditableTourneeStep {
  type: 'start' | 'job' | 'end';
  isParking: boolean;
  parkingAddress: Adresse | null;
  livraison: LivraisonResponse | null;
}

type EditableTournee = EditableTourneeStep[];
type Tournee = readonly LivraisonResponse[];

@Component({
  selector: 'app-solution-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TuiButton,
    TuiDialog,
    TuiAccordion,
    TuiAppearance,
  ],
  templateUrl: './solution-editor.html',
  styleUrl: './solution-editor.scss',
})
export class SolutionEditor {
  public readonly routes = input<readonly Tournee[]>([]);
  public readonly parking = input<Adresse | null>(null);
  public readonly hiddenTournees = input<ReadonlySet<number>>(new Set());
  public readonly horsSolutionLivraisons = input<readonly LivraisonResponse[]>([]);
  public readonly missingLivraisonsHidden = input<boolean>(false);
  public readonly tourneeStats = input<readonly (TourneeStatisticsResponse | null)[]>([]);
  public readonly highlightedId = input<number | null>(null);
  public readonly pinnedLivraisonId = input<number | null>(null);
  public readonly onUpdate = output<readonly Tournee[]>();
  public readonly onToggleVisibility = output<number>();
  public readonly onToggleMissingVisibility = output<void>();
  public readonly onUpdateMissingLivraisons = output<readonly LivraisonResponse[]>();
  public readonly livraisonHovered = output<number | null>();

  // kept for backwards-compat with RoutesMap pin display in parent; unused inside editor
  public readonly destinations = input<readonly Adresse[]>([]);

  private readonly _el = inject(ElementRef<HTMLElement>);

  protected readonly draggedStep = signal<{ routeIndex: number; stepIndex: number } | null>(null);
  protected readonly openTournees = signal<Map<number, boolean>>(new Map());
  protected readonly missingOpen = signal<boolean>(true);
  protected moveDialogOpen = false;
  protected moveDialogStep = signal<{ routeIndex: number; stepIndex: number } | null>(null);
  protected assignMissingDialogOpen = false;
  protected readonly assignMissingLivraison = signal<LivraisonResponse | null>(null);

  private readonly routeColors = ROUTE_COLORS;

  protected readonly editableRoutes = computed<EditableTournee[]>(() => {
    const parking = this.parking();
    return this.routes().map(route => {
      const steps: EditableTourneeStep[] = [];
      steps.push({ type: 'start', isParking: true, parkingAddress: parking, livraison: null });
      route.forEach(livraison => {
        steps.push({ type: 'job', isParking: false, parkingAddress: null, livraison });
      });
      steps.push({ type: 'end', isParking: true, parkingAddress: parking, livraison: null });
      return steps;
    });
  });

  protected readonly formatIsoDuration = formatIsoDuration;
  protected readonly addIsoDurations = addIsoDurations;

  constructor() {
    // Default new route indices to open; clean up removed ones.
    effect(() => {
      const routes = this.routes();
      untracked(() => {
        this.openTournees.update(map => {
          const next = new Map(map);
          routes.forEach((_, i) => {
            if (!next.has(i)) next.set(i, true);
          });
          for (const k of next.keys()) {
            if (k >= routes.length) next.delete(k);
          }
          return next;
        });
      });
    });

    // When a livraison is pinned, expand its tournee accordion and scroll to the step.
    effect(() => {
      const id = this.pinnedLivraisonId();
      if (id === null) return;
      untracked(() => {
        const idx = this.routes().findIndex(r => r.some(l => l.id === id));
        if (idx !== -1) {
          this.openTournees.update(m => new Map(m).set(idx, true));
        } else if (this.horsSolutionLivraisons().some(l => l.id === id)) {
          this.missingOpen.set(true);
        }
        // Wait for accordion animation before scrolling (~300ms for TuiAccordion).
        setTimeout(() => {
          const el = this._el.nativeElement.querySelector(`[data-livraison-id="${id}"]`) as HTMLElement | null;
          el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }, 320);
      });
    });
  }

  protected onTourneeOpenChange(routeIndex: number, isOpen: boolean): void {
    this.openTournees.update(m => new Map(m).set(routeIndex, isOpen));
  }

  protected onStepMouseEnter(livraison: LivraisonResponse | null): void {
    if (livraison) this.livraisonHovered.emit(livraison.id);
  }

  protected onStepMouseLeave(livraison: LivraisonResponse | null): void {
    if (livraison) this.livraisonHovered.emit(null);
  }

  protected onDragStart(event: DragEvent, routeIndex: number, stepIndex: number): void {
    const step = this.editableRoutes()[routeIndex][stepIndex];
    if (step.isParking) {
      event.preventDefault();
      return;
    }
    this.draggedStep.set({ routeIndex, stepIndex });
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', `${routeIndex},${stepIndex}`);
    }
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  protected onDrop(event: DragEvent, targetRouteIndex: number, targetStepIndex: number): void {
    event.preventDefault();
    const dragged = this.draggedStep();
    if (!dragged) return;
    const targetStep = this.editableRoutes()[targetRouteIndex][targetStepIndex];
    if (targetStep.type === 'start') {
      this.draggedStep.set(null);
      return;
    }
    this.moveStep(dragged.routeIndex, dragged.stepIndex, targetRouteIndex, targetStepIndex);
    this.draggedStep.set(null);
  }

  protected onDragEnd(): void {
    this.draggedStep.set(null);
  }

  private moveStep(
    fromRouteIndex: number,
    fromStepIndex: number,
    toRouteIndex: number,
    toStepIndex: number
  ): void {
    const editable = this.editableRoutes();

    const editableToJobIndex = (edSteps: EditableTourneeStep[], stepIndex: number) => {
      let count = 0;
      for (let i = 0; i < stepIndex; i++) {
        if (!edSteps[i].isParking) count++;
      }
      return count;
    };

    const newRoutes: LivraisonResponse[][] = this.routes().map(r => [...r]);
    const fromJobIndex = editableToJobIndex(editable[fromRouteIndex], fromStepIndex);
    const toJobIndex = editableToJobIndex(editable[toRouteIndex], toStepIndex);

    const [moved] = newRoutes[fromRouteIndex].splice(fromJobIndex, 1);
    newRoutes[toRouteIndex] = [
      ...newRoutes[toRouteIndex].slice(0, toJobIndex),
      moved,
      ...newRoutes[toRouteIndex].slice(toJobIndex),
    ];

    this.onUpdateRoutes(newRoutes);
  }

  protected openMoveDialog(routeIndex: number, stepIndex: number): void {
    this.moveDialogStep.set({ routeIndex, stepIndex });
    this.moveDialogOpen = true;
  }

  protected confirmMoveToRoute(toRouteIndex: number, observer: { complete: () => void }): void {
    const step = this.moveDialogStep();
    if (step) {
      this.moveStepToRoute(step.routeIndex, step.stepIndex, toRouteIndex);
    }
    observer.complete();
  }

  protected moveStepToRoute(fromRouteIndex: number, stepIndex: number, toRouteIndex: number): void {
    const ed = this.editableRoutes();
    const step = ed[fromRouteIndex][stepIndex];
    if (step.isParking) return;

    const newRoutes: LivraisonResponse[][] = this.routes().map(r => [...r]);

    let jobIndex = 0;
    for (let i = 0; i < stepIndex; i++) if (!ed[fromRouteIndex][i].isParking) jobIndex++;

    const [moved] = newRoutes[fromRouteIndex].splice(jobIndex, 1);
    newRoutes[toRouteIndex] = newRoutes[toRouteIndex].concat(moved);

    this.onUpdateRoutes(newRoutes);
  }

  protected getStepLabel(step: EditableTourneeStep, deliveryNumber: number): string {
    if (step.isParking) {
      return step.type === 'start' ? '🅿️ Départ' : '🅿️ Retour';
    }
    const l = step.livraison!;
    return `📦 ${deliveryNumber}. ${l.adresse.numero} ${l.adresse.rue} [#${l.id}]`;
  }

  protected getDeliveryNumber(route: EditableTournee, stepIndex: number): number {
    let count = 0;
    for (let i = 0; i < stepIndex; i++) {
      if (!route[i].isParking) count++;
    }
    return count + 1;
  }

  protected getTourneeStats(routeIndex: number): TourneeStatisticsResponse | null {
    return this.tourneeStats()[routeIndex] ?? null;
  }

  protected isHidden(routeIndex: number): boolean {
    return this.hiddenTournees().has(routeIndex);
  }

  protected getRouteColor(routeIndex: number): string {
    return this.routeColors[routeIndex % this.routeColors.length];
  }

  protected getLivraisonColor(livraisonId: number): string {
    const hue = (livraisonId * 137.508) % 360;
    return `hsl(${hue}, 65%, 50%)`;
  }

  protected excludeFromRoutes(routeIndex: number, stepIndex: number): void {
    const ed = this.editableRoutes();
    const livraison = ed[routeIndex][stepIndex].livraison;
    if (!livraison) return;

    let jobIndex = 0;
    for (let i = 0; i < stepIndex; i++) {
      if (!ed[routeIndex][i].isParking) jobIndex++;
    }
    const newRoutes = this.routes().map(r => [...r]);
    newRoutes[routeIndex].splice(jobIndex, 1);
    this.onUpdateRoutes(newRoutes);
    this.onUpdateMissingLivraisons.emit([...this.horsSolutionLivraisons(), livraison]);
  }

  protected openAssignMissingDialog(livraison: LivraisonResponse): void {
    this.assignMissingLivraison.set(livraison);
    this.assignMissingDialogOpen = true;
  }

  protected confirmAssignMissing(tourneeIndex: number, observer: { complete: () => void }): void {
    const livraison = this.assignMissingLivraison();
    if (livraison) {
      const newRoutes = this.routes().map(r => [...r]);
      newRoutes[tourneeIndex] = [...newRoutes[tourneeIndex], livraison];
      this.onUpdateRoutes(newRoutes);
      this.onUpdateMissingLivraisons.emit(
        this.horsSolutionLivraisons().filter(l => l.id !== livraison.id)
      );
    }
    observer.complete();
  }

  protected onUpdateRoutes(updatedRoutes: Tournee[]): void {
    if (document.startViewTransition) {
      document.startViewTransition(() => this.onUpdate.emit(updatedRoutes));
    } else {
      this.onUpdate.emit(updatedRoutes);
    }
  }
}
