import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OptimizationControls } from './optimization-controls';

describe('OptimizationControls', () => {
  let component: OptimizationControls;
  let fixture: ComponentFixture<OptimizationControls>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OptimizationControls]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OptimizationControls);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
