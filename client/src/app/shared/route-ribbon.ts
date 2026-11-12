import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const MAX_DOTS = 6;

/**
 * Signature visuelle de l'application : une tournée dessinée comme un ruban
 * dépôt → arrêts → dépôt, dans la couleur du véhicule.
 */
@Component({
  selector: 'app-route-ribbon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'img', '[attr.aria-label]': 'label()' },
  template: `
    <span class="depot"></span>
    <span class="line" [style.background]="color()">
      @for (dot of dots(); track $index) {
        <span class="dot" [style.border-color]="color()"></span>
      }
    </span>
    <span class="depot"></span>
    @if (overflow() > 0) {
      <span class="more mono">+{{ overflow() }}</span>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 0;
      min-width: 0;
    }

    .depot {
      width: 6px;
      height: 6px;
      border-radius: 1px;
      background: var(--text);
      flex-shrink: 0;
    }

    .line {
      display: inline-flex;
      align-items: center;
      justify-content: space-evenly;
      height: 2px;
      width: var(--ribbon-width, 72px);
    }

    .dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      border: 2px solid;
      background: var(--surface);
    }

    .more {
      margin-left: 6px;
      color: var(--text-3);
    }
  `,
})
export class RouteRibbon {
  readonly color = input.required<string>();
  readonly stops = input.required<number>();

  protected readonly dots = computed(() => Array.from({ length: Math.min(this.stops(), MAX_DOTS) }));
  protected readonly overflow = computed(() => Math.max(0, this.stops() - MAX_DOTS));
  protected readonly label = computed(() => `Tournée de ${this.stops()} arrêt${this.stops() > 1 ? 's' : ''}`);
}
