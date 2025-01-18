import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AchievementsPanelComponent } from './achievements-panel.component';

describe('AchievementsPanelComponent', () => {
  let component: AchievementsPanelComponent;
  let fixture: ComponentFixture<AchievementsPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AchievementsPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AchievementsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
