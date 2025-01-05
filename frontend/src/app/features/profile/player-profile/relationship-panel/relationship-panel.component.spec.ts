import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RelationshipPanelComponent } from './relationship-panel.component';

describe('FriendsPanelComponent', () => {
  let component: RelationshipPanelComponent;
  let fixture: ComponentFixture<RelationshipPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RelationshipPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RelationshipPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
