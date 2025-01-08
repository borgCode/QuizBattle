import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WhispersDropdownComponent } from './whispers-dropdown.component';

describe('ChatDropdownComponent', () => {
  let component: WhispersDropdownComponent;
  let fixture: ComponentFixture<WhispersDropdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WhispersDropdownComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WhispersDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
