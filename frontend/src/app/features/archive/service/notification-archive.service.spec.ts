import { TestBed } from '@angular/core/testing';

import { NotificationArchiveService } from './notification-archive.service';

describe('NotificationArchiveService', () => {
  let service: NotificationArchiveService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationArchiveService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
