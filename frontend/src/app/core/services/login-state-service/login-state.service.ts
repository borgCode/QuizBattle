import { Injectable } from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {PlayerDto} from '../../../api/generated/models/player-dto';

@Injectable({
  providedIn: 'root'
})
export class LoginStateService {
  private isLoggedInSubject = new BehaviorSubject<boolean>(false);
  isLoggedIn$ = this.isLoggedInSubject.asObservable();

  private playerSubject = new BehaviorSubject<PlayerDto | null>(null);
  player$ = this.playerSubject.asObservable();

  private _loggedInUser: PlayerDto;

  constructor() {
    const storedUser = localStorage.getItem('loggedInUser');
    if (storedUser) {
      this._loggedInUser = JSON.parse(storedUser);
      this.playerSubject.next(this._loggedInUser);
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
    this.playerSubject.next(value);
    localStorage.setItem('loggedInUser', JSON.stringify(value));
    this.isLoggedInSubject.next(true);
  }
  clearLoggedInUser() {
    this._loggedInUser = null;
    this.playerSubject.next(null);
    localStorage.removeItem('loggedInUser');
    this.isLoggedInSubject.next(false);
  }

  get userId() {
    const storedPlayer = localStorage.getItem('loggedInUser');
    if (storedPlayer) {
      return JSON.parse(storedPlayer).id;
    }
  }
}
