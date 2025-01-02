import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AchievementPopupComponent } from './achievement-popup.component';

describe('AchievementPopupComponent', () => {
  let component: AchievementPopupComponent;
  let fixture: ComponentFixture<AchievementPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AchievementPopupComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AchievementPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
