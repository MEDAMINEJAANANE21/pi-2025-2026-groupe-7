import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { today } from '../core/util/dates';
import { ConfirmHost } from '../shared/confirm';
import { Icon, IconName } from '../shared/icon';
import { NotificationsHost } from '../shared/notifications';

interface NavItem {
  label: string;
  icon: IconName;
  link: string;
  exact: boolean;
}

@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, NotificationsHost, ConfirmHost],
  template: `
    @if (!landing()) {
    <aside class="sidebar no-print" [class.open]="menuOpen()">
      <a class="brand" routerLink="/" (click)="menuOpen.set(false)">
        <span class="mark" aria-hidden="true"><span></span><i></i><b></b></span>
        CyberCommandes
      </a>

      <nav aria-label="Navigation principale">
        @for (item of nav(); track item.link) {
          <a [routerLink]="item.link" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: item.exact }"
             (click)="menuOpen.set(false)">
            <app-icon [name]="item.icon" />
            {{ item.label }}
          </a>
        }
      </nav>

      <div class="footer">
        <a href="/swagger-ui/index.html" target="_blank" rel="noopener">
          <app-icon name="code" />
          Documentation de l'API
        </a>
        <p class="mono muted">Projet intégrateur · MIAGE Grenoble</p>
      </div>
    </aside>

    }

    @if (!landing()) {
    <button class="btn icon-only menu-toggle no-print" type="button" aria-label="Menu" (click)="menuOpen.set(!menuOpen())">
      <app-icon [name]="menuOpen() ? 'x' : 'route'" />
    </button>
    }

    <main>
      <router-outlet />
    </main>

    <app-notifications />
    <app-confirm />
  `,
  styleUrl: './app-shell.scss',
  host: { '[class.landing]': 'landing()' },
})
export class AppShell {
  protected readonly menuOpen = signal(false);

  // Le lien « Aujourd'hui » suit la date courante, même si l'onglet reste ouvert la nuit.
  private readonly router = inject(Router);
  private readonly url = toSignal(
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), map((e) => e.urlAfterRedirects)),
    { initialValue: this.router.url },
  );

  /** La page d'accueil se présente seule, sans la barre de navigation de l'application. */
  protected readonly landing = computed(() => (this.url() ?? '/').split(/[?#]/)[0] === '/');

  protected readonly nav = computed<NavItem[]>(() => {
    this.url();
    return [
      { label: 'Calendrier', icon: 'calendar', link: '/calendrier', exact: true },
      { label: "Aujourd'hui", icon: 'route', link: `/journees/${today()}`, exact: false },
      { label: 'Livraisons', icon: 'package', link: '/livraisons', exact: false },
      { label: 'Équipe', icon: 'users', link: '/equipe', exact: false },
    ];
  });
}
