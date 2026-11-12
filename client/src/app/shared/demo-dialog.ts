import { ChangeDetectionStrategy, Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { errorMessage } from '../core/api/api-client';
import { DonneesApi } from '../core/api/donnees-api';
import { Icon } from './icon';
import { Notifications } from './notifications';

/** Chargement du jeu d'essai, pour une démonstration sans saisie. */
@Component({
  selector: 'app-demo-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <dialog #dialog class="dialog">
      <header>
        <h2>Jeu d'essai</h2>
        <p>Une journée type pour découvrir l'application, prête à optimiser.</p>
      </header>
      <div class="dialog-body content">
        <ul class="contents">
          <li><app-icon name="warehouse" /><span><strong>1 dépôt</strong> à Seyssins</span></li>
          <li><app-icon name="package" /><span><strong>32 livraisons</strong> dans l'agglomération grenobloise, commandées ces derniers jours</span></li>
          <li><app-icon name="users" /><span><strong>6 chauffeurs</strong>, dont un indisponible</span></li>
          <li><app-icon name="truck" /><span><strong>5 véhicules</strong> avec charge utile et consommation</span></li>
        </ul>
        <p class="warning"><app-icon name="alert" [size]="14" /> Remplace toutes les livraisons, solutions, journées et l'équipe existantes.</p>
      </div>
      <footer>
        <button class="btn" type="button" (click)="close()">Annuler</button>
        <button class="btn primary" type="button" [disabled]="busy()" (click)="load()">
          {{ busy() ? 'Chargement…' : "Charger le jeu d'essai" }}
        </button>
      </footer>
    </dialog>
  `,
  styles: `
    .content {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .contents {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 12px;

      li {
        display: flex;
        align-items: flex-start;
        gap: 10px;
      }

      app-icon {
        margin-top: 2px;
        color: var(--accent);
      }

      strong {
        font-weight: 500;
      }
    }

    .warning {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      border-radius: var(--r);
      background: var(--finish-soft);
      color: #be185d;
      font-size: 13px;
    }
  `,
})
export class DemoDialog {
  readonly loaded = output<void>();

  private readonly api = inject(DonneesApi);
  private readonly notifications = inject(Notifications);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly busy = signal(false);

  open(): void {
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  protected async load(): Promise<void> {
    this.busy.set(true);
    try {
      const summary = await this.api.chargerDemo();
      this.notifications.success(
        `Jeu d'essai chargé : ${summary.livraisons} livraisons, ${summary.chauffeurs} chauffeurs, ${summary.vehicules} véhicules`);
      this.close();
      this.loaded.emit();
    } catch (error) {
      this.notifications.error(errorMessage(error, "Le jeu d'essai n'a pas pu être chargé"));
    } finally {
      this.busy.set(false);
    }
  }
}
