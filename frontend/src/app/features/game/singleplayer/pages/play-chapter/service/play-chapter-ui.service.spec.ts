import { TestBed } from '@angular/core/testing';

import { PlayChapterUiService } from './play-chapter-ui.service';

describe('PlayChapterUiService', () => {
  let service: PlayChapterUiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PlayChapterUiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
