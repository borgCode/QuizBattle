import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArchivedNotificationItemComponent } from './archived-notification-item.component';

describe('ArchivedNotificationItemComponent', () => {
  let component: ArchivedNotificationItemComponent;
  let fixture: ComponentFixture<ArchivedNotificationItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArchivedNotificationItemComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ArchivedNotificationItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
