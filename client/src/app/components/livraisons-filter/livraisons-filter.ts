import { Component, input, output, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TuiButton, TuiLabel, TuiTitle, TuiTextfield } from '@taiga-ui/core';
import { TuiCheckbox } from '@taiga-ui/kit';
import { LivraisonsChecklistComponent } from '../livraisons-checklist/livraisons-checklist';
import { LivraisonResponse } from '../../models/backend';

@Component({
  selector: 'app-livraisons-filter',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TuiButton, TuiLabel, TuiTitle, TuiTextfield, TuiCheckbox, LivraisonsChecklistComponent],
  templateUrl: './livraisons-filter.html',
  styleUrl: './livraisons-filter.scss',
})
export class LivraisonsFilterComponent {
  public readonly livraisons = input<LivraisonResponse[]>([]);
  public readonly hiddenIds = input<ReadonlySet<number>>(new Set());
  public readonly highlightedId = input<number | null>(null);
  public readonly filterChange = output<{ livree?: boolean; dateDebut?: string; dateFin?: string }>();
  public readonly toggleLivraison = output<number>();
  public readonly livraisonHovered = output<number | null>();

  protected filterLivreeOnly = true;
  protected filterDateDebut = '';
  protected filterDateFin = '';

  protected readonly visibleCount = computed(
    () => this.livraisons().filter(l => !this.hiddenIds().has(l.id)).length,
  );

  protected applyFilter(): void {
    const params: { livree?: boolean; dateDebut?: string; dateFin?: string } = {};
    if (this.filterLivreeOnly) params['livree'] = false;
    if (this.filterDateDebut) params['dateDebut'] = this.filterDateDebut;
    if (this.filterDateFin) params['dateFin'] = this.filterDateFin;
    this.filterChange.emit(params);
  }
}
