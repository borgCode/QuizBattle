import { TestBed } from '@angular/core/testing';

import { NotificationBellService } from './notification-bell.service';

describe('NotificationBellService', () => {
  let service: NotificationBellService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationBellService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
