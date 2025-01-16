import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MultiplayerScoreWindowComponent } from './multiplayer-score-window.component';

describe('MultiplayerScoreWindowComponent', () => {
  let component: MultiplayerScoreWindowComponent;
  let fixture: ComponentFixture<MultiplayerScoreWindowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MultiplayerScoreWindowComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MultiplayerScoreWindowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
