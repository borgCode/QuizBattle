import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlayerDropdownComponent } from './player-dropdown.component';

describe('PlayerDropdownComponent', () => {
  let component: PlayerDropdownComponent;
  let fixture: ComponentFixture<PlayerDropdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlayerDropdownComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlayerDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
