import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MultiplayerCategoryComponent } from './multiplayer-category.component';

describe('MultiplayerCategoryComponent', () => {
  let component: MultiplayerCategoryComponent;
  let fixture: ComponentFixture<MultiplayerCategoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MultiplayerCategoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MultiplayerCategoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
