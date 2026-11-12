import { Component, input, output, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TuiLabel } from '@taiga-ui/core';
import { TuiCheckbox } from '@taiga-ui/kit';
import { LivraisonResponse } from '../../models/backend';
import { formatIsoDuration } from '../../utils/duration';

export type SortField = 'id' | 'dateCommande';

@Component({
  selector: 'app-livraisons-checklist',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TuiLabel, TuiCheckbox],
  templateUrl: './livraisons-checklist.html',
  styleUrl: './livraisons-checklist.scss',
})
export class LivraisonsChecklistComponent {
  public readonly livraisons = input<LivraisonResponse[]>([]);
  public readonly hiddenIds = input<ReadonlySet<number>>(new Set());
  public readonly highlightedId = input<number | null>(null);
  public readonly toggleLivraison = output<number>();
  public readonly livraisonHovered = output<number | null>();

  protected readonly sortField = signal<SortField>('id');

  protected readonly sorted = computed(() => {
    const field = this.sortField();
    return [...this.livraisons()].sort((a, b) => {
      if (field === 'dateCommande') {
        const da = a.dateCommande ?? '';
        const db = b.dateCommande ?? '';
        return da < db ? -1 : da > db ? 1 : 0;
      }
      return a.id - b.id;
    });
  });

  protected isVisible(id: number): boolean {
    return !this.hiddenIds().has(id);
  }

  protected onToggle(id: number): void {
    this.toggleLivraison.emit(id);
  }

  protected setSortField(field: SortField): void {
    this.sortField.set(field);
  }

  protected readonly formatIsoDuration = formatIsoDuration;
}
