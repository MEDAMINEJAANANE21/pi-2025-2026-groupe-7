import { ChangeDetectionStrategy, Component, inject, Injectable, signal } from '@angular/core';
import { Icon } from './icon';

export interface Notification {
  id: number;
  kind: 'success' | 'error';
  message: string;
}

const DISPLAY_MS = 4_000;

/** Messages courts en bas d'écran pour confirmer une action ou signaler une erreur. */
@Injectable({ providedIn: 'root' })
export class Notifications {
  private nextId = 1;
  readonly items = signal<Notification[]>([]);

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  dismiss(id: number): void {
    this.items.update((items) => items.filter((item) => item.id !== id));
  }

  private push(kind: Notification['kind'], message: string): void {
    const id = this.nextId++;
    this.items.update((items) => [...items.slice(-2), { id, kind, message }]);
    setTimeout(() => this.dismiss(id), DISPLAY_MS);
  }
}

@Component({
  selector: 'app-notifications',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  host: { 'aria-live': 'polite' },
  template: `
    @for (item of notifications.items(); track item.id) {
      <div class="toast" [class.error]="item.kind === 'error'">
        <app-icon [name]="item.kind === 'error' ? 'alert' : 'circle-check'" />
        <span>{{ item.message }}</span>
        <button class="btn ghost small icon-only" type="button" aria-label="Fermer" (click)="notifications.dismiss(item.id)">
          <app-icon name="x" />
        </button>
      </div>
    }
  `,
  styles: `
    :host {
      position: fixed;
      right: 24px;
      bottom: 24px;
      z-index: 2000;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: min(420px, calc(100vw - 48px));
    }

    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 8px 8px 16px;
      border-radius: var(--r-card);
      background: var(--text);
      color: #fafafa;
      box-shadow: var(--shadow-float);
      animation: rise var(--medium) var(--ease);

      app-icon {
        color: #4cc38a;
      }

      &.error app-icon {
        color: #f87171;
      }

      span {
        flex: 1;
      }

      .btn {
        color: #a1a1a6;
      }

      .btn:hover {
        background: #2b2b2e;
        color: #fafafa;
      }
    }
  `,
})
export class NotificationsHost {
  protected readonly notifications = inject(Notifications);
}
