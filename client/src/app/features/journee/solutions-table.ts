import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SolutionStatistiques } from '../../core/models/api';
import { formatDuration, isoToSeconds } from '../../core/util/duration';
import { formatDistance, formatEuros, formatFuel } from '../../core/util/format';
import { optimisationLabel, separationLabel } from '../../core/util/labels';

type Column = 'nbVehicules' | 'nbLivraisonsTotal' | 'distanceTotal' | 'tempsTotal' | 'carburantTotal' | 'coutTotal' | 'marge';

const VALUE: Record<Column, (s: SolutionStatistiques) => number | null> = {
  nbVehicules: (s) => s.nbVehicules,
  nbLivraisonsTotal: (s) => s.nbLivraisonsTotal,
  distanceTotal: (s) => s.distanceTotal,
  tempsTotal: (s) => isoToSeconds(s.tempsTotal),
  carburantTotal: (s) => s.carburantTotal,
  coutTotal: (s) => s.finances?.coutTotal ?? null,
  marge: (s) => s.finances?.marge ?? null,
};

/** Tableau comparatif : le planificateur trie selon son critère du jour. */
@Component({
  selector: 'app-solutions-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <div class="scroll">
      <table class="table">
        <thead>
          <tr>
            <th>Stratégie</th>
            @for (column of columns; track column.key) {
              <th class="right sortable" [class.sorted]="sort().column === column.key" (click)="sortBy(column.key)"
                  [attr.aria-sort]="sort().column === column.key ? (sort().ascending ? 'ascending' : 'descending') : null">
                {{ column.label }}{{ sort().column === column.key ? (sort().ascending ? ' ↑' : ' ↓') : '' }}
              </th>
            }
            <th class="right"><span class="sr-only">Actions</span></th>
          </tr>
        </thead>
        <tbody>
          @for (solution of sorted(); track solution.id) {
            <tr [class.chosen]="solution.id === choisieId()">
              <td>
                <div class="strategy">
                  <span>{{ separation(solution) }} · {{ optimisation(solution) }}</span>
                  <span class="mono muted">n° {{ solution.id }}</span>
                </div>
              </td>
              <td class="right num">{{ solution.nbVehicules }}</td>
              <td class="right num">{{ solution.nbLivraisonsTotal }}</td>
              <td class="right num">{{ distance(solution.distanceTotal) }}</td>
              <td class="right num">{{ duration(solution.tempsTotal) }}</td>
              <td class="right num">{{ fuel(solution.carburantTotal) }}</td>
              <td class="right num">{{ euros(solution.finances?.coutTotal) }}</td>
              <td class="right num" [class.negative]="(solution.finances?.marge ?? 0) < 0">{{ euros(solution.finances?.marge) }}</td>
              <td class="right actions">
                <a class="btn small" [routerLink]="['/solutions', solution.id]">Ouvrir</a>
                @if (solution.id === choisieId()) {
                  <span class="tag accent">Retenue</span>
                } @else {
                  <button class="btn small ghost" type="button" (click)="choose.emit(solution.id)">Retenir</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .scroll {
      overflow-x: auto;
    }

    .strategy {
      display: flex;
      flex-direction: column;
      gap: 2px;
      padding: 4px 0;
    }

    tr.chosen td:first-child {
      box-shadow: inset 3px 0 0 var(--accent);
    }

    td {
      height: 52px;
    }

    .negative {
      color: var(--negative);
    }

    .actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
      align-items: center;
      height: 52px;
    }

    .actions .tag,
    .actions .ghost {
      min-width: 72px;
      justify-content: center;
    }
  `,
})
export class SolutionsTable {
  readonly solutions = input.required<SolutionStatistiques[]>();
  readonly choisieId = input<number | null>(null);
  readonly choose = output<number>();

  protected readonly columns: { key: Column; label: string }[] = [
    { key: 'nbVehicules', label: 'Véhicules' },
    { key: 'nbLivraisonsTotal', label: 'Livraisons' },
    { key: 'distanceTotal', label: 'Distance' },
    { key: 'tempsTotal', label: 'Durée' },
    { key: 'carburantTotal', label: 'Carburant' },
    { key: 'coutTotal', label: 'Coût' },
    { key: 'marge', label: 'Marge' },
  ];

  protected readonly sort = signal<{ column: Column; ascending: boolean }>({ column: 'coutTotal', ascending: true });

  protected readonly sorted = computed(() => {
    const { column, ascending } = this.sort();
    const value = VALUE[column];
    return [...this.solutions()].sort((a, b) => {
      const [va, vb] = [value(a), value(b)];
      if (va === vb) return a.id - b.id;
      if (va === null) return 1;
      if (vb === null) return -1;
      return ascending ? va - vb : vb - va;
    });
  });

  protected readonly distance = formatDistance;
  protected readonly duration = formatDuration;
  protected readonly fuel = formatFuel;
  protected readonly euros = formatEuros;

  protected separation(solution: SolutionStatistiques): string {
    return separationLabel(solution.strategie?.separation);
  }

  protected optimisation(solution: SolutionStatistiques): string {
    return optimisationLabel(solution.strategie?.optimisation);
  }

  protected sortBy(column: Column): void {
    this.sort.update((current) => ({
      column,
      ascending: current.column === column ? !current.ascending : column !== 'nbLivraisonsTotal' && column !== 'marge',
    }));
  }
}
