import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { TuiTextfield, TuiButton, TuiLabel, TuiTitle } from '@taiga-ui/core';
import { TuiInputNumber } from '@taiga-ui/kit';
import { TuiCardLarge } from '@taiga-ui/layout';
import { TuiSurface } from '@taiga-ui/core';
import { LivraisonResponse } from '../../models/backend';

export interface OptimizationConfig {
  nbVehicules: number;
  // milliseconds
  maxTimePerVehicule: number;
  livraisonIds?: number[];
}

@Component({
  selector: 'app-optimization-controls',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    TuiTextfield,
    TuiButton,
    TuiLabel,
    TuiInputNumber,
    TuiCardLarge,
    TuiSurface,
    TuiTitle
  ],
  templateUrl: './optimization-controls.html',
  styleUrl: './optimization-controls.scss',
})
export class OptimizationControls {
  public readonly livraisons = input<LivraisonResponse[]>([]);
  public readonly onOptimize = output<OptimizationConfig>();

  protected readonly nbVehiculesControl = new FormControl(3, { nonNullable: true });
  // New controls: hours and minutes for max time per vehicle
  protected readonly maxTimeHoursControl = new FormControl(8, { nonNullable: true });
  protected readonly maxTimeMinutesControl = new FormControl(0, { nonNullable: true });

  protected onSubmit(event?: Event): void {
    event?.preventDefault();
    const nbVehicules = this.nbVehiculesControl.value;
    const hours = this.maxTimeHoursControl.value ?? 0;
    const minutes = this.maxTimeMinutesControl.value ?? 0;

    // Total milliseconds
    const totalMinutes = Number(hours) * 60 + Number(minutes);
    const totalMs = totalMinutes > 0 ? totalMinutes * 60 * 1000 : 0;

    if (nbVehicules && totalMs > 0) {
      const livraisonIds = this.livraisons().map(l => l.id);
      this.onOptimize.emit({
        nbVehicules,
        maxTimePerVehicule: totalMs,
        livraisonIds: livraisonIds.length > 0 ? livraisonIds : undefined,
      });
    }
  }
}
