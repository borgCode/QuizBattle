import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArchivedNotificationsComponent } from './archived-notifications.component';

describe('ArchivedNotificationsComponent', () => {
  let component: ArchivedNotificationsComponent;
  let fixture: ComponentFixture<ArchivedNotificationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArchivedNotificationsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ArchivedNotificationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
