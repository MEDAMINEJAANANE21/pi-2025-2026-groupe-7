import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-value-duration',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './value-duration.html',
})
export class ValueDurationComponent {
  // accept ISO duration string or number (milliseconds)
  @Input() value: string | number | null | undefined;

  format(): string {
    if (this.value == null) return '—';
    if (typeof this.value === 'number') {
      // milliseconds
      try {
        const ms = this.value;
        const d = Math.floor(ms / 1000);
        const hours = Math.floor(d / 3600);
        const mins = Math.floor((d % 3600) / 60);
        const secs = d % 60;
        return `${hours}h${String(mins).padStart(2,'0')}m${String(secs).padStart(2,'0')}s`;
      } catch (e) {
        return String(this.value);
      }
    }
    // assume ISO-8601 Duration (PT#H#M#S)
    return this.formatIso(this.value as string);
  }

  private formatIso(iso: string): string {
    if (!iso) return '—';
    // naive parse: look for H M S
    try {
      const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);
      if (!match) return iso;
      const h = match[1] ? Number(match[1]) : 0;
      const m = match[2] ? Number(match[2]) : 0;
      const s = match[3] ? Number(match[3]) : 0;
      return `${h}h${String(m).padStart(2,'0')}m${String(s).padStart(2,'0')}s`;
    } catch (e) {
      return iso;
    }
  }
}


