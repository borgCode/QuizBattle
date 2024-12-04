import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MultiplayerComponent } from './multiplayer.component';

describe('MultiplayerComponent', () => {
  let component: MultiplayerComponent;
  let fixture: ComponentFixture<MultiplayerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MultiplayerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MultiplayerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
