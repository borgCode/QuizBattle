import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WhisperWindowComponent } from './whisper-window.component';

describe('WhispersPanelComponent', () => {
  let component: WhisperWindowComponent;
  let fixture: ComponentFixture<WhisperWindowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WhisperWindowComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WhisperWindowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
