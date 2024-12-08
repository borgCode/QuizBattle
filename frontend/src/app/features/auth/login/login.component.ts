import { Component } from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {NgForOf, NgIf} from '@angular/common';
import {TokenService} from '../../../core/services/token/token.service';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {AuthRequest} from '../../../api/generated/models/auth-request';
import {AuthenticationService} from '../../../api/generated/services/authentication.service';
import {AuthResponse} from '../../../api/generated/models/auth-response';

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
        this.loginStateService.loggedInUser = res.playerDTO;
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
