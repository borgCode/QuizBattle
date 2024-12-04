import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MultiplayerQuestionsComponent } from './multiplayer-questions.component';

describe('MultiplayerQuestionsComponent', () => {
  let component: MultiplayerQuestionsComponent;
  let fixture: ComponentFixture<MultiplayerQuestionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MultiplayerQuestionsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MultiplayerQuestionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
