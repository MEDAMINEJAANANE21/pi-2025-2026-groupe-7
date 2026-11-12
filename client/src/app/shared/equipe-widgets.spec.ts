import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Avatar } from './avatar';
import { LicensePlate } from './license-plate';
import { MAX_PHOTO_BYTES, photoError } from './photo-picker';

describe('widgets de l’équipe', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }));

  it('affiche les initiales quand il n’y a pas de photo', async () => {
    const fixture = TestBed.createComponent(Avatar);
    fixture.componentRef.setInput('name', 'léa martin');
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent.trim()).toBe('LM');
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
  });

  it('affiche la photo quand elle existe', async () => {
    const fixture = TestBed.createComponent(Avatar);
    fixture.componentRef.setInput('name', 'Léa Martin');
    fixture.componentRef.setInput('photoUrl', '/api/chauffeurs/1/photo?v=3');
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('img').getAttribute('src')).toBe('/api/chauffeurs/1/photo?v=3');
  });

  it('écrit l’immatriculation sur la plaque', async () => {
    const fixture = TestBed.createComponent(LicensePlate);
    fixture.componentRef.setInput('value', 'GE-421-KT');
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.number').textContent).toBe('GE-421-KT');
    expect(fixture.nativeElement.getAttribute('aria-label')).toBe('Immatriculation GE-421-KT');
  });

  it('refuse les fichiers qui ne sont pas des photos ou trop lourds', () => {
    expect(photoError(new File(['x'], 'cv.pdf', { type: 'application/pdf' }))).toContain('JPEG, PNG ou WebP');
    expect(photoError(new File([new Uint8Array(MAX_PHOTO_BYTES + 1)], 'grande.png', { type: 'image/png' }))).toContain('3 Mo');
    expect(photoError(new File(['x'], 'portrait.webp', { type: 'image/webp' }))).toBeNull();
  });
});
