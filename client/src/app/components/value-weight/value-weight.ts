import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-value-weight',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './value-weight.html',
})
export class ValueWeightComponent {
  @Input() kg: number | null | undefined;

  format(): string {
    if (this.kg == null) return '—';
    if (this.kg >= 1000) {
      return `${(this.kg / 1000).toFixed(2)} t`;
    }
    return `${Math.round(this.kg * 100) / 100} kg`;
  }
}

