import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoutesMap } from './RoutesMap';

describe('RoutesMap', () => {
  let component: RoutesMap;
  let fixture: ComponentFixture<RoutesMap>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoutesMap]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoutesMap);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
