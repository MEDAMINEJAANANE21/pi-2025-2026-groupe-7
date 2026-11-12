import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Plaque d'immatriculation au format français : bandeau bleu, lettres noires sur fond blanc. */
@Component({
  selector: 'app-license-plate',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'img', '[attr.aria-label]': '"Immatriculation " + value()' },
  template: `
    <span class="band" aria-hidden="true"><i></i>F</span>
    <span class="number">{{ value() }}</span>
    <span class="band end" aria-hidden="true"></span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: stretch;
      height: 26px;
      border-radius: 4px;
      overflow: hidden;
      background: #ffffff;
      box-shadow: 0 0 0 1px rgba(23, 23, 23, 0.35), 0 1px 2px rgba(0, 0, 0, 0.12);
    }

    .band {
      width: 14px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: flex-end;
      padding-bottom: 2px;
      background: #1e40af;
      color: #ffffff;
      font-size: 8px;
      font-weight: 600;
      line-height: 1;

      i {
        width: 6px;
        height: 6px;
        margin-bottom: auto;
        margin-top: 4px;
        border-radius: 50%;
        border: 1px dotted #facc15;
      }

      &.end {
        width: 8px;
      }
    }

    .number {
      display: flex;
      align-items: center;
      padding: 0 8px;
      font-family: var(--mono);
      font-size: 13px;
      font-weight: 500;
      letter-spacing: 0.04em;
      color: var(--text);
    }
  `,
})
export class LicensePlate {
  readonly value = input.required<string>();
}
