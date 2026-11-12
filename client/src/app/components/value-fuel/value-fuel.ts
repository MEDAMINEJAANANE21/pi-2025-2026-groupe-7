import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-value-fuel',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './value-fuel.html',
})
export class ValueFuelComponent {
  @Input() ml: number | null | undefined;

  format(): string {
    if (this.ml == null) return '—';
    if (this.ml >= 1000) {
      return `${(this.ml / 1000).toFixed(2)} L`;
    }
    return `${Math.round(this.ml)} mL`;
  }
}

