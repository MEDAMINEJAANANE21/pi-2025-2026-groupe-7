import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SolutionEditor } from './solution-editor';

describe('SolutionEditor', () => {
  let component: SolutionEditor;
  let fixture: ComponentFixture<SolutionEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SolutionEditor]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SolutionEditor);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
