import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ScoreBoxesComponent } from './score-boxes.component';

describe('ScoreBoxesComponent', () => {
  let component: ScoreBoxesComponent;
  let fixture: ComponentFixture<ScoreBoxesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScoreBoxesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ScoreBoxesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
