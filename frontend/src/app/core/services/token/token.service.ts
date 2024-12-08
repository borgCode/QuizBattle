import { Injectable } from '@angular/core';
import {LoginStateService} from '../login-state-service/login-state.service';

@Injectable({
  providedIn: 'root'
})
export class TokenService {

  constructor(private loginStateService: LoginStateService) {
    this.loginStateService.updateLoginState(!!this.token);
  }

  set token(token: string) {
    localStorage.setItem('token', token);
    this.loginStateService.updateLoginState(!!token);
  }

  get token() {
    return localStorage.getItem('token') as string;
  }

  clearToken() {
    localStorage.removeItem('token');
    this.loginStateService.updateLoginState(false);
  }

  isTokenValid() {
    return !!localStorage.getItem('token');
  }
}
