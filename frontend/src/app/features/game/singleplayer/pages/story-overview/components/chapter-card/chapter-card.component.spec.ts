import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChapterCardComponent } from './chapter-card.component';

describe('ChapterCardComponent', () => {
  let component: ChapterCardComponent;
  let fixture: ComponentFixture<ChapterCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChapterCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChapterCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
