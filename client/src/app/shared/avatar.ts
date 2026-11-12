import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Photo ronde d'une personne, ou ses initiales quand il n'y a pas de photo. */
@Component({
  selector: 'app-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[style.--size.px]': 'size()', '[class.muted]': 'inactive()' },
  template: `
    @if (photoUrl(); as url) {
      <img [src]="url" [alt]="name()" loading="lazy" />
    } @else {
      <span aria-hidden="true">{{ initials() }}</span>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      width: var(--size);
      height: var(--size);
      border-radius: 50%;
      overflow: hidden;
      background: var(--accent-soft);
      color: var(--accent-strong);
      font-weight: 500;
      font-size: calc(var(--size) * 0.36);
      box-shadow: inset 0 0 0 1px var(--line);
    }

    :host(.muted) {
      filter: grayscale(1);
      opacity: 0.6;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  `,
})
export class Avatar {
  readonly name = input.required<string>();
  readonly photoUrl = input<string | null>(null);
  readonly size = input(48);
  readonly inactive = input(false);

  protected readonly initials = computed(() =>
    this.name()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join(''),
  );
}
