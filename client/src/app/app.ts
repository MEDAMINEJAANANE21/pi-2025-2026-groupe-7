import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AppShell } from './layout/app-shell';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AppShell],
  template: `<app-shell />`,
})
export class App {}
