import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { LivraisonResponse } from '../../models/backend';
import { formatIsoDuration } from '../../utils/duration';

@Component({
  selector: 'app-livraison-popover',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './livraison-popover.html',
  styleUrl: './livraison-popover.scss',
})
export class LivraisonPopover {
  public readonly livraison = input.required<LivraisonResponse>();
  public readonly pinned = input<boolean>(false);
  public readonly close = output<void>();

  protected readonly formatIsoDuration = formatIsoDuration;
}
