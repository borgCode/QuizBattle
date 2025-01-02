import { TestBed } from '@angular/core/testing';

import { AchievementNotificationService } from './achievement-notification.service';

describe('AchievementNotificationService', () => {
  let service: AchievementNotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AchievementNotificationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
