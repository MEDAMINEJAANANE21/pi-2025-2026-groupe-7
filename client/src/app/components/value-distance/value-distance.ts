import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-value-distance',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './value-distance.html',
})
export class ValueDistanceComponent {
  @Input() meters: number | null | undefined;

  format(): string {
    if (this.meters == null) return '—';
    if (this.meters >= 1000) {
      return `${(this.meters / 1000).toFixed(2)} km`;
    }
    return `${Math.round(this.meters)} m`;
  }
}

