import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatchFoundDialogComponent } from './match-found-dialog.component';

describe('MatchFoundDialogComponent', () => {
  let component: MatchFoundDialogComponent;
  let fixture: ComponentFixture<MatchFoundDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatchFoundDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MatchFoundDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
