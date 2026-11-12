import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OptimizationConfig, OptimizationControls } from './optimization-controls';
import { LivraisonResponse } from '../../models/backend';

function livraison(id: number): LivraisonResponse {
  return {
    id,
    poids: 1,
    livree: false,
    adresse: {
      coordonnees: { lat: 45.19, lng: 5.72 },
      numero: '1',
      rue: 'rue des Alpes',
      codePostal: '38000',
      ville: 'Grenoble',
    },
  };
}

describe('OptimizationControls', () => {
  let component: OptimizationControls;
  let fixture: ComponentFixture<OptimizationControls>;
  let emitted: OptimizationConfig[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OptimizationControls],
    }).compileComponents();

    fixture = TestBed.createComponent(OptimizationControls);
    component = fixture.componentInstance;
    emitted = [];
    component.onOptimize.subscribe(config => emitted.push(config));
    await fixture.whenStable();
  });

  function submit(): void {
    const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
    form.dispatchEvent(new Event('submit'));
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('emits the visible livraison ids with the default configuration', async () => {
    fixture.componentRef.setInput('livraisons', [livraison(4), livraison(7)]);
    await fixture.whenStable();

    submit();

    expect(emitted).toEqual([
      { nbVehicules: 3, maxTimePerVehiculeMs: 8 * 60 * 60_000, livraisonIds: [4, 7] },
    ]);
  });

  it('does not emit and disables the button when no livraison is visible', async () => {
    fixture.componentRef.setInput('livraisons', []);
    await fixture.whenStable();

    submit();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(emitted).toEqual([]);
    expect(button.disabled).toBeTrue();
  });
});
