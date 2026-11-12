import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type Shape =
  | { t: 'path'; d: string }
  | { t: 'circle'; cx: number; cy: number; r: number }
  | { t: 'rect'; x: number; y: number; w: number; h: number; rx: number }
  | { t: 'ellipse'; cx: number; cy: number; rx: number; ry: number };

const p = (d: string): Shape => ({ t: 'path', d });
const c = (cx: number, cy: number, r: number): Shape => ({ t: 'circle', cx, cy, r });
const r = (x: number, y: number, w: number, h: number, rx: number): Shape => ({ t: 'rect', x, y, w, h, rx });

/**
 * Sous-ensemble des icônes Lucide (https://lucide.dev, licence ISC), recopiées ici
 * pour éviter une dépendance entière pour une vingtaine de pictogrammes.
 */
const ICONS = {
  calendar: [p('M8 2v4'), p('M16 2v4'), r(3, 4, 18, 18, 2), p('M3 10h18')],
  route: [c(6, 19, 3), p('M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15'), c(18, 5, 3)],
  package: [
    p('M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z'),
    p('M12 22V12'), p('m3.3 7 7.703 4.734a2 2 0 0 0 1.994 0L20.7 7'), p('m7.5 4.27 9 5.15'),
  ],
  users: [p('M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2'), c(9, 7, 4), p('M22 21v-2a4 4 0 0 0-3-3.87'), p('M16 3.13a4 4 0 0 1 0 7.75')],
  truck: [
    p('M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2'), p('M15 18H9'),
    p('M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14'), c(17, 18, 2), c(7, 18, 2),
  ],
  'chevron-left': [p('m15 18-6-6 6-6')],
  'chevron-right': [p('m9 18 6-6-6-6')],
  'arrow-left': [p('m12 19-7-7 7-7'), p('M19 12H5')],
  plus: [p('M5 12h14'), p('M12 5v14')],
  pencil: [
    p('M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z'),
    p('m15 5 4 4'),
  ],
  trash: [p('M3 6h18'), p('M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6'), p('M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2')],
  check: [p('M20 6 9 17l-5-5')],
  'circle-check': [c(12, 12, 10), p('m9 12 2 2 4-4')],
  x: [p('M18 6 6 18'), p('m6 6 12 12')],
  download: [p('M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'), p('m7 10 5 5 5-5'), p('M12 15V3')],
  printer: [
    p('M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2'),
    p('M6 9V3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v6'), r(6, 14, 12, 8, 1),
  ],
  refresh: [
    p('M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8'), p('M21 3v5h-5'),
    p('M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16'), p('M8 16H3v5'),
  ],
  search: [c(11, 11, 8), p('m21 21-4.3-4.3')],
  eye: [p('M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0'), c(12, 12, 3)],
  'eye-off': [
    p('M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49'),
    p('M14.084 14.158a3 3 0 0 1-4.242-4.242'),
    p('M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143'), p('m2 2 20 20'),
  ],
  grip: [c(9, 12, 1), c(9, 5, 1), c(9, 19, 1), c(15, 12, 1), c(15, 5, 1), c(15, 19, 1)],
  database: [{ t: 'ellipse', cx: 12, cy: 5, rx: 9, ry: 3 }, p('M3 5V19A9 3 0 0 0 21 19V5'), p('M3 12A9 3 0 0 0 21 12')],
  warehouse: [
    p('M18 21V10a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1v11'),
    p('M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8.35a2 2 0 0 1 1.26-1.86l8-3.2a2 2 0 0 1 1.48 0l8 3.2A2 2 0 0 1 22 8.35Z'),
    p('M6 13h12'), p('M6 17h12'),
  ],
  clock: [c(12, 12, 10), p('M12 6v6l4 2')],
  alert: [p('m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3'), p('M12 9v4'), p('M12 17h.01')],
  play: [p('M6 3 20 12 6 21Z')],
  code: [p('M10 9.5 8 12l2 2.5'), p('m14 9.5 2 2.5-2 2.5'), r(3, 3, 18, 18, 2)],
  shuffle: [
    p('m18 14 4 4-4 4'), p('m18 2 4 4-4 4'), p('M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.1-8.6c.7-1.1 2-1.7 3.3-1.7H22'),
    p('M2 6h1.4c1.3 0 2.5.6 3.3 1.7l.5.7'), p('M22 18h-5.9c-1.3 0-2.6-.7-3.3-1.8l-.5-.8'),
  ],
  pin: [p('M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0'), c(12, 10, 3)],
  undo: [p('M9 14 4 9l5-5'), p('M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5a5.5 5.5 0 0 1-5.5 5.5H11')],
  camera: [p('M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z'), c(12, 13, 3)],
  phone: [p('M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z')],
  fuel: [p('M3 22h12'), p('M4 9h10'), p('M14 22V4a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v18'), p('M14 13h2a2 2 0 0 1 2 2v2a2 2 0 0 0 2 2a2 2 0 0 0 2-2V9.83a2 2 0 0 0-.59-1.42L18 5')],
  weight: [c(12, 5, 3), p('M6.5 8a2 2 0 0 0-1.905 1.46L2.1 18.5A2 2 0 0 0 4 21h16a2 2 0 0 0 1.925-2.54L19.4 9.5A2 2 0 0 0 17.48 8Z')],
  euro: [p('M4 10h12'), p('M4 14h9'), p('M19 6a7.7 7.7 0 0 0-5.2-2A7.9 7.9 0 0 0 6 12c0 4.4 3.5 8 7.8 8 2 0 3.8-.8 5.2-2')],
  'arrow-right': [p('M5 12h14'), p('m12 5 7 7-7 7')],
} satisfies Record<string, Shape[]>;

export type IconName = keyof typeof ICONS;

@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'icon', 'aria-hidden': 'true' },
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      @for (shape of shapes(); track $index) {
        @switch (shape.t) {
          @case ('path') { <path [attr.d]="$any(shape).d" /> }
          @case ('circle') { <circle [attr.cx]="$any(shape).cx" [attr.cy]="$any(shape).cy" [attr.r]="$any(shape).r" /> }
          @case ('rect') {
            <rect [attr.x]="$any(shape).x" [attr.y]="$any(shape).y" [attr.width]="$any(shape).w"
                  [attr.height]="$any(shape).h" [attr.rx]="$any(shape).rx" />
          }
          @case ('ellipse') {
            <ellipse [attr.cx]="$any(shape).cx" [attr.cy]="$any(shape).cy" [attr.rx]="$any(shape).rx" [attr.ry]="$any(shape).ry" />
          }
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex-shrink: 0;
    }
  `,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly size = input(16);

  protected readonly shapes = computed<Shape[]>(() => ICONS[this.name()]);
}
