import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouteRibbon } from './route-ribbon';

describe('RouteRibbon', () => {
  async function render(stops: number) {
    await TestBed.configureTestingModule({ imports: [RouteRibbon], providers: [provideZonelessChangeDetection()] }).compileComponents();
    const fixture = TestBed.createComponent(RouteRibbon);
    fixture.componentRef.setInput('color', '#2563eb');
    fixture.componentRef.setInput('stops', stops);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('dessine un point par arrêt et décrit la tournée', async () => {
    const element = await render(3);
    expect(element.querySelectorAll('.dot').length).toBe(3);
    expect(element.getAttribute('aria-label')).toBe('Tournée de 3 arrêts');
  });

  it('résume les longues tournées', async () => {
    const element = await render(14);
    expect(element.querySelectorAll('.dot').length).toBe(6);
    expect(element.querySelector('.more')?.textContent).toBe('+8');
  });
});
