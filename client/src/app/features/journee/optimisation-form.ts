import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StatutGeneration } from '../../core/models/api';
import { Icon } from '../../shared/icon';

export interface OptimisationParams {
  nbVehicules: number;
  maxMinutes: number;
}

interface Step {
  label: string;
  state: 'done' | 'current' | 'todo';
}

const STEPS: { statut: StatutGeneration; label: string }[] = [
  { statut: 'GENERATION_MATRICE', label: 'Distances entre les adresses' },
  { statut: 'GENERATION_ROUTE', label: 'Itinéraires par stratégie' },
  { statut: 'TERMINE', label: 'Solutions prêtes à comparer' },
];

@Component({
  selector: 'app-optimisation-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  template: `
    <form class="form" (ngSubmit)="submit()">
      <div class="row">
        <label class="field">
          <span>Véhicules disponibles</span>
          <input class="input num" type="number" name="vehicules" min="1" max="20" required [(ngModel)]="nbVehicules" />
        </label>
        <label class="field">
          <span>Durée maximale par tournée</span>
          <div class="duration">
            <div class="input-suffix">
              <input class="input num" type="number" name="heures" min="0" max="24" [(ngModel)]="heures" />
              <em>h</em>
            </div>
            <div class="input-suffix">
              <input class="input num" type="number" name="minutes" min="0" max="59" step="5" [(ngModel)]="minutes" />
              <em>min</em>
            </div>
          </div>
        </label>
      </div>

      <p class="secondary">
        {{ livraisonsCount() }} livraison{{ livraisonsCount() > 1 ? 's' : '' }} à répartir.
        Chaque combinaison de découpage et de moteur donne une solution à comparer.
      </p>

      @if (statut(); as current) {
        <ol class="steps" aria-label="Avancement">
          @for (step of steps(); track step.label) {
            <li [class]="step.state">
              <span class="bullet">
                @if (step.state === 'done') { <app-icon name="check" [size]="12" /> }
              </span>
              {{ step.label }}
            </li>
          }
        </ol>
      }

      <button class="btn primary" type="submit" [disabled]="!canSubmit()">
        <app-icon name="play" />
        {{ statut() === 'TERMINE' ? 'Relancer une optimisation' : "Lancer l'optimisation" }}
      </button>
    </form>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .row {
      display: grid;
      grid-template-columns: 1fr;
      gap: 16px;
    }

    .duration {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }

    .steps {
      list-style: none;
      margin: 0;
      padding: 12px 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      border-radius: var(--r);
      background: var(--raised);

      li {
        display: flex;
        align-items: center;
        gap: 10px;
        color: var(--text-3);
      }

      .bullet {
        width: 16px;
        height: 16px;
        border-radius: 50%;
        border: 1.5px solid currentColor;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }

      .done {
        color: var(--text-2);

        .bullet {
          border-color: var(--positive);
          background: var(--positive);
          color: #ffffff;
        }
      }

      .current {
        color: var(--text);

        .bullet {
          border-color: var(--accent);
          box-shadow: inset 0 0 0 3px var(--surface);
          background: var(--accent);
        }
      }
    }
  `,
})
export class OptimisationForm {
  readonly livraisonsCount = input.required<number>();
  readonly statut = input<StatutGeneration | null>(null);
  readonly busy = input(false);
  readonly launch = output<OptimisationParams>();

  protected readonly nbVehicules = signal(3);
  protected readonly heures = signal(7);
  protected readonly minutes = signal(0);

  protected readonly running = computed(() => {
    const statut = this.statut();
    return !!statut && statut !== 'TERMINE';
  });

  protected readonly canSubmit = computed(() =>
    !this.busy() && !this.running() && this.livraisonsCount() > 0
    && this.nbVehicules() >= 1 && this.totalMinutes() > 0);

  protected readonly steps = computed<Step[]>(() => {
    const statut = this.statut();
    const reached = STEPS.findIndex((step) => step.statut === statut);
    return STEPS.map((step, index) => ({
      label: step.label,
      state: statut === 'TERMINE' || index < reached ? 'done' : index === reached ? 'current' : 'todo',
    }));
  });

  private readonly totalMinutes = computed(() => (Number(this.heures()) || 0) * 60 + (Number(this.minutes()) || 0));

  protected submit(): void {
    if (!this.canSubmit()) return;
    this.launch.emit({ nbVehicules: Number(this.nbVehicules()), maxMinutes: this.totalMinutes() });
  }
}
