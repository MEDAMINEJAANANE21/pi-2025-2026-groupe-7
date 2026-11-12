import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('affiche la navigation principale', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const links = [...fixture.nativeElement.querySelectorAll('nav a')].map((a: Element) => a.textContent?.trim());
    expect(links).toEqual(['Calendrier', "Aujourd'hui", 'Livraisons', 'Équipe']);
  });
});
