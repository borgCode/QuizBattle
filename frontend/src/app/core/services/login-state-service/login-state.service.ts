import { Injectable } from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {PlayerDto} from '../../../api/generated/models/player-dto';

@Injectable({
  providedIn: 'root'
})
export class LoginStateService {
  private isLoggedInSubject = new BehaviorSubject<boolean>(false);
  isLoggedIn$ = this.isLoggedInSubject.asObservable();
  private _loggedInUser: PlayerDto;

  constructor() {
    const storedUser = localStorage.getItem('loggedInUser');
    if (storedUser) {
      this._loggedInUser = JSON.parse(storedUser);
      this.isLoggedInSubject.next(true);
    }
  }


  updateLoginState(hasToken: boolean): void {
    this.isLoggedInSubject.next(hasToken);
  }


  get loggedInUser(): PlayerDto {
    return this._loggedInUser;
  }

  set loggedInUser(value: PlayerDto) {
    this._loggedInUser = value;

    localStorage.setItem('loggedInUser', JSON.stringify(value));
    this.isLoggedInSubject.next(true);
  }
  clearLoggedInUser() {
    this._loggedInUser = null;
    localStorage.removeItem('loggedInUser');
    this.isLoggedInSubject.next(false);
  }
}
