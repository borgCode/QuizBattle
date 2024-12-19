import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoundResultsDialogComponent } from './round-results-dialog.component';

describe('RoundResultsDialogComponent', () => {
  let component: RoundResultsDialogComponent;
  let fixture: ComponentFixture<RoundResultsDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoundResultsDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoundResultsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
