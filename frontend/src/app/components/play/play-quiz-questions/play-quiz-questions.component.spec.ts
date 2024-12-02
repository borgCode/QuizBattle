import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlayQuizQuestionsComponent } from './play-quiz-questions.component';

describe('PlayComponent', () => {
  let component: PlayQuizQuestionsComponent;
  let fixture: ComponentFixture<PlayQuizQuestionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlayQuizQuestionsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlayQuizQuestionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
