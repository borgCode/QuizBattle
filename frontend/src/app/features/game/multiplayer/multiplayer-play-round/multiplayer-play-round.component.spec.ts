import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MultiplayerPlayRoundComponent } from './multiplayer-play-round.component';

describe('MultiplayerPlayRoundComponent', () => {
  let component: MultiplayerPlayRoundComponent;
  let fixture: ComponentFixture<MultiplayerPlayRoundComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MultiplayerPlayRoundComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MultiplayerPlayRoundComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
