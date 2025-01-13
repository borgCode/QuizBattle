import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {PlayerDto} from '../../../api/generated/models/player-dto';

@Injectable({
  providedIn: 'root'
})
export class LoginStateService {
  private isLoggedInSubject = new BehaviorSubject<boolean>(false);
  isLoggedIn$ = this.isLoggedInSubject.asObservable();
  private _loggedInUser: PlayerDto | null = null;

  private _loggedInUserSubject: BehaviorSubject<PlayerDto | null> = new BehaviorSubject<PlayerDto | null>(this._loggedInUser);
  public loggedInUser$: Observable<PlayerDto | null> = this._loggedInUserSubject.asObservable();

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
    this._loggedInUserSubject.next(this._loggedInUser);
  }

  clearLoggedInUser() {
    this._loggedInUser = null;
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
