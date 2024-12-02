import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {User} from '../model/user';


@Injectable({
  providedIn: 'root',
})

export class UserService {
  private apiUrl = 'http://localhost:8080/user';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }
  getUserByName(username: string): Observable<User> {
    return this.http.get<User>(this.apiUrl + '/' + username);
  }
}
