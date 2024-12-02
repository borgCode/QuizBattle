import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlayQuizCategoryComponent } from './play-quiz-category.component';

describe('PlayQuizCategoryComponent', () => {
  let component: PlayQuizCategoryComponent;
  let fixture: ComponentFixture<PlayQuizCategoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlayQuizCategoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlayQuizCategoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
