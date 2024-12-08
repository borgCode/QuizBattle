import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatchConfirmedDialogComponent } from './match-confirmed-dialog.component';

describe('MatchConfirmedDialogComponent', () => {
  let component: MatchConfirmedDialogComponent;
  let fixture: ComponentFixture<MatchConfirmedDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatchConfirmedDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MatchConfirmedDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
