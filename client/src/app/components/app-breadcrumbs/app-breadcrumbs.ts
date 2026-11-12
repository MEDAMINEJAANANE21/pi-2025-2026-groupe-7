import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TuiItem } from '@taiga-ui/cdk';
import { TuiLink } from '@taiga-ui/core';
import { TuiBreadcrumbs } from '@taiga-ui/kit';

export interface BreadcrumbItem {
  caption: string;
  routerLink?: string | string[];
}

@Component({
  selector: 'app-breadcrumbs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TuiBreadcrumbs, TuiItem, TuiLink],
  template: `
    <tui-breadcrumbs>
      @for (item of items(); track item.caption; let last = $last) {
        @if (!last && item.routerLink) {
          <a *tuiItem tuiLink [routerLink]="item.routerLink">{{ item.caption }}</a>
        } @else {
          <span *tuiItem>{{ item.caption }}</span>
        }
      }
    </tui-breadcrumbs>
  `,
})
export class AppBreadcrumbsComponent {
  readonly items = input<BreadcrumbItem[]>([]);
}
