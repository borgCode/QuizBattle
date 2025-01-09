import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WhispersContainerComponent } from './whispers-container.component';

describe('WhispersContainerComponent', () => {
  let component: WhispersContainerComponent;
  let fixture: ComponentFixture<WhispersContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WhispersContainerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WhispersContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
