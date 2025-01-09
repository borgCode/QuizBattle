import { TestBed } from '@angular/core/testing';

import { WhisperWindowService } from './whisper-window.service';

describe('WhisperWindowService', () => {
  let service: WhisperWindowService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WhisperWindowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
