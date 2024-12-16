import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StartStoryDialogComponent } from './start-story-dialog.component';

describe('StartStoryDialogComponent', () => {
  let component: StartStoryDialogComponent;
  let fixture: ComponentFixture<StartStoryDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StartStoryDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StartStoryDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
