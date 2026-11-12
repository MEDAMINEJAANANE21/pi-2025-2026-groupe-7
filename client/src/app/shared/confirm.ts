import { ChangeDetectionStrategy, Component, effect, ElementRef, inject, Injectable, signal, viewChild } from '@angular/core';

interface ConfirmRequest {
  title: string;
  message: string;
  action: string;
  resolve: (confirmed: boolean) => void;
}

/** Confirmation avant une action irréversible, sans la boîte native du navigateur. */
@Injectable({ providedIn: 'root' })
export class Confirm {
  readonly pending = signal<ConfirmRequest | null>(null);

  ask(title: string, message: string, action = 'Supprimer'): Promise<boolean> {
    return new Promise((resolve) => this.pending.set({ title, message, action, resolve }));
  }

  answer(confirmed: boolean): void {
    this.pending()?.resolve(confirmed);
    this.pending.set(null);
  }
}

@Component({
  selector: 'app-confirm',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog #dialog class="dialog" (close)="confirm.answer(false)">
      @if (confirm.pending(); as request) {
        <header>
          <h2>{{ request.title }}</h2>
          <p>{{ request.message }}</p>
        </header>
        <footer>
          <button class="btn" type="button" (click)="confirm.answer(false)">Annuler</button>
          <button class="btn primary" type="button" (click)="confirm.answer(true)">{{ request.action }}</button>
        </footer>
      }
    </dialog>
  `,
  styles: `
    dialog {
      width: min(440px, calc(100vw - 32px));
    }

    footer {
      margin-top: 20px;
    }
  `,
})
export class ConfirmHost {
  protected readonly confirm = inject(Confirm);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const element = this.dialog().nativeElement;
      if (this.confirm.pending() && !element.open) element.showModal();
      if (!this.confirm.pending() && element.open) element.close();
    });
  }
}
