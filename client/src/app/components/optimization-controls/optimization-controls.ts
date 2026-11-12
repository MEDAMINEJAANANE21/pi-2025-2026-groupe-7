import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TuiButton, TuiLabel, TuiSurface, TuiTextfield, TuiTitle } from '@taiga-ui/core';
import { TuiInputNumber } from '@taiga-ui/kit';
import { TuiCardLarge } from '@taiga-ui/layout';
import { LivraisonResponse } from '../../models/backend';

const MS_PER_MINUTE = 60_000;

export interface OptimizationConfig {
  nbVehicules: number;
  maxTimePerVehiculeMs: number;
  livraisonIds: number[];
}

@Component({
  selector: 'app-optimization-controls',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TuiTextfield,
    TuiButton,
    TuiLabel,
    TuiInputNumber,
    TuiCardLarge,
    TuiSurface,
    TuiTitle,
  ],
  templateUrl: './optimization-controls.html',
  styleUrl: './optimization-controls.scss',
})
export class OptimizationControls {
  public readonly livraisons = input<LivraisonResponse[]>([]);
  public readonly onOptimize = output<OptimizationConfig>();

  protected readonly nbVehiculesControl = new FormControl(3, { nonNullable: true });
  protected readonly maxTimeHoursControl = new FormControl(8, { nonNullable: true });
  protected readonly maxTimeMinutesControl = new FormControl(0, { nonNullable: true });

  protected readonly hasLivraisons = computed(() => this.livraisons().length > 0);

  protected onSubmit(event?: Event): void {
    event?.preventDefault();
    const nbVehicules = this.nbVehiculesControl.value;
    const totalMinutes =
      Number(this.maxTimeHoursControl.value ?? 0) * 60 + Number(this.maxTimeMinutesControl.value ?? 0);
    const livraisonIds = this.livraisons().map(l => l.id);

    if (!nbVehicules || totalMinutes <= 0 || livraisonIds.length === 0) return;

    this.onOptimize.emit({
      nbVehicules,
      maxTimePerVehiculeMs: totalMinutes * MS_PER_MINUTE,
      livraisonIds,
    });
  }
}
