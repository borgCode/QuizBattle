import { TestBed } from '@angular/core/testing';

import { LoginStateService } from './login-state.service';

describe('LoginStateServiceService', () => {
  let service: LoginStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoginStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
