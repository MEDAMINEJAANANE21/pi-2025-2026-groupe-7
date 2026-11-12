import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TuiButton, TuiSurface } from '@taiga-ui/core';
import { TuiTable } from '@taiga-ui/addon-table';
import { ValueDurationComponent } from '../value-duration/value-duration';
import { ValueDistanceComponent } from '../value-distance/value-distance';
import { ValueFuelComponent } from '../value-fuel/value-fuel';
import { SolutionStatisticsResponse } from '../../models/backend';
import { formatNumber } from '../../utils/format';

type SortableColumn =
  | 'nbVehicules'
  | 'nbLivraisonsTotal'
  | 'nbLivraisonsMin'
  | 'nbLivraisonsMax'
  | 'nbLivraisonsMoyenne'
  | 'distanceTotal'
  | 'distanceMin'
  | 'distanceMax'
  | 'distanceMoyenne'
  | 'carburantTotal'
  | 'carburantMin'
  | 'carburantMax'
  | 'carburantMoyenne'
  | 'tempsTotal'
  | 'tempsTotalMin'
  | 'tempsTotalMax'
  | 'tempsTotalMoyenne';

@Component({
  selector: 'app-solutions-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TuiTable, TuiButton, TuiSurface, ValueDurationComponent, ValueDistanceComponent, ValueFuelComponent],
  templateUrl: './solutions-table.html',
  styleUrl: './solutions-table.scss',
})
export class SolutionsTableComponent {
  private readonly _solutions = signal<SolutionStatisticsResponse[]>([]);

  @Input() set solutions(value: SolutionStatisticsResponse[] | null | undefined) {
    this._solutions.set(value ?? []);
  }

  @Input() solutionChoisieId: number | null = null;

  @Output() choisirSolution = new EventEmitter<number>();

  protected readonly sortColumn = signal<SortableColumn>('nbLivraisonsTotal');
  protected readonly sortDirection = signal<'asc' | 'desc'>('desc');

  protected readonly sortedSolutions = computed(() => {
    const data = [...this._solutions()];
    const column = this.sortColumn();
    const direction = this.sortDirection();
    const factor = direction === 'asc' ? 1 : -1;

    return data.sort((a, b) => {
      const av = this.sortValue(a, column);
      const bv = this.sortValue(b, column);
      if (av === bv) return 0;
      if (av === undefined) return 1;
      if (bv === undefined) return -1;
      return av > bv ? factor : -factor;
    });
  });

  protected toggleSort(column: SortableColumn): void {
    if (this.sortColumn() === column) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(column);
      this.sortDirection.set('desc');
    }
  }

  protected readonly formatNumber = formatNumber;

  protected formatDuration(value: string | null | undefined): string {
    return value ?? '—';
  }

  // Parse an ISO 8601 duration (e.g. "PT1H30M15.5S") to total seconds.
  // Returns undefined for null/undefined/invalid inputs.
  private parseIsoDuration(value: string | null | undefined): number | undefined {
    if (!value) return undefined;
    // Basic ISO 8601 duration regex for years, months, days, hours, minutes, seconds
    // We'll convert everything to seconds; years/months are not supported reliably and will be ignored
    const isoRegex = /^P(?:(\d+(?:[.,]\d+)?)Y)?(?:(\d+(?:[.,]\d+)?)M)?(?:(\d+(?:[.,]\d+)?)D)?(?:T(?:(\d+(?:[.,]\d+)?)H)?(?:(\d+(?:[.,]\d+)?)M)?(?:(\d+(?:[.,]\d+)?)S)?)?$/;
    const m = isoRegex.exec(value);
    if (!m) return undefined;

    const toNumber = (s: string | undefined) => (s === undefined ? 0 : Number(s.replace(',', '.')));

    // Note: years and months are ignored because their lengths vary; if present, treat them as 0
    const days = toNumber(m[3]);
    const hours = toNumber(m[4]);
    const minutes = toNumber(m[5]);
    const seconds = toNumber(m[6]);

    const totalSeconds = days * 24 * 3600 + hours * 3600 + minutes * 60 + seconds;
    return Number.isFinite(totalSeconds) ? totalSeconds : undefined;
  }

  private sortValue(row: SolutionStatisticsResponse, column: SortableColumn): number | string | undefined {
    switch (column) {
      case 'nbVehicules':
        return row.nbVehicules;
      case 'nbLivraisonsTotal':
        return row.nbLivraisonsTotal;
      case 'distanceTotal':
        return row.distanceTotal ?? undefined;
      case 'distanceMin':
        return row.distanceMin ?? undefined;
      case 'distanceMax':
        return row.distanceMax ?? undefined;
      case 'distanceMoyenne':
        return row.distanceMoyenne ?? undefined;
      case 'carburantTotal':
        return row.carburantTotal ?? undefined;
      case 'carburantMin':
        return row.carburantMin ?? undefined;
      case 'carburantMax':
        return row.carburantMax ?? undefined;
      case 'carburantMoyenne':
        return row.carburantMoyenne ?? undefined;
      case 'nbLivraisonsMin':
        return row.nbLivraisonsMin ?? undefined;
      case 'nbLivraisonsMax':
        return row.nbLivraisonsMax ?? undefined;
      case 'nbLivraisonsMoyenne':
        return row.nbLivraisonsMoyenne ?? undefined;
      case 'tempsTotal':
        return this.parseIsoDuration(row.tempsTotal);
      case 'tempsTotalMin':
        return this.parseIsoDuration(row.tempsTotalMin);
      case 'tempsTotalMax':
        return this.parseIsoDuration(row.tempsTotalMax);
      case 'tempsTotalMoyenne':
        return this.parseIsoDuration(row.tempsTotalMoyenne);
      default:
        return undefined;
    }
  }
   
}
