import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../core/api/api-client';
import { JourneesApi } from '../../core/api/journees-api';
import { LivraisonsApi } from '../../core/api/livraisons-api';
import { JourneeResume } from '../../core/models/api';
import { formatMonth, monthGrid, today } from '../../core/util/dates';
import { formatInteger } from '../../core/util/format';
import { isRunning } from '../../core/util/labels';
import { Icon } from '../../shared/icon';
import { Notifications } from '../../shared/notifications';

const WEEKDAYS = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

@Component({
  selector: 'app-calendrier-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon],
  templateUrl: './calendrier-page.html',
  styleUrl: './calendrier-page.scss',
})
export class CalendrierPage {
  private readonly journeesApi = inject(JourneesApi);
  private readonly livraisonsApi = inject(LivraisonsApi);
  private readonly notifications = inject(Notifications);

  protected readonly weekdays = WEEKDAYS;
  protected readonly today = today();
  protected readonly formatInteger = formatInteger;

  private readonly cursor = signal(firstOfMonth(new Date()));
  protected readonly title = computed(() => formatMonth(this.cursor().getFullYear(), this.cursor().getMonth()));
  protected readonly weeks = computed(() => monthGrid(this.cursor().getFullYear(), this.cursor().getMonth()));

  protected readonly journees = signal<Map<string, JourneeResume>>(new Map());
  protected readonly aPlanifier = signal<number | null>(null);

  protected readonly monthJournees = computed(() => {
    const inMonth = new Set(this.weeks().flat().filter((d) => d.inMonth).map((d) => d.iso));
    return [...this.journees().values()].filter((j) => inMonth.has(j.date));
  });
  protected readonly planifiees = computed(() => this.monthJournees().filter((j) => j.nbSolutions > 0).length);
  protected readonly retenues = computed(() => this.monthJournees().filter((j) => j.solutionChoisieId !== null).length);

  constructor() {
    effect(() => void this.loadMonth(this.weeks()));
    void this.loadBacklog();
  }

  protected move(months: number): void {
    const current = this.cursor();
    this.cursor.set(new Date(current.getFullYear(), current.getMonth() + months, 1));
  }

  protected backToToday(): void {
    this.cursor.set(firstOfMonth(new Date()));
  }

  protected resume(day: string): JourneeResume | undefined {
    return this.journees().get(day);
  }

  protected running(resume: JourneeResume | undefined): boolean {
    return isRunning(resume?.statutGeneration);
  }

  private async loadMonth(weeks: { iso: string }[][]): Promise<void> {
    const first = weeks[0][0].iso;
    const last = weeks[weeks.length - 1][6].iso;
    try {
      const resumes = await this.journeesApi.calendrier(first, last);
      this.journees.set(new Map(resumes.map((r) => [r.date, r])));
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Impossible de charger le calendrier'));
    }
  }

  private async loadBacklog(): Promise<void> {
    try {
      this.aPlanifier.set((await this.livraisonsApi.list({ livree: false })).length);
    } catch {
      this.aPlanifier.set(null);
    }
  }
}

function firstOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}
