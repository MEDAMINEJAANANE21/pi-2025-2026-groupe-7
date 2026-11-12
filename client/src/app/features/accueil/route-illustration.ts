import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouteRibbon } from '../../shared/route-ribbon';

interface Route {
  color: string;
  points: [number, number][];
}

const DEPOT: [number, number] = [214, 300];

// Trois tournées autour d'un dépôt, comme sur la carte d'une journée optimisée.
const ROUTES: Route[] = [
  { color: '#2563eb', points: [[262, 248], [318, 214], [372, 232], [404, 178], [356, 138]] },
  { color: '#0f9d6b', points: [[188, 236], [150, 186], [176, 130], [236, 104], [278, 142]] },
  { color: '#d97706', points: [[256, 336], [318, 354], [382, 330], [430, 372], [356, 402]] },
];

/** Illustration de la page d'accueil : une journée de tournées dessinée en SVG. */
@Component({
  selector: 'app-route-illustration',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouteRibbon],
  template: `
    <div class="frame">
      <svg viewBox="0 0 560 460" role="img" aria-label="Trois tournées de livraison partant d'un même dépôt">
        <rect width="560" height="460" fill="#eef3ea" />
        <path d="M0 330 C 90 300, 150 380, 250 360 S 420 300, 560 340 L 560 460 L 0 460 Z" fill="#e3ecdc" />
        <path d="M-10 70 C 120 120, 200 40, 330 80 S 480 150, 570 110" fill="none" stroke="#bcd7ee" stroke-width="14" stroke-linecap="round" />
        <path d="M40 0 L 120 460 M 0 210 L 560 250 M 300 0 L 260 460 M 460 0 L 520 460 M 0 400 L 560 380"
              fill="none" stroke="#ffffff" stroke-width="6" />
        <path d="M40 0 L 120 460 M 0 210 L 560 250 M 300 0 L 260 460 M 460 0 L 520 460 M 0 400 L 560 380"
              fill="none" stroke="#d9ddd3" stroke-width="1" />

        @for (route of routes; track route.color) {
          <polyline [attr.points]="path(route)" fill="none" [attr.stroke]="route.color" stroke-width="4"
                    stroke-linejoin="round" stroke-linecap="round" opacity="0.9" />
          @for (point of route.points; track $index) {
            <circle [attr.cx]="point[0]" [attr.cy]="point[1]" r="11" [attr.fill]="route.color" stroke="#ffffff" stroke-width="3" />
            <text [attr.x]="point[0]" [attr.y]="point[1] + 4" text-anchor="middle" class="order">{{ $index + 1 }}</text>
          }
        }
        <rect [attr.x]="depot[0] - 11" [attr.y]="depot[1] - 11" width="22" height="22" rx="5" fill="#171717" stroke="#ffffff" stroke-width="3" />
      </svg>

      <div class="card tile route">
        <div class="tile-head">
          <app-route-ribbon color="#2563eb" [stops]="5" />
          <span class="mono muted">Tournée 1</span>
        </div>
        <strong>Léa Martin · Renault Master</strong>
        <span class="secondary">5 arrêts · 38,4 km · 2 h 10</span>
      </div>

      <div class="card tile margin">
        <span class="secondary">Marge de la journée</span>
        <strong class="figure num">214,60 €</strong>
        <span class="muted">sur 396,00 € facturés</span>
      </div>
    </div>
  `,
  styles: `
    .frame {
      position: relative;
      border-radius: var(--r-panel);
      overflow: hidden;
      box-shadow: var(--shadow-float), 0 0 0 1px var(--line);
      background: #eef3ea;
    }

    svg {
      display: block;
      width: 100%;
      height: auto;
    }

    .order {
      fill: #ffffff;
      font-family: var(--mono);
      font-size: 11px;
      font-weight: 500;
    }

    .tile {
      position: absolute;
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 12px 16px;
      font-size: 13px;
      box-shadow: var(--shadow-float);

      strong {
        font-weight: 500;
      }
    }

    .tile-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .route {
      top: 20px;
      right: 20px;
      min-width: 236px;
    }

    .margin {
      left: 20px;
      bottom: 20px;

      .figure {
        font-size: 24px;
        line-height: 1.2;
        color: var(--accent);
      }
    }

    @media (max-width: 560px) {
      .tile {
        position: static;
        margin: 8px;
        box-shadow: none;
      }
    }
  `,
})
export class RouteIllustration {
  protected readonly routes = ROUTES;
  protected readonly depot = DEPOT;

  protected path(route: Route): string {
    return [DEPOT, ...route.points, DEPOT].map(([x, y]) => `${x},${y}`).join(' ');
  }
}
