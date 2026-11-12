import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { Icon } from './icon';

export const MAX_PHOTO_BYTES = 3 * 1024 * 1024;
const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp'];

/** Ce que le formulaire doit faire de la photo à l'enregistrement. */
export type PhotoChange = { kind: 'unchanged' } | { kind: 'replace'; file: File } | { kind: 'remove' };

/** Erreur lisible si le fichier ne convient pas, null sinon. */
export function photoError(file: File): string | null {
  if (!ACCEPTED.includes(file.type)) return 'Choisissez une image JPEG, PNG ou WebP';
  if (file.size > MAX_PHOTO_BYTES) return 'La photo dépasse 3 Mo';
  return null;
}

/**
 * Choix d'une photo avec aperçu immédiat. Le fichier n'est envoyé qu'à l'enregistrement
 * du formulaire, pour qu'« Annuler » ne laisse aucune trace.
 */
@Component({
  selector: 'app-photo-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  host: { '[class.round]': 'shape() === "round"' },
  template: `
    <button class="preview" type="button" (click)="input.click()" [attr.aria-label]="label()">
      @if (preview(); as url) {
        <img [src]="url" alt="" />
      } @else {
        <span class="empty"><app-icon name="camera" [size]="20" /></span>
      }
    </button>
    <div class="side">
      <span class="title">{{ label() }}</span>
      <span class="hint">JPEG, PNG ou WebP, 3 Mo au plus</span>
      <div class="actions">
        <button class="btn small" type="button" (click)="input.click()">{{ preview() ? 'Changer' : 'Choisir une photo' }}</button>
        @if (preview()) {
          <button class="btn small ghost" type="button" (click)="remove()">Retirer</button>
        }
      </div>
      @if (error(); as message) {
        <span class="error">{{ message }}</span>
      }
    </div>
    <input #input type="file" accept="image/jpeg,image/png,image/webp" hidden (change)="pick(input)" />
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .preview {
      flex-shrink: 0;
      width: 128px;
      height: 80px;
      padding: 0;
      border: 1px dashed var(--line-strong);
      border-radius: var(--r-card);
      background: var(--raised);
      overflow: hidden;
      cursor: pointer;
      transition: border-color var(--fast) var(--ease);

      &:hover {
        border-color: var(--accent);
      }

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }
    }

    :host(.round) .preview {
      width: 80px;
      border-radius: 50%;
    }

    .empty {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: var(--text-3);
    }

    .side {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .title {
      font-size: 13px;
      color: var(--text-2);
    }

    .hint {
      font-size: 12px;
      color: var(--text-3);
    }

    .actions {
      display: flex;
      gap: 4px;
      margin-top: 4px;
    }

    .error {
      font-size: 12px;
      color: var(--negative);
    }
  `,
})
export class PhotoPicker {
  readonly current = input<string | null>(null);
  readonly label = input('Photo');
  readonly shape = input<'round' | 'wide'>('wide');
  readonly changed = output<PhotoChange>();

  private readonly chosenUrl = signal<string | null>(null);
  private readonly removed = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly preview = computed(() => this.chosenUrl() ?? (this.removed() ? null : this.current()));

  constructor() {
    inject(DestroyRef).onDestroy(() => this.revoke());
  }

  /** Remet le sélecteur à zéro quand le formulaire s'ouvre sur une autre fiche. */
  reset(): void {
    this.revoke();
    this.chosenUrl.set(null);
    this.removed.set(false);
    this.error.set(null);
  }

  protected pick(element: HTMLInputElement): void {
    const file = element.files?.[0];
    element.value = '';
    if (!file) return;
    const problem = photoError(file);
    this.error.set(problem);
    if (problem) return;
    this.revoke();
    this.chosenUrl.set(URL.createObjectURL(file));
    this.removed.set(false);
    this.changed.emit({ kind: 'replace', file });
  }

  protected remove(): void {
    this.revoke();
    this.chosenUrl.set(null);
    this.removed.set(true);
    this.error.set(null);
    this.changed.emit({ kind: 'remove' });
  }

  private revoke(): void {
    const url = this.chosenUrl();
    if (url) URL.revokeObjectURL(url);
  }
}
