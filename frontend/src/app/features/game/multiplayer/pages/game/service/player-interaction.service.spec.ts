import { TestBed } from '@angular/core/testing';

import { PlayerInteractionService } from './player-interaction.service';

describe('PlayerInteractionService', () => {
  let service: PlayerInteractionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PlayerInteractionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
