import { Component } from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {AuthRequest} from '../../services/models/auth-request';
import {AuthenticationService} from '../../services/services/authentication.service';
import {AuthResponse} from '../../services/models/auth-response';
import {NgForOf, NgIf} from '@angular/common';
import {TokenService} from '../../services/token/token.service';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {UserDto} from '../../services/models/user-dto';

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    NgForOf,
    NgIf
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  authRequest: AuthRequest = {username: '', password: ''};
  errorMsg: Array<string> = [];

  constructor(
    private router: Router,
    private authService: AuthenticationService,
    private tokenService: TokenService,
    private loginStateService: LoginStateService,
  ) {
  }

  login() {
    this.errorMsg = [];
    this.authService.authenticate({
      body: this.authRequest
    }).subscribe({
      next: (res:AuthResponse) => {
        this.tokenService.token = res.token as string;
        this.loginStateService.loggedInUser = res.userDTO;
        this.router.navigate(['']);
      },
      error: (err) => {
        console.log(err);
        if (err.error.validationErrors) {
          this.errorMsg = err.error.validationErrors
        } else {
          this.errorMsg.push(err.error.errorMsg);
        }
      }
    })
  }

  register() {
    this.router.navigate(['register'])
  }
}
