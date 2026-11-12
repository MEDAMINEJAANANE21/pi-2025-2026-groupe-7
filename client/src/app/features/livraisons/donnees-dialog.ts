import { ChangeDetectionStrategy, Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../../core/api/api-client';
import { DonneesApi } from '../../core/api/donnees-api';
import { Notifications } from '../../shared/notifications';

/** Jeux d'essai fournis avec le sujet : utiles pour une démonstration sans saisie. */
@Component({
  selector: 'app-donnees-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  template: `
    <dialog #dialog class="dialog">
      <header>
        <h2>Données d'essai</h2>
        <p>Chargez un jeu d'adresses autour de Grenoble, puis générez des livraisons sur ces adresses.</p>
      </header>
      <div class="dialog-body content">
        <section>
          <label class="field">
            <span>Jeu d'adresses</span>
            <select class="input" [ngModel]="dataset()" (ngModelChange)="dataset.set($event)">
              @for (name of datasets(); track name) {
                <option [value]="name">{{ label(name) }}</option>
              }
            </select>
            <span class="hint">Remplace toutes les livraisons, solutions et journées existantes</span>
          </label>
          <button class="btn" type="button" [disabled]="busy() || !dataset()" (click)="importer()">Importer le jeu</button>
        </section>
        <section>
          <label class="field">
            <span>Livraisons à générer</span>
            <input class="input num" type="number" min="1" max="500" [ngModel]="count()" (ngModelChange)="count.set($event)" />
            <span class="hint">Poids, temps de montage et dates de commande tirés au hasard</span>
          </label>
          <button class="btn" type="button" [disabled]="busy() || count() < 1" (click)="generer()">Générer</button>
        </section>
      </div>
      <footer>
        <button class="btn" type="button" (click)="close()">Fermer</button>
      </footer>
    </dialog>
  `,
  styles: `
    .content {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    section {
      display: grid;
      grid-template-columns: 1fr auto;
      align-items: end;
      gap: 12px;
    }

    section .btn {
      margin-bottom: 20px;
    }
  `,
})
export class DonneesDialog {
  readonly changed = output<void>();

  private readonly api = inject(DonneesApi);
  private readonly notifications = inject(Notifications);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly datasets = signal<string[]>([]);
  protected readonly dataset = signal('');
  protected readonly count = signal(30);
  protected readonly busy = signal(false);

  async open(): Promise<void> {
    this.dialog().nativeElement.showModal();
    if (this.datasets().length) return;
    try {
      const names = await this.api.jeuxDisponibles();
      this.datasets.set(names);
      this.dataset.set(names[0] ?? '');
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Liste des jeux indisponible'));
    }
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  /** DATASET_50_PAR_3_EXEMPLE_1 → « 50 adresses · exemple 1 » */
  protected label(name: string): string {
    const match = /DATASET_(\d+)_PAR_\d+_EXEMPLE_(\d+)/.exec(name);
    return match ? `${match[1]} adresses · exemple ${match[2]}` : name;
  }

  protected async importer(): Promise<void> {
    await this.run(async () => {
      const result = await this.api.importer(this.dataset());
      this.notifications.success(`${result.addressesImported} adresses importées`);
    });
  }

  protected async generer(): Promise<void> {
    await this.run(async () => {
      const result = await this.api.genererLivraisons(Number(this.count()));
      this.notifications.success(`${result.generated} livraisons générées`);
    });
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    try {
      await action();
      this.changed.emit();
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Opération impossible'));
    } finally {
      this.busy.set(false);
    }
  }
}
