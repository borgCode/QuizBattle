import { TestBed } from '@angular/core/testing';

import { PlayChapterFacadeService } from './play-chapter-facade.service';

describe('PlayChapterFacadeService', () => {
  let service: PlayChapterFacadeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PlayChapterFacadeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
