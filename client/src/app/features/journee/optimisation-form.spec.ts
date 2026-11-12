import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OptimisationForm, OptimisationParams } from './optimisation-form';

describe('OptimisationForm', () => {
  let fixture: ComponentFixture<OptimisationForm>;
  let emitted: OptimisationParams[];

  const button = () => fixture.nativeElement.querySelector('button[type=submit]') as HTMLButtonElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OptimisationForm],
      providers: [provideZonelessChangeDetection()],
    }).compileComponents();
    fixture = TestBed.createComponent(OptimisationForm);
    fixture.componentRef.setInput('livraisonsCount', 12);
    emitted = [];
    fixture.componentInstance.launch.subscribe((params) => emitted.push(params));
    await fixture.whenStable();
  });

  it('envoie le nombre de véhicules et la durée en minutes', async () => {
    button().click();
    await fixture.whenStable();
    expect(emitted).toEqual([{ nbVehicules: 3, maxMinutes: 420 }]);
  });

  it('bloque le lancement sans livraison', async () => {
    fixture.componentRef.setInput('livraisonsCount', 0);
    await fixture.whenStable();
    expect(button().disabled).toBeTrue();
  });

  it('bloque le lancement et montre l’avancement pendant un calcul', async () => {
    fixture.componentRef.setInput('statut', 'GENERATION_ROUTE');
    await fixture.whenStable();

    expect(button().disabled).toBeTrue();
    const steps = [...fixture.nativeElement.querySelectorAll('.steps li')].map((li: Element) => li.className);
    expect(steps).toEqual(['done', 'current', 'todo']);
  });

  it('propose de relancer une fois terminé', async () => {
    fixture.componentRef.setInput('statut', 'TERMINE');
    await fixture.whenStable();
    expect(button().disabled).toBeFalse();
    expect(button().textContent).toContain('Relancer');
  });
});
