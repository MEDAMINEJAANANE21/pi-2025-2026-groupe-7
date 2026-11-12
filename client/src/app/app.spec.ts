import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';

@Component({ template: '' })
class Empty {}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([{ path: '', component: Empty }, { path: 'calendrier', component: Empty }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  async function renderAt(url: string): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl(url);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('affiche la navigation principale dans l’application', async () => {
    const element = await renderAt('/calendrier');
    const links = [...element.querySelectorAll('nav a')].map((a) => a.textContent?.trim());
    expect(links).toEqual(['Calendrier', "Aujourd'hui", 'Livraisons', 'Équipe']);
  });

  it('présente la page d’accueil sans la barre latérale', async () => {
    const element = await renderAt('/');
    expect(element.querySelector('aside.sidebar')).toBeNull();
  });
});
