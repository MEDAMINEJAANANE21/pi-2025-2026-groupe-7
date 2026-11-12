import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { solutionStats } from '../../testing/fixtures';
import { SolutionsTable } from './solutions-table';

describe('SolutionsTable', () => {
  let fixture: ComponentFixture<SolutionsTable>;

  const rows = () => [...fixture.nativeElement.querySelectorAll('tbody tr')] as HTMLElement[];
  const firstIds = () => rows().map((row) => row.querySelector('.mono')?.textContent?.trim());

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SolutionsTable],
      providers: [provideZonelessChangeDetection(), provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(SolutionsTable);
    fixture.componentRef.setInput('solutions', [
      solutionStats(1, { finances: { coutCarburant: 0, coutChauffeur: 0, coutTotal: 80, chiffreAffaires: 100, marge: 20 } }),
      solutionStats(2, { finances: { coutCarburant: 0, coutChauffeur: 0, coutTotal: 40, chiffreAffaires: 100, marge: 60 } }),
    ]);
    fixture.componentRef.setInput('choisieId', 1);
    await fixture.whenStable();
  });

  it('trie par coût croissant par défaut', () => {
    expect(firstIds()).toEqual(['n° 2', 'n° 1']);
  });

  it('inverse le tri au second clic', async () => {
    const coutHeader = [...fixture.nativeElement.querySelectorAll('th')].find((th: HTMLElement) => th.textContent?.includes('Coût'));
    coutHeader.click();
    await fixture.whenStable();
    expect(firstIds()).toEqual(['n° 1', 'n° 2']);
  });

  it('marque la solution retenue et propose de retenir les autres', async () => {
    const emitted: number[] = [];
    fixture.componentInstance.choose.subscribe((id) => emitted.push(id));

    expect(rows()[1].textContent).toContain('Retenue');
    (rows()[0].querySelector('button') as HTMLButtonElement).click();
    expect(emitted).toEqual([2]);
  });
});
